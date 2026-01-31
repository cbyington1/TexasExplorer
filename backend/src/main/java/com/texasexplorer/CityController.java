package com.texasexplorer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


//@RestController let's the class handle HTTP requests and  automatically converts the response to JSON
@RestController

//@RequestMapping Let's you set the base URL path for all endpoints
@RequestMapping("/api/cities")

//@CrossOrigin prevents CORS errors :)
@CrossOrigin(origins = "*")
public class CityController {

    //@Autowired automatically sets up objects for you to remove dependency issues
    @Autowired
    private CityService cityService;

    //GET /api/cities/years - Get all available years
    @GetMapping("/years")
    public List<Integer> getAvailableYears() {
        return cityService.getAvailableYears();
    }

    //GET /api/cities/2022 - Get all cities for a year
    @GetMapping("/{year}")
    public ResponseEntity<List<City>> getCitiesForYear(@PathVariable Integer year) {
        if (!cityService.yearExists(year)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cityService.getCitiesForYear(year));
    }

    //GET /api/cities/2022/top - Get top 10 cities for a year
    @GetMapping("/{year}/top")
    public ResponseEntity<List<City>> getTopCities(@PathVariable Integer year) {
        if (!cityService.yearExists(year)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cityService.getTopCities(year));
    }

    //GET /api/cities/2022/search?q=houston - Search cities
    @GetMapping("/{year}/search")
    public ResponseEntity<List<City>> searchCities(
            @PathVariable Integer year,
            @RequestParam("q") String query) {
        if (!cityService.yearExists(year)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cityService.searchCities(year, query));
    }

    //GET /api/cities/2022/population/100000 - Cities above population
    @GetMapping("/{year}/population/{minPop}")
    public ResponseEntity<List<City>> getCitiesAbovePopulation(
            @PathVariable Integer year,
            @PathVariable Integer minPop) {
        if (!cityService.yearExists(year)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cityService.getCitiesAbovePopulation(year, minPop));
    }

    //GET /api/cities/history/4835000 - Get all years for a city
    @GetMapping("/history/{geoid}")
    public ResponseEntity<List<City>> getCityHistory(@PathVariable String geoid) {
        List<City> history = cityService.getCityHistory(geoid);
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(history);
    }

    //GET /api/cities/city/4835000/2022 - Get specific city for specific year
    @GetMapping("/city/{geoid}/{year}")
    public ResponseEntity<City> getCityForYear(
            @PathVariable String geoid,
            @PathVariable Integer year) {
        return cityService.getCityForYear(geoid, year)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //GET /api/cities/stats - Get statistics
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Integer> years = cityService.getAvailableYears();
        stats.put("years", years);
        stats.put("yearCount", years.size());
        stats.put("totalRecords", cityService.getTotalCount());
        
        if (!years.isEmpty()) {
            Integer latestYear = years.get(years.size() - 1);
            stats.put("latestYear", latestYear);
            stats.put("citiesInLatestYear", cityService.getCountForYear(latestYear));
        }
        
        return stats;
    }

    //GET /api/cities/compare?city=Houston&years=2012,2022 - Compare across years
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareCityAcrossYears(
            @RequestParam String city,
            @RequestParam String years) {
        
        String[] yearArray = years.split(",");
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("city", city);
        
        Map<Integer, City> yearData = new HashMap<>();
        for (String yearStr : yearArray) {
            try {
                Integer year = Integer.parseInt(yearStr.trim());
                cityService.getCityByNameAndYear(city, year)
                    .ifPresent(data -> yearData.put(year, data));
            } catch (NumberFormatException e) {
                // Skip invalid years
            }
        }
        
        if (yearData.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        comparison.put("data", yearData);
        return ResponseEntity.ok(comparison);
    }
}