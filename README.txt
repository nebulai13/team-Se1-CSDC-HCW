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

Option 3 - Direct JAR (after running ./gradlew shadowJar):
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar


CLI MODE
--------
Search command:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar search "machine learning"

Interactive REPL:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar --interactive

Bookmark management:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar bookmark list

Index statistics:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar index stats

Network diagnostics:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar network diag


RUNNING TESTS
-------------
   ./gradlew test

================================================================================
                        DETAILED COMPILATION GUIDE
================================================================================

PREREQUISITES
-------------
1. Install Java 21+ (Amazon Corretto recommended):
   - macOS:    brew install --cask corretto
   - Windows:  Download from https://aws.amazon.com/corretto/
   - Linux:    sudo apt install openjdk-21-jdk

2. Verify Java installation:
   java -version   (should show version 21 or higher)

3. Gradle will be auto-downloaded via the wrapper (gradlew)

BUILD STEPS
-----------
1. Clone and enter the project directory:
   git clone <repository-url>
   cd team-Se1-CSDC-HCW

2. Make the Gradle wrapper executable (Unix/macOS):
   chmod +x gradlew

3. Build the project:
   ./gradlew clean build

4. Create a runnable fat JAR with all dependencies:
   ./gradlew shadowJar

   Output: build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

5. Run the application:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

COMMON BUILD ISSUES
-------------------
- "JAVA_HOME not set": Set JAVA_HOME to your JDK 21 installation path
- Gradle download fails: Check internet connection and proxy settings
- JavaFX missing: The shadowJar includes all JavaFX dependencies

================================================================================
                          REPL (INTERACTIVE MODE)
================================================================================

Start the interactive REPL:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar repl

   OR with the --interactive flag:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar -i

REPL COMMANDS
-------------
Once in the REPL, you can run any command without the "libsearch" prefix:

  libsearch> search "neural networks"
  libsearch> search "machine learning" -s arxiv,pubmed -m 20
  libsearch> bookmark list
  libsearch> bookmark list -v              (verbose: shows URLs, notes, tags)
  libsearch> bookmark find ai              (find bookmarks tagged "ai")
  libsearch> bookmark delete <id>
  libsearch> session list
  libsearch> session create "My Research"
  libsearch> index build
  libsearch> index status
  libsearch> network diag
  libsearch> clear                         (clear screen)
  libsearch> exit                          (quit REPL)

SEARCH OPTIONS IN REPL
----------------------
  -s, --sources     Comma-separated sources (arxiv,pubmed,crossref,scholar)
  -m, --max-results Maximum results per source (default: 50)
  -o, --offline     Search local index only
  -a, --author      Filter by author name
  -y, --year        Filter by year or range (2023 or 2020-2024)
  -f, --format      Output format: table, json, simple

EXAMPLES:
  search "deep learning" -s arxiv -m 10 -f json
  search "CRISPR" -s pubmed,crossref -y 2020-2024
  search "machine learning" -o                    (offline search)

================================================================================
                             GUI USAGE GUIDE
================================================================================

LAUNCHING THE GUI
-----------------
Start without any arguments to launch the GUI:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

   OR using Gradle:
   ./gradlew run

MAIN INTERFACE SECTIONS
-----------------------
1. SEARCH PANEL (Top)
   - Enter search query in the text field
   - Select sources: arXiv, PubMed, CrossRef, Semantic Scholar
   - Set year range filters
   - Choose max results per source
   - Toggle "Offline Mode" to search local index

2. RESULTS TABLE (Center)
   - Displays search results with columns: Title, Authors, Year, Source, Access
   - Click column headers to sort
   - Double-click a row to open the paper URL
   - Right-click for context menu: Open URL, Download PDF, Bookmark, etc.

3. SIDEBAR (Left)
   - Shows source connection status (green = online, red = offline)
   - Displays current session name and result count

4. STATUS BAR (Bottom)
   - Shows current status, progress indicator, and search time

MENU OPTIONS
------------
File Menu:
  - New Session       Create a new search session
  - Open Session      Switch to a previous session
  - Export Results    Export current results to CSV file
  - Exit              Close the application

Search Menu:
  - New Search        Focus the search field
  - Advanced Search   Open advanced search options
  - Clear Results     Clear the current results
  - Search History    View and re-run previous searches

Bookmarks Menu:
  - Bookmark Selected   Add selected results to bookmarks
  - Manage Bookmarks    View, filter, and delete bookmarks

Tools Menu:
  - Download Manager    View and manage PDF downloads
  - Index Manager       Information about local search index
  - Settings            Application preferences

Help Menu:
  - User Guide          Search syntax help and shortcuts
  - About               Version and application info

KEYBOARD SHORTCUTS
------------------
  Ctrl+N    New search (focus search field)
  Ctrl+F    Focus search field
  Ctrl+B    Bookmark selected result
  Ctrl+E    Export results
  Enter     Execute search (when in search field)

SEARCH QUERY SYNTAX
-------------------
  Simple:       machine learning
  Phrase:       "deep learning"
  Author:       author:Smith  OR  author:"John Smith"
  Year:         year:2023     OR  year:2020-2024
  Boolean:      neural AND networks
                AI OR ML
                cancer NOT treatment

BOOKMARKING RESULTS
-------------------
1. Click the ★ button in the Actions column to toggle bookmark
2. OR right-click a result and select "Bookmark"
3. OR select multiple results and use Bookmarks > Bookmark Selected
4. Manage bookmarks via Bookmarks > Manage Bookmarks

EXPORTING DATA
--------------
1. File > Export Results saves results to CSV
2. Right-click a result > Export Citation for BibTeX/RIS/EndNote
3. Copy DOI via right-click context menu

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
