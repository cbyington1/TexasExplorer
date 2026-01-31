package com.texasexplorer.stats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TexasStatsRepository extends JpaRepository<TexasStats, Integer> {
    
    // Find stats for a specific year
    TexasStats findByYear(Integer year);
    
    // Get all stats ordered by year
    List<TexasStats> findAllByOrderByYearAsc();
    
    // Check if stats exist for a year
    boolean existsByYear(Integer year);
}