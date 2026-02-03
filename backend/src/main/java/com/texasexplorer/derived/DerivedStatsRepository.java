package com.texasexplorer.derived;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DerivedStatsRepository extends JpaRepository<DerivedStats, Long> {

    // Get derived stats for a single city+year
    Optional<DerivedStats> findByGeoidAndYear(String geoid, Integer year);

    // Get all derived stats for a year (bulk — for the /api/derived/{year} endpoint)
    List<DerivedStats> findByYear(Integer year);

    // Get all years of derived stats for a city (for history charts)
    List<DerivedStats> findByGeoidOrderByYearAsc(String geoid);

    // Check if stats exist for a year
    boolean existsByYear(Integer year);

    // Count records for a year
    long countByYear(Integer year);

    // Delete all records for a year (for recalculation)
    void deleteByYear(Integer year);

    // Get all available years
    @Query("SELECT DISTINCT d.year FROM DerivedStats d ORDER BY d.year")
    List<Integer> findAllYears();
}