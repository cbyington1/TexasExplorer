package com.texasexplorer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private CityService cityService;

    @Autowired
    private CensusApiService censusApiService;

    // All available ACS 5-year data years
    private static final int[] ALL_YEARS = {2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024};
    
    // TEST MODE: Set to true to only load one year for testing
    private static final boolean TEST_MODE = false;
    private static final int[] TEST_YEARS = {2022};

    @Override
    public void run(String... args) throws Exception {
        // Check if we already have data
        List<Integer> existingYears = cityService.getAvailableYears();
        
        if (!existingYears.isEmpty()) {
            System.out.println("=== Data Already Loaded ===");
            System.out.println("Years in database: " + existingYears);
            for (Integer year : existingYears) {
                long count = cityService.getCountForYear(year);
                System.out.println("  " + year + ": " + count + " cities");
            }
            System.out.println("\nTo reload, clear the cities table first.");
            return;
        }

        System.out.println("=== Starting Data Load ===");
        
        // Load Gazetteer for coordinates
        System.out.println("Loading Gazetteer file...");
        Map<String, GazetteerData> gazetteerMap = loadGazetteer();
        System.out.println("Loaded " + gazetteerMap.size() + " places from Gazetteer");

        // Determine which years to load
        int[] yearsToLoad = TEST_MODE ? TEST_YEARS : ALL_YEARS;
        System.out.println("\nLoading " + yearsToLoad.length + " year(s): " + Arrays.toString(yearsToLoad));
        if (TEST_MODE) {
            System.out.println("(TEST MODE - set TEST_MODE = false to load all years)");
        }
        
        int totalSaved = 0;
        
        for (int year : yearsToLoad) {
            System.out.println("\n--- Year " + year + " ---");
            
            // Fetch from Census API
            Map<String, CensusApiService.CensusData> censusData = censusApiService.fetchTexasDataForYear(year);
            
            if (censusData.isEmpty()) {
                System.out.println("  No data for " + year + ", skipping...");
                continue;
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
            totalSaved += cities.size();
            
            System.out.println("  Matched: " + matched + ", Unmatched: " + unmatched + ", Saved: " + cities.size());
            
            // Show top 5
            List<City> top = cityService.getTopCities(year);
            System.out.println("  Top 5:");
            top.stream().limit(5).forEach(c -> 
                System.out.println("    " + c.getName() + ": pop=" + c.getPopulation() + 
                    ", income=$" + c.getMedianHouseholdIncome() + ", home=$" + c.getMedianHomeValue())
            );
            
            // Delay to avoid rate limiting
            if (!TEST_MODE && yearsToLoad.length > 1) {
                Thread.sleep(1000);
            }
        }
        
        System.out.println("\n=== Load Complete ===");
        System.out.println("Total records: " + totalSaved);
    }

    private Map<String, GazetteerData> loadGazetteer() throws Exception {
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

    private static class GazetteerData {
        String geoid;
        String name;
        Double latitude;
        Double longitude;
        Double landAreaSqMi;
        Double waterAreaSqMi;
    }
}