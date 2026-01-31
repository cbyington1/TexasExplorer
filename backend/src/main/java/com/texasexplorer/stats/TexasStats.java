package com.texasexplorer.stats;

import jakarta.persistence.*;

@Entity
@Table(name = "texas_stats")
public class TexasStats {
    
    @Id
    private Integer year;
    
    // Total counts
    private Long totalPopulation;
    private Long totalMale;
    private Long totalFemale;
    
    // Weighted averages (medians)
    private Double medianAge;
    private Double medianHouseholdIncome;
    private Double perCapitaIncome;
    private Double medianHomeValue;
    private Double medianRent;
    private Double meanCommuteMinutes;
    
    // Race totals
    private Long whitePopulation;
    private Long blackPopulation;
    private Long nativeAmericanPopulation;
    private Long asianPopulation;
    private Long pacificIslanderPopulation;
    private Long otherRacePopulation;
    private Long twoOrMoreRacesPopulation;
    private Long hispanicPopulation;
    
    // Age groups
    private Long ageUnder5;
    private Long ageUnder18;
    private Long age18to24;
    private Long age25to44;
    private Long age45to64;
    private Long age65plus;
    
    // Nativity
    private Long foreignBorn;
    private Long naturalizedCitizen;
    private Long nonCitizen;
    
    // Language
    private Long speakOnlyEnglish;
    private Long speakSpanish;
    
    // Veterans
    private Long veterans;
    
    // Income distribution
    private Long incomeUnder25k;
    private Long income25kTo50k;
    private Long income50kTo100k;
    private Long income100kTo200k;
    private Long income200kPlus;
    
    // Poverty
    private Long povertyTotal;
    private Long povertyChildren;
    private Long snapHouseholds;
    
    // Employment
    private Long employed;
    private Long unemployed;
    private Long laborForce;
    private Long notInLaborForce;
    
    // Education
    private Long eduNoHighSchool;
    private Long eduHighSchoolOnly;
    private Long eduSomeCollege;
    private Long eduBachelors;
    private Long eduMasters;
    private Long eduDoctorate;
    
    // Housing
    private Long ownerOccupied;
    private Long renterOccupied;
    private Long vacantUnits;
    
    // Commute
    private Long workFromHome;
    private Long driveAlone;
    private Long publicTransit;
    
    // Constructors
    public TexasStats() {}
    
    public TexasStats(Integer year) {
        this.year = year;
    }
    
    // Getters and Setters
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public Long getTotalPopulation() { return totalPopulation; }
    public void setTotalPopulation(Long totalPopulation) { this.totalPopulation = totalPopulation; }
    
    public Long getTotalMale() { return totalMale; }
    public void setTotalMale(Long totalMale) { this.totalMale = totalMale; }
    
    public Long getTotalFemale() { return totalFemale; }
    public void setTotalFemale(Long totalFemale) { this.totalFemale = totalFemale; }
    
    public Double getMedianAge() { return medianAge; }
    public void setMedianAge(Double medianAge) { this.medianAge = medianAge; }
    
    public Double getMedianHouseholdIncome() { return medianHouseholdIncome; }
    public void setMedianHouseholdIncome(Double medianHouseholdIncome) { this.medianHouseholdIncome = medianHouseholdIncome; }
    
    public Double getPerCapitaIncome() { return perCapitaIncome; }
    public void setPerCapitaIncome(Double perCapitaIncome) { this.perCapitaIncome = perCapitaIncome; }
    
    public Double getMedianHomeValue() { return medianHomeValue; }
    public void setMedianHomeValue(Double medianHomeValue) { this.medianHomeValue = medianHomeValue; }
    
    public Double getMedianRent() { return medianRent; }
    public void setMedianRent(Double medianRent) { this.medianRent = medianRent; }
    
    public Double getMeanCommuteMinutes() { return meanCommuteMinutes; }
    public void setMeanCommuteMinutes(Double meanCommuteMinutes) { this.meanCommuteMinutes = meanCommuteMinutes; }
    
    public Long getWhitePopulation() { return whitePopulation; }
    public void setWhitePopulation(Long whitePopulation) { this.whitePopulation = whitePopulation; }
    
    public Long getBlackPopulation() { return blackPopulation; }
    public void setBlackPopulation(Long blackPopulation) { this.blackPopulation = blackPopulation; }
    
    public Long getNativeAmericanPopulation() { return nativeAmericanPopulation; }
    public void setNativeAmericanPopulation(Long nativeAmericanPopulation) { this.nativeAmericanPopulation = nativeAmericanPopulation; }
    
    public Long getAsianPopulation() { return asianPopulation; }
    public void setAsianPopulation(Long asianPopulation) { this.asianPopulation = asianPopulation; }
    
    public Long getPacificIslanderPopulation() { return pacificIslanderPopulation; }
    public void setPacificIslanderPopulation(Long pacificIslanderPopulation) { this.pacificIslanderPopulation = pacificIslanderPopulation; }
    
    public Long getOtherRacePopulation() { return otherRacePopulation; }
    public void setOtherRacePopulation(Long otherRacePopulation) { this.otherRacePopulation = otherRacePopulation; }
    
    public Long getTwoOrMoreRacesPopulation() { return twoOrMoreRacesPopulation; }
    public void setTwoOrMoreRacesPopulation(Long twoOrMoreRacesPopulation) { this.twoOrMoreRacesPopulation = twoOrMoreRacesPopulation; }
    
    public Long getHispanicPopulation() { return hispanicPopulation; }
    public void setHispanicPopulation(Long hispanicPopulation) { this.hispanicPopulation = hispanicPopulation; }
    
    public Long getAgeUnder5() { return ageUnder5; }
    public void setAgeUnder5(Long ageUnder5) { this.ageUnder5 = ageUnder5; }
    
    public Long getAgeUnder18() { return ageUnder18; }
    public void setAgeUnder18(Long ageUnder18) { this.ageUnder18 = ageUnder18; }
    
    public Long getAge18to24() { return age18to24; }
    public void setAge18to24(Long age18to24) { this.age18to24 = age18to24; }
    
    public Long getAge25to44() { return age25to44; }
    public void setAge25to44(Long age25to44) { this.age25to44 = age25to44; }
    
    public Long getAge45to64() { return age45to64; }
    public void setAge45to64(Long age45to64) { this.age45to64 = age45to64; }
    
    public Long getAge65plus() { return age65plus; }
    public void setAge65plus(Long age65plus) { this.age65plus = age65plus; }
    
    public Long getForeignBorn() { return foreignBorn; }
    public void setForeignBorn(Long foreignBorn) { this.foreignBorn = foreignBorn; }
    
    public Long getNaturalizedCitizen() { return naturalizedCitizen; }
    public void setNaturalizedCitizen(Long naturalizedCitizen) { this.naturalizedCitizen = naturalizedCitizen; }
    
    public Long getNonCitizen() { return nonCitizen; }
    public void setNonCitizen(Long nonCitizen) { this.nonCitizen = nonCitizen; }
    
    public Long getSpeakOnlyEnglish() { return speakOnlyEnglish; }
    public void setSpeakOnlyEnglish(Long speakOnlyEnglish) { this.speakOnlyEnglish = speakOnlyEnglish; }
    
    public Long getSpeakSpanish() { return speakSpanish; }
    public void setSpeakSpanish(Long speakSpanish) { this.speakSpanish = speakSpanish; }
    
    public Long getVeterans() { return veterans; }
    public void setVeterans(Long veterans) { this.veterans = veterans; }
    
    public Long getIncomeUnder25k() { return incomeUnder25k; }
    public void setIncomeUnder25k(Long incomeUnder25k) { this.incomeUnder25k = incomeUnder25k; }
    
    public Long getIncome25kTo50k() { return income25kTo50k; }
    public void setIncome25kTo50k(Long income25kTo50k) { this.income25kTo50k = income25kTo50k; }
    
    public Long getIncome50kTo100k() { return income50kTo100k; }
    public void setIncome50kTo100k(Long income50kTo100k) { this.income50kTo100k = income50kTo100k; }
    
    public Long getIncome100kTo200k() { return income100kTo200k; }
    public void setIncome100kTo200k(Long income100kTo200k) { this.income100kTo200k = income100kTo200k; }
    
    public Long getIncome200kPlus() { return income200kPlus; }
    public void setIncome200kPlus(Long income200kPlus) { this.income200kPlus = income200kPlus; }
    
    public Long getPovertyTotal() { return povertyTotal; }
    public void setPovertyTotal(Long povertyTotal) { this.povertyTotal = povertyTotal; }
    
    public Long getPovertyChildren() { return povertyChildren; }
    public void setPovertyChildren(Long povertyChildren) { this.povertyChildren = povertyChildren; }
    
    public Long getSnapHouseholds() { return snapHouseholds; }
    public void setSnapHouseholds(Long snapHouseholds) { this.snapHouseholds = snapHouseholds; }
    
    public Long getEmployed() { return employed; }
    public void setEmployed(Long employed) { this.employed = employed; }
    
    public Long getUnemployed() { return unemployed; }
    public void setUnemployed(Long unemployed) { this.unemployed = unemployed; }
    
    public Long getLaborForce() { return laborForce; }
    public void setLaborForce(Long laborForce) { this.laborForce = laborForce; }
    
    public Long getNotInLaborForce() { return notInLaborForce; }
    public void setNotInLaborForce(Long notInLaborForce) { this.notInLaborForce = notInLaborForce; }
    
    public Long getEduNoHighSchool() { return eduNoHighSchool; }
    public void setEduNoHighSchool(Long eduNoHighSchool) { this.eduNoHighSchool = eduNoHighSchool; }
    
    public Long getEduHighSchoolOnly() { return eduHighSchoolOnly; }
    public void setEduHighSchoolOnly(Long eduHighSchoolOnly) { this.eduHighSchoolOnly = eduHighSchoolOnly; }
    
    public Long getEduSomeCollege() { return eduSomeCollege; }
    public void setEduSomeCollege(Long eduSomeCollege) { this.eduSomeCollege = eduSomeCollege; }
    
    public Long getEduBachelors() { return eduBachelors; }
    public void setEduBachelors(Long eduBachelors) { this.eduBachelors = eduBachelors; }
    
    public Long getEduMasters() { return eduMasters; }
    public void setEduMasters(Long eduMasters) { this.eduMasters = eduMasters; }
    
    public Long getEduDoctorate() { return eduDoctorate; }
    public void setEduDoctorate(Long eduDoctorate) { this.eduDoctorate = eduDoctorate; }
    
    public Long getOwnerOccupied() { return ownerOccupied; }
    public void setOwnerOccupied(Long ownerOccupied) { this.ownerOccupied = ownerOccupied; }
    
    public Long getRenterOccupied() { return renterOccupied; }
    public void setRenterOccupied(Long renterOccupied) { this.renterOccupied = renterOccupied; }
    
    public Long getVacantUnits() { return vacantUnits; }
    public void setVacantUnits(Long vacantUnits) { this.vacantUnits = vacantUnits; }
    
    public Long getWorkFromHome() { return workFromHome; }
    public void setWorkFromHome(Long workFromHome) { this.workFromHome = workFromHome; }
    
    public Long getDriveAlone() { return driveAlone; }
    public void setDriveAlone(Long driveAlone) { this.driveAlone = driveAlone; }
    
    public Long getPublicTransit() { return publicTransit; }
    public void setPublicTransit(Long publicTransit) { this.publicTransit = publicTransit; }
}