import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { City } from './city.model';

export interface DerivedStats {
  id: number;
  geoid: string;
  year: number;
  urbanizationIndex: number;
  classification: string;
  populationScore: number;
  densityScore: number;
  proximityScore: number;
  incomeRatioScore: number;
  diversityIndex: number;
}

export interface CityTrend {
  geoid: string;
  name: string;
  currentYear: number;
  baseYear: number;
  populationGrowthPct: number | null;
  medianAgeGrowthPct: number | null;
  medianIncomeGrowthPct: number | null;
  perCapitaIncomeGrowthPct: number | null;
  medianHomeValueGrowthPct: number | null;
  medianRentGrowthPct: number | null;
  homeownershipRateGrowthPct: number | null;
  unemploymentRateGrowthPct: number | null;
  laborForceParticipationGrowthPct: number | null;
  workFromHomePctGrowthPct: number | null;
  malePctGrowthPct: number | null;
  femalePctGrowthPct: number | null;
  whitePctGrowthPct: number | null;
  blackPctGrowthPct: number | null;
  asianPctGrowthPct: number | null;
  nativeAmericanPctGrowthPct: number | null;
  pacificIslanderPctGrowthPct: number | null;
  twoOrMoreRacesPctGrowthPct: number | null;
  otherRacePctGrowthPct: number | null;
  hispanicPctGrowthPct: number | null;
  urbanizationIndexGrowthPct: number | null;
  diversityIndexGrowthPct: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class CityService {
  private apiUrl = 'https://texasexplorer-production.up.railway.app/api/cities';

  constructor(private http: HttpClient) {}

  // Get all available years
  getAvailableYears(): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/years`);
  }

  // Get all cities for a specific year
  getCitiesForYear(year: number): Observable<City[]> {
    return this.http.get<City[]>(`${this.apiUrl}/${year}`);
  }

  // Get top 10 cities by population for a year
  getTopCities(year: number): Observable<City[]> {
    return this.http.get<City[]>(`${this.apiUrl}/${year}/top`);
  }

  // Search cities by name
  searchCities(year: number, query: string): Observable<City[]> {
    return this.http.get<City[]>(`${this.apiUrl}/${year}/search?q=${query}`);
  }

  // Get cities above population threshold
  getCitiesAbovePopulation(year: number, minPopulation: number): Observable<City[]> {
    return this.http.get<City[]>(`${this.apiUrl}/${year}/population/${minPopulation}`);
  }

  // Get all years for a specific city (history)
  getCityHistory(geoid: string): Observable<City[]> {
    return this.http.get<City[]>(`${this.apiUrl}/history/${geoid}`);
  }

  // Get specific city for specific year
  getCityForYear(geoid: string, year: number): Observable<City> {
    return this.http.get<City>(`${this.apiUrl}/city/${geoid}/${year}`);
  }

  // Get database statistics
  getStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats`);
  }

  // Compare city across years
  compareCityAcrossYears(cityName: string, years: number[]): Observable<any> {
    const yearsParam = years.join(',');
    return this.http.get<any>(`${this.apiUrl}/compare?city=${cityName}&years=${yearsParam}`);
  }

  // Get Texas-wide statistics for a year
  getTexasStats(year: number): Observable<any> {
    return this.http.get<any>(`https://texasexplorer-production.up.railway.app/api/texas-stats/${year}`);
  }

  // Get derived stats (classification, urbanization index) for a year
  getDerivedStats(year: number): Observable<DerivedStats[]> {
    return this.http.get<DerivedStats[]>(`https://texasexplorer-production.up.railway.app/api/derived/${year}`);
  }

  // Get derived stats history for a single city
  getDerivedHistory(geoid: string): Observable<DerivedStats[]> {
    return this.http.get<DerivedStats[]>(`https://texasexplorer-production.up.railway.app/api/derived/history/${geoid}`);
  }

  // Get trend data comparing two years
  getTrends(currentYear: number, baseYear: number): Observable<CityTrend[]> {
    return this.http.get<CityTrend[]>(`https://texasexplorer-production.up.railway.app/api/derived/trends?current=${currentYear}&base=${baseYear}`);
  }
}