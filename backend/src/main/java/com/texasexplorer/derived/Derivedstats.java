package com.texasexplorer.derived;

import jakarta.persistence.*;

/**
 * Stores derived classification metrics computed from raw Census data.
 * Keeps a clean separation: cities table = pure Census, derived_stats = our calculations.
 * 
 * Trends are NOT stored — they're computed on the fly via the trends endpoint.
 * Only classification data lives here since it's needed on every map load.
 * 
 * Keyed on (geoid, year) to match the cities table.
 * New derived metrics that need persistence should be added here over time.
 */
@Entity
@Table(name = "derived_stats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"geoid", "year"})
})
public class DerivedStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Keys — match the cities table
    @Column(nullable = false)
    private String geoid;

    @Column(nullable = false)
    private Integer year;

    // ============================================================
    // CLASSIFICATION
    // ============================================================

    /** Continuous urbanization score 0-100 */
    private Double urbanizationIndex;

    /** Discrete label: "Urban", "Suburban", or "Rural" */
    private String classification;

    // Urbanization sub-scores (useful for debugging/transparency)
    private Double populationScore;
    private Double densityScore;
    private Double proximityScore;
    private Double incomeRatioScore;

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public DerivedStats() {}

    public DerivedStats(String geoid, Integer year) {
        this.geoid = geoid;
        this.year = year;
    }

    // ============================================================
    // GETTERS AND SETTERS
    // ============================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGeoid() { return geoid; }
    public void setGeoid(String geoid) { this.geoid = geoid; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Double getUrbanizationIndex() { return urbanizationIndex; }
    public void setUrbanizationIndex(Double urbanizationIndex) { this.urbanizationIndex = urbanizationIndex; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public Double getPopulationScore() { return populationScore; }
    public void setPopulationScore(Double populationScore) { this.populationScore = populationScore; }

    public Double getDensityScore() { return densityScore; }
    public void setDensityScore(Double densityScore) { this.densityScore = densityScore; }

    public Double getProximityScore() { return proximityScore; }
    public void setProximityScore(Double proximityScore) { this.proximityScore = proximityScore; }

    public Double getIncomeRatioScore() { return incomeRatioScore; }
    public void setIncomeRatioScore(Double incomeRatioScore) { this.incomeRatioScore = incomeRatioScore; }
}