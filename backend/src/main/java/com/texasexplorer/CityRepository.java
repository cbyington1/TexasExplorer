package com.texasexplorer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

//@Repository defines this class as a connection to the database that let's us interact with it
@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    
    //Find by geoid and year
    Optional<City> findByGeoidAndYear(String geoid, Integer year);
    
    //Find all data for a specific city across all years
    List<City> findByGeoidOrderByYearAsc(String geoid);
    
    //Find all cities for a specific year
    List<City> findByYearOrderByPopulationDesc(Integer year);
    
    //Find city by name and year
    Optional<City> findByNameIgnoreCaseAndYear(String name, Integer year);
    
    //Get all years we have data for
    @Query("SELECT DISTINCT c.year FROM City c ORDER BY c.year")
    List<Integer> findAllYears();
    
    //Count records for a specific year
    long countByYear(Integer year);
    
    //Find cities with population above threshold for a year
    List<City> findByYearAndPopulationGreaterThanOrderByPopulationDesc(Integer year, Integer minPopulation);
    
    //Check if we have data for a specific year
    boolean existsByYear(Integer year);
    
    //Get top N cities by population for a year
    List<City> findTop10ByYearOrderByPopulationDesc(Integer year);
    
    //Search cities by name pattern
    List<City> findByYearAndNameContainingIgnoreCaseOrderByPopulationDesc(Integer year, String namePart);
}