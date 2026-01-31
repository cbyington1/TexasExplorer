package com.texasexplorer.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/texas-stats")
@CrossOrigin(origins = "*")
public class TexasStatsController {

    @Autowired
    private TexasStatsService texasStatsService;

    /**
     * GET /api/texas-stats/{year}
     * Get Texas-wide statistics for a specific year
     */
    @GetMapping("/{year}")
    public ResponseEntity<TexasStats> getStatsForYear(@PathVariable Integer year) {
        TexasStats stats = texasStatsService.getStatsForYear(year);
        
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/texas-stats/all
     * Get Texas-wide statistics for all years
     */
    @GetMapping("/all")
    public ResponseEntity<List<TexasStats>> getAllStats() {
        List<TexasStats> allStats = texasStatsService.getAllStats();
        return ResponseEntity.ok(allStats);
    }
    
    /**
     * POST /api/texas-stats/recalculate/{year}
     * Manually recalculate stats for a specific year
     */
    @PostMapping("/recalculate/{year}")
    public ResponseEntity<TexasStats> recalculateYear(@PathVariable Integer year) {
        TexasStats stats = texasStatsService.recalculateStatsForYear(year);
        
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(stats);
    }
}