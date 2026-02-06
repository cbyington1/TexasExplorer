import { Component, OnInit, AfterViewInit, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { forkJoin } from 'rxjs';
import { CityService, DerivedStats, CityTrend } from '../city.service';
import { City } from '../city.model';

// Filter metric definition
interface FilterMetric {
  key: string;
  label: string;
  type: 'raw' | 'percent' | 'currency' | 'years' | 'minutes' | 'balance';
  field: keyof City;
  percentOf?: keyof City; // For percentage calculations
  min: number;
  max: number;
  step: number;
  dropdownOnly?: boolean; // Only show in color/size dropdowns, not in filter sliders
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

// Trend filter metric definition
interface TrendMetric {
  key: string;           // e.g. 'trend_populationGrowthPct'
  label: string;         // e.g. 'Population'
  trendField: string;    // field on CityTrend, e.g. 'populationGrowthPct'
  unit: string;          // '%', 'pp' (percentage points)
  min: number;
  max: number;
  step: number;
}

// Trend filter category
interface TrendCategory {
  key: string;
  label: string;
  expanded: boolean;
  metrics: TrendMetric[];
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
  private selectedCityMarker: L.CircleMarker | null = null; // NEW: Dedicated marker for selected city
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

  // City classification — now from backend
  cityClassification: string = '';
  classificationFilters: Set<string> = new Set(); // empty = show all
  classificationExpanded: boolean = false;

  // Backend derived stats lookup (geoid -> DerivedStats)
  private derivedStatsMap: Map<string, DerivedStats> = new Map();

  // Trend data
  trendData: Map<string, CityTrend> = new Map(); // geoid -> CityTrend
  trendBaseYear: number = 2012;
  trendLoading: boolean = false;
  trendEnabled: boolean = false; // master toggle for trend section
  basicsEnabled: boolean = false; // master toggle for basics filter section
  trendCategories: TrendCategory[] = [];
  trendFilterValues: Map<string, { min: number; max: number }> = new Map();
  activeTrendFilters: Map<string, ActiveFilter> = new Map();
  private trendDebounceTimer: any = null;

  // Data quality filter
  requireCompleteData: boolean = true;

  // List panel
  showListPanel: boolean = false;
  sortedCitiesList: City[] = [];

  // History panel
  showHistoryPanel: boolean = false;
  historyData: City[] = [];
  historyLoading: boolean = false;
  
  // Derived stats history for urbanization chart
  private derivedHistoryData: DerivedStats[] = [];

  historyMetrics: { key: string; label: string; category: string; getValue: (city: City) => number | null; format: (v: number) => string; yFormat: (v: number) => string }[] = [
    // Overview - standalone numbers
    { key: 'population', label: 'Population', category: 'Overview', getValue: (c) => c.population, format: (v) => v?.toLocaleString() || 'N/A', yFormat: (v) => v >= 1000 ? (v/1000).toFixed(0) + 'k' : v?.toString() },
    { key: 'medianAge', label: 'Median Age', category: 'Overview', getValue: (c) => c.medianAge, format: (v) => v?.toFixed(1) + ' yrs', yFormat: (v) => v?.toFixed(0) },
    
    // Income - standalone numbers
    { key: 'medianHouseholdIncome', label: 'Median Household Income', category: 'Income', getValue: (c) => c.medianHouseholdIncome, format: (v) => v ? '$' + v.toLocaleString() : 'N/A', yFormat: (v) => '$' + (v/1000).toFixed(0) + 'k' },
    { key: 'perCapitaIncome', label: 'Per Capita Income', category: 'Income', getValue: (c) => c.perCapitaIncome, format: (v) => v ? '$' + v.toLocaleString() : 'N/A', yFormat: (v) => '$' + (v/1000).toFixed(0) + 'k' },
    
    // Housing - standalone numbers + composition %
    { key: 'medianHomeValue', label: 'Median Home Value', category: 'Housing', getValue: (c) => c.medianHomeValue, format: (v) => v ? '$' + v.toLocaleString() : 'N/A', yFormat: (v) => '$' + (v/1000).toFixed(0) + 'k' },
    { key: 'medianRent', label: 'Median Rent', category: 'Housing', getValue: (c) => c.medianRent, format: (v) => v ? '$' + v.toLocaleString() : 'N/A', yFormat: (v) => '$' + v?.toFixed(0) },
    { key: 'ownershipRate', label: 'Homeownership Rate', category: 'Housing', getValue: (c) => (c.ownerOccupied && (c.ownerOccupied + c.renterOccupied) > 0) ? (c.ownerOccupied / (c.ownerOccupied + c.renterOccupied)) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(0) + '%' },
    
    // Sex - composition %
    { key: 'malePct', label: 'Male', category: 'Sex', getValue: (c) => (c.malePopulation && c.population > 0) ? (c.malePopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(1) + '%' },
    { key: 'femalePct', label: 'Female', category: 'Sex', getValue: (c) => (c.femalePopulation && c.population > 0) ? (c.femalePopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(1) + '%' },
    
    // Race - composition %
    { key: 'whitePct', label: 'White', category: 'Race', getValue: (c) => (c.whitePopulation && c.population > 0) ? (c.whitePopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(0) + '%' },
    { key: 'blackPct', label: 'Black', category: 'Race', getValue: (c) => (c.blackPopulation && c.population > 0) ? (c.blackPopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(0) + '%' },
    { key: 'asianPct', label: 'Asian', category: 'Race', getValue: (c) => (c.asianPopulation && c.population > 0) ? (c.asianPopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(0) + '%' },
    { key: 'nativeAmericanPct', label: 'Native American', category: 'Race', getValue: (c) => (c.nativeAmericanPopulation && c.population > 0) ? (c.nativeAmericanPopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(1) + '%' },
    { key: 'pacificIslanderPct', label: 'Pacific Islander', category: 'Race', getValue: (c) => (c.pacificIslanderPopulation && c.population > 0) ? (c.pacificIslanderPopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(1) + '%' },
    { key: 'twoOrMorePct', label: 'Two or More Races', category: 'Race', getValue: (c) => (c.twoOrMoreRacesPopulation && c.population > 0) ? (c.twoOrMoreRacesPopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(1) + '%' },
    { key: 'otherRacePct', label: 'Other Race', category: 'Race', getValue: (c) => (c.otherRacePopulation && c.population > 0) ? (c.otherRacePopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(1) + '%' },
    
    // Ethnicity - composition % (separate from race)
    { key: 'hispanicPct', label: 'Hispanic/Latino', category: 'Ethnicity', getValue: (c) => (c.hispanicPopulation && c.population > 0) ? (c.hispanicPopulation / c.population) * 100 : null, format: (v) => v?.toFixed(1) + '%', yFormat: (v) => v?.toFixed(0) + '%' },

    // Classification - urbanization index from backend
    // Classification - urbanization index from backend
    { key: 'urbanIndex', label: 'Urbanization Index', category: 'Classification', 
      getValue: (c) => (c as any)._texasUrbanizationIndex ?? this.getUrbanizationIndex(c), 
      format: (v) => v?.toFixed(1) + ' / 100', yFormat: (v) => v?.toFixed(0) },

    // Diversity - Simpson's diversity index from backend
    // Diversity - Simpson's diversity index from backend
    { key: 'diversityIndex', label: 'Diversity Index', category: 'Diversity', 
      getValue: (c) => (c as any)._texasDiversityIndex ?? this.getDiversityIndex(c), 
      format: (v) => v?.toFixed(1) + ' / 100', yFormat: (v) => v?.toFixed(0) },
  ];

  // ============ NEW: Mobile state ============
  isMobile: boolean = false;
  mobileCardExpanded: boolean = false;
  mobileFiltersOpen: boolean = false;
  
  // Balance slider values (single value, not min/max range)
  balanceValues: Map<string, number> = new Map([['malePct', 50]]);
  // ============================================

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
    this.checkMobile(); // NEW
  }

  // ============ NEW: Window resize listener ============
  @HostListener('window:resize')
  onResize(): void {
    this.checkMobile();
  }

  // NEW: Check if we're on mobile
  private checkMobile(): void {
    this.isMobile = window.innerWidth <= 768;
  }

  toggleMobileCard(): void {
    this.mobileCardExpanded = !this.mobileCardExpanded;
    if (this.mobileCardExpanded) {
      this.mobileFiltersOpen = false;
    }
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
    if (this.mobileFiltersOpen) {
      this.mobileCardExpanded = false;
    }
  }
  
  // NEW: Get balance slider value
  getBalanceValue(metric: FilterMetric): number {
    return this.balanceValues.get(metric.key) ?? 50;
  }

  // Update balance slider visual only (called during drag via input event)
  updateBalanceVisual(metric: FilterMetric, value: number): void {
    this.balanceValues.set(metric.key, value);
  }

  // Apply balance filter (called on release via change event)
  applyBalanceFilter(metric: FilterMetric): void {
    const value = this.getBalanceValue(metric);
    
    if (value === 50) {
      this.activeFilters.delete(metric.key);
    } else if (value > 50) {
      this.activeFilters.set(metric.key, {
        metricKey: metric.key,
        min: value,
        max: metric.max
      });
    } else {
      this.activeFilters.set(metric.key, {
        metricKey: metric.key,
        min: metric.min,
        max: value
      });
    }
    
    this.applyFilters();
  }

  // Get a human-readable description of what the balance filter is doing
  getBalanceDescription(metric: FilterMetric): string {
    const value = this.getBalanceValue(metric);
    if (value === 50) return 'Showing all cities';
    if (value > 50) return `≥${value}% Male`;
    return `≥${100 - value}% Female`;
  }

  // Get the color class for balance description text
  getBalanceColorClass(metric: FilterMetric): string {
    const value = this.getBalanceValue(metric);
    if (value > 50) return 'male';
    if (value < 50) return 'female';
    return '';
  }

  // Get the slider track gradient style based on current value
  getBalanceSplit(metric: FilterMetric): string {
    return this.getBalanceValue(metric) + '%';
  }

  // NEW: Check if balance is at default (50/50)
  isBalanceDefault(metric: FilterMetric): boolean {
    return this.getBalanceValue(metric) === 50;
  }

  // NEW: Reset balance to 50/50
  resetBalance(metric: FilterMetric): void {
    this.balanceValues.set(metric.key, 50);
    this.activeFilters.delete(metric.key);
    this.applyFilters();
  }

  // NEW: Handle typed input for balance slider
  onBalanceInputChange(event: Event, metric: FilterMetric, type: 'male' | 'female'): void {
    const input = event.target as HTMLInputElement;
    let value = parseFloat(input.value);
    
    // Validate 0-100
    if (isNaN(value) || value < 0 || value > 100) {
      // Revert
      const currentMale = this.getBalanceValue(metric);
      input.value = type === 'male' ? String(currentMale) : String(100 - currentMale);
      return;
    }
    
    // If female was typed, convert to male percentage
    if (type === 'female') {
      value = 100 - value;
    }
    
    this.updateBalanceVisual(metric, value);
    this.applyBalanceFilter(metric);
  }

  // ============ Custom Range Slider Handling ============
  private draggingMetric: FilterMetric | null = null;
  private draggingThumb: 'min' | 'max' | null = null;
  private sliderRect: DOMRect | null = null;
  rangeError: string | null = null;
  private errorTimeout: any = null;

  onMinInputChange(event: Event, metric: FilterMetric): void {
    const input = event.target as HTMLInputElement;
    const value = parseFloat(input.value);
    const current = this.filterValues.get(metric.key) || { min: metric.min, max: metric.max };
    
    // Validate: must be >= metric.min and < current.max
    if (isNaN(value) || value < metric.min || value >= current.max) {
      // Revert and show error
      input.value = String(current.min);
      this.showRangeError(metric.key);
      return;
    }
    
    current.min = value;
    this.filterValues.set(metric.key, current);
    this.onFilterChange(metric);
    this.cdr.detectChanges();
  }

  onMaxInputChange(event: Event, metric: FilterMetric): void {
    const input = event.target as HTMLInputElement;
    const value = parseFloat(input.value);
    const current = this.filterValues.get(metric.key) || { min: metric.min, max: metric.max };
    
    // Validate: must be <= metric.max and > current.min
    if (isNaN(value) || value > metric.max || value <= current.min) {
      // Revert and show error
      input.value = String(current.max);
      this.showRangeError(metric.key);
      return;
    }
    
    current.max = value;
    this.filterValues.set(metric.key, current);
    this.onFilterChange(metric);
    this.cdr.detectChanges();
  }

  private showRangeError(metricKey: string): void {
    this.rangeError = metricKey;
    
    // Clear any existing timeout
    if (this.errorTimeout) {
      clearTimeout(this.errorTimeout);
    }
    
    // Hide after 2 seconds
    this.errorTimeout = setTimeout(() => {
      this.rangeError = null;
      this.cdr.detectChanges();
    }, 2000);
  }

  // Auto-resize input based on value length
  autoResizeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const length = input.value.length;
    // Base width + character width
    input.style.width = Math.max(45, Math.min(100, 20 + length * 9)) + 'px';
  }

  onRangeMouseDown(event: MouseEvent, metric: FilterMetric): void {
    const container = event.currentTarget as HTMLElement;
    this.startDrag(event.clientX, container, metric);
    
    const onMouseMove = (e: MouseEvent) => this.onDragMove(e.clientX);
    const onMouseUp = () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
      this.endDrag();
    };
    
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  onRangeTouchStart(event: TouchEvent, metric: FilterMetric): void {
    const container = event.currentTarget as HTMLElement;
    const touch = event.touches[0];
    this.startDrag(touch.clientX, container, metric);
    
    const onTouchMove = (e: TouchEvent) => {
      e.preventDefault();
      this.onDragMove(e.touches[0].clientX);
    };
    const onTouchEnd = () => {
      document.removeEventListener('touchmove', onTouchMove);
      document.removeEventListener('touchend', onTouchEnd);
      this.endDrag();
    };
    
    document.addEventListener('touchmove', onTouchMove, { passive: false });
    document.addEventListener('touchend', onTouchEnd);
  }

  private startDrag(clientX: number, container: HTMLElement, metric: FilterMetric): void {
    this.sliderRect = container.getBoundingClientRect();
    this.draggingMetric = metric;
    
    // Figure out which thumb is closer to the click
    const percent = (clientX - this.sliderRect.left) / this.sliderRect.width;
    const clickValue = metric.min + percent * (metric.max - metric.min);
    
    const minVal = this.getFilterMin(metric);
    const maxVal = this.getFilterMax(metric);
    
    const distToMin = Math.abs(clickValue - minVal);
    const distToMax = Math.abs(clickValue - maxVal);
    
    // Choose the closer thumb, but if they're at the same spot, choose based on direction
    if (distToMin <= distToMax) {
      this.draggingThumb = 'min';
    } else {
      this.draggingThumb = 'max';
    }
    
    // Immediately update to click position
    this.onDragMove(clientX);
  }

  private onDragMove(clientX: number): void {
    if (!this.sliderRect || !this.draggingMetric || !this.draggingThumb) return;
    
    const metric = this.draggingMetric;
    let percent = (clientX - this.sliderRect.left) / this.sliderRect.width;
    percent = Math.max(0, Math.min(1, percent)); // Clamp 0-1
    
    let value = metric.min + percent * (metric.max - metric.min);
    // Snap to step
    value = Math.round(value / metric.step) * metric.step;
    value = Math.max(metric.min, Math.min(metric.max, value));
    
    const current = this.filterValues.get(metric.key) || { min: metric.min, max: metric.max };
    
    if (this.draggingThumb === 'min') {
      // Min can't exceed max - step
      current.min = Math.min(value, current.max - metric.step);
    } else {
      // Max can't go below min + step
      current.max = Math.max(value, current.min + metric.step);
    }
    
    this.filterValues.set(metric.key, current);
    this.onFilterChange(metric);
    this.cdr.detectChanges();
  }

  private endDrag(): void {
    this.draggingMetric = null;
    this.draggingThumb = null;
    this.sliderRect = null;
  }

  // ============ Trend Range Slider Handling ============
  private draggingTrendMetric: TrendMetric | null = null;
  private draggingTrendThumb: 'min' | 'max' | null = null;
  private trendSliderRect: DOMRect | null = null;

  onTrendRangeMouseDown(event: MouseEvent, metric: TrendMetric): void {
    const container = event.currentTarget as HTMLElement;
    this.startTrendDrag(event.clientX, container, metric);

    const onMouseMove = (e: MouseEvent) => this.onTrendDragMove(e.clientX);
    const onMouseUp = () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
      this.endTrendDrag();
    };

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  onTrendRangeTouchStart(event: TouchEvent, metric: TrendMetric): void {
    const container = event.currentTarget as HTMLElement;
    const touch = event.touches[0];
    this.startTrendDrag(touch.clientX, container, metric);

    const onTouchMove = (e: TouchEvent) => {
      e.preventDefault();
      this.onTrendDragMove(e.touches[0].clientX);
    };
    const onTouchEnd = () => {
      document.removeEventListener('touchmove', onTouchMove);
      document.removeEventListener('touchend', onTouchEnd);
      this.endTrendDrag();
    };

    document.addEventListener('touchmove', onTouchMove, { passive: false });
    document.addEventListener('touchend', onTouchEnd);
  }

  private startTrendDrag(clientX: number, container: HTMLElement, metric: TrendMetric): void {
    this.trendSliderRect = container.getBoundingClientRect();
    this.draggingTrendMetric = metric;

    const percent = (clientX - this.trendSliderRect.left) / this.trendSliderRect.width;
    const clickValue = metric.min + percent * (metric.max - metric.min);

    const minVal = this.getTrendFilterMin(metric);
    const maxVal = this.getTrendFilterMax(metric);

    this.draggingTrendThumb = Math.abs(clickValue - minVal) <= Math.abs(clickValue - maxVal) ? 'min' : 'max';
    this.onTrendDragMove(clientX);
  }

  private onTrendDragMove(clientX: number): void {
    if (!this.trendSliderRect || !this.draggingTrendMetric || !this.draggingTrendThumb) return;

    const metric = this.draggingTrendMetric;
    let percent = (clientX - this.trendSliderRect.left) / this.trendSliderRect.width;
    percent = Math.max(0, Math.min(1, percent));

    let value = metric.min + percent * (metric.max - metric.min);
    value = Math.round(value / metric.step) * metric.step;
    value = Math.max(metric.min, Math.min(metric.max, value));

    const current = this.trendFilterValues.get(metric.key) || { min: metric.min, max: metric.max };

    if (this.draggingTrendThumb === 'min') {
      current.min = Math.min(value, current.max - metric.step);
    } else {
      current.max = Math.max(value, current.min + metric.step);
    }

    this.trendFilterValues.set(metric.key, current);
    this.onTrendFilterChange(metric);
    this.cdr.detectChanges();
  }

  private endTrendDrag(): void {
    this.draggingTrendMetric = null;
    this.draggingTrendThumb = null;
    this.trendSliderRect = null;
  }
  // ======================================================

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
        key: 'growth',
        label: 'Growth',
        icon: '',
        expanded: false,
        metrics: [
          { key: 'population', label: 'Population', type: 'raw', field: 'population', min: 100, max: 500000, step: 1000 },
          { key: 'medianAge', label: 'Median Age', type: 'years', field: 'medianAge', min: 18, max: 70, step: 1 },
          { key: 'medianHouseholdIncome', label: 'Median Household Income', type: 'currency', field: 'medianHouseholdIncome', min: 25000, max: 125000, step: 5000 },
          { key: 'perCapitaIncome', label: 'Per Capita Income', type: 'currency', field: 'perCapitaIncome', min: 15000, max: 75000, step: 2500 },
        ]
      },
      {
        key: 'demographics',
        label: 'Demographics',
        icon: '',
        expanded: false,
        metrics: [
          { key: 'malePct', label: 'Male / Female Balance', type: 'balance', field: 'malePopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'malePercent', label: 'Male', type: 'percent', field: 'malePopulation', percentOf: 'population', min: 0, max: 100, step: 1, dropdownOnly: true },
          { key: 'femalePercent', label: 'Female', type: 'percent', field: 'femalePopulation', percentOf: 'population', min: 0, max: 100, step: 1, dropdownOnly: true },
          { key: 'ageUnder18Pct', label: 'Under 18', type: 'percent', field: 'ageUnder18', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'age65plusPct', label: 'Age 65+', type: 'percent', field: 'age65plus', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'whitePct', label: 'White', type: 'percent', field: 'whitePopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'blackPct', label: 'Black', type: 'percent', field: 'blackPopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'asianPct', label: 'Asian', type: 'percent', field: 'asianPopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'hispanicPct', label: 'Hispanic/Latino', type: 'percent', field: 'hispanicPopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'nativeAmericanPct', label: 'Native American', type: 'percent', field: 'nativeAmericanPopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'pacificIslanderPct', label: 'Pacific Islander', type: 'percent', field: 'pacificIslanderPopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'twoOrMorePct', label: 'Two or More Races', type: 'percent', field: 'twoOrMoreRacesPopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'otherRacePct', label: 'Other Race', type: 'percent', field: 'otherRacePopulation', percentOf: 'population', min: 0, max: 100, step: 1 },
          { key: 'diversityIndex', label: 'Diversity Index', type: 'raw', field: 'population' as any, min: 0, max: 100, step: 1 },
        ]
      },
      {
        key: 'housing',
        label: 'Housing',
        icon: '',
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

    // Trend filter categories — only the metrics worth trending
    this.trendCategories = [
      {
        key: 'trend_growth',
        label: 'Growth',
        expanded: false,
        metrics: [
          { key: 'trend_population', label: 'Population', trendField: 'populationGrowthPct', unit: '%', min: -500, max: 500, step: 25 },
          { key: 'trend_medianIncome', label: 'Median Income', trendField: 'medianIncomeGrowthPct', unit: '%', min: -500, max: 500, step: 25 },
          { key: 'trend_perCapitaIncome', label: 'Per Capita Income', trendField: 'perCapitaIncomeGrowthPct', unit: '%', min: -500, max: 500, step: 25 },
        ]
      },
      {
        key: 'trend_demographics',
        label: 'Demographics',
        expanded: false,
        metrics: [
          { key: 'trend_male', label: 'Male', trendField: 'malePctGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
          { key: 'trend_female', label: 'Female', trendField: 'femalePctGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
          { key: 'trend_hispanic', label: 'Hispanic', trendField: 'hispanicPctGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
          { key: 'trend_white', label: 'White', trendField: 'whitePctGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
          { key: 'trend_black', label: 'Black', trendField: 'blackPctGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
          { key: 'trend_asian', label: 'Asian', trendField: 'asianPctGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
        ]
      },
      {
        key: 'trend_housing',
        label: 'Housing',
        expanded: false,
        metrics: [
          { key: 'trend_homeValue', label: 'Median Home Value', trendField: 'medianHomeValueGrowthPct', unit: '%', min: -500, max: 500, step: 25 },
          { key: 'trend_rent', label: 'Median Rent', trendField: 'medianRentGrowthPct', unit: '%', min: -500, max: 500, step: 25 },
          { key: 'trend_homeownership', label: 'Homeownership Rate', trendField: 'homeownershipRateGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
        ]
      },
      {
        key: 'trend_classification',
        label: 'Classification',
        expanded: false,
        metrics: [
          { key: 'trend_urbanIndex', label: 'Urbanization Index', trendField: 'urbanizationIndexGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
          { key: 'trend_diversityIndex', label: 'Diversity Index', trendField: 'diversityIndexGrowthPct', unit: '%', min: -100, max: 100, step: 5 },
        ]
      },
    ];

    // Initialize trend filter values to full range
    this.trendCategories.forEach(cat => {
      cat.metrics.forEach(metric => {
        this.trendFilterValues.set(metric.key, { min: metric.min, max: metric.max });
      });
    });
  }

  private initMap(): void {
    const texasBounds = L.latLngBounds(
      [25.0, -107.5],
      [37.0, -93.0]
    );

    // Start more zoomed in on mobile
    const initialZoom = this.isMobile ? 5 : 5.5;

    const mapOptions: any = {
    center: [31.0, -100.0],
    zoom: initialZoom,
    minZoom: 5,
    maxZoom: 12,
    zoomControl: !this.isMobile,
    dragging: true,
    touchZoom: true,
    scrollWheelZoom: true,
    doubleClickZoom: true,
    boxZoom: false,
    keyboard: true,
    bounceAtZoomLimits: false
  };

  // Only use maxBounds on desktop
  if (!this.isMobile) {
    mapOptions.maxBounds = texasBounds;
    mapOptions.maxBoundsViscosity = 1.0;
  }

  this.map = L.map('map', mapOptions);

  // Mobile: gently bring user back to Texas if they pan too far
  if (this.isMobile) {
    this.map.on('moveend', () => {
      const center = this.map.getCenter();
      if (center.lat < 24 || center.lat > 38 || center.lng < -108 || center.lng > -92) {
        this.map.panTo([31.0, -100.0], { animate: true, duration: 0.5 });
      }
    });
  }

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '© OpenStreetMap contributors',
      bounds: texasBounds
    }).addTo(this.map);

    this.map.on('click', () => {
      if (this.selectedCity) {
        this.selectedCity = null;
        this.cityClassification = '';
        this.updateSelectedCityMarker(); // Will fade out the overlay
      }
      this.showListPanel = false;
      this.mobileCardExpanded = false;
      this.cdr.detectChanges();
    });

    this.map.on('movestart', () => {
      // Collapse card when user starts panning/zooming
      if (this.isMobile && this.mobileCardExpanded) {
        this.mobileCardExpanded = false;
        this.cdr.detectChanges();
      }
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
    // Reload trends if enabled (since the current year changed)
    if (this.trendEnabled) {
      this.loadTrendData();
    }
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

    // Load cities and derived stats in parallel
    forkJoin({
      cities: this.cityService.getCitiesForYear(this.selectedYear),
      derived: this.cityService.getDerivedStats(this.selectedYear)
    }).subscribe({
      next: ({ cities, derived }) => {
        this.allCities = cities;

        // Build geoid -> DerivedStats lookup
        this.derivedStatsMap.clear();
        derived.forEach(d => this.derivedStatsMap.set(d.geoid, d));
        console.log('Loaded derived stats for', this.selectedYear, ':', derived.length, 'cities');

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
    // Special case: computed urbanization index from backend
    if (metric.key === 'urbanIndex') {
      return this.getUrbanizationIndex(city) ?? 0;
    }
    // Special case: computed diversity index from backend
    if (metric.key === 'diversityIndex') {
      return this.getDiversityIndex(city) ?? 0;
    }

    const rawValue = city[metric.field] as number;
    
    if ((metric.type === 'percent' || metric.type === 'balance') && metric.percentOf) {
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

  // Classification filter
  toggleClassificationFilter(label: string): void {
    if (this.classificationFilters.has(label)) {
      this.classificationFilters.delete(label);
    } else {
      this.classificationFilters.add(label);
    }
    this.applyFilters();
  }

  isClassificationActive(label: string): boolean {
    return this.classificationFilters.has(label);
  }

  // Get classification label from backend derived stats
  private getCityClassificationLabel(city: City): string {
    const ds = this.derivedStatsMap.get(city.geoid);
    return ds?.classification || 'Rural';
  }

  // ============================================================
  // TREND FILTERING
  // ============================================================

  // Toggle the entire trends section on/off
  toggleTrends(): void {
    this.trendEnabled = !this.trendEnabled;
    if (this.trendEnabled && this.trendData.size === 0) {
      this.loadTrendData();
    } else if (!this.trendEnabled) {
      // Clear all trend filters when disabled
      this.activeTrendFilters.clear();
      this.trendCategories.forEach(cat => {
        cat.metrics.forEach(m => {
          this.trendFilterValues.set(m.key, { min: m.min, max: m.max });
        });
      });
      this.applyFilters();
    }
  }

  // Load trend data from backend
  loadTrendData(): void {
    this.trendLoading = true;
    this.cityService.getTrends(this.selectedYear, this.trendBaseYear).subscribe({
      next: (trends) => {
        this.trendData.clear();
        trends.forEach(t => this.trendData.set(t.geoid, t));
        console.log('Loaded trend data:', trends.length, 'cities,', this.trendBaseYear, '→', this.selectedYear);
        this.trendLoading = false;

        // Auto-calibrate slider ranges from actual data
        this.calibrateTrendRanges();
        this.applyFilters();
      },
      error: (err: Error) => {
        console.error('Error loading trends:', err);
        this.trendLoading = false;
      }
    });
  }

  // Auto-calibrate trend slider ranges based on actual data
  private calibrateTrendRanges(): void {
    // Ranges are fixed (±100 or ±500) — just reset filter values to full range
    this.trendCategories.forEach(cat => {
      cat.metrics.forEach(metric => {
        this.trendFilterValues.set(metric.key, { min: metric.min, max: metric.max });
      });
    });
    // Clear any active trend filters since data changed
    this.activeTrendFilters.clear();
  }

  // Change the base year for trend comparison
  onTrendBaseYearChange(): void {
    if (this.trendEnabled) {
      this.loadTrendData();
    }
  }

  // Handle trend slider change with debounce
  onTrendFilterChange(metric: TrendMetric): void {
    if (this.trendDebounceTimer) {
      clearTimeout(this.trendDebounceTimer);
    }

    this.trendDebounceTimer = setTimeout(() => {
      const values = this.trendFilterValues.get(metric.key);
      if (!values) return;

      const isDefault = values.min === metric.min && values.max === metric.max;

      if (isDefault) {
        this.activeTrendFilters.delete(metric.key);
      } else {
        this.activeTrendFilters.set(metric.key, {
          metricKey: metric.key,
          min: values.min,
          max: values.max
        });
      }

      this.applyFilters();
    }, 150);
  }

  // Get trend filter values
  getTrendFilterMin(metric: TrendMetric): number {
    return this.trendFilterValues.get(metric.key)?.min ?? metric.min;
  }

  getTrendFilterMax(metric: TrendMetric): number {
    return this.trendFilterValues.get(metric.key)?.max ?? metric.max;
  }

  // Set trend filter min
  setTrendFilterMin(metric: TrendMetric, value: number): void {
    const current = this.trendFilterValues.get(metric.key) || { min: metric.min, max: metric.max };
    current.min = Math.max(metric.min, Math.min(value, current.max - metric.step));
    this.trendFilterValues.set(metric.key, current);
    this.onTrendFilterChange(metric);
  }

  // Set trend filter max
  setTrendFilterMax(metric: TrendMetric, value: number): void {
    const current = this.trendFilterValues.get(metric.key) || { min: metric.min, max: metric.max };
    current.max = Math.min(metric.max, Math.max(value, current.min + metric.step));
    this.trendFilterValues.set(metric.key, current);
    this.onTrendFilterChange(metric);
  }

  // Clear a single trend filter
  clearTrendFilter(metric: TrendMetric): void {
    this.activeTrendFilters.delete(metric.key);
    this.trendFilterValues.set(metric.key, { min: metric.min, max: metric.max });
    this.applyFilters();
  }

  // Check if a trend filter is active (not at default)
  isTrendFilterActive(metric: TrendMetric): boolean {
    return this.activeTrendFilters.has(metric.key);
  }

  // Get active trend filter count for a category
  getActiveTrendFilterCount(category: TrendCategory): number {
    return category.metrics.filter(m => this.activeTrendFilters.has(m.key)).length;
  }

  // Format trend value for display
  formatTrendValue(value: number, metric: TrendMetric): string {
    const sign = value > 0 ? '+' : '';
    return sign + value.toFixed(0) + '%';
  }

  // Get the trend value for a city
  getCityTrendValue(city: City, metric: TrendMetric): number | null {
    const trend = this.trendData.get(city.geoid);
    if (!trend) return null;
    return (trend as any)[metric.trendField] as number | null;
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
    this.updateCityClassification();
    this.showListPanel = false;
    this.mobileCardExpanded = false;
    
    if (city.latitude && city.longitude && this.map) {
      this.map.setView([city.latitude, city.longitude], 9, { animate: true });
    }
    
    this.updateSelectedCityMarker(); // NEW: Update the selected marker
    this.cdr.detectChanges();
  }

  // Apply all active filters
  applyFilters(): void {
    let filtered = [...this.allCities];
    
    // Apply complete data filter first if enabled
    if (this.requireCompleteData) {
      filtered = filtered.filter(city => this.hasCompleteData(city));
    }

    // Apply classification filter
    if (this.classificationFilters.size > 0) {
      filtered = filtered.filter(city => this.classificationFilters.has(this.getCityClassificationLabel(city)));
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

    // Apply trend filters
    if (this.trendEnabled && this.activeTrendFilters.size > 0) {
      this.activeTrendFilters.forEach((filter, filterKey) => {
        // Find the trend metric definition
        let trendMetric: TrendMetric | null = null;
        for (const cat of this.trendCategories) {
          const found = cat.metrics.find(m => m.key === filterKey);
          if (found) { trendMetric = found; break; }
        }
        if (!trendMetric) return;

        filtered = filtered.filter(city => {
          const trend = this.trendData.get(city.geoid);
          if (!trend) return false; // no trend data = exclude
          const value = (trend as any)[trendMetric!.trendField] as number | null;
          if (value == null) return false;
          return value >= filter.min && value <= filter.max;
        });
      });
    }
    
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
        // Skip if this city is currently selected (the overlay handles it)
        if (this.selectedCity && city.geoid === this.selectedCity.geoid) {
          return;
        }
        
        const radius = this.getRadius(city);
        const cityColor = this.getColorForCity(city);
        
        const marker = L.circleMarker([city.latitude, city.longitude], {
          radius: radius,
          fillColor: cityColor,
          color: '#fff',
          weight: 1,
          opacity: 1,
          fillOpacity: 0.7
        }).addTo(this.map);

        // Store the color and radius on the marker for later use
        (marker as any)._cityColor = cityColor;
        (marker as any)._radius = radius;
        (marker as any)._cityGeoid = city.geoid;

        marker.on('click', (e) => {
          L.DomEvent.stopPropagation(e);
          this.selectCity(city);
        });

        marker.bindTooltip(
          city.name.replace(' city', '').replace(' town', ''),
          { permanent: false, direction: 'top' }
        );

        this.markers.push(marker);
      }
    });

    // Update the selected city marker after adding regular markers
    this.updateSelectedCityMarker();
  }

  // Handle city selection with animation
  private selectCity(city: City): void {
    // If we already have a selected city, do simultaneous transition
    if (this.selectedCity && this.selectedCity.geoid !== city.geoid) {
      // Re-add the old marker (for its fade out reveal, only if still visible)
      if ((this as any)._hiddenMarkerData) {
        const data = (this as any)._hiddenMarkerData;
        if (data.marker && data.city && this.visibleCities.some((c: City) => c.geoid === data.city.geoid)) {
          data.marker.addTo(this.map);
        }
        (this as any)._hiddenMarkerData = null;
      }
      
      // Fade out the old overlay
      if ((this as any)._animatedRing) {
        const oldRing = (this as any)._animatedRing;
        const oldElement = oldRing.getElement() as HTMLElement | undefined;
        if (oldElement) {
          const svg = oldElement.querySelector('svg') as SVGElement | null;
          if (svg) {
            const mainCircle = svg.querySelector('.main-circle') as SVGCircleElement | null;
            const gradientRing = svg.querySelector('.gradient-ring') as SVGCircleElement | null;
            
            if (mainCircle) {
              mainCircle.style.transition = 'fill-opacity 250ms ease-out';
              mainCircle.style.fillOpacity = '0';
            }
            if (gradientRing) {
              gradientRing.style.transition = 'opacity 250ms ease-out';
              gradientRing.style.opacity = '0';
            }
            
            // Remove after fade
            setTimeout(() => oldRing.remove(), 260);
          } else {
            oldRing.remove();
          }
        } else {
          oldRing.remove();
        }
        (this as any)._animatedRing = null;
        (this as any)._animatedRingCityGeoid = null;
      }
      
      // Immediately start the new city's transition (no waiting)
      this.selectedCity = city;
      this.updateCityClassification();
      this.mobileCardExpanded = false;
      this.createSelectedCityOverlay();
      this.cdr.detectChanges();
    } else {
      // No previous selection, just select
      this.selectedCity = city;
      this.updateCityClassification();
      this.mobileCardExpanded = false;
      this.updateSelectedCityMarker();
      this.cdr.detectChanges();
    }
  }
  
  // Create the overlay for the selected city (extracted for reuse)
  private createSelectedCityOverlay(): void {
    if (!this.selectedCity || !this.selectedCity.latitude || !this.selectedCity.longitude) {
      return;
    }

    // Check if the selected city is in visible cities
    const isInVisible = this.visibleCities.some(c => c.geoid === this.selectedCity!.geoid);

    // Find the underlying marker for this city (to hide after fade in)
    const underlyingMarker = this.markers.find(m => (m as any)._cityGeoid === this.selectedCity!.geoid);

    // Base radius
    const baseRadius = isInVisible ? this.getRadius(this.selectedCity) : 10;
    const strokeWidth = 3;
    const svgSize = (baseRadius + strokeWidth + 4) * 2;
    const center = svgSize / 2;

    // Create SVG overlay
    const svgHtml = `
      <svg width="${svgSize}" height="${svgSize}" viewBox="0 0 ${svgSize} ${svgSize}" 
           style="position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);">
        <defs>
          <linearGradient id="blueGradient-${this.selectedCity.geoid}" gradientUnits="userSpaceOnUse" 
            x1="${center - baseRadius}" y1="${center}" 
            x2="${center + baseRadius}" y2="${center}">
            <stop offset="0%" stop-color="#1a5276"/>
            <stop offset="50%" stop-color="#3498db"/>
            <stop offset="100%" stop-color="#85c1e9"/>
          </linearGradient>
        </defs>
        
        <circle 
          class="main-circle"
          cx="${center}" 
          cy="${center}" 
          r="${baseRadius}" 
          fill="white"
          fill-opacity="0"
          stroke="none"
          style="transition: fill-opacity 250ms ease-in;"
        />
        
        <g>
          <animateTransform 
            attributeName="transform" 
            type="rotate" 
            from="0 ${center} ${center}" 
            to="360 ${center} ${center}" 
            dur="3s" 
            repeatCount="indefinite"
          />
          <circle 
            class="gradient-ring"
            cx="${center}" 
            cy="${center}" 
            r="${baseRadius + 1}" 
            fill="none"
            stroke="url(#blueGradient-${this.selectedCity.geoid})" 
            stroke-width="${strokeWidth}"
            opacity="0"
            style="transition: opacity 250ms ease-in;"
          />
        </g>
      </svg>
    `;

    const animatedIcon = L.divIcon({
      html: svgHtml,
      className: 'selected-city-animated-ring',
      iconSize: [svgSize, svgSize],
      iconAnchor: [svgSize / 2, svgSize / 2]
    });

    const animatedRing = L.marker(
      [this.selectedCity.latitude, this.selectedCity.longitude],
      { icon: animatedIcon, interactive: false }
    ).addTo(this.map);

    // Fade in
    setTimeout(() => {
      const element = animatedRing.getElement() as HTMLElement | undefined;
      if (element) {
        const mainCircle = element.querySelector('.main-circle') as SVGCircleElement | null;
        const gradientRing = element.querySelector('.gradient-ring') as SVGCircleElement | null;
        
        if (mainCircle) {
          mainCircle.style.fillOpacity = '1';
        }
        if (gradientRing) {
          gradientRing.style.opacity = '1';
        }
      }
    }, 10);

    // After fade in completes, remove the underlying marker
    if (underlyingMarker) {
      setTimeout(() => {
        underlyingMarker.remove();
        (this as any)._hiddenMarkerData = {
          city: this.selectedCity,
          marker: underlyingMarker
        };
      }, 260);
    }

    (this as any)._animatedRing = animatedRing;
    (this as any)._animatedRingCityGeoid = this.selectedCity.geoid;
  }

  // Create/update an SVG overlay for the selected city (doesn't hide the original marker)
  private updateSelectedCityMarker(): void {
    // If no city is selected, fade out and remove existing overlay
    if (!this.selectedCity || !this.selectedCity.latitude || !this.selectedCity.longitude) {
      // First, re-add the old marker before fading out (only if city is still in visible results)
      if ((this as any)._hiddenMarkerData) {
        const data = (this as any)._hiddenMarkerData;
        if (data.marker && data.city && this.visibleCities.some((c: City) => c.geoid === data.city.geoid)) {
          data.marker.addTo(this.map);
        }
        (this as any)._hiddenMarkerData = null;
      }
      
      if ((this as any)._animatedRing) {
        const oldRing = (this as any)._animatedRing;
        const oldElement = oldRing.getElement() as HTMLElement | undefined;
        if (oldElement) {
          const svg = oldElement.querySelector('svg') as SVGElement | null;
          if (svg) {
            // Fade out the white fill to reveal colored marker underneath (slower - 350ms)
            const mainCircle = svg.querySelector('.main-circle') as SVGCircleElement | null;
            const gradientRing = svg.querySelector('.gradient-ring') as SVGCircleElement | null;
            
            if (mainCircle) {
              mainCircle.style.transition = 'fill-opacity 350ms ease-out';
              mainCircle.style.fillOpacity = '0';
            }
            if (gradientRing) {
              gradientRing.style.transition = 'opacity 350ms ease-out';
              gradientRing.style.opacity = '0';
            }
            
            setTimeout(() => oldRing.remove(), 360);
          } else {
            oldRing.remove();
          }
        } else {
          oldRing.remove();
        }
        (this as any)._animatedRing = null;
        (this as any)._animatedRingCityGeoid = null;
      }
      
      if (this.selectedCityMarker) {
        this.selectedCityMarker.remove();
        this.selectedCityMarker = null;
      }
      return;
    }

    // If overlay already exists for this city, don't recreate it
    if ((this as any)._animatedRing && (this as any)._animatedRingCityGeoid === this.selectedCity.geoid) {
      return;
    }

    // Remove existing without animation if we're replacing
    if ((this as any)._animatedRing) {
      (this as any)._animatedRing.remove();
      (this as any)._animatedRing = null;
      (this as any)._animatedRingCityGeoid = null;
    }
    if (this.selectedCityMarker) {
      this.selectedCityMarker.remove();
      this.selectedCityMarker = null;
    }

    // Check if the selected city is in visible cities
    const isInVisible = this.visibleCities.some(c => c.geoid === this.selectedCity!.geoid);

    // Find the underlying marker for this city (to hide after fade in)
    const underlyingMarker = this.markers.find(m => (m as any)._cityGeoid === this.selectedCity!.geoid);

    // Base radius
    const baseRadius = isInVisible ? this.getRadius(this.selectedCity) : 10;
    const strokeWidth = 3;
    const svgSize = (baseRadius + strokeWidth + 4) * 2;
    const center = svgSize / 2;

    // Create SVG overlay - white fill starts transparent, fades in to cover the colored marker
    const svgHtml = `
      <svg width="${svgSize}" height="${svgSize}" viewBox="0 0 ${svgSize} ${svgSize}" 
           style="position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);">
        <defs>
          <linearGradient id="blueGradient-${this.selectedCity.geoid}" gradientUnits="userSpaceOnUse" 
            x1="${center - baseRadius}" y1="${center}" 
            x2="${center + baseRadius}" y2="${center}">
            <stop offset="0%" stop-color="#1a5276"/>
            <stop offset="50%" stop-color="#3498db"/>
            <stop offset="100%" stop-color="#85c1e9"/>
          </linearGradient>
        </defs>
        
        <!-- White circle overlay - starts transparent, fades in -->
        <circle 
          class="main-circle"
          cx="${center}" 
          cy="${center}" 
          r="${baseRadius}" 
          fill="white"
          fill-opacity="0"
          stroke="none"
          style="transition: fill-opacity 250ms ease-in;"
        />
        
        <!-- Rotating gradient ring -->
        <g>
          <animateTransform 
            attributeName="transform" 
            type="rotate" 
            from="0 ${center} ${center}" 
            to="360 ${center} ${center}" 
            dur="3s" 
            repeatCount="indefinite"
          />
          <circle 
            class="gradient-ring"
            cx="${center}" 
            cy="${center}" 
            r="${baseRadius + 1}" 
            fill="none"
            stroke="url(#blueGradient-${this.selectedCity.geoid})" 
            stroke-width="${strokeWidth}"
            opacity="0"
            style="transition: opacity 250ms ease-in;"
          />
        </g>
      </svg>
    `;

    const animatedIcon = L.divIcon({
      html: svgHtml,
      className: 'selected-city-animated-ring',
      iconSize: [svgSize, svgSize],
      iconAnchor: [svgSize / 2, svgSize / 2]
    });

    const animatedRing = L.marker(
      [this.selectedCity.latitude, this.selectedCity.longitude],
      { icon: animatedIcon, interactive: false }
    ).addTo(this.map);

    // Fade in the white fill and gradient ring
    setTimeout(() => {
      const element = animatedRing.getElement() as HTMLElement | undefined;
      if (element) {
        const mainCircle = element.querySelector('.main-circle') as SVGCircleElement | null;
        const gradientRing = element.querySelector('.gradient-ring') as SVGCircleElement | null;
        
        if (mainCircle) {
          mainCircle.style.fillOpacity = '1';
        }
        if (gradientRing) {
          gradientRing.style.opacity = '1';
        }
      }
    }, 10);

    // After fade in completes, remove the underlying marker entirely
    if (underlyingMarker) {
      setTimeout(() => {
        underlyingMarker.remove();
        (this as any)._hiddenMarkerData = {
          city: this.selectedCity,
          marker: underlyingMarker
        };
      }, 260); // Right when fade completes
    }

    (this as any)._animatedRing = animatedRing;
    (this as any)._animatedRingCityGeoid = this.selectedCity.geoid;

    // Dummy marker for tracking
    this.selectedCityMarker = L.circleMarker(
      [this.selectedCity.latitude, this.selectedCity.longitude],
      { radius: 0, opacity: 0, fillOpacity: 0 }
    );
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

  // Get metrics suitable for color/size dropdowns (exclude balance type)
  getDropdownMetrics(category: FilterCategory): FilterMetric[] {
    return category.metrics.filter(m => m.type !== 'balance');
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
    // Clamp: can't go below metric.min or above current.max - step
    const clampedValue = Math.max(metric.min, Math.min(value, current.max - metric.step));
    current.min = clampedValue;
    this.filterValues.set(metric.key, current);
    this.onFilterChange(metric);
  }

  // Set filter max value
  setFilterMax(metric: FilterMetric, value: number): void {
    const current = this.filterValues.get(metric.key) || { min: metric.min, max: metric.max };
    // Clamp: can't go above metric.max or below current.min + step
    const clampedValue = Math.min(metric.max, Math.max(value, current.min + metric.step));
    current.max = clampedValue;
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

  getTotalFilterCount(): number {
    return this.activeFilters.size + this.classificationFilters.size + this.activeTrendFilters.size;
  }

  // Clear all filters
  clearAllFilters(): void {
    this.activeFilters.clear();
    this.classificationFilters.clear();
    this.activeTrendFilters.clear();
    
    // Reset all filter values to defaults
    this.filterCategories.forEach(cat => {
      cat.metrics.forEach(metric => {
        this.filterValues.set(metric.key, { min: metric.min, max: metric.max });
      });
    });

    // Reset trend filter values to defaults
    this.trendCategories.forEach(cat => {
      cat.metrics.forEach(metric => {
        this.trendFilterValues.set(metric.key, { min: metric.min, max: metric.max });
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
    this.cityClassification = '';
    this.updateSelectedCityMarker(); // NEW
  }

  getCityDisplayName(city: City): string {
    return city.name.replace(' city', '').replace(' town', '');
  }

  getCityNameFontSize(city: City): string {
    const name = this.getCityDisplayName(city);
    const len = name.length;
    if (len <= 10) return '1.5rem';
    if (len <= 14) return '1.3rem';
    if (len <= 18) return '1.15rem';
    if (len <= 22) return '1.0rem';
    return '0.9rem';
  }

  // ============================================================
  // URBANIZATION INDEX — now from backend derived stats
  // ============================================================

  getUrbanizationIndex(city: City): number | null {
    if (!city.geoid) return null;
    const ds = this.derivedStatsMap.get(city.geoid);
    return ds?.urbanizationIndex ?? null;
  }

  getDiversityIndex(city: City): number | null {
    if (!city.geoid) return null;
    const ds = this.derivedStatsMap.get(city.geoid);
    return ds?.diversityIndex ?? null;
  }

  // City Classification — from backend
  private updateCityClassification(): void {
    if (!this.selectedCity) {
      this.cityClassification = '';
      return;
    }
    this.cityClassification = this.getCityClassificationLabel(this.selectedCity);
  }

  // History Panel Methods
  openHistoryPanel(): void {
    this.showHistoryPanel = true;
    this.historyLoading = true;
    this.historyData = [];
    this.derivedHistoryData = [];
    
    if (this.selectedCity) {
      // City-level history - fetch city data + derived history in parallel
      forkJoin({
        cityHistory: this.cityService.getCityHistory(this.selectedCity.geoid),
        derivedHistory: this.cityService.getDerivedHistory(this.selectedCity.geoid)
      }).subscribe({
        next: ({ cityHistory, derivedHistory }) => {
          this.historyData = cityHistory.sort((a, b) => a.year - b.year);
          this.derivedHistoryData = derivedHistory.sort((a, b) => a.year - b.year);
          this.historyLoading = false;
          setTimeout(() => this.createAllHistoryCharts(), 50);
        },
        error: (err: Error) => {
          console.error('Error loading city history:', err);
          this.historyLoading = false;
        }
      });
    } else {
      // Texas-wide history - loop through years 2012-2024
      const years = Array.from({ length: 13 }, (_, i) => 2012 + i);
      const requests = years.map(y => this.cityService.getTexasStats(y));
      
      forkJoin(requests).subscribe({
        next: (results) => {
          this.historyData = results.map((stats: any, i: number) => this.texasStatsToCity(stats, years[i]))
            .sort((a, b) => a.year - b.year);
          this.historyLoading = false;
          setTimeout(() => this.createAllHistoryCharts(), 50);
        },
        error: (err: Error) => {
          console.error('Error loading Texas history:', err);
          this.historyLoading = false;
        }
      });
    }
  }

  // Convert texasStats response to City-shaped object for chart reuse
  // Convert texasStats response to City-shaped object for chart reuse
private texasStatsToCity(stats: any, year: number): City {
  const city = {
    year,
    geoid: 'texas',
    name: 'Texas',
    population: stats.totalPopulation,
    medianAge: stats.medianAge,
    medianHouseholdIncome: stats.medianHouseholdIncome,
    perCapitaIncome: stats.perCapitaIncome,
    medianHomeValue: stats.medianHomeValue,
    medianRent: stats.medianRent,
    ownerOccupied: stats.ownerOccupied,
    renterOccupied: stats.renterOccupied,
    employed: stats.employed,
    unemployed: stats.unemployed,
    laborForce: stats.laborForce,
    workFromHome: stats.workFromHome,
    malePopulation: stats.totalMale,
    femalePopulation: stats.totalFemale,
    whitePopulation: stats.whitePopulation,
    blackPopulation: stats.blackPopulation,
    asianPopulation: stats.asianPopulation,
    hispanicPopulation: stats.hispanicPopulation,
    nativeAmericanPopulation: stats.nativeAmericanPopulation,
    pacificIslanderPopulation: stats.pacificIslanderPopulation,
    twoOrMoreRacesPopulation: stats.twoOrMoreRacesPopulation,
    otherRacePopulation: stats.otherRacePopulation,
    latitude: 31.0,
    longitude: -100.0,
    landAreaSqMi: 268596,
  } as City;
  
  // Attach Texas-specific derived stats directly to the object
  (city as any)._texasDiversityIndex = stats.diversityIndex;
  (city as any)._texasUrbanizationIndex = stats.weightedUrbanizationIndex;
  
  return city;
}

  closeHistoryPanel(): void {
    this.showHistoryPanel = false;
    this.destroyAllHistoryCharts();
  }

  private historyChartInstances: any[] = [];

  private destroyAllHistoryCharts(): void {
    this.historyChartInstances.forEach(chart => {
      if (chart) chart.destroy();
    });
    this.historyChartInstances = [];
  }

  private getMetricColor(key: string): string {
    const colors: { [k: string]: string } = {
      population: '#3498db', medianAge: '#e67e22',
      medianHouseholdIncome: '#27ae60', perCapitaIncome: '#2ecc71',
      medianHomeValue: '#9b59b6', medianRent: '#1abc9c', ownershipRate: '#16a085',
      unemploymentRate: '#e74c3c', laborForcePct: '#3498db', workFromHomePct: '#1abc9c',
      malePct: '#3498db', femalePct: '#e91e8c',
      whitePct: '#95a5a6', blackPct: '#2c3e50', asianPct: '#e74c3c',
      nativeAmericanPct: '#8e44ad', pacificIslanderPct: '#1abc9c', twoOrMorePct: '#f39c12', otherRacePct: '#7f8c8d',
      hispanicPct: '#e67e22',
      urbanIndex: '#2c3e50',
      diversityIndex: '#e74c3c',
    };
    return colors[key] || '#3498db';
  }

    private getChartYMin(metricKey: string, data: (number | null)[]): number | undefined {
    const fixedScaleMetrics = [
      'diversityIndex', 'ownershipRate',
      'whitePct', 'blackPct', 'asianPct', 'hispanicPct',
      'nativeAmericanPct', 'pacificIslanderPct', 'twoOrMorePct', 'otherRacePct'
    ];
    
    if (fixedScaleMetrics.includes(metricKey)) {
      return 0;
    }
    
    // Median Age: ensure at least 1 unit range with integer bounds
    if (metricKey === 'medianAge') {
      const validData = data.filter(d => d != null) as number[];
      if (validData.length === 0) return undefined;
      const min = Math.min(...validData);
      const max = Math.max(...validData);
      const range = max - min;
      if (range < 1) {
        return Math.floor(min);
      }
    }
    
    return undefined;
  }

  private getChartYMax(metricKey: string, data: (number | null)[]): number | undefined {
    const fixedScaleMetrics = [
      'diversityIndex', 'ownershipRate',
      'whitePct', 'blackPct', 'asianPct', 'hispanicPct',
      'nativeAmericanPct', 'pacificIslanderPct', 'twoOrMorePct', 'otherRacePct'
    ];
    
    if (fixedScaleMetrics.includes(metricKey)) {
      return 100;
    }
    
    // Median Age: ensure at least 1 unit range with integer bounds
    if (metricKey === 'medianAge') {
      const validData = data.filter(d => d != null) as number[];
      if (validData.length === 0) return undefined;
      const min = Math.min(...validData);
      const max = Math.max(...validData);
      const range = max - min;
      if (range < 1) {
        return Math.ceil(max);
      }
    }
    
    return undefined;
  }

  private createAllHistoryCharts(): void {
    this.destroyAllHistoryCharts();
    
    if (!this.historyData.length) return;

    const years = this.historyData.map(d => d.year);

    this.historyMetrics.forEach(metric => {
      // DONT Skip urbanization/diversity index for Texas overall 

      const canvas = document.getElementById('chart-' + metric.key) as HTMLCanvasElement;
      if (!canvas) return;

      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      // For derived stats (urbanization/diversity index), use derived history data from backend
      let data: (number | null)[];
      let chartYears: number[];
      if (metric.key === 'urbanIndex' && this.derivedHistoryData.length > 0 && this.selectedCity) {
        chartYears = this.derivedHistoryData.map(d => d.year);
        data = this.derivedHistoryData.map(d => d.urbanizationIndex);
      } else if (metric.key === 'diversityIndex' && this.derivedHistoryData.length > 0 && this.selectedCity) {
        chartYears = this.derivedHistoryData.map(d => d.year);
        data = this.derivedHistoryData.map(d => d.diversityIndex);
      } else {
        chartYears = years;
        data = this.historyData.map(d => metric.getValue(d));
        if (metric.key === 'malePct') {
  const validData = data.filter(d => d != null) as number[];
  const min = Math.min(...validData);
  const max = Math.max(...validData);
  console.log('malePct data:', { min, max, range: max - min, yMin: this.getChartYMin(metric.key, data), yMax: this.getChartYMax(metric.key, data) });
}
      }

      const color = this.getMetricColor(metric.key);

      // Special config for urbanization index chart
      const isUrbanIndex = metric.key === 'urbanIndex';
      
      // Zone background plugin for urbanization chart
      const zoneBgPlugin = isUrbanIndex ? {
        id: 'zoneBg',
        beforeDraw: (chart: any) => {
          const { ctx, chartArea, scales } = chart;
          if (!chartArea || !scales?.y) return;
          const yScale = scales.y;
          const left = chartArea.left;
          const right = chartArea.right;

          // Rural zone: 0-37 (brownish)
          const ruralTop = yScale.getPixelForValue(37);
          const ruralBottom = yScale.getPixelForValue(0);
          ctx.fillStyle = 'rgba(160, 120, 80, 0.10)';
          ctx.fillRect(left, ruralTop, right - left, ruralBottom - ruralTop);

          // Suburban zone: 37-75 (yellowish)
          const subTop = yScale.getPixelForValue(73);
          const subBottom = yScale.getPixelForValue(37);
          ctx.fillStyle = 'rgba(241, 196, 15, 0.10)';
          ctx.fillRect(left, subTop, right - left, subBottom - subTop);

          // Urban zone: 75-100 (greenish)
          const urbTop = yScale.getPixelForValue(100);
          const urbBottom = yScale.getPixelForValue(73);
          ctx.fillStyle = 'rgba(39, 174, 96, 0.10)';
          ctx.fillRect(left, urbTop, right - left, urbBottom - urbTop);

          // Zone labels
          ctx.font = '10px sans-serif';
          ctx.fillStyle = 'rgba(160, 120, 80, 0.5)';
          ctx.fillText('Rural', left + 4, ruralBottom - 6);
          ctx.fillStyle = 'rgba(180, 150, 10, 0.5)';
          ctx.fillText('Suburban', left + 4, subBottom - 6);
          ctx.fillStyle = 'rgba(39, 174, 96, 0.5)';
          ctx.fillText('Urban', left + 4, urbBottom - 6);
        }
      } : null;

      const chartConfig: any = {
        type: 'line',
        data: {
          labels: chartYears,
          datasets: [{
            label: metric.label,
            data: data,
            borderColor: color,
            backgroundColor: isUrbanIndex ? 'transparent' : color + '20',
            borderWidth: 2.5,
            fill: !isUrbanIndex,
            tension: 0.3,
            pointRadius: 4,
            pointHoverRadius: 7,
            pointBackgroundColor: color,
            pointBorderColor: '#fff',
            pointBorderWidth: 2
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: {
            duration: 400,
            easing: 'easeOutQuart'
          },
          plugins: {
            legend: { display: false },
            tooltip: {
              backgroundColor: '#2c3e50',
              titleFont: { size: 13 },
              bodyFont: { size: 13 },
              padding: 12,
              cornerRadius: 8,
              callbacks: {
                label: (context: any) => ' ' + metric.format(context.raw)
              }
            }
          },
          scales: {
            x: {
              grid: { display: false },
              ticks: { font: { size: 11 }, color: '#6c757d' }
            },
           y: {
            min: isUrbanIndex ? 0 : this.getChartYMin(metric.key, data),
            max: isUrbanIndex ? 100 : this.getChartYMax(metric.key, data),
            grace: 0,
            grid: { color: isUrbanIndex ? 'transparent' : '#f0f0f0' },
            ticks: {
              font: { size: 11 },
              color: '#6c757d',
              stepSize: metric.key === 'medianAge' ? 1 : undefined,
              autoSkip: true,
              maxTicksLimit: (metric.key === 'malePct' || metric.key === 'femalePct') ? 5 : undefined,
              callback: (value: number) => metric.yFormat(value)
            }
          }
          }
        }
      };

      // Add zone background plugin for urbanization chart
      if (zoneBgPlugin) {
        chartConfig.plugins = [zoneBgPlugin];
      }

      const chart = new (window as any).Chart(ctx, chartConfig);

      this.historyChartInstances.push(chart);
    });
  }

  getMetricChange(metric: any): { value: string; positive: boolean } | null {
    // For urbanization index, use derived history data
    if (metric.key === 'urbanIndex' && this.derivedHistoryData.length >= 2) {
      const first = this.derivedHistoryData[0].urbanizationIndex;
      const last = this.derivedHistoryData[this.derivedHistoryData.length - 1].urbanizationIndex;
      if (!first || !last) return null;
      const change = ((last - first) / Math.abs(first)) * 100;
      return {
        value: (change >= 0 ? '+' : '') + change.toFixed(1) + '%',
        positive: change >= 0
      };
    }

    // For diversity index, use derived history data
    if (metric.key === 'diversityIndex' && this.derivedHistoryData.length >= 2) {
      const first = this.derivedHistoryData[0].diversityIndex;
      const last = this.derivedHistoryData[this.derivedHistoryData.length - 1].diversityIndex;
      if (!first || !last) return null;
      const change = ((last - first) / Math.abs(first)) * 100;
      return {
        value: (change >= 0 ? '+' : '') + change.toFixed(1) + '%',
        positive: change >= 0
      };
    }

    if (this.historyData.length < 2) return null;
    
    const first = metric.getValue(this.historyData[0]);
    const last = metric.getValue(this.historyData[this.historyData.length - 1]);
    
    if (!first || !last) return null;
    
    const change = ((last - first) / Math.abs(first)) * 100;
    return {
      value: (change >= 0 ? '+' : '') + change.toFixed(1) + '%',
      positive: change >= 0
    };
  }

  getHistoryMetricCategories(): string[] {
    const cats: string[] = [];
    this.historyMetrics.forEach(m => {
      if (!cats.includes(m.category)) cats.push(m.category);
    });
    return cats;
  }

  getHistoryMetricsByCategory(category: string) {
    return this.historyMetrics.filter(m => m.category === category);
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
    this.updateCityClassification();
    this.searchQuery = '';
    this.searchResults = [];
    this.mobileCardExpanded = false;
    
    if (city.latitude && city.longitude && this.map) {
      this.map.setView([city.latitude, city.longitude], 9, { animate: true });
    }
    
    this.updateSelectedCityMarker(); // NEW: Show the selected marker
    this.cdr.detectChanges();
  }
}