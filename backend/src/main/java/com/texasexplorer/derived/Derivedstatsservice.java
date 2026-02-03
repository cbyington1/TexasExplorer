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
 * Classification: stored in derived_stats table (needed on every map load).
 * Trends: computed on the fly (comparing any two years of raw city data).
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
    // Regional hubs with 100k+ that aren't satellites of something bigger.
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

    // ============================================================
    // PUBLIC API — CLASSIFICATION (stored)
    // ============================================================

    /**
     * Get classification data for a year — returns from DB if already computed,
     * otherwise computes and saves first.
     */
    public List<DerivedStats> getClassificationForYear(Integer year) {
        if (derivedStatsRepository.existsByYear(year)) {
            return derivedStatsRepository.findByYear(year);
        }
        return calculateAndSaveClassificationForYear(year);
    }

    /**
     * Get classification history for a single city across all years.
     */
    public List<DerivedStats> getCityClassificationHistory(String geoid) {
        return derivedStatsRepository.findByGeoidOrderByYearAsc(geoid);
    }

    /**
     * Calculate and persist classification for all cities in a given year.
     * Called after city data is loaded.
     */
    @Transactional
    public List<DerivedStats> calculateAndSaveClassificationForYear(Integer year) {
        log("Calculating classification for year " + year);

        List<City> cities = cityRepository.findByYearOrderByPopulationDesc(year);
        if (cities.isEmpty()) {
            log("No cities found for year " + year);
            return Collections.emptyList();
        }

        List<DerivedStats> results = new ArrayList<>();
        for (City city : cities) {
            if (city.getGeoid() == null) continue;

            DerivedStats ds = new DerivedStats(city.getGeoid(), year);
            computeClassification(ds, city);
            results.add(ds);
        }

        derivedStatsRepository.saveAll(results);
        log("Saved " + results.size() + " classification records for year " + year);

        return results;
    }

    /**
     * Recalculate classification for a year (delete existing first).
     */
    @Transactional
    public List<DerivedStats> recalculateClassificationForYear(Integer year) {
        derivedStatsRepository.deleteByYear(year);
        return calculateAndSaveClassificationForYear(year);
    }

    /**
     * Recalculate classification for all years.
     */
    @Transactional
    public void recalculateAllClassifications() {
        List<Integer> years = cityRepository.findAllYears();
        log("Recalculating classification for " + years.size() + " years");
        for (Integer year : years) {
            derivedStatsRepository.deleteByYear(year);
            calculateAndSaveClassificationForYear(year);
        }
    }

    // ============================================================
    // PUBLIC API — TRENDS (computed on the fly, never stored)
    // ============================================================

    /**
     * Compute trends for all cities comparing currentYear vs baseYear.
     * Returns a list of CityTrend DTOs with growth % for every metric.
     * Nothing is persisted — pure computation.
     */
    public List<CityTrend> computeTrends(Integer currentYear, Integer baseYear) {
        log("Computing trends: " + baseYear + " → " + currentYear);

        List<City> currentCities = cityRepository.findByYearOrderByPopulationDesc(currentYear);
        List<City> baseCities = cityRepository.findByYearOrderByPopulationDesc(baseYear);

        if (currentCities.isEmpty() || baseCities.isEmpty()) {
            log("Missing data — current: " + currentCities.size() + ", base: " + baseCities.size());
            return Collections.emptyList();
        }

        // Build base year lookup by geoid
        Map<String, City> baseMap = new HashMap<>();
        for (City c : baseCities) {
            if (c.getGeoid() != null) {
                baseMap.put(c.getGeoid(), c);
            }
        }

        // Also need classification data for urbanization index trends
        // Get stored classification for both years (compute if missing)
        Map<String, DerivedStats> currentClassMap = buildClassificationMap(currentYear);
        Map<String, DerivedStats> baseClassMap = buildClassificationMap(baseYear);

        List<CityTrend> trends = new ArrayList<>();
        for (City current : currentCities) {
            if (current.getGeoid() == null) continue;
            City base = baseMap.get(current.getGeoid());
            if (base == null) continue; // City didn't exist in base year

            CityTrend trend = new CityTrend(
                current.getGeoid(), current.getName(), currentYear, baseYear
            );

            // Overview
            trend.setPopulationGrowthPct(pctGrowthInt(base.getPopulation(), current.getPopulation()));
            trend.setMedianAgeGrowthPct(pctGrowthDouble(base.getMedianAge(), current.getMedianAge()));

            // Income
            trend.setMedianIncomeGrowthPct(pctGrowthInt(base.getMedianHouseholdIncome(), current.getMedianHouseholdIncome()));
            trend.setPerCapitaIncomeGrowthPct(pctGrowthInt(base.getPerCapitaIncome(), current.getPerCapitaIncome()));

            // Housing
            trend.setMedianHomeValueGrowthPct(pctGrowthInt(base.getMedianHomeValue(), current.getMedianHomeValue()));
            trend.setMedianRentGrowthPct(pctGrowthInt(base.getMedianRent(), current.getMedianRent()));
            trend.setHomeownershipRateGrowthPct(rateGrowthPct(
                base.getOwnerOccupied(), sumInts(base.getOwnerOccupied(), base.getRenterOccupied()),
                current.getOwnerOccupied(), sumInts(current.getOwnerOccupied(), current.getRenterOccupied())
            ));

            // Employment
            trend.setUnemploymentRateGrowthPct(rateGrowthPct(
                base.getUnemployed(), base.getLaborForce(),
                current.getUnemployed(), current.getLaborForce()
            ));
            trend.setLaborForceParticipationGrowthPct(rateGrowthPct(
                base.getLaborForce(), sumInts(base.getLaborForce(), base.getNotInLaborForce()),
                current.getLaborForce(), sumInts(current.getLaborForce(), current.getNotInLaborForce())
            ));
            trend.setWorkFromHomePctGrowthPct(rateGrowthPct(
                base.getWorkFromHome(), base.getEmployed(),
                current.getWorkFromHome(), current.getEmployed()
            ));

            // Sex
            trend.setMalePctGrowthPct(rateGrowthPct(
                base.getMalePopulation(), base.getPopulation(),
                current.getMalePopulation(), current.getPopulation()
            ));
            trend.setFemalePctGrowthPct(rateGrowthPct(
                base.getFemalePopulation(), base.getPopulation(),
                current.getFemalePopulation(), current.getPopulation()
            ));

            // Race
            trend.setWhitePctGrowthPct(rateGrowthPct(
                base.getWhitePopulation(), base.getPopulation(),
                current.getWhitePopulation(), current.getPopulation()
            ));
            trend.setBlackPctGrowthPct(rateGrowthPct(
                base.getBlackPopulation(), base.getPopulation(),
                current.getBlackPopulation(), current.getPopulation()
            ));
            trend.setAsianPctGrowthPct(rateGrowthPct(
                base.getAsianPopulation(), base.getPopulation(),
                current.getAsianPopulation(), current.getPopulation()
            ));
            trend.setNativeAmericanPctGrowthPct(rateGrowthPct(
                base.getNativeAmericanPopulation(), base.getPopulation(),
                current.getNativeAmericanPopulation(), current.getPopulation()
            ));
            trend.setPacificIslanderPctGrowthPct(rateGrowthPct(
                base.getPacificIslanderPopulation(), base.getPopulation(),
                current.getPacificIslanderPopulation(), current.getPopulation()
            ));
            trend.setTwoOrMoreRacesPctGrowthPct(rateGrowthPct(
                base.getTwoOrMoreRacesPopulation(), base.getPopulation(),
                current.getTwoOrMoreRacesPopulation(), current.getPopulation()
            ));
            trend.setOtherRacePctGrowthPct(rateGrowthPct(
                base.getOtherRacePopulation(), base.getPopulation(),
                current.getOtherRacePopulation(), current.getPopulation()
            ));

            // Ethnicity
            trend.setHispanicPctGrowthPct(rateGrowthPct(
                base.getHispanicPopulation(), base.getPopulation(),
                current.getHispanicPopulation(), current.getPopulation()
            ));

            // Classification — urbanization index growth
            DerivedStats currentClass = currentClassMap.get(current.getGeoid());
            DerivedStats baseClass = baseClassMap.get(current.getGeoid());
            if (currentClass != null && baseClass != null) {
                trend.setUrbanizationIndexGrowthPct(
                    pctGrowthDouble(baseClass.getUrbanizationIndex(), currentClass.getUrbanizationIndex())
                );
            }

            trends.add(trend);
        }

        log("Computed " + trends.size() + " trend records");
        return trends;
    }

    // ============================================================
    // CLASSIFICATION LOGIC
    // Ported from frontend map.component.ts getUrbanizationIndex()
    // ============================================================

    private void computeClassification(DerivedStats ds, City city) {
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

        // Store everything
        ds.setUrbanizationIndex(round2(index));
        ds.setPopulationScore(round2(popScore));
        ds.setDensityScore(round2(densityScore));
        ds.setProximityScore(round2(proxScore));
        ds.setIncomeRatioScore(round2(incomeScore));

        // Classification label
        if (index >= URBAN_THRESHOLD) {
            ds.setClassification("Urban");
        } else if (index >= SUBURBAN_THRESHOLD) {
            ds.setClassification("Suburban");
        } else {
            ds.setClassification("Rural");
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Build a geoid → DerivedStats map for a year, computing if not yet stored.
     */
    private Map<String, DerivedStats> buildClassificationMap(Integer year) {
        List<DerivedStats> list = getClassificationForYear(year);
        Map<String, DerivedStats> map = new HashMap<>();
        for (DerivedStats ds : list) {
            map.put(ds.getGeoid(), ds);
        }
        return map;
    }

    /**
     * Percentage growth for Integer values: ((new - old) / |old|) * 100.
     * Returns null if data missing or old is 0.
     */
    private Double pctGrowthInt(Integer oldVal, Integer newVal) {
        if (oldVal == null || newVal == null || oldVal == 0) return null;
        return round2(((double) (newVal - oldVal) / Math.abs(oldVal)) * 100.0);
    }

    /**
     * Percentage growth for Double values: ((new - old) / |old|) * 100.
     * Returns null if data missing or old is 0.
     */
    private Double pctGrowthDouble(Double oldVal, Double newVal) {
        if (oldVal == null || newVal == null || oldVal == 0.0) return null;
        return round2(((newVal - oldVal) / Math.abs(oldVal)) * 100.0);
    }

    /**
     * Rate growth in percentage: computes rate for both years, then % change of the rate.
     * e.g. unemployment 8% → 5% = ((5-8)/|8|)*100 = -37.5%
     * Returns null if data missing.
     */
    private Double rateGrowthPct(Integer oldNum, Integer oldDenom, Integer newNum, Integer newDenom) {
        if (oldNum == null || oldDenom == null || newNum == null || newDenom == null) return null;
        if (oldDenom == 0 || newDenom == 0) return null;
        double oldRate = (double) oldNum / oldDenom * 100.0;
        double newRate = (double) newNum / newDenom * 100.0;
        if (oldRate == 0.0) return null;
        return round2(((newRate - oldRate) / Math.abs(oldRate)) * 100.0);
    }

    /** Safe sum of two nullable Integers. Returns null if both null. */
    private Integer sumInts(Integer a, Integer b) {
        if (a == null && b == null) return null;
        return (a != null ? a : 0) + (b != null ? b : 0);
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