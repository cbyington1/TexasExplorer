package com.texasexplorer.derived;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/derived")
@CrossOrigin(origins = "*")
public class DerivedStatsController {

    @Autowired
    private DerivedStatsService derivedStatsService;

    // ============================================================
    // CLASSIFICATION ENDPOINTS (data stored in derived_stats table)
    // ============================================================

    /**
     * GET /api/derived/{year}
     * Get classification data for all cities in a year.
     * Returns urbanization index, label, and sub-scores.
     * Frontend merges with city data by geoid.
     */
    @GetMapping("/{year}")
    public ResponseEntity<List<DerivedStats>> getStatsForYear(@PathVariable Integer year) {
        List<DerivedStats> stats = derivedStatsService.getStatsForYear(year);
        if (stats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/derived/history/{geoid}
     * Get classification history for a single city across all years.
     * For history panel charts showing urbanization index over time.
     */
    @GetMapping("/history/{geoid}")
    public ResponseEntity<List<DerivedStats>> getCityHistory(@PathVariable String geoid) {
        List<DerivedStats> history = derivedStatsService.getCityHistory(geoid);
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(history);
    }

    /**
     * POST /api/derived/recalculate/{year}
     * Force recalculation of classification for a year.
     */
    @PostMapping("/recalculate/{year}")
    public ResponseEntity<List<DerivedStats>> recalculateYear(@PathVariable Integer year) {
        List<DerivedStats> stats = derivedStatsService.recalculateForYear(year);
        if (stats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * POST /api/derived/recalculate-all
     * Force recalculation of classification for all years.
     */
    @PostMapping("/recalculate-all")
    public ResponseEntity<String> recalculateAll() {
        derivedStatsService.recalculateAll();
        return ResponseEntity.ok("Recalculation complete for all years");
    }

    // ============================================================
    // TRENDS ENDPOINT (computed on the fly, nothing stored)
    // ============================================================

    /**
     * GET /api/derived/trends?current=2024&base=2012
     * Compute growth % for every metric comparing two years.
     * Returns a CityTrend DTO per city — lightweight, not persisted.
     */
    @GetMapping("/trends")
    public ResponseEntity<List<CityTrend>> getTrends(
            @RequestParam Integer current,
            @RequestParam Integer base) {
        if (current.equals(base)) {
            return ResponseEntity.badRequest().build();
        }
        List<CityTrend> trends = derivedStatsService.computeTrends(current, base);
        if (trends.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(trends);
    }
}