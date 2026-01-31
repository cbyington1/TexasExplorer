import { Component, OnInit, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { CityService } from '../city.service';
import { City } from '../city.model';

// Filter metric definition
interface FilterMetric {
  key: string;
  label: string;
  type: 'raw' | 'percent' | 'currency' | 'years' | 'minutes';
  field: keyof City;
  percentOf?: keyof City; // For percentage calculations
  min: number;
  max: number;
  step: number;
}

// Filter category with metrics
interface FilterCategory {
  key: string;
  label: string;
  icon: string;
  expanded: boolean;
  metrics: FilterMetric[];
}

// Active filter range
interface ActiveFilter {
  metricKey: string;
  min: number;
  max: number;
}

// Color configuration
interface ColorConfig {
  metricKey: string;
  highIsGreen: boolean;
}

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css']
})
export class MapComponent implements OnInit, AfterViewInit {
  private map!: L.Map;
  private markers: L.CircleMarker[] = [];
  private allCities: City[] = [];
  
  visibleCities: City[] = [];
  filteredCities: City[] = []; // Cities after filter applied, before zoom threshold
  selectedCity: City | null = null;
  loading = true;
  cityCount = 0;
  filteredCount = 0; // Total cities matching filters (regardless of zoom)
  
  availableYears: number[] = [];
  selectedYear: number = 2024;

  searchQuery: string = '';
  searchResults: City[] = [];
  
  texasStats: any = null;

  // Data quality filter
  requireCompleteData: boolean = true;

  // List panel
  showListPanel: boolean = false;
  sortedCitiesList: City[] = [];

  // Filter system
  filterCategories: FilterCategory[] = [];
  activeFilters: Map<string, ActiveFilter> = new Map();
  filterValues: Map<string, { min: number; max: number }> = new Map();
  
  // Color/Legend system
  colorConfig: ColorConfig = {
    metricKey: 'medianHouseholdIncome',
    highIsGreen: true
  };
  
  // Size system
  sizeConfig: ColorConfig = {
    metricKey: 'population',
    highIsGreen: true  // highIsGreen here means "high is big"
  };
  
  // For template access to current metrics
  currentColorMetric: FilterMetric | null = null;
  currentSizeMetric: FilterMetric | null = null;
  
  // Debounce timer for slider changes
  private filterDebounceTimer: any = null;

  constructor(
    private cityService: CityService, 
    private cdr: ChangeDetectorRef
  ) {
    this.initFilterCategories();
  }

  ngOnInit(): void {
    this.loadAvailableYears();
    this.loadTexasStats(2024);
    this.updateCurrentColorMetric();
    this.updateCurrentSizeMetric();
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  private initFilterCategories(): void {
    this.filterCategories = [
      {
        key: 'population',
        label: 'Population & Age',
        icon: '👥',
        expanded: false,
        metrics: [
          { key: 'population', label: 'Population', type: 'raw', field: 'population', min: 100, max: 300000, step: 1000 },
          { key: 'medianAge', label: 'Median Age', type: 'years', field: 'medianAge', min: 18, max: 70, step: 1 },
          { key: 'ageUnder18Pct', label: 'Under 18', type: 'percent', field: 'ageUnder18', percentOf: 'population', min: 0, max: 45, step: 1 },
          { key: 'age65plusPct', label: 'Age 65+', type: 'percent', field: 'age65plus', percentOf: 'population', min: 0, max: 50, step: 1 },
        ]
      },
      {
        key: 'sex',
        label: 'Sex',
        icon: '⚤',
        expanded: false,
        metrics: [
          { key: 'malePct', label: 'Male', type: 'percent', field: 'malePopulation', percentOf: 'population', min: 25, max: 75, step: 1 },
          { key: 'femalePct', label: 'Female', type: 'percent', field: 'femalePopulation', percentOf: 'population', min: 25, max: 75, step: 1 },
        ]
      },
      {
        key: 'race',
        label: 'Race',
        icon: '🌍',
        expanded: false,
        metrics: [
          { key: 'whitePct', label: 'White', type: 'percent', field: 'whitePopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'blackPct', label: 'Black', type: 'percent', field: 'blackPopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'asianPct', label: 'Asian', type: 'percent', field: 'asianPopulation', percentOf: 'population', min: 0, max: 50, step: 1 },
          { key: 'nativeAmericanPct', label: 'Native American', type: 'percent', field: 'nativeAmericanPopulation', percentOf: 'population', min: 0, max: 50, step: 1 },
          { key: 'pacificIslanderPct', label: 'Pacific Islander', type: 'percent', field: 'pacificIslanderPopulation', percentOf: 'population', min: 0, max: 20, step: 1 },
          { key: 'twoOrMorePct', label: 'Two or More Races', type: 'percent', field: 'twoOrMoreRacesPopulation', percentOf: 'population', min: 0, max: 30, step: 1 },
          { key: 'otherRacePct', label: 'Other Race', type: 'percent', field: 'otherRacePopulation', percentOf: 'population', min: 0, max: 50, step: 1 },
        ]
      },
      {
        key: 'ethnicity',
        label: 'Ethnicity',
        icon: '🌎',
        expanded: false,
        metrics: [
          { key: 'hispanicPct', label: 'Hispanic/Latino', type: 'percent', field: 'hispanicPopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
        ]
      },
      {
        key: 'economic',
        label: 'Economic',
        icon: '💰',
        expanded: false,
        metrics: [
          { key: 'medianHouseholdIncome', label: 'Median Household Income', type: 'currency', field: 'medianHouseholdIncome', min: 25000, max: 125000, step: 5000 },
          { key: 'perCapitaIncome', label: 'Per Capita Income', type: 'currency', field: 'perCapitaIncome', min: 15000, max: 75000, step: 2500 },
          { key: 'povertyPct', label: 'Poverty Rate', type: 'percent', field: 'povertyTotal', percentOf: 'population', min: 0, max: 50, step: 1 },
        ]
      },
      {
        key: 'employment',
        label: 'Employment',
        icon: '💼',
        expanded: false,
        metrics: [
          { key: 'unemploymentPct', label: 'Unemployment Rate', type: 'percent', field: 'unemployed', percentOf: 'laborForce', min: 0, max: 25, step: 1 },
          { key: 'laborForcePct', label: 'Labor Force Participation', type: 'percent', field: 'laborForce', percentOf: 'population', min: 30, max: 80, step: 1 },
          { key: 'workFromHomePct', label: 'Work From Home', type: 'percent', field: 'workFromHome', percentOf: 'employed', min: 0, max: 50, step: 1 },
        ]
      },
      {
        key: 'education',
        label: 'Education',
        icon: '🎓',
        expanded: false,
        metrics: [
          { key: 'noHighSchoolPct', label: 'No High School', type: 'percent', field: 'eduNoHighSchool', percentOf: 'population', min: 0, max: 40, step: 1 },
          { key: 'highSchoolOnlyPct', label: 'High School Only', type: 'percent', field: 'eduHighSchoolOnly', percentOf: 'population', min: 0, max: 50, step: 1 },
          { key: 'bachelorsPct', label: "Bachelor's Degree+", type: 'percent', field: 'eduBachelors', percentOf: 'population', min: 0, max: 50, step: 1 },
          { key: 'mastersPct', label: "Master's Degree+", type: 'percent', field: 'eduMasters', percentOf: 'population', min: 0, max: 30, step: 1 },
        ]
      },
      {
        key: 'housing',
        label: 'Housing',
        icon: '🏠',
        expanded: false,
        metrics: [
          { key: 'medianHomeValue', label: 'Median Home Value', type: 'currency', field: 'medianHomeValue', min: 50000, max: 500000, step: 25000 },
          { key: 'medianRent', label: 'Median Rent', type: 'currency', field: 'medianRent', min: 400, max: 2500, step: 50 },
          { key: 'homeownershipPct', label: 'Homeownership Rate', type: 'percent', field: 'ownerOccupied', percentOf: 'population', min: 0, max: 100, step: 1 },
        ]
      },
    ];

    // Initialize filter values to full range for each metric
    this.filterCategories.forEach(cat => {
      cat.metrics.forEach(metric => {
        this.filterValues.set(metric.key, { min: metric.min, max: metric.max });
      });
    });
  }

  private initMap(): void {
    const texasBounds = L.latLngBounds(
      [25.0, -107.5],
      [37.0, -93.0]
    );

    this.map = L.map('map', {
      center: [31.0, -100.0],
      zoom: 5.5,
      minZoom: 5.5,
      maxZoom: 12,
      maxBounds: texasBounds,
      maxBoundsViscosity: 1.0,
      zoomControl: true,
      dragging: true,
      touchZoom: true,
      scrollWheelZoom: true,
      doubleClickZoom: true,
      boxZoom: false,
      keyboard: true
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '© OpenStreetMap contributors',
      bounds: texasBounds
    }).addTo(this.map);

    this.map.on('click', () => {
      this.selectedCity = null;
      this.showListPanel = false;
      this.cdr.detectChanges();
    });

    this.map.on('zoomend', () => {
      this.updateVisibleCities();
    });
  }

  private loadAvailableYears(): void {
    this.cityService.getAvailableYears().subscribe({
      next: (years) => {
        this.availableYears = years;
        if (years.length > 0) {
          this.selectedYear = years[years.length - 1];
          this.loadCitiesForYear();
        }
      },
      error: (err: Error) => {
        console.error('Error loading years:', err);
        this.loading = false;
      }
    });
  }

  onYearChange(): void {
    this.loadCitiesForYear();
    this.loadTexasStats(this.selectedYear);
  }

  private loadTexasStats(year: number): void {
    this.cityService.getTexasStats(year).subscribe({
      next: (stats) => {
        this.texasStats = stats;
        console.log('Loaded Texas stats for', year, stats);
      },
      error: (err: Error) => {
        console.error('Error loading Texas stats:', err);
      }
    });
  }

  private loadCitiesForYear(): void {
    this.loading = true;
    this.cityService.getCitiesForYear(this.selectedYear).subscribe({
      next: (cities) => {
        this.allCities = cities;
        this.loading = false;
        this.applyFilters();
      },
      error: (err: Error) => {
        console.error('Error loading cities:', err);
        this.loading = false;
      }
    });
  }

  private getVisibilityCount(): number {
    if (!this.map) return 25;
    
    const zoom = this.map.getZoom();
    
    // Return number of cities to show at each zoom level
    if (zoom <= 6) return 25;
    if (zoom === 7) return 50;
    if (zoom === 8) return 100;
    if (zoom === 9) return 200;
    if (zoom === 10) return 400;
    return Infinity; // Show all
  }

  // Get the value for a metric from a city (handles percentages)
  getMetricValue(city: City, metric: FilterMetric): number {
    const rawValue = city[metric.field] as number;
    
    if (metric.type === 'percent' && metric.percentOf) {
      const total = city[metric.percentOf] as number;
      if (!total || total === 0) return 0;
      return (rawValue / total) * 100;
    }
    
    return rawValue || 0;
  }

  // Check if a city has sufficient data (key fields populated)
  hasCompleteData(city: City): boolean {
    // Require these essential fields to be present and non-zero
    return !!(
      city.population && city.population > 0 &&
      city.medianHouseholdIncome && city.medianHouseholdIncome > 0 &&
      city.medianAge && city.medianAge > 0 &&
      city.medianHomeValue && city.medianHomeValue > 0 &&
      city.laborForce && city.laborForce > 0
    );
  }

  // Toggle complete data filter
  toggleCompleteData(): void {
    this.requireCompleteData = !this.requireCompleteData;
    this.applyFilters();
  }

  // Toggle list panel
  toggleListPanel(): void {
    this.showListPanel = !this.showListPanel;
    if (this.showListPanel) {
      this.updateSortedList();
    }
  }

  // Update the sorted list based on current filters and size metric
  updateSortedList(): void {
    const sizeMetric = this.findMetricByKey(this.sizeConfig.metricKey);
    if (!sizeMetric) {
      this.sortedCitiesList = [...this.filteredCities];
      return;
    }

    this.sortedCitiesList = [...this.filteredCities].sort((a, b) => {
      const valueA = this.getMetricValue(a, sizeMetric);
      const valueB = this.getMetricValue(b, sizeMetric);
      
      return this.sizeConfig.highIsGreen 
        ? valueB - valueA 
        : valueA - valueB;
    });
  }

  // Get display value for a city based on a metric key
  getCityMetricDisplay(city: City, metricKey: string): string {
    const metric = this.findMetricByKey(metricKey);
    if (!metric) return 'N/A';
    const value = this.getMetricValue(city, metric);
    return this.formatMetricValue(value, metric);
  }

  // Select city from list and zoom to it
  selectCityFromList(city: City): void {
    this.selectedCity = city;
    this.showListPanel = false;
    
    if (city.latitude && city.longitude && this.map) {
      this.map.setView([city.latitude, city.longitude], 9, { animate: true });
    }
    
    this.cdr.detectChanges();
  }

  // Apply all active filters
  applyFilters(): void {
    let filtered = [...this.allCities];
    
    // Apply complete data filter first if enabled
    if (this.requireCompleteData) {
      filtered = filtered.filter(city => this.hasCompleteData(city));
    }
    
    // Apply each active filter
    this.activeFilters.forEach((filter, metricKey) => {
      const metric = this.findMetricByKey(metricKey);
      if (!metric) return;
      
      filtered = filtered.filter(city => {
        const value = this.getMetricValue(city, metric);
        return value >= filter.min && value <= filter.max;
      });
    });
    
    this.filteredCities = filtered;
    this.filteredCount = filtered.length;
    this.updateVisibleCities();
    
    // Update list if panel is open
    if (this.showListPanel) {
      this.updateSortedList();
    }
  }

  updateVisibleCities(): void {
    const maxCount = this.getVisibilityCount();
    
    // Get the size metric to determine visibility
    const sizeMetric = this.findMetricByKey(this.sizeConfig.metricKey);
    
    if (!sizeMetric || maxCount === Infinity || this.filteredCities.length <= maxCount) {
      // Show all filtered cities
      this.visibleCities = this.filteredCities;
    } else {
      // Sort by size metric and take top N
      const sorted = [...this.filteredCities].sort((a, b) => {
        const valueA = this.getMetricValue(a, sizeMetric);
        const valueB = this.getMetricValue(b, sizeMetric);
        
        // If highIsGreen (high is big), sort descending (highest first)
        // If !highIsGreen (low is big), sort ascending (lowest first)
        return this.sizeConfig.highIsGreen 
          ? valueB - valueA 
          : valueA - valueB;
      });
      
      this.visibleCities = sorted.slice(0, maxCount);
    }
    
    this.cityCount = this.visibleCities.length;
    this.addMarkers();
  }

  addMarkers(): void {
    this.markers.forEach(m => m.remove());
    this.markers = [];

    this.visibleCities.forEach(city => {
      if (city.latitude && city.longitude) {
        const radius = this.getRadius(city);
        
        const marker = L.circleMarker([city.latitude, city.longitude], {
          radius: radius,
          fillColor: this.getColorForCity(city),
          color: '#fff',
          weight: 1,
          opacity: 1,
          fillOpacity: 0.7
        }).addTo(this.map);

        marker.on('click', (e) => {
          L.DomEvent.stopPropagation(e);
          this.selectedCity = city;
          this.cdr.detectChanges();
        });

        marker.bindTooltip(
          city.name.replace(' city', '').replace(' town', ''),
          { permanent: false, direction: 'top' }
        );

        this.markers.push(marker);
      }
    });
  }

  // Dynamic radius based on selected size metric
  private getRadius(city: City): number {
    const metric = this.findMetricByKey(this.sizeConfig.metricKey);
    if (!metric) return 6;
    
    const value = this.getMetricValue(city, metric);
    
    // Get the scale range - use active filter if set, otherwise default
    const activeFilter = this.activeFilters.get(this.sizeConfig.metricKey);
    const scaleMin = activeFilter ? activeFilter.min : metric.min;
    const scaleMax = activeFilter ? activeFilter.max : metric.max;
    
    // Normalize to 0-1
    const range = scaleMax - scaleMin;
    if (range === 0) return 6;
    
    let normalized = (value - scaleMin) / range;
    normalized = Math.max(0, Math.min(1, normalized)); // Clamp to 0-1
    
    // Flip if low is big
    if (!this.sizeConfig.highIsGreen) {
      normalized = 1 - normalized;
    }
    
    // Scale from 4px to 18px
    return 4 + (normalized * 14);
  }

  // Dynamic color based on selected metric - 5 color gradient (public for template)
  getColorForCityPublic(city: City): string {
    return this.getColorForCity(city);
  }

  // Dynamic color based on selected metric - 5 color gradient
  // Scale is dynamic based on active filter range for the color metric
  private getColorForCity(city: City): string {
    const metric = this.findMetricByKey(this.colorConfig.metricKey);
    if (!metric) return '#999';
    
    const value = this.getMetricValue(city, metric);
    if (value === 0 && metric.type !== 'percent') return '#999';
    
    // Get the scale range - use active filter if set, otherwise default
    const activeFilter = this.activeFilters.get(this.colorConfig.metricKey);
    const scaleMin = activeFilter ? activeFilter.min : metric.min;
    const scaleMax = activeFilter ? activeFilter.max : metric.max;
    
    // Calculate where this value falls in the range (0-1)
    const range = scaleMax - scaleMin;
    if (range === 0) return '#f1c40f'; // Yellow if no range
    
    let normalized = (value - scaleMin) / range;
    normalized = Math.max(0, Math.min(1, normalized)); // Clamp to 0-1
    
    // Flip if low is green
    if (!this.colorConfig.highIsGreen) {
      normalized = 1 - normalized;
    }
    
    // 5-color gradient with custom distribution:
    // Red: 0-15%, Orange: 15-35%, Yellow: 35-65%, Light Green: 65-85%, Green: 85-100%
    const colors = [
      { pos: 0, r: 231, g: 76, b: 60 },     // #e74c3c - red
      { pos: 0.15, r: 230, g: 126, b: 34 }, // #e67e22 - orange
      { pos: 0.35, r: 241, g: 196, b: 15 }, // #f1c40f - yellow
      { pos: 0.65, r: 241, g: 196, b: 15 }, // #f1c40f - yellow (plateau)
      { pos: 0.85, r: 46, g: 204, b: 113 }, // #2ecc71 - light green
      { pos: 1, r: 39, g: 174, b: 96 }      // #27ae60 - green
    ];
    
    // Find which two colors to interpolate between
    let lower = colors[0];
    let upper = colors[colors.length - 1];
    
    for (let i = 0; i < colors.length - 1; i++) {
      if (normalized >= colors[i].pos && normalized <= colors[i + 1].pos) {
        lower = colors[i];
        upper = colors[i + 1];
        break;
      }
    }
    
    // Interpolate between the two colors
    const range2 = upper.pos - lower.pos;
    const t = range2 === 0 ? 0 : (normalized - lower.pos) / range2;
    
    const r = Math.round(lower.r + (upper.r - lower.r) * t);
    const g = Math.round(lower.g + (upper.g - lower.g) * t);
    const b = Math.round(lower.b + (upper.b - lower.b) * t);
    
    return `rgb(${r}, ${g}, ${b})`;
  }

  // Find a metric by its key
  findMetricByKey(key: string): FilterMetric | null {
    for (const cat of this.filterCategories) {
      const metric = cat.metrics.find(m => m.key === key);
      if (metric) return metric;
    }
    return null;
  }

  // Toggle category expansion
  toggleCategory(category: FilterCategory): void {
    category.expanded = !category.expanded;
  }

  // Handle slider change with debounce
  onFilterChange(metric: FilterMetric): void {
    if (this.filterDebounceTimer) {
      clearTimeout(this.filterDebounceTimer);
    }
    
    this.filterDebounceTimer = setTimeout(() => {
      const values = this.filterValues.get(metric.key);
      if (!values) return;
      
      // Check if filter is at default (full range)
      const isDefault = values.min === metric.min && values.max === metric.max;
      
      if (isDefault) {
        // Remove filter if at default
        this.activeFilters.delete(metric.key);
      } else {
        // Add/update filter
        this.activeFilters.set(metric.key, {
          metricKey: metric.key,
          min: values.min,
          max: values.max
        });
      }
      
      this.applyFilters();
    }, 150); // 150ms debounce
  }

  // Get current filter values for a metric
  getFilterMin(metric: FilterMetric): number {
    return this.filterValues.get(metric.key)?.min ?? metric.min;
  }

  getFilterMax(metric: FilterMetric): number {
    return this.filterValues.get(metric.key)?.max ?? metric.max;
  }

  // Set filter min value
  setFilterMin(metric: FilterMetric, value: number): void {
    const current = this.filterValues.get(metric.key) || { min: metric.min, max: metric.max };
    current.min = Math.min(value, current.max); // Don't exceed max
    this.filterValues.set(metric.key, current);
    this.onFilterChange(metric);
  }

  // Set filter max value
  setFilterMax(metric: FilterMetric, value: number): void {
    const current = this.filterValues.get(metric.key) || { min: metric.min, max: metric.max };
    current.max = Math.max(value, current.min); // Don't go below min
    this.filterValues.set(metric.key, current);
    this.onFilterChange(metric);
  }

  // Check if a metric has an active filter
  isFilterActive(metric: FilterMetric): boolean {
    return this.activeFilters.has(metric.key);
  }

  // Get count of active filters in a category
  getActiveFilterCount(category: FilterCategory): number {
    return category.metrics.filter(m => this.activeFilters.has(m.key)).length;
  }

  // Clear all filters
  clearAllFilters(): void {
    this.activeFilters.clear();
    
    // Reset all filter values to defaults
    this.filterCategories.forEach(cat => {
      cat.metrics.forEach(metric => {
        this.filterValues.set(metric.key, { min: metric.min, max: metric.max });
      });
    });
    
    this.applyFilters();
  }

  // Clear a single filter
  clearFilter(metric: FilterMetric): void {
    this.activeFilters.delete(metric.key);
    this.filterValues.set(metric.key, { min: metric.min, max: metric.max });
    this.applyFilters();
  }

  // Color by metric selection
  setColorMetric(metricKey: string): void {
    this.colorConfig.metricKey = metricKey;
    this.updateCurrentColorMetric();
    this.addMarkers();
  }

  toggleColorDirection(): void {
    this.colorConfig.highIsGreen = !this.colorConfig.highIsGreen;
    this.addMarkers();
  }

  private updateCurrentColorMetric(): void {
    this.currentColorMetric = this.findMetricByKey(this.colorConfig.metricKey);
  }

  // Size by metric selection
  setSizeMetric(metricKey: string): void {
    this.sizeConfig.metricKey = metricKey;
    this.updateCurrentSizeMetric();
    this.updateVisibleCities();
    if (this.showListPanel) {
      this.updateSortedList();
    }
  }

  toggleSizeDirection(): void {
    this.sizeConfig.highIsGreen = !this.sizeConfig.highIsGreen;
    this.updateVisibleCities();
    if (this.showListPanel) {
      this.updateSortedList();
    }
  }

  private updateCurrentSizeMetric(): void {
    this.currentSizeMetric = this.findMetricByKey(this.sizeConfig.metricKey);
  }

  // Get all metrics as flat list for dropdowns
  getAllMetrics(): FilterMetric[] {
    const metrics: FilterMetric[] = [];
    this.filterCategories.forEach(cat => {
      cat.metrics.forEach(m => metrics.push(m));
    });
    return metrics;
  }

  // Format value for display based on metric type
  formatMetricValue(value: number, metric: FilterMetric): string {
    switch (metric.type) {
      case 'currency':
        return '$' + Math.round(value).toLocaleString();
      case 'percent':
        return value.toFixed(0) + '%';
      case 'years':
        return value.toFixed(0) + ' yrs';
      case 'minutes':
        return value.toFixed(0) + ' min';
      default:
        return value.toLocaleString();
    }
  }

  // Legend helpers - use dynamic scale based on active filter
  private getColorScaleMin(): number {
    if (!this.currentColorMetric) return 0;
    const activeFilter = this.activeFilters.get(this.colorConfig.metricKey);
    return activeFilter ? activeFilter.min : this.currentColorMetric.min;
  }

  private getColorScaleMax(): number {
    if (!this.currentColorMetric) return 100;
    const activeFilter = this.activeFilters.get(this.colorConfig.metricKey);
    return activeFilter ? activeFilter.max : this.currentColorMetric.max;
  }

  getLegendLow(): string {
    if (!this.currentColorMetric) return '';
    return this.formatMetricValue(this.getColorScaleMin(), this.currentColorMetric);
  }

  getLegendMid(): string {
    if (!this.currentColorMetric) return '';
    const mid = (this.getColorScaleMin() + this.getColorScaleMax()) / 2;
    return this.formatMetricValue(mid, this.currentColorMetric);
  }

  getLegendHigh(): string {
    if (!this.currentColorMetric) return '';
    return this.formatMetricValue(this.getColorScaleMax(), this.currentColorMetric);
  }

  // Size legend helpers
  private getSizeScaleMin(): number {
    if (!this.currentSizeMetric) return 0;
    const activeFilter = this.activeFilters.get(this.sizeConfig.metricKey);
    return activeFilter ? activeFilter.min : this.currentSizeMetric.min;
  }

  private getSizeScaleMax(): number {
    if (!this.currentSizeMetric) return 100;
    const activeFilter = this.activeFilters.get(this.sizeConfig.metricKey);
    return activeFilter ? activeFilter.max : this.currentSizeMetric.max;
  }

  getSizeLegendLow(): string {
    if (!this.currentSizeMetric) return '';
    return this.formatMetricValue(this.getSizeScaleMin(), this.currentSizeMetric);
  }

  getSizeLegendHigh(): string {
    if (!this.currentSizeMetric) return '';
    return this.formatMetricValue(this.getSizeScaleMax(), this.currentSizeMetric);
  }

  formatNumber(num: number): string {
    return num?.toLocaleString() || 'N/A';
  }

  formatMoney(num: number): string {
    return num ? '$' + Math.round(num).toLocaleString() : 'N/A';
  }

  getPercentage(part: number, total: number): string {
    if (!part || !total) return 'N/A';
    return ((part / total) * 100).toFixed(1) + '%';
  }

  closePanel(): void {
    this.selectedCity = null;
  }

  onSearchInput(): void {
    const query = this.searchQuery.trim().toLowerCase();
    
    if (query.length < 1) {
      this.searchResults = [];
      return;
    }

    const startsWith = this.allCities.filter(city => 
      city.name.toLowerCase().startsWith(query)
    );
    
    const contains = this.allCities.filter(city => 
      city.name.toLowerCase().includes(query) && 
      !city.name.toLowerCase().startsWith(query)
    );

    this.searchResults = [
      ...startsWith.sort((a, b) => b.population - a.population),
      ...contains.sort((a, b) => b.population - a.population)
    ].slice(0, 10);
  }

  selectSearchResult(city: City): void {
    this.selectedCity = city;
    this.searchQuery = '';
    this.searchResults = [];
    
    if (city.latitude && city.longitude && this.map) {
      this.map.setView([city.latitude, city.longitude], 9, { animate: true });
    }
    
    this.cdr.detectChanges();
  }
}