package com.texasexplorer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

//For administration and just checking or manually calling
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private ScheduledDataUpdater scheduledDataUpdater;

    @Autowired
    private CityService cityService;

    @Autowired
    private com.texasexplorer.stats.TexasStatsService texasStatsService;

    //Manually trigger update
    @GetMapping("/update-data")
    public ResponseEntity<Map<String, Object>> triggerDataUpdate() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("message", "Data update check initiated");
            response.put("timestamp", LocalDateTime.now().toString());
            
            // Get current state before update
            var yearsBefore = cityService.getAvailableYears();
            long recordsBefore = cityService.getTotalCount();
            
            response.put("years_before", yearsBefore);
            response.put("records_before", recordsBefore);
            
            // Trigger the update
            scheduledDataUpdater.manualDataCheck();
            
            // Get state after update
            var yearsAfter = cityService.getAvailableYears();
            long recordsAfter = cityService.getTotalCount();
            
            response.put("years_after", yearsAfter);
            response.put("records_after", recordsAfter);
            response.put("new_records", recordsAfter - recordsBefore);
            
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //Get status
    @GetMapping("/update-status")
    public ResponseEntity<Map<String, Object>> getUpdateStatus() {
        Map<String, Object> response = new HashMap<>();
        
        var availableYears = cityService.getAvailableYears();
        long totalRecords = cityService.getTotalCount();
        
        // Calculate expected latest year (current year - 2 for ACS lag)
        int currentYear = LocalDateTime.now().getYear();
        int expectedLatestYear = currentYear - 2;
        
        response.put("available_years", availableYears);
        response.put("total_records", totalRecords);
        response.put("current_year", currentYear);
        response.put("expected_latest_year", expectedLatestYear);
        response.put("years_count", availableYears.size());
        
        if (!availableYears.isEmpty()) {
            int latestYear = availableYears.get(availableYears.size() - 1);
            response.put("latest_year_in_db", latestYear);
            response.put("is_up_to_date", latestYear >= expectedLatestYear);
            response.put("years_behind", Math.max(0, expectedLatestYear - latestYear));
        } else {
            response.put("latest_year_in_db", null);
            response.put("is_up_to_date", false);
            response.put("database_empty", true);
        }
        
        return ResponseEntity.ok(response);
    }

    //Get Health of api
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Texas Explorer API");
        return ResponseEntity.ok(response);
    }

    // Recalculate Texas-wide stats for all years (e.g. after adding diversity index)
    @PostMapping("/recalculate-texas-stats")
    public ResponseEntity<String> recalculateTexasStats() {
        var years = cityService.getAvailableYears();
        for (Integer year : years) {
            texasStatsService.recalculateStatsForYear(year);
        }
        return ResponseEntity.ok("Recalculated Texas stats for " + years.size() + " years");
    }
}