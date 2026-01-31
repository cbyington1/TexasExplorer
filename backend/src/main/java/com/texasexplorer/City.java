package com.texasexplorer;

import jakarta.persistence.*;
import java.time.LocalDateTime;


//@Entity defines this class as a database table
@Entity

/*@Table let's you name and specify constraints. We have the 
constraint geoid and year because we have multiple years for each city so
we need both the location and the year to differentiate*/
@Table(name = "cities", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"geoid", "year"})
})
public class City {

    //Creates unique id for each record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Identifiers
    private String geoid;
    private String name;
    private Integer year;
    
    //Geographic
    private Double latitude;
    private Double longitude;
    private Double landAreaSqMi;
    private Double waterAreaSqMi;
    
    //Demographic
    private Integer population;
    private Integer malePopulation;
    private Integer femalePopulation;
    private Double medianAge;
    private Integer ageUnder5;
    private Integer ageUnder18;
    private Integer age18to24;
    private Integer age25to44;
    private Integer age45to64;
    private Integer age65plus;
    
    //Race
    private Integer whitePopulation;
    private Integer blackPopulation;
    private Integer nativeAmericanPopulation;
    private Integer asianPopulation;
    private Integer pacificIslanderPopulation;
    private Integer otherRacePopulation;
    private Integer twoOrMoreRacesPopulation;
    private Integer hispanicPopulation;
    
    //Nativity
    private Integer foreignBorn;
    private Integer naturalizedCitizen;
    private Integer nonCitizen;
    
    //Language
    private Integer speakOnlyEnglish;
    private Integer speakSpanish;
    
    //Veterans
    private Integer veterans;

    //Economics
    private Integer medianHouseholdIncome;
    private Integer perCapitaIncome;
    private Integer incomeUnder25k;
    private Integer income25kTo50k;
    private Integer income50kTo100k;
    private Integer income100kTo200k;
    private Integer income200kPlus;
    private Integer povertyTotal;
    private Integer povertyChildren;
    private Integer snapHouseholds;
    
    //Employment
    private Integer employed;
    private Integer unemployed;
    private Integer laborForce;
    private Integer notInLaborForce;

    //Education
    private Integer eduNoHighSchool;
    private Integer eduHighSchoolOnly;
    private Integer eduSomeCollege;
    private Integer eduBachelors;
    private Integer eduMasters;
    private Integer eduDoctorate;

    //Housing
    private Integer medianHomeValue;
    private Integer medianRent;
    private Integer ownerOccupied;
    private Integer renterOccupied;
    private Integer vacantUnits;

    //Commute
    private Integer workFromHome;
    private Integer driveAlone;
    private Integer publicTransit;
    private Double meanCommuteMinutes;

    // Metadata
    private LocalDateTime lastUpdated;


    public City() {}

    //Getters and Setters
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGeoid() { return geoid; }
    public void setGeoid(String geoid) { this.geoid = geoid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLandAreaSqMi() { return landAreaSqMi; }
    public void setLandAreaSqMi(Double landAreaSqMi) { this.landAreaSqMi = landAreaSqMi; }

    public Double getWaterAreaSqMi() { return waterAreaSqMi; }
    public void setWaterAreaSqMi(Double waterAreaSqMi) { this.waterAreaSqMi = waterAreaSqMi; }

    public Integer getPopulation() { return population; }
    public void setPopulation(Integer population) { this.population = population; }

    public Integer getMalePopulation() { return malePopulation; }
    public void setMalePopulation(Integer malePopulation) { this.malePopulation = malePopulation; }

    public Integer getFemalePopulation() { return femalePopulation; }
    public void setFemalePopulation(Integer femalePopulation) { this.femalePopulation = femalePopulation; }

    public Double getMedianAge() { return medianAge; }
    public void setMedianAge(Double medianAge) { this.medianAge = medianAge; }

    public Integer getAgeUnder5() { return ageUnder5; }
    public void setAgeUnder5(Integer ageUnder5) { this.ageUnder5 = ageUnder5; }

    public Integer getAgeUnder18() { return ageUnder18; }
    public void setAgeUnder18(Integer ageUnder18) { this.ageUnder18 = ageUnder18; }

    public Integer getAge18to24() { return age18to24; }
    public void setAge18to24(Integer age18to24) { this.age18to24 = age18to24; }

    public Integer getAge25to44() { return age25to44; }
    public void setAge25to44(Integer age25to44) { this.age25to44 = age25to44; }

    public Integer getAge45to64() { return age45to64; }
    public void setAge45to64(Integer age45to64) { this.age45to64 = age45to64; }

    public Integer getAge65plus() { return age65plus; }
    public void setAge65plus(Integer age65plus) { this.age65plus = age65plus; }

    public Integer getWhitePopulation() { return whitePopulation; }
    public void setWhitePopulation(Integer whitePopulation) { this.whitePopulation = whitePopulation; }

    public Integer getBlackPopulation() { return blackPopulation; }
    public void setBlackPopulation(Integer blackPopulation) { this.blackPopulation = blackPopulation; }

    public Integer getNativeAmericanPopulation() { return nativeAmericanPopulation; }
    public void setNativeAmericanPopulation(Integer nativeAmericanPopulation) { this.nativeAmericanPopulation = nativeAmericanPopulation; }

    public Integer getAsianPopulation() { return asianPopulation; }
    public void setAsianPopulation(Integer asianPopulation) { this.asianPopulation = asianPopulation; }

    public Integer getPacificIslanderPopulation() { return pacificIslanderPopulation; }
    public void setPacificIslanderPopulation(Integer pacificIslanderPopulation) { this.pacificIslanderPopulation = pacificIslanderPopulation; }

    public Integer getOtherRacePopulation() { return otherRacePopulation; }
    public void setOtherRacePopulation(Integer otherRacePopulation) { this.otherRacePopulation = otherRacePopulation; }

    public Integer getTwoOrMoreRacesPopulation() { return twoOrMoreRacesPopulation; }
    public void setTwoOrMoreRacesPopulation(Integer twoOrMoreRacesPopulation) { this.twoOrMoreRacesPopulation = twoOrMoreRacesPopulation; }

    public Integer getHispanicPopulation() { return hispanicPopulation; }
    public void setHispanicPopulation(Integer hispanicPopulation) { this.hispanicPopulation = hispanicPopulation; }

    public Integer getForeignBorn() { return foreignBorn; }
    public void setForeignBorn(Integer foreignBorn) { this.foreignBorn = foreignBorn; }

    public Integer getNaturalizedCitizen() { return naturalizedCitizen; }
    public void setNaturalizedCitizen(Integer naturalizedCitizen) { this.naturalizedCitizen = naturalizedCitizen; }

    public Integer getNonCitizen() { return nonCitizen; }
    public void setNonCitizen(Integer nonCitizen) { this.nonCitizen = nonCitizen; }

    public Integer getSpeakOnlyEnglish() { return speakOnlyEnglish; }
    public void setSpeakOnlyEnglish(Integer speakOnlyEnglish) { this.speakOnlyEnglish = speakOnlyEnglish; }

    public Integer getSpeakSpanish() { return speakSpanish; }
    public void setSpeakSpanish(Integer speakSpanish) { this.speakSpanish = speakSpanish; }

    public Integer getVeterans() { return veterans; }
    public void setVeterans(Integer veterans) { this.veterans = veterans; }

    public Integer getMedianHouseholdIncome() { return medianHouseholdIncome; }
    public void setMedianHouseholdIncome(Integer medianHouseholdIncome) { this.medianHouseholdIncome = medianHouseholdIncome; }

    public Integer getPerCapitaIncome() { return perCapitaIncome; }
    public void setPerCapitaIncome(Integer perCapitaIncome) { this.perCapitaIncome = perCapitaIncome; }

    public Integer getIncomeUnder25k() { return incomeUnder25k; }
    public void setIncomeUnder25k(Integer incomeUnder25k) { this.incomeUnder25k = incomeUnder25k; }

    public Integer getIncome25kTo50k() { return income25kTo50k; }
    public void setIncome25kTo50k(Integer income25kTo50k) { this.income25kTo50k = income25kTo50k; }

    public Integer getIncome50kTo100k() { return income50kTo100k; }
    public void setIncome50kTo100k(Integer income50kTo100k) { this.income50kTo100k = income50kTo100k; }

    public Integer getIncome100kTo200k() { return income100kTo200k; }
    public void setIncome100kTo200k(Integer income100kTo200k) { this.income100kTo200k = income100kTo200k; }

    public Integer getIncome200kPlus() { return income200kPlus; }
    public void setIncome200kPlus(Integer income200kPlus) { this.income200kPlus = income200kPlus; }

    public Integer getPovertyTotal() { return povertyTotal; }
    public void setPovertyTotal(Integer povertyTotal) { this.povertyTotal = povertyTotal; }

    public Integer getPovertyChildren() { return povertyChildren; }
    public void setPovertyChildren(Integer povertyChildren) { this.povertyChildren = povertyChildren; }

    public Integer getSnapHouseholds() { return snapHouseholds; }
    public void setSnapHouseholds(Integer snapHouseholds) { this.snapHouseholds = snapHouseholds; }

    public Integer getEmployed() { return employed; }
    public void setEmployed(Integer employed) { this.employed = employed; }

    public Integer getUnemployed() { return unemployed; }
    public void setUnemployed(Integer unemployed) { this.unemployed = unemployed; }

    public Integer getLaborForce() { return laborForce; }
    public void setLaborForce(Integer laborForce) { this.laborForce = laborForce; }

    public Integer getNotInLaborForce() { return notInLaborForce; }
    public void setNotInLaborForce(Integer notInLaborForce) { this.notInLaborForce = notInLaborForce; }

    public Integer getEduNoHighSchool() { return eduNoHighSchool; }
    public void setEduNoHighSchool(Integer eduNoHighSchool) { this.eduNoHighSchool = eduNoHighSchool; }

    public Integer getEduHighSchoolOnly() { return eduHighSchoolOnly; }
    public void setEduHighSchoolOnly(Integer eduHighSchoolOnly) { this.eduHighSchoolOnly = eduHighSchoolOnly; }

    public Integer getEduSomeCollege() { return eduSomeCollege; }
    public void setEduSomeCollege(Integer eduSomeCollege) { this.eduSomeCollege = eduSomeCollege; }

    public Integer getEduBachelors() { return eduBachelors; }
    public void setEduBachelors(Integer eduBachelors) { this.eduBachelors = eduBachelors; }

    public Integer getEduMasters() { return eduMasters; }
    public void setEduMasters(Integer eduMasters) { this.eduMasters = eduMasters; }

    public Integer getEduDoctorate() { return eduDoctorate; }
    public void setEduDoctorate(Integer eduDoctorate) { this.eduDoctorate = eduDoctorate; }

    public Integer getMedianHomeValue() { return medianHomeValue; }
    public void setMedianHomeValue(Integer medianHomeValue) { this.medianHomeValue = medianHomeValue; }

    public Integer getMedianRent() { return medianRent; }
    public void setMedianRent(Integer medianRent) { this.medianRent = medianRent; }

    public Integer getOwnerOccupied() { return ownerOccupied; }
    public void setOwnerOccupied(Integer ownerOccupied) { this.ownerOccupied = ownerOccupied; }

    public Integer getRenterOccupied() { return renterOccupied; }
    public void setRenterOccupied(Integer renterOccupied) { this.renterOccupied = renterOccupied; }

    public Integer getVacantUnits() { return vacantUnits; }
    public void setVacantUnits(Integer vacantUnits) { this.vacantUnits = vacantUnits; }

    public Integer getWorkFromHome() { return workFromHome; }
    public void setWorkFromHome(Integer workFromHome) { this.workFromHome = workFromHome; }

    public Integer getDriveAlone() { return driveAlone; }
    public void setDriveAlone(Integer driveAlone) { this.driveAlone = driveAlone; }

    public Integer getPublicTransit() { return publicTransit; }
    public void setPublicTransit(Integer publicTransit) { this.publicTransit = publicTransit; }

    public Double getMeanCommuteMinutes() { return meanCommuteMinutes; }
    public void setMeanCommuteMinutes(Double meanCommuteMinutes) { this.meanCommuteMinutes = meanCommuteMinutes; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}