package com.texasexplorer.derived;

import com.texasexplorer.City;
import com.texasexplorer.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Computes all derived metrics from raw Census data.
 * This is the single home for any logic that transforms Census data
 * into new insights — classification, trends, indexes, scores, etc.
 * 
 * New derived computations should be added here over time.
 */
@Service
public class DerivedStatsService {

    @Autowired
    private DerivedStatsRepository derivedStatsRepository;

    @Autowired
    private CityRepository cityRepository;

    // ============================================================
    // TEXAS METRO REFERENCE CITIES (for proximity calculation)
    // ============================================================
    private static final double[][] TEXAS_METROS = {
        {29.7604, -95.3698},   // Houston
        {32.7767, -96.7970},   // Dallas
        {29.4241, -98.4936},   // San Antonio
        {30.2672, -97.7431},   // Austin
        {32.7555, -97.3308},   // Fort Worth
        {31.7619, -106.4850},  // El Paso
        {27.8006, -97.3964},   // Corpus Christi
        {33.5779, -101.8552},  // Lubbock
        {35.2220, -101.8313},  // Amarillo
        {26.2034, -98.2300},   // McAllen
        {25.9017, -97.4975},   // Brownsville
        {27.5036, -99.5076},   // Laredo
        {31.9973, -102.0779},  // Midland
    };

    // Classification thresholds
    private static final double URBAN_THRESHOLD = 73.0;
    private static final double SUBURBAN_THRESHOLD = 37.0;

    // Population floor: any city at or above this is automatically Urban
    private static final int URBAN_POPULATION_FLOOR = 250000;

    // Regional dominance: if a city is the largest within this radius (miles)
    // AND above the minimum population, it gets boosted to Urban
    private static final double REGIONAL_DOMINANCE_RADIUS_MILES = 60.0;
    private static final int REGIONAL_DOMINANCE_MIN_POPULATION = 75000;

    // ============================================================
    // PUBLIC API
    // ============================================================

    /**
     * Get derived stats for a year — returns from DB if already computed,
     * otherwise computes and saves first.
     */
    public List<DerivedStats> getStatsForYear(Integer year) {
        if (derivedStatsRepository.existsByYear(year)) {
            return derivedStatsRepository.findByYear(year);
        }
        return calculateAndSaveForYear(year);
    }

    /**
     * Get derived stats history for a single city across all years.
     */
    public List<DerivedStats> getCityHistory(String geoid) {
        return derivedStatsRepository.findByGeoidOrderByYearAsc(geoid);
    }

    /**
     * Calculate and persist derived stats for all cities in a given year.
     * Called after city data is loaded.
     */
    @Transactional
    public List<DerivedStats> calculateAndSaveForYear(Integer year) {
        log("Calculating derived stats for year " + year);

        List<City> cities = cityRepository.findByYearOrderByPopulationDesc(year);
        if (cities.isEmpty()) {
            log("No cities found for year " + year);
            return Collections.emptyList();
        }

        // Pre-compute regional dominance: for each city, determine if it's the
        // largest city within REGIONAL_DOMINANCE_RADIUS_MILES
        Set<String> regionallyDominant = computeRegionallyDominantCities(cities);
        log("Found " + regionallyDominant.size() + " regionally dominant cities");

        // Compute derived stats for each city
        List<DerivedStats> results = new ArrayList<>();
        for (City city : cities) {
            if (city.getGeoid() == null) continue;

            DerivedStats ds = new DerivedStats(city.getGeoid(), year);

            // Classification (now with population floor + regional dominance)
            boolean isDominant = regionallyDominant.contains(city.getGeoid());
            computeClassification(ds, city, isDominant);

            results.add(ds);
        }

        // Batch save
        derivedStatsRepository.saveAll(results);
        log("Saved " + results.size() + " derived stats for year " + year);

        return results;
    }

    /**
     * Recalculate for a year (delete existing first).
     */
    @Transactional
    public List<DerivedStats> recalculateForYear(Integer year) {
        derivedStatsRepository.deleteByYear(year);
        derivedStatsRepository.flush();
        return calculateAndSaveForYear(year);
    }

    /**
     * Recalculate all years.
     */
    @Transactional
    public void recalculateAll() {
        List<Integer> years = cityRepository.findAllYears();
        log("Recalculating derived stats for " + years.size() + " years");
        for (Integer year : years) {
            derivedStatsRepository.deleteByYear(year);
            derivedStatsRepository.flush();
            calculateAndSaveForYear(year);
        }
    }

    // ============================================================
    // REGIONAL DOMINANCE DETECTION
    // A city is "regionally dominant" if it's the largest city
    // within REGIONAL_DOMINANCE_RADIUS_MILES and has at least
    // REGIONAL_DOMINANCE_MIN_POPULATION people.
    // ============================================================

    private Set<String> computeRegionallyDominantCities(List<City> cities) {
        Set<String> dominant = new HashSet<>();

        // Filter to cities with valid coordinates and minimum population
        List<City> candidates = new ArrayList<>();
        for (City c : cities) {
            if (c.getGeoid() != null && c.getLatitude() != null && c.getLongitude() != null
                && c.getPopulation() != null && c.getPopulation() >= REGIONAL_DOMINANCE_MIN_POPULATION) {
                candidates.add(c);
            }
        }

        // For each candidate, check if any OTHER city within the radius is larger
        for (City candidate : candidates) {
            boolean isLargest = true;

            for (City other : candidates) {
                if (other.getGeoid().equals(candidate.getGeoid())) continue;

                double dist = haversineDistance(
                    candidate.getLatitude(), candidate.getLongitude(),
                    other.getLatitude(), other.getLongitude()
                );

                if (dist <= REGIONAL_DOMINANCE_RADIUS_MILES && other.getPopulation() > candidate.getPopulation()) {
                    isLargest = false;
                    break;
                }
            }

            if (isLargest) {
                dominant.add(candidate.getGeoid());
                log("  Regional hub: " + candidate.getName() + " (pop " + candidate.getPopulation() + ")");
            }
        }

        return dominant;
    }

    // ============================================================
    // CLASSIFICATION LOGIC
    // Computes urbanization index from weighted sub-scores, then
    // applies overrides:
    //   1. Population floor: 250k+ → Urban
    //   2. Regional dominance: largest city within 60mi, 75k+ → Urban
    // ============================================================

    private void computeClassification(DerivedStats ds, City city, boolean isRegionallyDominant) {
        if (city.getPopulation() == null || city.getPopulation() == 0) {
            ds.setClassification("Rural");
            ds.setUrbanizationIndex(0.0);
            return;
        }

        double population = city.getPopulation();

        // Population score (0-100): log scale
        // 1000→0, 50k→35, 200k→60, 500k→75, 2M+→100
        double popScore = population > 0
            ? clamp((Math.log10(population) - 3.0) / (6.3 - 3.0) * 100.0)
            : 0.0;

        // Density score (0-100): log scale
        // 100→0, 1000→30, 3000→60, 10000+→100
        double density = (city.getLandAreaSqMi() != null && city.getLandAreaSqMi() > 0)
            ? population / city.getLandAreaSqMi()
            : 0.0;
        double densityScore = density > 0
            ? clamp((Math.log10(density) - 2.0) / (4.0 - 2.0) * 100.0)
            : 0.0;

        // Metro proximity score (0-100)
        double proxScore = 0.0;
        if (city.getLatitude() != null && city.getLongitude() != null) {
            double minDist = Double.MAX_VALUE;
            for (double[] metro : TEXAS_METROS) {
                double d = haversineDistance(city.getLatitude(), city.getLongitude(), metro[0], metro[1]);
                if (d < minDist) minDist = d;
            }
            proxScore = clamp((1.0 - minDist / 80.0) * 100.0);
        }

        // Income ratio score: perCapita / householdIncome
        // Suburbs with dual-income families: ~0.35-0.42, Urban centers: ~0.45-0.65
        double incomeScore = 50.0; // neutral default
        if (city.getMedianHouseholdIncome() != null && city.getMedianHouseholdIncome() > 0
            && city.getPerCapitaIncome() != null && city.getPerCapitaIncome() > 0) {
            double ratio = (double) city.getPerCapitaIncome() / city.getMedianHouseholdIncome();
            incomeScore = clamp((ratio - 0.25) / (0.70 - 0.25) * 100.0);
        }

        // Weighted: population 50%, density 20%, income ratio 15%, proximity 15%
        double index = popScore * 0.50 + densityScore * 0.20 + incomeScore * 0.15 + proxScore * 0.15;

        // Store sub-scores
        ds.setPopulationScore(round2(popScore));
        ds.setDensityScore(round2(densityScore));
        ds.setProximityScore(round2(proxScore));
        ds.setIncomeRatioScore(round2(incomeScore));

        // ============================================================
        // CLASSIFICATION OVERRIDES
        // The raw index stays as-is (it's the honest composite score),
        // but the classification label gets promoted when overrides apply.
        // ============================================================

        String classification;

        // Override 1: Population floor — 250k+ is always Urban
        if (population >= URBAN_POPULATION_FLOOR) {
            classification = "Urban";
            // Bump index to at least the threshold so it's visually consistent
            index = Math.max(index, URBAN_THRESHOLD);

        // Override 2: Regional dominance — largest city in the area, 75k+
        } else if (isRegionallyDominant) {
            classification = "Urban";
            // Bump index to at least the threshold
            index = Math.max(index, URBAN_THRESHOLD);

        // Default: use thresholds on the raw index
        } else if (index >= URBAN_THRESHOLD) {
            classification = "Urban";
        } else if (index >= SUBURBAN_THRESHOLD) {
            classification = "Suburban";
        } else {
            classification = "Rural";
        }

        ds.setUrbanizationIndex(round2(index));
        ds.setClassification(classification);
    }

    // ============================================================
    // ON-THE-FLY TREND CALCULATIONS
    // Compares two years of city data, returns CityTrend DTOs.
    // Nothing is persisted — this is computed fresh each request.
    // ============================================================

    /**
     * Compute trend percentages for all cities between two years.
     * Returns a list of CityTrend DTOs with growth % for every metric.
     */
    public List<CityTrend> computeTrends(Integer currentYear, Integer baseYear) {
        log("Computing trends: " + baseYear + " → " + currentYear);

        List<City> currentCities = cityRepository.findByYearOrderByPopulationDesc(currentYear);
        List<City> baseCities = cityRepository.findByYearOrderByPopulationDesc(baseYear);

        if (currentCities.isEmpty() || baseCities.isEmpty()) {
            log("Missing data for trend calculation");
            return Collections.emptyList();
        }

        // Build base year lookup by geoid
        Map<String, City> baseMap = new HashMap<>();
        for (City c : baseCities) {
            if (c.getGeoid() != null) {
                baseMap.put(c.getGeoid(), c);
            }
        }

        // Also grab derived stats for both years (for urbanization index trend)
        Map<String, DerivedStats> currentDerived = new HashMap<>();
        Map<String, DerivedStats> baseDerived = new HashMap<>();
        for (DerivedStats ds : getStatsForYear(currentYear)) {
            currentDerived.put(ds.getGeoid(), ds);
        }
        for (DerivedStats ds : getStatsForYear(baseYear)) {
            baseDerived.put(ds.getGeoid(), ds);
        }

        List<CityTrend> trends = new ArrayList<>();
        for (City cur : currentCities) {
            if (cur.getGeoid() == null) continue;
            City base = baseMap.get(cur.getGeoid());
            if (base == null) continue;

            CityTrend t = new CityTrend(cur.getGeoid(), cur.getName(), currentYear, baseYear);

            // Overview
            t.setPopulationGrowthPct(pctGrowthInt(base.getPopulation(), cur.getPopulation()));
            t.setMedianAgeGrowthPct(pctGrowthDbl(base.getMedianAge(), cur.getMedianAge()));

            // Income
            t.setMedianIncomeGrowthPct(pctGrowthInt(base.getMedianHouseholdIncome(), cur.getMedianHouseholdIncome()));
            t.setPerCapitaIncomeGrowthPct(pctGrowthInt(base.getPerCapitaIncome(), cur.getPerCapitaIncome()));

            // Housing
            t.setMedianHomeValueGrowthPct(pctGrowthInt(base.getMedianHomeValue(), cur.getMedianHomeValue()));
            t.setMedianRentGrowthPct(pctGrowthInt(base.getMedianRent(), cur.getMedianRent()));
            t.setHomeownershipRateGrowthPct(ratePctChange(
                base.getOwnerOccupied(), base.getRenterOccupied(),
                cur.getOwnerOccupied(), cur.getRenterOccupied()));

            // Employment
            t.setUnemploymentRateGrowthPct(ratePctChange(
                base.getUnemployed(), base.getLaborForce(),
                cur.getUnemployed(), cur.getLaborForce()));
            t.setLaborForceParticipationGrowthPct(ratePctChange(
                base.getLaborForce(), base.getPopulation(),
                cur.getLaborForce(), cur.getPopulation()));
            t.setWorkFromHomePctGrowthPct(ratePctChange(
                base.getWorkFromHome(), base.getEmployed(),
                cur.getWorkFromHome(), cur.getEmployed()));

            // Sex
            t.setMalePctGrowthPct(ratePctChange(
                base.getMalePopulation(), base.getPopulation(),
                cur.getMalePopulation(), cur.getPopulation()));
            t.setFemalePctGrowthPct(ratePctChange(
                base.getFemalePopulation(), base.getPopulation(),
                cur.getFemalePopulation(), cur.getPopulation()));

            // Race
            t.setWhitePctGrowthPct(ratePctChange(
                base.getWhitePopulation(), base.getPopulation(),
                cur.getWhitePopulation(), cur.getPopulation()));
            t.setBlackPctGrowthPct(ratePctChange(
                base.getBlackPopulation(), base.getPopulation(),
                cur.getBlackPopulation(), cur.getPopulation()));
            t.setAsianPctGrowthPct(ratePctChange(
                base.getAsianPopulation(), base.getPopulation(),
                cur.getAsianPopulation(), cur.getPopulation()));
            t.setNativeAmericanPctGrowthPct(ratePctChange(
                base.getNativeAmericanPopulation(), base.getPopulation(),
                cur.getNativeAmericanPopulation(), cur.getPopulation()));
            t.setPacificIslanderPctGrowthPct(ratePctChange(
                base.getPacificIslanderPopulation(), base.getPopulation(),
                cur.getPacificIslanderPopulation(), cur.getPopulation()));
            t.setTwoOrMoreRacesPctGrowthPct(ratePctChange(
                base.getTwoOrMoreRacesPopulation(), base.getPopulation(),
                cur.getTwoOrMoreRacesPopulation(), cur.getPopulation()));
            t.setOtherRacePctGrowthPct(ratePctChange(
                base.getOtherRacePopulation(), base.getPopulation(),
                cur.getOtherRacePopulation(), cur.getPopulation()));

            // Ethnicity
            t.setHispanicPctGrowthPct(ratePctChange(
                base.getHispanicPopulation(), base.getPopulation(),
                cur.getHispanicPopulation(), cur.getPopulation()));

            // Classification — urbanization index change
            DerivedStats curDs = currentDerived.get(cur.getGeoid());
            DerivedStats baseDs = baseDerived.get(cur.getGeoid());
            if (curDs != null && baseDs != null
                && curDs.getUrbanizationIndex() != null && baseDs.getUrbanizationIndex() != null) {
                t.setUrbanizationIndexGrowthPct(pctGrowthDbl(
                    baseDs.getUrbanizationIndex(), curDs.getUrbanizationIndex()));
            }

            trends.add(t);
        }

        log("Computed " + trends.size() + " city trends");
        return trends;
    }

    // ============================================================
    // MATH HELPERS
    // ============================================================

    /** Percentage growth for Integer values: ((new - old) / |old|) * 100 */
    private Double pctGrowthInt(Integer oldVal, Integer newVal) {
        if (oldVal == null || newVal == null || oldVal == 0) return null;
        return round2(((double) (newVal - oldVal) / Math.abs(oldVal)) * 100.0);
    }

    /** Percentage growth for Double values: ((new - old) / |old|) * 100 */
    private Double pctGrowthDbl(Double oldVal, Double newVal) {
        if (oldVal == null || newVal == null || oldVal == 0.0) return null;
        return round2(((newVal - oldVal) / Math.abs(oldVal)) * 100.0);
    }

    /**
     * Rate percentage point change.
     * Computes (numerator/denominator) for both years, returns the difference.
     * For homeownership: pass (owner, owner+renter) as num/denom.
     * For unemployment: pass (unemployed, laborForce).
     */
    private Double ratePctChange(Integer oldNum, Integer oldDenom, Integer newNum, Integer newDenom) {
        if (oldNum == null || oldDenom == null || newNum == null || newDenom == null) return null;
        if (oldDenom == 0 || newDenom == 0) return null;
        double oldRate = (double) oldNum / oldDenom * 100.0;
        double newRate = (double) newNum / newDenom * 100.0;
        return round2(newRate - oldRate);
    }

    /** Haversine distance in miles between two lat/lng points. */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 3959.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    /** Clamp value to 0-100 range. */
    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    /** Round to 2 decimal places. */
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void log(String message) {
        System.out.println("[DerivedStatsService] " + message);
    }
}