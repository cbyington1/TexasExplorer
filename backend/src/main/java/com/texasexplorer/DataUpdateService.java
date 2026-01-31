package com.texasexplorer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for checking Census API for new data and loading it into the database.
 * Separated from DataLoader to allow reuse by both startup loading and scheduled updates.
 */
@Service
public class DataUpdateService {

    @Autowired
    private CityService cityService;

    @Autowired
    private CensusApiService censusApiService;
    
    @Autowired
    private com.texasexplorer.stats.TexasStatsService texasStatsService;

    // Census ACS 5-year data became available starting in 2009
    private static final int EARLIEST_AVAILABLE_YEAR = 2009;
    
    // ACS 5-year estimates lag 2 years (e.g., 2022 data released in 2024)
    private static final int DATA_LAG_YEARS = 2;

    /**
     * Check for missing years and load them
     * @param existingYears Years already in database
     * @param currentYear Current calendar year
     * @return true if any updates were performed
     */
    public boolean checkAndLoadMissingYears(List<Integer> existingYears, int currentYear) {
        // Calculate the latest year we should expect data for
        // ACS 5-year data has a 2-year lag (2022 data released in 2024)
        int latestExpectedYear = currentYear - DATA_LAG_YEARS;
        
        log("Latest expected ACS year: " + latestExpectedYear);
        
        // Build list of years we should have
        List<Integer> expectedYears = new ArrayList<>();
        for (int year = EARLIEST_AVAILABLE_YEAR; year <= latestExpectedYear; year++) {
            expectedYears.add(year);
        }
        
        // Find missing years
        List<Integer> missingYears = new ArrayList<>(expectedYears);
        missingYears.removeAll(existingYears);
        
        if (missingYears.isEmpty()) {
            log("No missing years found. Database is up to date.");
            return false;
        }
        
        log("Missing years detected: " + missingYears);
        log("Attempting to load " + missingYears.size() + " year(s)...");
        
        // Load gazetteer data (needed for all years)
        Map<String, GazetteerData> gazetteerMap;
        try {
            gazetteerMap = loadGazetteer();
            log("Loaded " + gazetteerMap.size() + " places from Gazetteer");
        } catch (Exception e) {
            logError("Failed to load Gazetteer: " + e.getMessage());
            return false;
        }
        
        // Load each missing year
        int successCount = 0;
        for (int year : missingYears) {
            boolean success = loadYearData(year, gazetteerMap);
            if (success) {
                successCount++;
            }
            
            // Add delay between API calls to avoid rate limiting
            if (missingYears.size() > 1 && year != missingYears.get(missingYears.size() - 1)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        log("Successfully loaded " + successCount + " of " + missingYears.size() + " missing year(s)");
        return successCount > 0;
    }

    /**
     * Load data for a specific year
     * @param year Year to load
     * @param gazetteerMap Gazetteer data for geographic info
     * @return true if successful
     */
    public boolean loadYearData(int year, Map<String, GazetteerData> gazetteerMap) {
        log("--- Loading Year " + year + " ---");
        
        try {
            // Fetch from Census API
            Map<String, CensusApiService.CensusData> censusData = 
                censusApiService.fetchTexasDataForYear(year);
            
            if (censusData.isEmpty()) {
                log("  No data returned from Census API for " + year);
                return false;
            }
            
            // Merge with Gazetteer and create City objects
            List<City> cities = new ArrayList<>();
            int matched = 0, unmatched = 0;
            
            for (Map.Entry<String, CensusApiService.CensusData> entry : censusData.entrySet()) {
                String placeCode = entry.getKey();
                CensusApiService.CensusData census = entry.getValue();
                GazetteerData gaz = gazetteerMap.get(placeCode);
                
                if (gaz != null) {
                    City city = buildCity(year, gaz, census);
                    cities.add(city);
                    matched++;
                } else {
                    unmatched++;
                }
            }
            
            // Save to database
            cityService.saveAll(cities);
            
            log("  Matched: " + matched + ", Unmatched: " + unmatched + ", Saved: " + cities.size());
            
            // Calculate and save Texas stats for this year
            log("  Calculating Texas-wide statistics for " + year + "...");
            try {
                texasStatsService.calculateAndSaveStatsForYear(year);
                log("  Texas stats saved for " + year);
            } catch (Exception e) {
                logError("  Failed to calculate Texas stats for " + year + ": " + e.getMessage());
            }
            
            // Show top 5 for verification
            List<City> top = cityService.getTopCities(year);
            log("  Top 5 cities:");
            top.stream().limit(5).forEach(c -> 
                log("    " + c.getName() + ": pop=" + c.getPopulation() + 
                    ", income=$" + c.getMedianHouseholdIncome())
            );
            
            return true;
            
        } catch (Exception e) {
            logError("Failed to load year " + year + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load Gazetteer file for geographic coordinates
     */
    public Map<String, GazetteerData> loadGazetteer() throws Exception {
        Map<String, GazetteerData> map = new HashMap<>();
        ClassPathResource resource = new ClassPathResource("2024_gaz_place_48.txt");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            reader.readLine(); // Skip header
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 12) {
                    String geoid = parts[1];
                    String placeCode = geoid.substring(geoid.length() - 5);
                    
                    GazetteerData data = new GazetteerData();
                    data.geoid = geoid;
                    data.name = parts[3];
                    data.latitude = parseDouble(parts[10]);
                    data.longitude = parseDouble(parts[11]);
                    data.landAreaSqMi = parseDouble(parts[8]);
                    data.waterAreaSqMi = parseDouble(parts[9]);
                    
                    map.put(placeCode, data);
                }
            }
        }
        return map;
    }

    /**
     * Build City entity from Gazetteer and Census data
     */
    private City buildCity(int year, GazetteerData gaz, CensusApiService.CensusData census) {
        City city = new City();
        
        // Identifiers
        city.setGeoid(gaz.geoid);
        city.setName(census.name);
        city.setYear(year);
        
        // Geography
        city.setLatitude(gaz.latitude);
        city.setLongitude(gaz.longitude);
        city.setLandAreaSqMi(gaz.landAreaSqMi);
        city.setWaterAreaSqMi(gaz.waterAreaSqMi);
        
        // Demographics
        city.setPopulation(census.population);
        city.setMalePopulation(census.malePopulation);
        city.setFemalePopulation(census.femalePopulation);
        city.setMedianAge(census.medianAge);
        city.setAgeUnder18(census.ageUnder18);
        
        // Race
        city.setWhitePopulation(census.white);
        city.setBlackPopulation(census.black);
        city.setNativeAmericanPopulation(census.nativeAmerican);
        city.setAsianPopulation(census.asian);
        city.setPacificIslanderPopulation(census.pacificIslander);
        city.setOtherRacePopulation(census.otherRace);
        city.setTwoOrMoreRacesPopulation(census.twoOrMore);
        city.setHispanicPopulation(census.hispanic);
        
        // Nativity
        city.setForeignBorn(census.foreignBorn);
        
        // Economic
        city.setMedianHouseholdIncome(census.medianHouseholdIncome);
        city.setPerCapitaIncome(census.perCapitaIncome);
        city.setPovertyTotal(census.poverty);
        
        // Employment
        city.setLaborForce(census.laborForce);
        city.setEmployed(census.employed);
        city.setUnemployed(census.unemployed);
        
        // Education
        city.setEduBachelors(census.bachelors);
        city.setEduMasters(census.masters);
        city.setEduDoctorate(census.doctorate);
        
        // Housing
        city.setMedianHomeValue(census.medianHomeValue);
        city.setMedianRent(census.medianRent);
        city.setOwnerOccupied(census.ownerOccupied);
        city.setRenterOccupied(census.renterOccupied);
        city.setVacantUnits(census.vacant);
        
        // Commute
        city.setWorkFromHome(census.workFromHome);
        city.setDriveAlone(census.driveAlone);
        city.setPublicTransit(census.publicTransit);
        city.setMeanCommuteMinutes(census.meanCommuteMinutes);
        
        city.setLastUpdated(LocalDateTime.now());
        
        return city;
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void log(String message) {
        System.out.println("[DataUpdateService] " + message);
    }

    private void logError(String message) {
        System.err.println("[DataUpdateService] ERROR: " + message);
    }

    // Inner class for Gazetteer data
    public static class GazetteerData {
        public String geoid;
        public String name;
        public Double latitude;
        public Double longitude;
        public Double landAreaSqMi;
        public Double waterAreaSqMi;
    }
}