================================================================================
                              LIBSEARCH
                Academic Federated Search Application
================================================================================

LibSearch is a comprehensive research paper discovery tool that enables
simultaneous searching across multiple academic sources. It features both a
rich JavaFX GUI and an interactive command-line interface (REPL).

================================================================================
                              FEATURES
================================================================================

FEDERATED SEARCH
  - Search multiple academic sources simultaneously:
    * arXiv, PubMed, CrossRef, Semantic Scholar, Google Scholar
  - Institutional sources: Primo, OBV Network (Austrian library network)
  - Developer resources: GitHub, StackOverflow, Reddit
  - General search engines: DuckDuckGo, Bing, Brave, Wikipedia

ADVANCED QUERY SYNTAX
  - Grep-like syntax with author filters, year ranges, file types
  - Boolean operators (AND, OR, NOT)
  - Field-specific searches

FULL-TEXT INDEXING
  - Apache Lucene-based offline search on cached results
  - Auto-indexing of search results
  - Field-specific indexing (title, authors, abstract, year, keywords)

PDF DOWNLOADS
  - Queue-based bulk download management
  - Progress tracking

RESEARCH FEATURES
  - Bookmarks with tags and notes
  - Citation export (BibTeX, RIS, EndNote formats)
  - Keyword monitoring and alerts
  - Session management and activity journaling

DUAL INTERFACES
  - Rich JavaFX GUI with modern styling
  - Interactive CLI (REPL) mode via Picocli/JLine3

================================================================================
                           PREREQUISITES
================================================================================

  - Java 21 or higher (Amazon Corretto recommended)
  - Gradle 8.5 or higher

================================================================================
                         BUILD INSTRUCTIONS
================================================================================

1. Clone the repository:
   git clone <repository-url>
   cd team-Se1-CSDC-HCW

2. Build the project:
   ./gradlew clean build

3. Create a fat JAR (includes all dependencies):
   ./gradlew shadowJar

================================================================================
                          RUN INSTRUCTIONS
================================================================================

GUI MODE
--------
Option 1 - Using Gradle:
   ./gradlew run

Option 2 - Using shell script:
   ./libsearch.sh

Option 3 - Direct JAR:
   java -jar target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar


CLI MODE
--------
Search command:
   java -jar target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar search "machine learning"

Interactive REPL:
   java -jar target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar --interactive

Bookmark management:
   java -jar target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar bookmark list

Index statistics:
   java -jar target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar index stats

Network diagnostics:
   java -jar target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar network diag


RUNNING TESTS
-------------
   ./gradlew test

================================================================================
                         PROJECT STRUCTURE
================================================================================

src/main/java/com/example/teamse1csdchcw/
|
+-- domain/                    Data models (POJOs, enums)
|   +-- search/               SearchResult, SearchQuery, AcademicPaper
|   +-- source/               SourceType enum
|   +-- user/                 Bookmark model
|   +-- monitoring/           Alert configuration
|   +-- export/               ExportFormat enum
|
+-- service/                  Business logic layer
|   +-- search/               FederatedSearchService, QueryParserService
|   +-- connector/            Academic source connectors + factory
|   +-- index/                IndexService (Lucene), LocalSearchService
|   +-- download/             DownloadService (queue management)
|   +-- bookmark/             BookmarkService
|   +-- export/               CitationExportService
|   +-- alert/                AlertService
|   +-- monitoring/           MonitoringService, NotificationService
|   +-- session/              SessionService, JournalService
|   +-- network/              ProxyConfiguration, NetworkDiagnostics
|
+-- repository/               Data access layer (SQLite)
|   +-- sqlite/               SQLiteConnection, DatabaseInitializer
|
+-- ui/                       JavaFX GUI layer
|   +-- controller/           MainController, SearchController
|   +-- component/            Reusable UI components
|   +-- model/                JavaFX observable models
|
+-- cli/                      Command-line interface
|   +-- LibSearchCLI.java     Main CLI command
|   +-- SearchCommand.java
|   +-- BookmarkCommand.java
|   +-- IndexCommand.java
|   +-- NetworkCommand.java
|   +-- MonitorCommand.java
|   +-- SessionCommand.java
|   +-- ReplCommand.java
|
+-- config/                   Configuration management
+-- exception/                Custom exceptions
+-- util/                     Utilities (HTTP, file, parser, validation)

src/main/resources/
|
+-- config/application.yaml   Default configuration
+-- db/schema.sql             SQLite schema
+-- com/example/teamse1csdchcw/
    +-- fxml/                 FXML layouts
    +-- css/main.css          Styling

================================================================================
                            TECHNOLOGIES
================================================================================

Language & Build:
  - Java 21 (Amazon Corretto)
  - Gradle 8.11.1

UI Framework:
  - JavaFX 21.0.6
  - ControlsFX, FormsFX, ValidatorFX, TilesFX, BootstrapFX

Backend Libraries:
  - OkHttp3 4.12.0         HTTP Client
  - Jsoup 1.17.2           HTML Parsing
  - Apache Lucene 9.9.1    Full-Text Search
  - SQLite JDBC 3.45.0     Database
  - Jackson 2.16.1         JSON/YAML Processing
  - JBibTeX 1.0.18         Citation Format

CLI Framework:
  - Picocli 4.7.5          Command Parsing
  - JLine3 3.25.1          Terminal UI

Logging:
  - SLF4J 2.0.11
  - Logback 1.4.14

Testing:
  - JUnit 5.12.1
  - Mockito 5.8.0

================================================================================
                           CONFIGURATION
================================================================================

Configuration file location: ~/.libsearch/config.yaml

Key settings include:
  - Source enable/disable and API keys
  - Search parameters (max concurrent sources, timeout, rate limiting)
  - Download directory and concurrent download limit
  - Index directory and auto-indexing toggle
  - Database path and WAL mode

Database location: ~/.libsearch/data/libsearch.db

Database tables:
  - sessions          Research session tracking
  - search_history    Query history
  - search_results    Cached results with metadata
  - bookmarks         User-saved results with tags/notes
  - alerts            Keyword monitoring configuration
  - alert_matches     Alert trigger records
  - downloads         Download queue tracking
  - journal           Audit log of all activities
  - config            Configuration key-value store

================================================================================
                              AUTHORS
================================================================================

  - qwitch13 (nebulai13)
  - zahieddo

================================================================================
                              VERSION
================================================================================

Version: 1.0.0

================================================================================
