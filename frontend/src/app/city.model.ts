export interface City {
  // Identifiers
  id: number;
  geoid: string;
  name: string;
  year: number;
  
  // Geographic
  latitude: number;
  longitude: number;
  landAreaSqMi: number;
  waterAreaSqMi: number;
  
  // Demographics
  population: number;
  malePopulation: number;
  femalePopulation: number;
  medianAge: number;
  ageUnder5: number;
  ageUnder18: number;
  age18to24: number;
  age25to44: number;
  age45to64: number;
  age65plus: number;
  
  // Race
  whitePopulation: number;
  blackPopulation: number;
  nativeAmericanPopulation: number;
  asianPopulation: number;
  pacificIslanderPopulation: number;
  otherRacePopulation: number;
  twoOrMoreRacesPopulation: number;
  hispanicPopulation: number;
  
  // Nativity
  foreignBorn: number;
  naturalizedCitizen: number;
  nonCitizen: number;
  
  // Language
  speakOnlyEnglish: number;
  speakSpanish: number;
  
  // Veterans
  veterans: number;
  
  // Economic
  medianHouseholdIncome: number;  // ← Fixed! Was medianIncome
  perCapitaIncome: number;
  incomeUnder25k: number;
  income25kTo50k: number;
  income50kTo100k: number;
  income100kTo200k: number;
  income200kPlus: number;
  povertyTotal: number;
  povertyChildren: number;
  snapHouseholds: number;
  
  // Employment
  employed: number;
  unemployed: number;
  laborForce: number;
  notInLaborForce: number;
  
  // Education
  eduNoHighSchool: number;
  eduHighSchoolOnly: number;
  eduSomeCollege: number;
  eduBachelors: number;
  eduMasters: number;
  eduDoctorate: number;
  
  // Housing
  medianHomeValue: number;
  medianRent: number;
  ownerOccupied: number;
  renterOccupied: number;
  vacantUnits: number;
  
  // Commute
  workFromHome: number;
  driveAlone: number;
  publicTransit: number;
  meanCommuteMinutes: number;
  
  // Metadata
  lastUpdated: string;
}