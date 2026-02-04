package com.texasexplorer.derived;

import jakarta.persistence.*;

/**
 * Stores all metrics derived/computed from raw Census data.
 * Keeps a clean separation: cities table = pure Census, derived_stats = our calculations.
 * 
 * Keyed on (geoid, year) to match the cities table.
 * New derived metrics should be added here over time.
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
    // DIVERSITY
    // ============================================================

    /** Simpson's Diversity Index normalized to 0-100 */
    private Double diversityIndex;

    // ============================================================
    // GROWTH TRENDS (% change from earliest available year)
    // ============================================================

    /** Base year used for trend calculations (e.g. 2012) */
    private Integer trendBaseYear;

    private Double populationGrowthPct;
    private Double medianIncomeGrowthPct;
    private Double perCapitaIncomeGrowthPct;
    private Double medianHomeValueGrowthPct;
    private Double medianRentGrowthPct;

    // Percentage point changes (not % growth — these are already percentages)
    private Double unemploymentRateChange;    // e.g. was 8%, now 5% → -3.0
    private Double povertyRateChange;
    private Double homeownershipRateChange;
    private Double bachelorsPlusChange;       // education attainment shift
    private Double hispanicPctChange;         // demographic shift
    private Double foreignBornPctChange;
    private Double workFromHomePctChange;

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

    // Classification
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

    // Trends
    public Integer getTrendBaseYear() { return trendBaseYear; }
    public void setTrendBaseYear(Integer trendBaseYear) { this.trendBaseYear = trendBaseYear; }

    public Double getPopulationGrowthPct() { return populationGrowthPct; }
    public void setPopulationGrowthPct(Double populationGrowthPct) { this.populationGrowthPct = populationGrowthPct; }

    public Double getMedianIncomeGrowthPct() { return medianIncomeGrowthPct; }
    public void setMedianIncomeGrowthPct(Double medianIncomeGrowthPct) { this.medianIncomeGrowthPct = medianIncomeGrowthPct; }

    public Double getPerCapitaIncomeGrowthPct() { return perCapitaIncomeGrowthPct; }
    public void setPerCapitaIncomeGrowthPct(Double perCapitaIncomeGrowthPct) { this.perCapitaIncomeGrowthPct = perCapitaIncomeGrowthPct; }

    public Double getMedianHomeValueGrowthPct() { return medianHomeValueGrowthPct; }
    public void setMedianHomeValueGrowthPct(Double medianHomeValueGrowthPct) { this.medianHomeValueGrowthPct = medianHomeValueGrowthPct; }

    public Double getMedianRentGrowthPct() { return medianRentGrowthPct; }
    public void setMedianRentGrowthPct(Double medianRentGrowthPct) { this.medianRentGrowthPct = medianRentGrowthPct; }

    public Double getUnemploymentRateChange() { return unemploymentRateChange; }
    public void setUnemploymentRateChange(Double unemploymentRateChange) { this.unemploymentRateChange = unemploymentRateChange; }

    public Double getPovertyRateChange() { return povertyRateChange; }
    public void setPovertyRateChange(Double povertyRateChange) { this.povertyRateChange = povertyRateChange; }

    public Double getHomeownershipRateChange() { return homeownershipRateChange; }
    public void setHomeownershipRateChange(Double homeownershipRateChange) { this.homeownershipRateChange = homeownershipRateChange; }

    public Double getBachelorsPlusChange() { return bachelorsPlusChange; }
    public void setBachelorsPlusChange(Double bachelorsPlusChange) { this.bachelorsPlusChange = bachelorsPlusChange; }

    public Double getHispanicPctChange() { return hispanicPctChange; }
    public void setHispanicPctChange(Double hispanicPctChange) { this.hispanicPctChange = hispanicPctChange; }

    public Double getForeignBornPctChange() { return foreignBornPctChange; }
    public void setForeignBornPctChange(Double foreignBornPctChange) { this.foreignBornPctChange = foreignBornPctChange; }

    public Double getWorkFromHomePctChange() { return workFromHomePctChange; }
    public void setWorkFromHomePctChange(Double workFromHomePctChange) { this.workFromHomePctChange = workFromHomePctChange; }

    // Diversity
    public Double getDiversityIndex() { return diversityIndex; }
    public void setDiversityIndex(Double diversityIndex) { this.diversityIndex = diversityIndex; }
}