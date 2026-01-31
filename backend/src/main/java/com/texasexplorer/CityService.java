package com.texasexplorer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;


/*@Service defines the class as a layer between the repository and the controller.
It's where we can implement our logic to get specific results*/
@Service
public class CityService {

    @Autowired
    private CityRepository cityRepository;

    //Get all cities for a specific year
    public List<City> getCitiesForYear(Integer year) {
        return cityRepository.findByYearOrderByPopulationDesc(year);
    }

    //Get all years of data for a specific city (history)
    public List<City> getCityHistory(String geoid) {
        return cityRepository.findByGeoidOrderByYearAsc(geoid);
    }

    //Get a specific city for a specific year
    public Optional<City> getCityForYear(String geoid, Integer year) {
        return cityRepository.findByGeoidAndYear(geoid, year);
    }

    //Get city by name and year
    public Optional<City> getCityByNameAndYear(String name, Integer year) {
        return cityRepository.findByNameIgnoreCaseAndYear(name, year);
    }

    //Get all available years
    public List<Integer> getAvailableYears() {
        return cityRepository.findAllYears();
    }

    //Get count for a year
    public long getCountForYear(Integer year) {
        return cityRepository.countByYear(year);
    }

    //Get top 10 cities by population for a year
    public List<City> getTopCities(Integer year) {
        return cityRepository.findTop10ByYearOrderByPopulationDesc(year);
    }

    //Search cities by name
    public List<City> searchCities(Integer year, String query) {
        return cityRepository.findByYearAndNameContainingIgnoreCaseOrderByPopulationDesc(year, query);
    }

    //Get cities above population threshold
    public List<City> getCitiesAbovePopulation(Integer year, Integer minPopulation) {
        return cityRepository.findByYearAndPopulationGreaterThanOrderByPopulationDesc(year, minPopulation);
    }

    //Check if year exists
    public boolean yearExists(Integer year) {
        return cityRepository.existsByYear(year);
    }

    //Save single city
    public City save(City city) {
        return cityRepository.save(city);
    }

    //Save multiple cities
    public List<City> saveAll(List<City> cities) {
        return cityRepository.saveAll(cities);
    }

    //Get total count
    public long getTotalCount() {
        return cityRepository.count();
    }
}