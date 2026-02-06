# Texas Explorer

Interactive map visualizing demographic, economic, and housing data for 1,800+ Texas cities from 2012-2024. Click around, filter by any metric, compare trends across years.

**Live**: https://dleqhwum800of.cloudfront.net/

## Stack

**Frontend**: Angular 17, Leaflet.js, Chart.js  
**Backend**: Spring Boot 3 (Java 17), PostgreSQL  
**Hosting**: Frontend on AWS S3/CloudFront, Backend on Railway

## Running Locally

**Backend**:
```bash
cd backend
./mvnw spring-boot:run
```
Runs on `http://localhost:5000`

**Frontend**:
```bash
cd frontend
npm install
ng serve
```
Runs on `http://localhost:4200`

Frontend proxy routes `/api/*` to the backend automatically.

## Project Structure
```
backend/
├── src/main/java/com/texasexplorer/
│   ├── City.java                    # Main entity - all Census fields
│   ├── CityRepository.java
│   ├── CityController.java          # CRUD endpoints
│   ├── derived/
│   │   ├── DerivedStats.java        # Urbanization/diversity/classification
│   │   ├── DerivedStatsService.java # Computes indexes and classifications
│   │   └── DerivedStatsController.java
│   └── stats/
│       ├── TexasStats.java          # Statewide aggregates per year
│       ├── TexasStatsService.java   # Population-weighted averages
│       └── TexasStatsController.java

frontend/
├── src/app/
│   ├── map/
│   │   ├── map.component.ts         # Main component (~2000 lines)
│   │   ├── map.component.html       # Desktop sidebar + mobile cards
│   │   └── map.component.css        # All styles (~2200 lines)
│   ├── city.service.ts              # API calls
│   └── city.model.ts                # TypeScript interfaces
```

## Database

PostgreSQL on Railway. Schema:

- **cities** - Raw Census data (population, income, race, housing, employment, etc.) - one row per city per year  
- **derived_stats** - Computed metrics (urbanization index 0-100, diversity index 0-100, classification Rural/Suburban/Urban)  
- **texas_stats** - Cached statewide aggregates per year

Unique constraints on `(geoid, year)` for cities and derived_stats.

## Key Features

- **Map**: Circle markers sized/colored by any metric, zoom-dependent visibility (top N cities)
- **Filters**: Dual-range sliders for 30+ metrics, classification checkboxes, male/female balance slider
- **Trends**: Compare two years, filter by percent change (±100% or ±500% ranges)
- **History**: Chart.js graphs showing 2012-2024 for any city or Texas overall. Smart y-axis: 0-100 for percentages, auto-scale for values, min 1-unit range for median age
- **Mobile**: Collapsible top card (filters) and bottom card (stats), auto-collapse when opening the other

## Algorithms

**Urbanization Index** (0-100): Weighted score from population (30%), density (25%), home value (20%), income (15%), labor force (10%). Log-scaled for population/density. Overrides: 250k+ pop → Urban, largest city within 60mi + 75k+ pop → Urban.

**Diversity Index** (Simpson's 0-100): Measures racial diversity. Requires 90% data coverage (many rural cities have NULL values for minority groups due to Census privacy rules). Formula: `(1 - Σ(p_i²)) * 100 / theoretical_max`

**Classification**: Rural (0-37), Suburban (37-73), Urban (73-100), with population overrides

## API Endpoints

**Cities**:
- `GET /api/cities/years` - Available years
- `GET /api/cities/year/{year}` - All cities
- `GET /api/cities/history/{geoid}` - City history

**Derived Stats**:
- `GET /api/derived/{year}` - Computed metrics for all cities
- `GET /api/derived/trends?current={year}&base={year}` - Trend data
- `POST /api/derived/recalculate/{year}` - Recalculate one year
- `POST /api/derived/recalculate-all` - Recalculate everything

**Texas Stats**:
- `GET /api/texas-stats/{year}` - Statewide aggregates
- `POST /api/admin/recalculate-texas-stats` - Regenerate all

## Deploying

**Backend** (Railway): Push to GitHub, auto-deploys from main branch

**Frontend** (AWS): Build → S3 → CloudFront invalidation (script in `.gitignore`)

After schema changes or algorithm updates, hit the recalculate endpoints to rebuild derived data.

## Data Source

American Community Survey (ACS) 5-year estimates, 2012-2024. ~1,800 incorporated cities in Texas. Total population coverage ~23M (Texas overall ~30M - difference is unincorporated areas).
