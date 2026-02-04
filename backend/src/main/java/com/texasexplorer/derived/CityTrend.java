package com.texasexplorer.derived;

/**
 * Lightweight DTO for on-demand trend calculations.
 * NOT a JPA entity — this is never stored in the database.
 * Computed on the fly by comparing two years of city data.
 * 
 * Every field matches a metric visible in the frontend history panel.
 * All growth values are percentage change: ((new - old) / |old|) * 100
 */
public class CityTrend {

    private String geoid;
    private String name;
    private Integer currentYear;
    private Integer baseYear;

    // Overview
    private Double populationGrowthPct;
    private Double medianAgeGrowthPct;

    // Income
    private Double medianIncomeGrowthPct;
    private Double perCapitaIncomeGrowthPct;

    // Housing
    private Double medianHomeValueGrowthPct;
    private Double medianRentGrowthPct;
    private Double homeownershipRateGrowthPct;

    // Employment
    private Double unemploymentRateGrowthPct;
    private Double laborForceParticipationGrowthPct;
    private Double workFromHomePctGrowthPct;

    // Sex
    private Double malePctGrowthPct;
    private Double femalePctGrowthPct;

    // Race
    private Double whitePctGrowthPct;
    private Double blackPctGrowthPct;
    private Double asianPctGrowthPct;
    private Double nativeAmericanPctGrowthPct;
    private Double pacificIslanderPctGrowthPct;
    private Double twoOrMoreRacesPctGrowthPct;
    private Double otherRacePctGrowthPct;

    // Ethnicity
    private Double hispanicPctGrowthPct;

    // Classification
    private Double urbanizationIndexGrowthPct;

    // Diversity
    private Double diversityIndexGrowthPct;

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public CityTrend() {}

    public CityTrend(String geoid, String name, Integer currentYear, Integer baseYear) {
        this.geoid = geoid;
        this.name = name;
        this.currentYear = currentYear;
        this.baseYear = baseYear;
    }

    // ============================================================
    // GETTERS AND SETTERS
    // ============================================================

    public String getGeoid() { return geoid; }
    public void setGeoid(String geoid) { this.geoid = geoid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCurrentYear() { return currentYear; }
    public void setCurrentYear(Integer currentYear) { this.currentYear = currentYear; }

    public Integer getBaseYear() { return baseYear; }
    public void setBaseYear(Integer baseYear) { this.baseYear = baseYear; }

    // Overview
    public Double getPopulationGrowthPct() { return populationGrowthPct; }
    public void setPopulationGrowthPct(Double populationGrowthPct) { this.populationGrowthPct = populationGrowthPct; }

    public Double getMedianAgeGrowthPct() { return medianAgeGrowthPct; }
    public void setMedianAgeGrowthPct(Double medianAgeGrowthPct) { this.medianAgeGrowthPct = medianAgeGrowthPct; }

    // Income
    public Double getMedianIncomeGrowthPct() { return medianIncomeGrowthPct; }
    public void setMedianIncomeGrowthPct(Double medianIncomeGrowthPct) { this.medianIncomeGrowthPct = medianIncomeGrowthPct; }

    public Double getPerCapitaIncomeGrowthPct() { return perCapitaIncomeGrowthPct; }
    public void setPerCapitaIncomeGrowthPct(Double perCapitaIncomeGrowthPct) { this.perCapitaIncomeGrowthPct = perCapitaIncomeGrowthPct; }

    // Housing
    public Double getMedianHomeValueGrowthPct() { return medianHomeValueGrowthPct; }
    public void setMedianHomeValueGrowthPct(Double medianHomeValueGrowthPct) { this.medianHomeValueGrowthPct = medianHomeValueGrowthPct; }

    public Double getMedianRentGrowthPct() { return medianRentGrowthPct; }
    public void setMedianRentGrowthPct(Double medianRentGrowthPct) { this.medianRentGrowthPct = medianRentGrowthPct; }

    public Double getHomeownershipRateGrowthPct() { return homeownershipRateGrowthPct; }
    public void setHomeownershipRateGrowthPct(Double homeownershipRateGrowthPct) { this.homeownershipRateGrowthPct = homeownershipRateGrowthPct; }

    // Employment
    public Double getUnemploymentRateGrowthPct() { return unemploymentRateGrowthPct; }
    public void setUnemploymentRateGrowthPct(Double unemploymentRateGrowthPct) { this.unemploymentRateGrowthPct = unemploymentRateGrowthPct; }

    public Double getLaborForceParticipationGrowthPct() { return laborForceParticipationGrowthPct; }
    public void setLaborForceParticipationGrowthPct(Double laborForceParticipationGrowthPct) { this.laborForceParticipationGrowthPct = laborForceParticipationGrowthPct; }

    public Double getWorkFromHomePctGrowthPct() { return workFromHomePctGrowthPct; }
    public void setWorkFromHomePctGrowthPct(Double workFromHomePctGrowthPct) { this.workFromHomePctGrowthPct = workFromHomePctGrowthPct; }

    // Sex
    public Double getMalePctGrowthPct() { return malePctGrowthPct; }
    public void setMalePctGrowthPct(Double malePctGrowthPct) { this.malePctGrowthPct = malePctGrowthPct; }

    public Double getFemalePctGrowthPct() { return femalePctGrowthPct; }
    public void setFemalePctGrowthPct(Double femalePctGrowthPct) { this.femalePctGrowthPct = femalePctGrowthPct; }

    // Race
    public Double getWhitePctGrowthPct() { return whitePctGrowthPct; }
    public void setWhitePctGrowthPct(Double whitePctGrowthPct) { this.whitePctGrowthPct = whitePctGrowthPct; }

    public Double getBlackPctGrowthPct() { return blackPctGrowthPct; }
    public void setBlackPctGrowthPct(Double blackPctGrowthPct) { this.blackPctGrowthPct = blackPctGrowthPct; }

    public Double getAsianPctGrowthPct() { return asianPctGrowthPct; }
    public void setAsianPctGrowthPct(Double asianPctGrowthPct) { this.asianPctGrowthPct = asianPctGrowthPct; }

    public Double getNativeAmericanPctGrowthPct() { return nativeAmericanPctGrowthPct; }
    public void setNativeAmericanPctGrowthPct(Double nativeAmericanPctGrowthPct) { this.nativeAmericanPctGrowthPct = nativeAmericanPctGrowthPct; }

    public Double getPacificIslanderPctGrowthPct() { return pacificIslanderPctGrowthPct; }
    public void setPacificIslanderPctGrowthPct(Double pacificIslanderPctGrowthPct) { this.pacificIslanderPctGrowthPct = pacificIslanderPctGrowthPct; }

    public Double getTwoOrMoreRacesPctGrowthPct() { return twoOrMoreRacesPctGrowthPct; }
    public void setTwoOrMoreRacesPctGrowthPct(Double twoOrMoreRacesPctGrowthPct) { this.twoOrMoreRacesPctGrowthPct = twoOrMoreRacesPctGrowthPct; }

    public Double getOtherRacePctGrowthPct() { return otherRacePctGrowthPct; }
    public void setOtherRacePctGrowthPct(Double otherRacePctGrowthPct) { this.otherRacePctGrowthPct = otherRacePctGrowthPct; }

    // Ethnicity
    public Double getHispanicPctGrowthPct() { return hispanicPctGrowthPct; }
    public void setHispanicPctGrowthPct(Double hispanicPctGrowthPct) { this.hispanicPctGrowthPct = hispanicPctGrowthPct; }

    // Classification
    public Double getUrbanizationIndexGrowthPct() { return urbanizationIndexGrowthPct; }
    public void setUrbanizationIndexGrowthPct(Double urbanizationIndexGrowthPct) { this.urbanizationIndexGrowthPct = urbanizationIndexGrowthPct; }

    // Diversity
    public Double getDiversityIndexGrowthPct() { return diversityIndexGrowthPct; }
    public void setDiversityIndexGrowthPct(Double diversityIndexGrowthPct) { this.diversityIndexGrowthPct = diversityIndexGrowthPct; }
}