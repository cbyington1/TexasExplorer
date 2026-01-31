# Texas Explorer - Backend Documentation

A Spring Boot REST API that provides comprehensive demographic, economic, and social data for all cities in Texas from 2012-2024 (as of 2026).

## 📋 Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Data Sources](#data-sources)
- [Automated Updates](#automated-updates)
- [Setup & Installation](#setup--installation)
- [Configuration](#configuration)
- [Testing](#testing)
- [Deployment](#deployment)

---

## 🎯 Overview

Texas Explorer provides a REST API for accessing historical demographic data for 1,700-1,860+ Texas cities across 13 years (2012-2024). The application:

- Pulls data from U.S. Census Bureau's American Community Survey (ACS) 5-Year Estimates
- Stores 50+ variables per city including demographics, economics, education, housing, and transportation
- Automatically checks for and loads new data as it becomes available
- Provides historical tracking to analyze trends over time

**Key Stats:**
- **23,145** total records
- **13** years of data (2012-2024)
- **1,700-1,860+** cities per year
- **50+** data points per city

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Programming language |
| **Spring Boot** | 3.5.10 | Application framework |
| **Spring Data JPA** | 3.5.x | Database ORM |
| **Hibernate** | 6.6.41 | JPA implementation |
| **PostgreSQL** | Latest | Database (Railway cloud hosting) |
| **Maven** | 3.9.x | Build & dependency management |
| **Jackson** | 2.x | JSON parsing |

---

## 📁 Project Structure

```
backend/
├── src/main/java/com/texasexplorer/
│   ├── BackendApplication.java          # Spring Boot entry point
│   │
│   ├── City.java                        # JPA Entity (data model)
│   ├── CityRepository.java              # Data access layer
│   ├── CityService.java                 # Business logic layer
│   ├── CityController.java              # REST API endpoints
│   │
│   ├── CensusApiService.java            # Census API integration
│   ├── DataLoader.java                  # Initial data loading
│   ├── DataUpdateService.java           # Update logic (reusable)
│   ├── ScheduledDataUpdater.java        # Automated scheduling
│   └── AdminController.java             # Admin API endpoints
│
├── src/main/resources/
│   ├── application.properties           # Configuration
│   └── 2024_gaz_place_48.txt           # Geographic data
│
└── pom.xml                              # Maven dependencies
```

### Architecture Layers

```
┌─────────────────────────────────────┐
│   REST Controllers                   │  ← HTTP endpoints (CityController, AdminController)
├─────────────────────────────────────┤
│   Service Layer                      │  ← Business logic (CityService, DataUpdateService)
├─────────────────────────────────────┤
│   Repository Layer                   │  ← Database queries (CityRepository)
├─────────────────────────────────────┤
│   JPA Entity                         │  ← Data model (City)
├─────────────────────────────────────┤
│   PostgreSQL Database                │  ← Persistence
└─────────────────────────────────────┘
```

---

## 🗄️ Database Schema

### Cities Table

The `cities` table stores all city data with a composite unique constraint on `(geoid, year)`, allowing multiple years per city without duplicates.

**Key Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGSERIAL | Auto-generated primary key |
| `geoid` | VARCHAR | Census GEOID (e.g., "4835000" for Houston) |
| `name` | VARCHAR | City name |
| `year` | INTEGER | Data year (2012-2024) |
| `latitude` | DOUBLE | Geographic latitude |
| `longitude` | DOUBLE | Geographic longitude |
| `population` | INTEGER | Total population |
| `median_household_income` | INTEGER | Median household income ($) |
| `median_home_value` | INTEGER | Median home value ($) |
| ... | ... | 50+ additional demographic fields |

**Unique Constraint:** `UNIQUE(geoid, year)` - Prevents duplicate records

**Example Records:**
```
Houston 2012: geoid=4835000, year=2012, population=2,195,914
Houston 2022: geoid=4835000, year=2022, population=2,314,157
Houston 2024: geoid=4835000, year=2024, population=2,328,253
```

### Data Categories

- **Demographics** (10 fields): Population, age distribution, gender
- **Race & Ethnicity** (8 fields): Racial composition, Hispanic population
- **Economic** (10 fields): Income distribution, poverty rates, SNAP usage
- **Employment** (4 fields): Employment status, labor force participation
- **Education** (6 fields): Educational attainment levels
- **Housing** (5 fields): Home values, rent, occupancy
- **Transportation** (4 fields): Commute patterns, work-from-home
- **Other** (6 fields): Nativity, citizenship, language, veterans

See full variable list in [Census Variables Documentation](../docs/census_variables.md).

---

## 🌐 API Endpoints

Base URL: `http://localhost:8080/api/cities`

### City Data Endpoints

#### Get All Available Years
```http
GET /api/cities/years
```
Returns array of years with data available.

**Response:**
```json
[2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024]
```

---

#### Get All Cities for a Year
```http
GET /api/cities/{year}
```

**Parameters:**
- `year` (path): Year to query (e.g., 2022)

**Response:** Array of all cities for that year, ordered by population (descending)

---

#### Get Top 10 Cities
```http
GET /api/cities/{year}/top
```

**Response:** Top 10 cities by population for specified year

**Example:**
```bash
curl http://localhost:8080/api/cities/2024/top
```

---

#### Search Cities
```http
GET /api/cities/{year}/search?q={query}
```

**Parameters:**
- `year` (path): Year to search
- `q` (query): Search term (case-insensitive, partial match)

**Example:**
```bash
curl "http://localhost:8080/api/cities/2022/search?q=san"
# Returns: San Antonio, San Marcos, San Angelo, etc.
```

---

#### Filter by Population
```http
GET /api/cities/{year}/population/{minPop}
```

**Parameters:**
- `year` (path): Year to query
- `minPop` (path): Minimum population threshold

**Example:**
```bash
curl http://localhost:8080/api/cities/2022/population/100000
# Returns all cities with population > 100,000
```

---

#### Get City History
```http
GET /api/cities/history/{geoid}
```

Returns all years of data for a specific city.

**Parameters:**
- `geoid` (path): Census GEOID (e.g., "4835000" for Houston)

**Response:** Array of city records from 2012-2024

**Example:**
```bash
curl http://localhost:8080/api/cities/history/4835000
# Returns Houston data for all 13 years
```

---

#### Get Specific City for Specific Year
```http
GET /api/cities/city/{geoid}/{year}
```

**Parameters:**
- `geoid` (path): Census GEOID
- `year` (path): Year to query

**Example:**
```bash
curl http://localhost:8080/api/cities/city/4835000/2022
```

---

#### Compare City Across Years
```http
GET /api/cities/compare?city={name}&years={y1,y2,...}
```

**Parameters:**
- `city` (query): City name (case-insensitive)
- `years` (query): Comma-separated years

**Example:**
```bash
curl "http://localhost:8080/api/cities/compare?city=Houston&years=2012,2022"
```

**Response:**
```json
{
  "city": "Houston",
  "data": {
    "2012": { "population": 2195914, "medianHouseholdIncome": 43820, ... },
    "2022": { "population": 2314157, "medianHouseholdIncome": 64813, ... }
  }
}
```

---

#### Get Database Statistics
```http
GET /api/cities/stats
```

Returns overall database statistics.

**Response:**
```json
{
  "years": [2012, 2013, ..., 2024],
  "yearCount": 13,
  "totalRecords": 23145,
  "latestYear": 2024,
  "citiesInLatestYear": 1863
}
```

---

### Admin Endpoints

Base URL: `http://localhost:8080/api/admin`

#### Check Update Status
```http
GET /api/admin/update-status
```

Returns information about data currency and missing years.

**Response:**
```json
{
  "available_years": [2012, ..., 2024],
  "total_records": 23145,
  "current_year": 2026,
  "expected_latest_year": 2024,
  "latest_year_in_db": 2024,
  "is_up_to_date": true,
  "years_behind": 0
}
```

---

#### Manually Trigger Data Update
```http
GET /api/admin/update-data
```

Manually checks for and loads missing years.

**Response:**
```json
{
  "message": "Data update check initiated",
  "timestamp": "2026-01-30T14:30:00",
  "years_before": [2012, ..., 2023],
  "records_before": 21286,
  "years_after": [2012, ..., 2024],
  "records_after": 23145,
  "new_records": 1859,
  "status": "success"
}
```

---

#### Health Check
```http
GET /api/admin/health
```

Simple health check endpoint.

---

## 📊 Data Sources

### U.S. Census Bureau API

**Source:** American Community Survey (ACS) 5-Year Estimates

**API Endpoint Pattern:**
```
https://api.census.gov/data/{YEAR}/acs/acs5?get={VARIABLES}&for=place:*&in=state:48&key={API_KEY}
```

**Parameters:**
- `YEAR`: 2012-2024
- `VARIABLES`: Comma-separated Census variable codes (e.g., B01003_001E for population)
- `state:48`: Texas FIPS code
- `place:*`: All places (cities, towns, CDPs)

**Data Release Schedule:**
- ACS 5-Year Estimates are released in **December** each year
- Data has a **2-year lag** (e.g., 2022 data released in December 2024)

### Census Gazetteer Files

**Source:** U.S. Census Bureau Gazetteer Files

**Purpose:** Provides geographic coordinates and land/water area

**File:** `2024_gaz_place_48.txt`

**Fields Used:**
- GEOID (unique place identifier)
- NAME (place name)
- INTPTLAT (latitude)
- INTPTLONG (longitude)
- ALAND_SQMI (land area in square miles)
- AWATER_SQMI (water area in square miles)

---

## 🔄 Automated Updates

The application automatically checks for and loads new Census data on a schedule.

### Scheduled Tasks

**Weekly Check (Every Sunday at 2:00 AM):**
```java
@Scheduled(cron = "0 0 2 * * SUN")
```
- Regular maintenance check for any missed data
- Runs in background without user intervention

**December Release Check (1st and 15th at 3:00 AM):**
```java
@Scheduled(cron = "0 0 3 1,15 12 *")
```
- Targets ACS data release windows
- Census typically releases new data in December

### How It Works

1. **Calculate Expected Years**
   - Current year: 2026
   - Data lag: 2 years
   - Latest expected: 2024

2. **Check Database**
   - Get years currently stored
   - Compare with expected years

3. **Load Missing Years**
   - If 2024 is missing, load it
   - If multiple years missing, load all

4. **Prevent Duplicates**
   - Database constraint ensures no duplicates
   - Safe to run multiple times

### Manual Trigger

You can manually trigger an update check:

```bash
curl http://localhost:8080/api/admin/update-data
```

**Use cases:**
- Testing the update process
- Emergency data load
- Verify scheduler is working

---

## 🚀 Setup & Installation

### Prerequisites

- Java 21 or higher
- Maven 3.9.x
- PostgreSQL database (or use provided Railway connection)
- Census API key ([Get one free here](https://api.census.gov/data/key_signup.html))

### Installation Steps

1. **Clone the repository**
```bash
git clone https://github.com/cbyington1/TexasExplorer.git
cd TexasExplorer/backend
```

2. **Configure application.properties**
```properties
# Database connection
spring.datasource.url=jdbc:postgresql://your-host:5432/your-database
spring.datasource.username=your-username
spring.datasource.password=your-password

# Census API key
census.api.key=your-api-key-here
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run the application**
```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### First Run

On first startup:
- Database tables are created automatically (Hibernate DDL)
- DataLoader checks if database is empty
- If empty, loads all available years (2012-2024)
- Process takes ~15-20 seconds per year
- Total initial load: ~3-5 minutes

**Console Output:**
```
=== Starting Initial Data Load ===
Loading 13 year(s): [2012, 2013, ..., 2024]
Loading Gazetteer file...
Loaded 1863 places from Gazetteer

--- Year 2012 ---
Fetching Census data for year 2012...
  Fetched 1727 places for 2012
  Matched: 1727, Saved: 1727

[... continues for each year ...]

=== Initial Load Complete ===
Total records: 23145
```

---

## ⚙️ Configuration

### application.properties

```properties
# Application name
spring.application.name=texas-explorer

# Database configuration
spring.datasource.url=jdbc:postgresql://host:port/database
spring.datasource.username=postgres
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Server configuration
server.port=8080

# Census API
census.api.key=your-api-key

# Scheduling configuration
spring.task.scheduling.enabled=true
spring.task.scheduling.pool.size=2
spring.task.scheduling.time-zone=America/Chicago
```

### Important Settings

**`spring.jpa.hibernate.ddl-auto`**
- `update`: Add new columns, preserve existing data (recommended)
- `create`: Drop and recreate tables on startup (data loss!)
- `validate`: Only validate schema, don't change anything

**`spring.task.scheduling.enabled`**
- `true`: Enable scheduled tasks (recommended for production)
- `false`: Disable automatic updates (useful for development)

**`spring.task.scheduling.time-zone`**
- Set to your server's timezone
- Default: `America/Chicago` (Central Time)

---

## 🧪 Testing

### Test API Endpoints

**Using curl:**
```bash
# Get all years
curl http://localhost:8080/api/cities/years

# Get top cities for 2024
curl http://localhost:8080/api/cities/2024/top

# Search for Houston
curl "http://localhost:8080/api/cities/2022/search?q=houston"

# Get statistics
curl http://localhost:8080/api/cities/stats

# Manually trigger update
curl http://localhost:8080/api/admin/update-data
```

**Using browser:**
```
http://localhost:8080/api/cities/years
http://localhost:8080/api/cities/2024/top
http://localhost:8080/api/cities/stats
```

### Test Scheduled Updates

Temporarily modify the cron expression for testing:

```java
// In ScheduledDataUpdater.java

// Test: Run every minute
@Scheduled(cron = "0 * * * * *")

// Test: Run every 30 seconds
@Scheduled(fixedDelay = 30000)
```

**Remember to change back before deploying!**

### Test Modes

**DataLoader Test Mode:**

```java
// In DataLoader.java
private static final boolean TEST_MODE = true;  // Only loads 2022
private static final boolean TEST_MODE = false; // Loads all years
```

---

## 🚢 Deployment

### Railway Deployment

This application is configured for Railway deployment with PostgreSQL.

**Steps:**

1. **Create Railway account** at [railway.app](https://railway.app)

2. **Create new project** and add PostgreSQL database

3. **Set environment variables:**
```
CENSUS_API_KEY=your-api-key
SPRING_DATASOURCE_URL=${DATABASE_URL}
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

4. **Deploy from GitHub:**
```bash
railway link
railway up
```

5. **Monitor logs:**
```bash
railway logs
```

### Production Considerations

**Database Backups:**
- Railway provides automatic daily backups
- Consider additional backup strategy for critical data

**Monitoring:**
- Use Railway metrics dashboard
- Monitor scheduled task logs
- Set up alerts for failed updates

**Scaling:**
- PostgreSQL connection pooling handled by HikariCP
- Increase `spring.task.scheduling.pool.size` if needed

**Security:**
- Never commit API keys or passwords
- Use environment variables for sensitive data
- Change `@CrossOrigin(origins = "*")` to specific domain in production

---

## 📝 Logging

### Log Format

All scheduled tasks use standardized logging:

```
[2026-01-30 02:00:00] [ScheduledDataUpdater] Starting data check
[2026-01-30 02:00:01] [DataUpdateService] Loading Year 2024
[2026-01-30 02:00:05] [ScheduledDataUpdater] === UPDATE COMPLETE ===
```

### Viewing Logs

**Local development:**
```bash
# Console output shows all logs
mvn spring-boot:run
```

**Production (Railway):**
```bash
railway logs
```

### Log Levels

Configure in `application.properties`:
```properties
logging.level.root=INFO
logging.level.com.texasexplorer=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

---

## 🤝 Contributing

### Adding New Endpoints

1. Add method to `CityRepository.java` (if new query needed)
2. Add method to `CityService.java` (business logic)
3. Add endpoint to `CityController.java` (HTTP handler)

**Example:**

```java
// 1. Repository
List<City> findByYearAndMedianHouseholdIncomeGreaterThan(Integer year, Integer minIncome);

// 2. Service
public List<City> getHighIncomeCities(Integer year, Integer minIncome) {
    return cityRepository.findByYearAndMedianHouseholdIncomeGreaterThan(year, minIncome);
}

// 3. Controller
@GetMapping("/{year}/income/{minIncome}")
public ResponseEntity<List<City>> getHighIncomeCities(
        @PathVariable Integer year,
        @PathVariable Integer minIncome) {
    return ResponseEntity.ok(cityService.getHighIncomeCities(year, minIncome));
}
```

### Adding New Census Variables

1. Add field to `City.java` entity
2. Add Census variable code to `CensusApiService.java`
3. Map in `buildCity()` method in `DataUpdateService.java`
4. Restart application (Hibernate will add new column)

---

## 📚 Additional Resources

- [U.S. Census Bureau API](https://www.census.gov/data/developers/data-sets.html)
- [ACS 5-Year Estimates](https://www.census.gov/data/developers/data-sets/acs-5year.html)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

---

## 📄 License

[Your License Here]

---

## 👤 Author

**Camden Byington**

- GitHub: [@cbyington1](https://github.com/cbyington1)
- Repository: [github.com/cbyington1/TexasExplorer](https://github.com/cbyington1/TexasExplorer)

---

## 📞 Support

For questions or issues:
- Open an issue on GitHub
- Check existing documentation
- Review API endpoint examples above

---

**Last Updated:** January 30, 2026