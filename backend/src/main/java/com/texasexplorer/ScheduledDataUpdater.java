package com.texasexplorer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduled task to automatically check for and load new Census data.
 * 
 * ACS 5-Year Estimates are typically released in December each year.
 * This scheduler runs:
 * - Weekly on Sundays at 2:00 AM (to catch new data releases)
 * - Can also be triggered manually via the API endpoint
 */
@Component
public class ScheduledDataUpdater {

    @Autowired
    private DataUpdateService dataUpdateService;

    @Autowired
    private CityService cityService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Runs every Sunday at 2:00 AM
     * Cron format: second minute hour day month weekday
     * "0 0 2 * * SUN" = At 02:00:00 AM, every Sunday
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void weeklyDataCheck() {
        log("=== SCHEDULED DATA CHECK - WEEKLY ===");
        checkAndUpdateData();
    }

    /**
     * Runs on the 1st and 15th of December at 3:00 AM
     * This is when ACS data is most likely to be released
     * "0 0 3 1,15 12 *" = At 03:00:00 AM, on the 1st and 15th of December
     */
    @Scheduled(cron = "0 0 3 1,15 12 *")
    public void decemberDataCheck() {
        log("=== SCHEDULED DATA CHECK - DECEMBER RELEASE ===");
        checkAndUpdateData();
    }

    /**
     * Public method that can be called manually via API endpoint
     */
    public void manualDataCheck() {
        log("=== MANUAL DATA CHECK TRIGGERED ===");
        checkAndUpdateData();
    }

    /**
     * Core logic to check for missing years and load them
     */
    private void checkAndUpdateData() {
        try {
            log("Starting data check at " + LocalDateTime.now().format(FORMATTER));
            
            // Get years currently in database
            var existingYears = cityService.getAvailableYears();
            log("Years currently in database: " + existingYears);
            
            // Determine current year and expected years
            int currentYear = LocalDateTime.now().getYear();
            log("Current year: " + currentYear);
            
            // Check for missing years and update
            boolean updatesPerformed = dataUpdateService.checkAndLoadMissingYears(existingYears, currentYear);
            
            if (updatesPerformed) {
                // Log final statistics
                long totalRecords = cityService.getTotalCount();
                var updatedYears = cityService.getAvailableYears();
                log("=== UPDATE COMPLETE ===");
                log("Total records in database: " + totalRecords);
                log("Years now available: " + updatedYears);
            } else {
                log("No updates needed - database is current");
            }
            
        } catch (Exception e) {
            logError("Error during scheduled data update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.println("[" + timestamp + "] [ScheduledDataUpdater] " + message);
    }

    private void logError(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.err.println("[" + timestamp + "] [ScheduledDataUpdater] ERROR: " + message);
    }
}