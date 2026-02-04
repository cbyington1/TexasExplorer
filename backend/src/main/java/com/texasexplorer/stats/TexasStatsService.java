package com.texasexplorer.stats;

import com.texasexplorer.City;
import com.texasexplorer.CityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TexasStatsService {

    @Autowired
    private CityService cityService;
    
    @Autowired
    private TexasStatsRepository texasStatsRepository;

    /**
     * Get stats for a specific year - checks database first, calculates if missing
     */
    public TexasStats getStatsForYear(Integer year) {
        // Check if already calculated and stored
        TexasStats existing = texasStatsRepository.findByYear(year);
        if (existing != null) {
            System.out.println("Found existing Texas stats for year " + year);
            return existing;
        }
        
        // Calculate and save
        System.out.println("Calculating Texas stats for year " + year);
        return calculateAndSaveStatsForYear(year);
    }

    /**
     * Get all stats - checks database first, calculates missing years
     */
    public List<TexasStats> getAllStats() {
        List<Integer> availableYears = cityService.getAvailableYears();
        
        // Calculate stats for any missing years
        for (Integer year : availableYears) {
            if (!texasStatsRepository.existsByYear(year)) {
                System.out.println("Calculating missing Texas stats for year " + year);
                calculateAndSaveStatsForYear(year);
            }
        }
        
        return texasStatsRepository.findAllByOrderByYearAsc();
    }

    /**
     * Calculate stats for a specific year and save to database
     */
    public TexasStats calculateAndSaveStatsForYear(Integer year) {
        List<City> cities = cityService.getCitiesForYear(year);
        
        if (cities.isEmpty()) {
            System.out.println("No cities found for year " + year);
            return null;
        }

        TexasStats stats = new TexasStats(year);
        
        // Variables for weighted averages
        long totalPopulation = 0;
        double weightedMedianAge = 0;
        double weightedMedianIncome = 0;
        double weightedPerCapitaIncome = 0;
        double weightedMedianHomeValue = 0;
        double weightedMedianRent = 0;
        double weightedMeanCommute = 0;
        
        // Counters for weighted averages (to handle nulls)
        long popForAge = 0;
        long popForIncome = 0;
        long popForPerCapita = 0;
        long popForHomeValue = 0;
        long popForRent = 0;
        long popForCommute = 0;
        
        // Sum all the counts
        long totalMale = 0, totalFemale = 0;
        long totalWhite = 0, totalBlack = 0, totalNativeAm = 0;
        long totalAsian = 0, totalPacific = 0, totalOther = 0, totalTwoPlus = 0;
        long totalHispanic = 0;
        long totalAgeU5 = 0, totalAgeU18 = 0, totalAge1824 = 0;
        long totalAge2544 = 0, totalAge4564 = 0, totalAge65 = 0;
        long totalForeignBorn = 0, totalNaturalized = 0, totalNonCitizen = 0;
        long totalEnglish = 0, totalSpanish = 0;
        long totalVeterans = 0;
        long totalIncU25 = 0, totalInc2550 = 0, totalInc50100 = 0;
        long totalInc100200 = 0, totalInc200 = 0;
        long totalPoverty = 0, totalPovKids = 0, totalSnap = 0;
        long totalEmployed = 0, totalUnemployed = 0, totalLaborForce = 0, totalNotInLabor = 0;
        long totalEduNoHS = 0, totalEduHS = 0, totalEduCollege = 0;
        long totalEduBach = 0, totalEduMast = 0, totalEduDoc = 0;
        long totalOwner = 0, totalRenter = 0, totalVacant = 0;
        long totalWFH = 0, totalDrive = 0, totalTransit = 0;

        // Aggregate all cities
        for (City city : cities) {
            int pop = city.getPopulation() != null ? city.getPopulation() : 0;
            totalPopulation += pop;
            
            // Weighted averages for medians - FIXED: Cast to long to prevent overflow
            if (city.getMedianAge() != null) {
                weightedMedianAge += city.getMedianAge() * pop;  // Double, no cast needed
                popForAge += pop;
            }
            if (city.getMedianHouseholdIncome() != null) {
                weightedMedianIncome += ((long)city.getMedianHouseholdIncome()) * pop;
                popForIncome += pop;
            }
            if (city.getPerCapitaIncome() != null) {
                weightedPerCapitaIncome += ((long)city.getPerCapitaIncome()) * pop;
                popForPerCapita += pop;
            }
            if (city.getMedianHomeValue() != null) {
                weightedMedianHomeValue += ((long)city.getMedianHomeValue()) * pop;
                popForHomeValue += pop;
            }
            if (city.getMedianRent() != null) {
                weightedMedianRent += ((long)city.getMedianRent()) * pop;
                popForRent += pop;
            }
            if (city.getMeanCommuteMinutes() != null) {
                weightedMeanCommute += city.getMeanCommuteMinutes() * pop;  // Double, no cast needed
                popForCommute += pop;
            }
            
            // Sum all counts
            totalMale += city.getMalePopulation() != null ? city.getMalePopulation() : 0;
            totalFemale += city.getFemalePopulation() != null ? city.getFemalePopulation() : 0;
            
            totalWhite += city.getWhitePopulation() != null ? city.getWhitePopulation() : 0;
            totalBlack += city.getBlackPopulation() != null ? city.getBlackPopulation() : 0;
            totalNativeAm += city.getNativeAmericanPopulation() != null ? city.getNativeAmericanPopulation() : 0;
            totalAsian += city.getAsianPopulation() != null ? city.getAsianPopulation() : 0;
            totalPacific += city.getPacificIslanderPopulation() != null ? city.getPacificIslanderPopulation() : 0;
            totalOther += city.getOtherRacePopulation() != null ? city.getOtherRacePopulation() : 0;
            totalTwoPlus += city.getTwoOrMoreRacesPopulation() != null ? city.getTwoOrMoreRacesPopulation() : 0;
            totalHispanic += city.getHispanicPopulation() != null ? city.getHispanicPopulation() : 0;
            
            totalAgeU5 += city.getAgeUnder5() != null ? city.getAgeUnder5() : 0;
            totalAgeU18 += city.getAgeUnder18() != null ? city.getAgeUnder18() : 0;
            totalAge1824 += city.getAge18to24() != null ? city.getAge18to24() : 0;
            totalAge2544 += city.getAge25to44() != null ? city.getAge25to44() : 0;
            totalAge4564 += city.getAge45to64() != null ? city.getAge45to64() : 0;
            totalAge65 += city.getAge65plus() != null ? city.getAge65plus() : 0;
            
            totalForeignBorn += city.getForeignBorn() != null ? city.getForeignBorn() : 0;
            totalNaturalized += city.getNaturalizedCitizen() != null ? city.getNaturalizedCitizen() : 0;
            totalNonCitizen += city.getNonCitizen() != null ? city.getNonCitizen() : 0;
            
            totalEnglish += city.getSpeakOnlyEnglish() != null ? city.getSpeakOnlyEnglish() : 0;
            totalSpanish += city.getSpeakSpanish() != null ? city.getSpeakSpanish() : 0;
            
            totalVeterans += city.getVeterans() != null ? city.getVeterans() : 0;
            
            totalIncU25 += city.getIncomeUnder25k() != null ? city.getIncomeUnder25k() : 0;
            totalInc2550 += city.getIncome25kTo50k() != null ? city.getIncome25kTo50k() : 0;
            totalInc50100 += city.getIncome50kTo100k() != null ? city.getIncome50kTo100k() : 0;
            totalInc100200 += city.getIncome100kTo200k() != null ? city.getIncome100kTo200k() : 0;
            totalInc200 += city.getIncome200kPlus() != null ? city.getIncome200kPlus() : 0;
            
            totalPoverty += city.getPovertyTotal() != null ? city.getPovertyTotal() : 0;
            totalPovKids += city.getPovertyChildren() != null ? city.getPovertyChildren() : 0;
            totalSnap += city.getSnapHouseholds() != null ? city.getSnapHouseholds() : 0;
            
            totalEmployed += city.getEmployed() != null ? city.getEmployed() : 0;
            totalUnemployed += city.getUnemployed() != null ? city.getUnemployed() : 0;
            totalLaborForce += city.getLaborForce() != null ? city.getLaborForce() : 0;
            totalNotInLabor += city.getNotInLaborForce() != null ? city.getNotInLaborForce() : 0;
            
            totalEduNoHS += city.getEduNoHighSchool() != null ? city.getEduNoHighSchool() : 0;
            totalEduHS += city.getEduHighSchoolOnly() != null ? city.getEduHighSchoolOnly() : 0;
            totalEduCollege += city.getEduSomeCollege() != null ? city.getEduSomeCollege() : 0;
            totalEduBach += city.getEduBachelors() != null ? city.getEduBachelors() : 0;
            totalEduMast += city.getEduMasters() != null ? city.getEduMasters() : 0;
            totalEduDoc += city.getEduDoctorate() != null ? city.getEduDoctorate() : 0;
            
            totalOwner += city.getOwnerOccupied() != null ? city.getOwnerOccupied() : 0;
            totalRenter += city.getRenterOccupied() != null ? city.getRenterOccupied() : 0;
            totalVacant += city.getVacantUnits() != null ? city.getVacantUnits() : 0;
            
            totalWFH += city.getWorkFromHome() != null ? city.getWorkFromHome() : 0;
            totalDrive += city.getDriveAlone() != null ? city.getDriveAlone() : 0;
            totalTransit += city.getPublicTransit() != null ? city.getPublicTransit() : 0;
        }
        
        // Set all the totals
        stats.setTotalPopulation(totalPopulation);
        stats.setTotalMale(totalMale);
        stats.setTotalFemale(totalFemale);
        
        // Calculate weighted averages
        stats.setMedianAge(popForAge > 0 ? weightedMedianAge / popForAge : null);
        stats.setMedianHouseholdIncome(popForIncome > 0 ? weightedMedianIncome / popForIncome : null);
        stats.setPerCapitaIncome(popForPerCapita > 0 ? weightedPerCapitaIncome / popForPerCapita : null);
        stats.setMedianHomeValue(popForHomeValue > 0 ? weightedMedianHomeValue / popForHomeValue : null);
        stats.setMedianRent(popForRent > 0 ? weightedMedianRent / popForRent : null);
        stats.setMeanCommuteMinutes(popForCommute > 0 ? weightedMeanCommute / popForCommute : null);
        
        // Set all count totals
        stats.setWhitePopulation(totalWhite);
        stats.setBlackPopulation(totalBlack);
        stats.setNativeAmericanPopulation(totalNativeAm);
        stats.setAsianPopulation(totalAsian);
        stats.setPacificIslanderPopulation(totalPacific);
        stats.setOtherRacePopulation(totalOther);
        stats.setTwoOrMoreRacesPopulation(totalTwoPlus);
        stats.setHispanicPopulation(totalHispanic);
        
        stats.setAgeUnder5(totalAgeU5);
        stats.setAgeUnder18(totalAgeU18);
        stats.setAge18to24(totalAge1824);
        stats.setAge25to44(totalAge2544);
        stats.setAge45to64(totalAge4564);
        stats.setAge65plus(totalAge65);
        
        stats.setForeignBorn(totalForeignBorn);
        stats.setNaturalizedCitizen(totalNaturalized);
        stats.setNonCitizen(totalNonCitizen);
        
        stats.setSpeakOnlyEnglish(totalEnglish);
        stats.setSpeakSpanish(totalSpanish);
        
        stats.setVeterans(totalVeterans);
        
        stats.setIncomeUnder25k(totalIncU25);
        stats.setIncome25kTo50k(totalInc2550);
        stats.setIncome50kTo100k(totalInc50100);
        stats.setIncome100kTo200k(totalInc100200);
        stats.setIncome200kPlus(totalInc200);
        
        stats.setPovertyTotal(totalPoverty);
        stats.setPovertyChildren(totalPovKids);
        stats.setSnapHouseholds(totalSnap);
        
        stats.setEmployed(totalEmployed);
        stats.setUnemployed(totalUnemployed);
        stats.setLaborForce(totalLaborForce);
        stats.setNotInLaborForce(totalNotInLabor);
        
        stats.setEduNoHighSchool(totalEduNoHS);
        stats.setEduHighSchoolOnly(totalEduHS);
        stats.setEduSomeCollege(totalEduCollege);
        stats.setEduBachelors(totalEduBach);
        stats.setEduMasters(totalEduMast);
        stats.setEduDoctorate(totalEduDoc);
        
        stats.setOwnerOccupied(totalOwner);
        stats.setRenterOccupied(totalRenter);
        stats.setVacantUnits(totalVacant);
        
        stats.setWorkFromHome(totalWFH);
        stats.setDriveAlone(totalDrive);
        stats.setPublicTransit(totalTransit);

        // Diversity Index — Simpson's on statewide race totals
        // Same logic as city-level: normalize to known total, require 90% coverage
        long[] raceCounts = { totalWhite, totalBlack, totalAsian, totalNativeAm,
                              totalPacific, totalTwoPlus, totalOther, totalHispanic };
        double knownRaceTotal = 0;
        int raceGroups = 0;
        for (long c : raceCounts) {
            if (c > 0) { knownRaceTotal += c; raceGroups++; }
        }
        if (raceGroups >= 2 && knownRaceTotal >= totalPopulation * 0.90) {
            double sumSq = 0;
            for (long c : raceCounts) {
                if (c > 0) {
                    double p = c / knownRaceTotal;
                    sumSq += p * p;
                }
            }
            double simpson = 1.0 - sumSq;
            double maxDiv = (double)(raceGroups - 1) / raceGroups;
            stats.setDiversityIndex(Math.round((simpson / maxDiv) * 10000.0) / 100.0);
        }
        
        // Save to database
        TexasStats saved = texasStatsRepository.save(stats);
        System.out.println("Saved Texas stats for year " + year + " - Population: " + totalPopulation);
        
        return saved;
    }

    /**
     * Recalculate stats for a specific year (overwrites existing)
     */
    public TexasStats recalculateStatsForYear(Integer year) {
        // Delete existing if present
        if (texasStatsRepository.existsByYear(year)) {
            texasStatsRepository.deleteById(year);
        }
        
        return calculateAndSaveStatsForYear(year);
    }
}