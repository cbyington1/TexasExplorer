package com.texasexplorer.derived;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DerivedStatsRepository extends JpaRepository<DerivedStats, Long> {

    Optional<DerivedStats> findByGeoidAndYear(String geoid, Integer year);

    List<DerivedStats> findByYear(Integer year);

    List<DerivedStats> findByGeoidOrderByYearAsc(String geoid);

    boolean existsByYear(Integer year);

    long countByYear(Integer year);

    // Bulk delete — executes immediately, no flush timing issues
    @Modifying
    @Query("DELETE FROM DerivedStats d WHERE d.year = :year")
    void deleteAllByYear(@Param("year") Integer year);

    @Query("SELECT DISTINCT d.year FROM DerivedStats d ORDER BY d.year")
    List<Integer> findAllYears();
}