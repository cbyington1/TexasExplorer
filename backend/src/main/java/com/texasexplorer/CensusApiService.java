package com.texasexplorer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Service
public class CensusApiService {

    @Value("${census.api.key}")
    private String apiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches all Texas places demographic data from Census API for a specific year
     * @param year The ACS 5-year data year (2009-2024)
     * @return Map of placeCode -> CensusData
     */
    public Map<String, CensusData> fetchTexasDataForYear(int year) {
        Map<String, CensusData> results = new HashMap<>();
        
        String variables = String.join(",",
            "NAME",
            "B01003_001E",  // Total population
            "B01001_002E",  // Male
            "B01001_026E",  // Female
            "B01002_001E",  // Median age
            "B09001_001E",  // Under 18
            "B02001_002E",  // White
            "B02001_003E",  // Black
            "B02001_004E",  // Native American
            "B02001_005E",  // Asian
            "B02001_006E",  // Pacific Islander
            "B02001_007E",  // Other
            "B02001_008E",  // Two or more
            "B03003_003E",  // Hispanic
            "B05002_013E",  // Foreign born
            "B19013_001E",  // Median household income
            "B19301_001E",  // Per capita income
            "B17001_002E",  // Poverty
            "B23025_002E",  // Labor force
            "B23025_004E",  // Employed
            "B23025_005E",  // Unemployed
            "B15003_022E",  // Bachelor's
            "B15003_023E",  // Master's
            "B15003_025E",  // Doctorate
            "B25077_001E",  // Median home value
            "B25064_001E",  // Median rent
            "B25003_002E",  // Owner occupied
            "B25003_003E",  // Renter occupied
            "B25002_003E",  // Vacant
            "B08301_021E",  // Work from home
            "B08301_003E",  // Drive alone
            "B08301_010E", // Public transit
            "B08013_001E"   // Mean commute time
        );
        
        String url = String.format(
            "https://api.census.gov/data/%d/acs/acs5?get=%s&for=place:*&in=state:48&key=%s",
            year, variables, apiKey
        );
        
        try {
            System.out.println("Fetching Census data for year " + year + "...");
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            // Process data rows (skip header row 0)
            for (int i = 1; i < root.size(); i++) {
                JsonNode row = root.get(i);
                
                String placeCode = row.get(row.size() - 1).asText();
                
                CensusData data = new CensusData();
                data.name = cleanName(row.get(0).asText());
                data.population = safeParseInt(row.get(1));
                data.malePopulation = safeParseInt(row.get(2));
                data.femalePopulation = safeParseInt(row.get(3));
                data.medianAge = safeParseDouble(row.get(4));
                data.ageUnder18 = safeParseInt(row.get(5));
                data.white = safeParseInt(row.get(6));
                data.black = safeParseInt(row.get(7));
                data.nativeAmerican = safeParseInt(row.get(8));
                data.asian = safeParseInt(row.get(9));
                data.pacificIslander = safeParseInt(row.get(10));
                data.otherRace = safeParseInt(row.get(11));
                data.twoOrMore = safeParseInt(row.get(12));
                data.hispanic = safeParseInt(row.get(13));
                data.foreignBorn = safeParseInt(row.get(14));
                data.medianHouseholdIncome = safeParseInt(row.get(15));
                data.perCapitaIncome = safeParseInt(row.get(16));
                data.poverty = safeParseInt(row.get(17));
                data.laborForce = safeParseInt(row.get(18));
                data.employed = safeParseInt(row.get(19));
                data.unemployed = safeParseInt(row.get(20));
                data.bachelors = safeParseInt(row.get(21));
                data.masters = safeParseInt(row.get(22));
                data.doctorate = safeParseInt(row.get(23));
                data.medianHomeValue = safeParseInt(row.get(24));
                data.medianRent = safeParseInt(row.get(25));
                data.ownerOccupied = safeParseInt(row.get(26));
                data.renterOccupied = safeParseInt(row.get(27));
                data.vacant = safeParseInt(row.get(28));
                data.workFromHome = safeParseInt(row.get(29));
                data.driveAlone = safeParseInt(row.get(30));
                data.publicTransit = safeParseInt(row.get(31));
                data.meanCommuteMinutes = safeParseDouble(row.get(32));
                
                results.put(placeCode, data);
            }
            
            System.out.println("  Fetched " + results.size() + " places for " + year);
            
        } catch (Exception e) {
            System.err.println("Error fetching Census data for " + year + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    private String cleanName(String name) {
        return name.replace(", Texas", "")
                   .replace(" city", "")
                   .replace(" town", "")
                   .replace(" CDP", "")
                   .trim();
    }
    
    private Integer safeParseInt(JsonNode node) {
        if (node == null || node.isNull() || node.asText().isEmpty()) return null;
        try {
            int val = Integer.parseInt(node.asText());
            return val >= 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Double safeParseDouble(JsonNode node) {
        if (node == null || node.isNull() || node.asText().isEmpty()) return null;
        try {
            double val = Double.parseDouble(node.asText());
            return val >= 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    // Data holder class
    public static class CensusData {
        public String name;
        public Integer population;
        public Integer malePopulation;
        public Integer femalePopulation;
        public Double medianAge;
        public Integer ageUnder18;
        public Integer white;
        public Integer black;
        public Integer nativeAmerican;
        public Integer asian;
        public Integer pacificIslander;
        public Integer otherRace;
        public Integer twoOrMore;
        public Integer hispanic;
        public Integer foreignBorn;
        public Integer medianHouseholdIncome;
        public Integer perCapitaIncome;
        public Integer poverty;
        public Integer laborForce;
        public Integer employed;
        public Integer unemployed;
        public Integer bachelors;
        public Integer masters;
        public Integer doctorate;
        public Integer medianHomeValue;
        public Integer medianRent;
        public Integer ownerOccupied;
        public Integer renterOccupied;
        public Integer vacant;
        public Integer workFromHome;
        public Integer driveAlone;
        public Integer publicTransit;
        public Double meanCommuteMinutes;
    }
}