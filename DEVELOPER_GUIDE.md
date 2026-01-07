# LibSearch Developer Guide

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Package Structure](#package-structure)
5. [Core Components](#core-components)
6. [Database Schema](#database-schema)
7. [Design Patterns](#design-patterns)
8. [Implementation Details](#implementation-details)
9. [Testing Strategy](#testing-strategy)
10. [Build & Deployment](#build--deployment)

---

## Project Overview

LibSearch is a comprehensive academic federated search application built with Java 21 and JavaFX 21. It provides:

- **Federated Search**: Multi-source academic search across arXiv, PubMed, CrossRef, Semantic Scholar, and more
- **Full-Text Indexing**: Apache Lucene integration for offline search capability
- **Dual Interface**: Rich JavaFX GUI and interactive CLI with REPL mode
- **Advanced Features**: PDF downloads, bookmarks, citation export, keyword alerts, session tracking
- **Network Features**: Proxy support, network diagnostics, retry logic
- **Monitoring**: Keyword alerts with multi-channel notifications

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │   JavaFX GUI (FXML)  │    │   CLI/REPL (Picocli)     │  │
│  │   - Controllers      │    │   - Commands             │  │
│  │   - Views (.fxml)    │    │   - JLine3 Terminal      │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
│  ┌──────────────┬──────────────┬──────────────┬──────────┐  │
│  │ Search       │ Download     │ Index        │ Session  │  │
│  │ - Federated  │ - Queue      │ - Lucene     │ - Track  │  │
│  │ - Parser     │ - Progress   │ - Local      │ - Journal│  │
│  ├──────────────┼──────────────┼──────────────┼──────────┤  │
│  │ Bookmark     │ Monitor      │ Network      │ Citation │  │
│  │ - CRUD       │ - Alerts     │ - Proxy      │ - Export │  │
│  │ - Tags       │ - Notify     │ - Diag       │ - BibTeX │  │
│  └──────────────┴──────────────┴──────────────┴──────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Connector Layer                           │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐  │
│  │ arXiv    │ PubMed   │CrossRef  │ Semantic │ GitHub   │  │
│  │          │          │          │ Scholar  │          │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘  │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐  │
│  │ DuckDuck │ Bing     │ Brave    │Wikipedia │MoreSoon  │  │
│  │ Go       │          │          │          │          │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                 Repository/Data Layer                        │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │   SQLite Database    │    │   Lucene Index           │  │
│  │   - Sessions         │    │   - Documents            │  │
│  │   - Search History   │    │   - Full-text search     │  │
│  │   - Bookmarks        │    │   - Field indexing       │  │
│  │   - Alerts           │    │   - Year/author filters  │  │
│  │   - Downloads        │    │                          │  │
│  │   - Journal          │    │                          │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Request Flow Example

**GUI Search Flow:**
```
User Input (SearchController)
    → QueryParserService.parse()
    → FederatedSearchService.search()
        → ConnectorFactory.getConnectors()
        → Multiple SourceConnector.search() (parallel)
        → ResultAggregator.aggregate()
        → IndexService.indexResults() (auto)
    → ResultsController.displayResults()
```

**CLI Search Flow:**
```
Command Line Input
    → Picocli.parse() → SearchCommand.call()
    → FederatedSearchService.search()
    → Format output (table/json/simple)
    → Print to console
```

---

## Technology Stack

### Core Technologies

**Language & Platform:**
- **Java 21** (Amazon Corretto)
  - Records for data transfer objects
  - Pattern matching for type checks
- **Maven 3.8+** - Build automation and dependency management
- **Java Module System** - Modular application structure (module-info.java)

**UI Frameworks:**
- **JavaFX 21.0.6** - Desktop GUI framework
  - FXML for declarative UI
  - CSS for styling
  - MVC architecture with controllers
- **Picocli 4.7.5** - Command-line interface framework
  - Annotation-based command definition
  - Hierarchical subcommands
  - Auto-generated help
- **JLine3 3.25.1** - Terminal UI for REPL
  - Line editing with history
  - Tab completion support
  - Custom prompts

### Data & Persistence

**Database:**
- **SQLite JDBC 3.45.0** - Embedded relational database
  - Zero-configuration
  - File-based storage
  - ACID compliance

**Search Engine:**
- **Apache Lucene 9.9.1** - Full-text search library
  - StandardAnalyzer for text processing
  - Field-specific indexing (TextField, StringField, IntPoint)
  - Query parsing with Boolean operators
  - Year range queries with IntPoint

**Serialization:**
- **Jackson 2.16.1** - JSON/YAML processing
  - ObjectMapper for JSON serialization
  - YAMLFactory for configuration files
  - Tree model for dynamic data

### Networking

**HTTP Client:**
- **OkHttp 4.12.0** - Modern HTTP client
  - Connection pooling
  - HTTP/2 support
  - Transparent GZIP
  - Request/response interceptors

**HTML Parsing:**
- **Jsoup 1.17.2** - HTML parser
  - CSS selectors
  - DOM traversal
  - Data extraction from web pages

### Additional Libraries

**Logging:**
- **SLF4J 2.0.9** - Logging facade
- **Logback 1.4.14** - Logging implementation
  - Rolling file appenders
  - Level-based filtering
  - Pattern-based formatting

**Citation Formats:**
- **JBibTeX 1.0.18** - BibTeX library
  - BibTeX entry creation
  - Field validation
  - String escaping

**Utilities:**
- **Google Guava** - Collections and utilities
- **Apache Commons Lang** - String utilities
- **ControlsFX, TilesFX** - Enhanced JavaFX controls

### Build & Testing

**Build Tools:**
- **Maven Compiler Plugin 3.13.0** - Java compilation
- **Maven Shade Plugin** - Uber JAR creation
- **JavaFX Maven Plugin** - JavaFX application packaging

**Testing (Framework Ready):**
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **TestFX** - JavaFX testing

---

## Package Structure

```
com.example.teamse1csdchcw/
├── domain/                    # POJOs and value objects
│   ├── SearchResult.java     # Base search result
│   ├── AcademicPaper.java    # Academic paper with metadata
│   ├── SearchQuery.java      # Parsed query with filters
│   ├── SourceType.java       # Enum of search sources
│   ├── user/
│   │   └── Bookmark.java     # Bookmark entity
│   └── monitoring/
│       └── Alert.java        # Alert configuration
│
├── service/                   # Business logic layer
│   ├── search/
│   │   ├── QueryParserService.java      # Query parsing (grep-like)
│   │   ├── FederatedSearchService.java  # Multi-source orchestration
│   │   └── ResultAggregator.java        # Result deduplication
│   ├── download/
│   │   └── DownloadService.java         # PDF download queue
│   ├── index/
│   │   ├── IndexService.java            # Lucene indexing
│   │   └── LocalSearchService.java      # Offline search
│   ├── bookmark/
│   │   └── BookmarkService.java         # Bookmark CRUD
│   ├── citation/
│   │   └── CitationExportService.java   # BibTeX/RIS/EndNote export
│   ├── network/
│   │   ├── ProxyConfiguration.java      # Proxy settings
│   │   ├── NetworkDiagnostics.java      # Connectivity testing
│   │   └── RetryHandler.java            # Exponential backoff
│   ├── monitoring/
│   │   ├── MonitoringService.java       # Alert checking
│   │   └── NotificationService.java     # Multi-channel notifications
│   └── session/
│       ├── SessionService.java          # Session lifecycle
│       └── JournalService.java          # Activity logging
│
├── connector/                 # API connectors (Strategy pattern)
│   ├── SourceConnector.java  # Interface for all connectors
│   ├── ArxivConnector.java   # arXiv.org API
│   ├── PubMedConnector.java  # PubMed/NCBI
│   ├── CrossRefConnector.java # CrossRef DOI metadata
│   └── SemanticScholarConnector.java # Semantic Scholar API
│
├── repository/                # Data access layer
│   ├── sqlite/
│   │   ├── SQLiteConnection.java        # Connection management
│   │   ├── SearchHistoryRepository.java # Search history
│   │   ├── SessionRepository.java       # Session storage
│   │   └── JournalRepository.java       # Activity logs
│   ├── BookmarkRepository.java          # Bookmark persistence
│   ├── AlertRepository.java             # Alert storage
│   └── DownloadQueueRepository.java     # Download queue
│
├── ui/                        # JavaFX controllers
│   ├── MainController.java   # Main window controller
│   ├── SearchController.java # Search panel controller
│   └── ResultsController.java # Results table controller
│
├── cli/                       # Command-line interface
│   ├── LibSearchCLI.java     # Main CLI entry point
│   ├── SearchCommand.java    # search command
│   ├── BookmarkCommand.java  # bookmark command
│   ├── IndexCommand.java     # index command
│   ├── NetworkCommand.java   # network command
│   ├── MonitorCommand.java   # monitor command
│   ├── SessionCommand.java   # session command
│   └── ReplCommand.java      # Interactive REPL
│
├── util/                      # Utility classes
│   ├── HttpClientFactory.java # OkHttp client factory
│   ├── RateLimiter.java      # Rate limiting
│   └── DateUtils.java        # Date formatting
│
├── config/                    # Configuration management
│   └── ConfigurationService.java # YAML config loader
│
├── exception/                 # Custom exceptions
│   ├── SearchException.java  # Search-related errors
│   └── ConnectorException.java # API connector errors
│
└── HelloApplication.java      # Dual-mode entry point (GUI/CLI)
```

---

## Core Components

### 1. Search System

#### QueryParserService

**Purpose**: Parse user queries with grep-like syntax into structured SearchQuery objects.

**Features**:
- Boolean operators: `AND`, `OR`, `NOT`, `+`, `-`
- Field filters: `author:`, `year:`, `type:`, `site:`, `filetype:`
- Year ranges: `year:2020..2024`, `year:>2020`, `year:<2024`
- Phrase search: `"machine learning"`

**Implementation**:
```java
public class QueryParserService {
    public SearchQuery parse(String queryString) {
        SearchQuery query = new SearchQuery();

        // Extract filters using regex
        Pattern authorPattern = Pattern.compile("author:(\\w+)");
        Matcher matcher = authorPattern.matcher(queryString);
        if (matcher.find()) {
            query.setAuthorFilter(matcher.group(1));
        }

        // Parse year ranges
        Pattern yearRangePattern = Pattern.compile("year:(\\d{4})\\.\\.(\\d{4})");
        // ...

        return query;
    }
}
```

#### FederatedSearchService

**Purpose**: Orchestrate parallel searches across multiple sources and aggregate results.

**Key Techniques**:
- **Parallel Execution**: Uses `ExecutorService` with configurable thread pool
- **Timeout Handling**: Per-source timeouts to prevent blocking
- **Auto-Indexing**: Automatically indexes successful results
- **Error Isolation**: Connector failures don't affect other sources

**Implementation**:
```java
public class FederatedSearchService {
    private final ExecutorService executor;
    private final List<SourceConnector> connectors;
    private final IndexService indexService;

    public List<SearchResult> search(SearchQuery query, Set<SourceType> sources) {
        List<Future<List<SearchResult>>> futures = new ArrayList<>();

        // Submit all searches in parallel
        for (SourceConnector connector : getActiveConnectors(sources)) {
            futures.add(executor.submit(() -> {
                try {
                    return connector.search(query, maxResults);
                } catch (Exception e) {
                    logger.error("Search failed for " + connector.getSourceType(), e);
                    return Collections.emptyList();
                }
            }));
        }

        // Collect results with timeout
        List<SearchResult> aggregated = ResultAggregator.aggregate(
            collectResults(futures, timeout)
        );

        // Auto-index if enabled
        if (autoIndexEnabled && !aggregated.isEmpty()) {
            indexService.indexResults(aggregated);
        }

        return aggregated;
    }
}
```

#### SourceConnector Interface

**Purpose**: Define contract for all search source implementations.

```java
public interface SourceConnector {
    List<SearchResult> search(SearchQuery query, int maxResults) throws Exception;
    SourceType getSourceType();
    boolean isAvailable();
}
```

**Implementations**:

**ArxivConnector** - arXiv.org API
```java
public class ArxivConnector implements SourceConnector {
    private static final String API_BASE = "http://export.arxiv.org/api/query";

    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) {
        // Build query URL
        String url = API_BASE + "?search_query=" +
                     URLEncoder.encode(query.toQueryString(), UTF_8) +
                     "&max_results=" + maxResults;

        // HTTP GET request
        Response response = httpClient.newCall(request).execute();

        // Parse Atom/XML response
        Document doc = Jsoup.parse(response.body().string(), "", Parser.xmlParser());

        // Extract entries
        return doc.select("entry").stream()
            .map(this::parseEntry)
            .collect(Collectors.toList());
    }

    private AcademicPaper parseEntry(Element entry) {
        AcademicPaper paper = new AcademicPaper();
        paper.setTitle(entry.select("title").text());
        paper.setAuthors(parseAuthors(entry.select("author")));
        paper.setAbstract(entry.select("summary").text());
        paper.setUrl(entry.select("id").text());
        // ...
        return paper;
    }
}
```

**PubMedConnector** - PubMed/NCBI E-utilities
```java
public class PubMedConnector implements SourceConnector {
    // Two-step process: search → fetch

    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) {
        // Step 1: Get PMIDs
        List<String> pmids = searchPubMed(query, maxResults);

        // Step 2: Fetch details
        return fetchDetails(pmids);
    }

    private List<String> searchPubMed(SearchQuery query, int max) {
        String url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?" +
                     "db=pubmed&term=" + URLEncoder.encode(query.toQueryString()) +
                     "&retmax=" + max + "&retmode=json";
        // Parse JSON response for ID list
    }

    private List<SearchResult> fetchDetails(List<String> pmids) {
        String url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?" +
                     "db=pubmed&id=" + String.join(",", pmids) +
                     "&retmode=xml";
        // Parse PubmedArticle XML elements
    }
}
```

### 2. Indexing System

#### IndexService (Apache Lucene)

**Purpose**: Full-text indexing of search results for offline search capability.

**Lucene Configuration**:
- **Analyzer**: `StandardAnalyzer` - Tokenization, lowercasing, stop words
- **Directory**: `FSDirectory` - File-based index storage
- **IndexWriter**: Write-once-per-batch with manual commits

**Field Mapping**:
```java
private Document createDocument(SearchResult result) {
    Document doc = new Document();

    // Stored, not indexed (for retrieval)
    doc.add(new StringField("id", result.getId(), Field.Store.YES));

    // Full-text indexed
    doc.add(new TextField("title", result.getTitle(), Field.Store.YES));
    doc.add(new TextField("description", result.getDescription(), Field.Store.YES));

    // Filter fields
    doc.add(new StringField("source", result.getSource().name(), Field.Store.YES));

    if (result instanceof AcademicPaper paper) {
        // Author field (stored + indexed for exact match)
        for (String author : paper.getAuthors()) {
            doc.add(new TextField("author", author, Field.Store.YES));
        }

        // Year as numeric field for range queries
        doc.add(new IntPoint("year", paper.getPublicationDate().getYear()));
        doc.add(new StoredField("year_stored", paper.getPublicationDate().getYear()));

        // Abstract
        doc.add(new TextField("abstract", paper.getAbstract(), Field.Store.YES));
    }

    return doc;
}
```

**Indexing Strategy**:
```java
public void indexResults(List<SearchResult> results) throws IOException {
    for (SearchResult result : results) {
        indexResult(result);
    }
    commit(); // Batch commit for performance
}

private void indexResult(SearchResult result) throws IOException {
    // Update if exists, add if new
    Term idTerm = new Term("id", result.getId());
    indexWriter.updateDocument(idTerm, createDocument(result));
}
```

#### LocalSearchService

**Purpose**: Query the Lucene index for offline searches.

**Query Building**:
```java
private Query buildQuery(SearchQuery query) throws ParseException {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();

    // Main text query (title + description + abstract)
    String queryText = query.toQueryString();
    QueryParser parser = new MultiFieldQueryParser(
        new String[]{"title", "description", "abstract"},
        new StandardAnalyzer()
    );
    builder.add(parser.parse(queryText), BooleanClause.Occur.MUST);

    // Author filter
    if (query.getAuthorFilter() != null) {
        TermQuery authorQuery = new TermQuery(
            new Term("author", query.getAuthorFilter().toLowerCase())
        );
        builder.add(authorQuery, BooleanClause.Occur.MUST);
    }

    // Year range filter
    if (query.getYearFrom() != null || query.getYearTo() != null) {
        int from = query.getYearFrom() != null ? query.getYearFrom() : 0;
        int to = query.getYearTo() != null ? query.getYearTo() : 9999;
        Query yearQuery = IntPoint.newRangeQuery("year", from, to);
        builder.add(yearQuery, BooleanClause.Occur.MUST);
    }

    return builder.build();
}
```

**Result Conversion**:
```java
private List<SearchResult> convertToSearchResults(IndexSearcher searcher, TopDocs topDocs) {
    List<SearchResult> results = new ArrayList<>();

    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        Document doc = searcher.doc(scoreDoc.doc);

        AcademicPaper paper = new AcademicPaper();
        paper.setId(doc.get("id"));
        paper.setTitle(doc.get("title"));
        paper.setDescription(doc.get("description"));

        // Reconstruct authors from multiple fields
        String[] authors = doc.getValues("author");
        paper.setAuthors(Arrays.asList(authors));

        // Get year from stored field
        IndexableField yearField = doc.getField("year_stored");
        if (yearField != null) {
            paper.setPublicationDate(
                LocalDate.of(yearField.numericValue().intValue(), 1, 1)
            );
        }

        results.add(paper);
    }

    return results;
}
```

### 3. Download System

#### DownloadService

**Purpose**: Queue-based PDF download management with progress tracking.

**Features**:
- Concurrent downloads (configurable limit)
- Progress tracking per download
- Retry logic for failed downloads
- Rate limiting to respect server policies

**Implementation**:
```java
public class DownloadService {
    private final ExecutorService downloadExecutor;
    private final DownloadQueueRepository repository;
    private final Map<String, DownloadProgress> progressMap;

    public String queueDownload(SearchResult result, Path outputDir) {
        String downloadId = UUID.randomUUID().toString();

        // Persist to queue
        repository.add(downloadId, result.getUrl(), outputDir);

        // Submit download task
        downloadExecutor.submit(() -> executeDownload(downloadId, result));

        return downloadId;
    }

    private void executeDownload(String downloadId, SearchResult result) {
        try {
            updateProgress(downloadId, 0, "Starting download...");

            // Build request
            Request request = new Request.Builder()
                .url(result.getUrl())
                .build();

            // Execute with streaming
            try (Response response = httpClient.newCall(request).execute();
                 InputStream input = response.body().byteStream();
                 OutputStream output = Files.newOutputStream(outputPath)) {

                long totalBytes = response.body().contentLength();
                byte[] buffer = new byte[8192];
                long downloadedBytes = 0;
                int read;

                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    downloadedBytes += read;

                    // Update progress
                    int percent = (int) ((downloadedBytes * 100) / totalBytes);
                    updateProgress(downloadId, percent, "Downloading...");
                }
            }

            updateProgress(downloadId, 100, "Complete");
            repository.markComplete(downloadId);

        } catch (IOException e) {
            updateProgress(downloadId, -1, "Failed: " + e.getMessage());
            repository.markFailed(downloadId, e.getMessage());
        }
    }
}
```

### 4. Network System

#### ProxyConfiguration

**Purpose**: Proxy configuration with persistent storage using Java Preferences API.

**Storage**:
```java
public class ProxyConfiguration {
    private final Preferences prefs = Preferences.userNodeForPackage(ProxyConfiguration.class);

    private static final String PROXY_ENABLED_KEY = "proxy.enabled";
    private static final String PROXY_TYPE_KEY = "proxy.type";
    private static final String PROXY_HOST_KEY = "proxy.host";
    private static final String PROXY_PORT_KEY = "proxy.port";

    public void save() {
        prefs.putBoolean(PROXY_ENABLED_KEY, enabled);
        prefs.put(PROXY_TYPE_KEY, type.name());
        prefs.put(PROXY_HOST_KEY, host);
        prefs.putInt(PROXY_PORT_KEY, port);

        // Flush to persistent storage
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            logger.error("Failed to save proxy configuration", e);
        }
    }

    public void load() {
        enabled = prefs.getBoolean(PROXY_ENABLED_KEY, false);
        type = Proxy.Type.valueOf(prefs.get(PROXY_TYPE_KEY, "HTTP"));
        host = prefs.get(PROXY_HOST_KEY, "");
        port = prefs.getInt(PROXY_PORT_KEY, 8080);
    }
}
```

**OkHttp Integration**:
```java
public Proxy createProxy() {
    if (!enabled || host == null || host.isEmpty()) {
        return Proxy.NO_PROXY;
    }

    InetSocketAddress address = new InetSocketAddress(host, port);
    return new Proxy(type, address);
}

// In HttpClientFactory:
ProxyConfiguration proxyConfig = new ProxyConfiguration();
proxyConfig.load();

OkHttpClient client = new OkHttpClient.Builder()
    .proxy(proxyConfig.createProxy())
    .proxyAuthenticator((route, response) -> {
        String credential = Credentials.basic(username, password);
        return response.request().newBuilder()
            .header("Proxy-Authorization", credential)
            .build();
    })
    .build();
```

#### RetryHandler

**Purpose**: Exponential backoff retry logic for transient failures.

**Algorithm**:
```java
public class RetryHandler {
    private final int maxRetries;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;

    public <T> T executeWithRetry(Supplier<T> operation) throws IOException {
        int attempt = 0;
        long delay = initialDelayMs;
        Exception lastException = null;

        while (attempt <= maxRetries) {
            try {
                return operation.get();

            } catch (Exception e) {
                lastException = e;

                if (!isRetriable(e) || attempt == maxRetries) {
                    break;
                }

                logger.warn("Attempt {} failed, retrying in {}ms", attempt + 1, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", ie);
                }

                // Exponential backoff
                delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);
                attempt++;
            }
        }

        throw new IOException("Max retries exceeded", lastException);
    }

    private boolean isRetriable(Exception e) {
        // Retry on network errors, timeouts, 503/429 HTTP errors
        if (e instanceof SocketTimeoutException ||
            e instanceof UnknownHostException ||
            e instanceof ConnectException) {
            return true;
        }

        if (e instanceof IOException && e.getMessage().contains("503")) {
            return true; // Service unavailable
        }

        return false;
    }
}
```

### 5. Monitoring System

#### Alert System

**Domain Model**:
```java
public class Alert {
    private String id;
    private String name;
    private List<String> keywords;           // Must contain all
    private String authorFilter;             // Optional
    private Integer yearFrom, yearTo;        // Optional range
    private Set<SourceType> sources;         // Which sources to check
    private NotificationType notificationType; // CONSOLE, LOG, EMAIL
    private LocalDateTime lastChecked;
    private Integer lastResultCount;

    public enum NotificationType {
        CONSOLE,  // Print to System.out
        LOG,      // Write to log file
        EMAIL     // Send email (requires SMTP config)
    }
}
```

**Monitoring Workflow**:
```java
public class MonitoringService {
    private final FederatedSearchService searchService;
    private final NotificationService notificationService;
    private final AlertRepository alertRepository;

    public void checkAlert(Alert alert) throws Exception {
        // Build query from alert criteria
        SearchQuery query = new SearchQuery();
        query.setRawQuery(String.join(" AND ", alert.getKeywords()));
        query.setAuthorFilter(alert.getAuthorFilter());
        query.setYearFrom(alert.getYearFrom());
        query.setYearTo(alert.getYearTo());

        // Execute search
        Set<SourceType> sources = alert.getSources().isEmpty() ?
            Set.of(SourceType.values()) : alert.getSources();

        List<SearchResult> results = searchService.search(query, sources);

        // Notify if results found
        if (!results.isEmpty()) {
            notificationService.notify(alert, results);
        }

        // Update last checked
        alertRepository.updateLastChecked(
            alert.getId(),
            LocalDateTime.now(),
            results.size()
        );
    }

    public void checkAllAlerts() {
        List<Alert> alerts = alertRepository.findAll();

        for (Alert alert : alerts) {
            try {
                checkAlert(alert);
            } catch (Exception e) {
                logger.error("Failed to check alert: " + alert.getName(), e);
            }
        }
    }
}
```

**Notification Channels**:
```java
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void notify(Alert alert, List<SearchResult> results) {
        String message = buildMessage(alert, results);

        switch (alert.getNotificationType()) {
            case CONSOLE -> notifyConsole(message);
            case LOG -> notifyLog(message);
            case EMAIL -> notifyEmail(alert, message);
        }
    }

    private void notifyConsole(String message) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ALERT NOTIFICATION");
        System.out.println("=".repeat(80));
        System.out.println(message);
        System.out.println("=".repeat(80) + "\n");
    }

    private void notifyLog(String message) {
        logger.info("ALERT: {}", message);
    }

    private void notifyEmail(Alert alert, String message) {
        // TODO: Implement SMTP email sending
        // Requires JavaMail API and SMTP configuration
        logger.warn("Email notifications not yet implemented");
    }
}
```

### 6. Session & Journaling

#### SessionService

**Purpose**: Track user sessions with start/end times and statistics.

**Session Lifecycle**:
```java
public class SessionService {
    private String currentSessionId;

    public String startSession() throws SQLException {
        currentSessionId = UUID.randomUUID().toString();

        String sql = """
            INSERT INTO sessions (id, started_at, status)
            VALUES (?, ?, 'ACTIVE')
        """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, currentSessionId);
            stmt.setObject(2, LocalDateTime.now());
            stmt.executeUpdate();
        }

        logger.info("Started session: {}", currentSessionId);
        return currentSessionId;
    }

    public void endSession() throws SQLException {
        if (currentSessionId == null) return;

        String sql = """
            UPDATE sessions
            SET ended_at = ?, status = 'COMPLETED'
            WHERE id = ?
        """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, LocalDateTime.now());
            stmt.setString(2, currentSessionId);
            stmt.executeUpdate();
        }

        logger.info("Ended session: {}", currentSessionId);
        currentSessionId = null;
    }
}
```

**Session Statistics**:
```java
public List<SessionInfo> getRecentSessions(int limit) throws SQLException {
    String sql = """
        SELECT s.id, s.started_at, s.ended_at, s.status,
               COUNT(sh.id) as search_count
        FROM sessions s
        LEFT JOIN search_history sh ON s.id = sh.session_id
        GROUP BY s.id
        ORDER BY s.started_at DESC
        LIMIT ?
    """;

    List<SessionInfo> sessions = new ArrayList<>();

    try (Connection conn = SQLiteConnection.getInstance().getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setInt(1, limit);

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                SessionInfo info = new SessionInfo();
                info.id = rs.getString("id");
                info.startedAt = (LocalDateTime) rs.getObject("started_at");
                info.endedAt = (LocalDateTime) rs.getObject("ended_at");
                info.searchCount = rs.getInt("search_count");

                // Calculate duration
                if (info.endedAt != null) {
                    info.duration = Duration.between(info.startedAt, info.endedAt);
                }

                sessions.add(info);
            }
        }
    }

    return sessions;
}
```

#### JournalService

**Purpose**: Log all user activities for audit trail and analysis.

**Event Types**:
```java
public enum EventType {
    SEARCH,    // User performed a search
    DOWNLOAD,  // User downloaded a file
    BOOKMARK,  // User added/removed a bookmark
    EXPORT,    // User exported citations
    INDEX,     // Indexing operation occurred
    ALERT,     // Alert was triggered
    SESSION    // Session started/ended
}
```

**Event Logging**:
```java
public void logEvent(String sessionId, EventType eventType,
                     String description, String metadata) {
    try {
        String sql = """
            INSERT INTO journal (id, session_id, event_type, description, metadata, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, sessionId);
            stmt.setString(3, eventType.name());
            stmt.setString(4, description);
            stmt.setString(5, metadata);
            stmt.setObject(6, LocalDateTime.now());

            stmt.executeUpdate();
            logger.debug("Logged event: {} - {}", eventType, description);
        }

    } catch (SQLException e) {
        logger.error("Failed to log journal event", e);
    }
}
```

**Journal Queries**:
```java
// Get recent entries across all sessions
public List<JournalEntry> getRecentEntries(int limit) throws SQLException {
    String sql = """
        SELECT * FROM journal
        ORDER BY timestamp DESC
        LIMIT ?
    """;
    // Execute and map to JournalEntry objects
}

// Get entries for specific session
public List<JournalEntry> getSessionEntries(String sessionId) throws SQLException {
    String sql = """
        SELECT * FROM journal
        WHERE session_id = ?
        ORDER BY timestamp ASC
    """;
    // Execute and map to JournalEntry objects
}
```

---

## Database Schema

### SQLite Tables

**sessions**
```sql
CREATE TABLE sessions (
    id TEXT PRIMARY KEY,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    status TEXT CHECK(status IN ('ACTIVE', 'COMPLETED', 'ABANDONED'))
);
```

**search_history**
```sql
CREATE TABLE search_history (
    id TEXT PRIMARY KEY,
    session_id TEXT,
    query TEXT NOT NULL,
    source_types TEXT,  -- Comma-separated list
    result_count INTEGER,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
```

**search_results** (cached results)
```sql
CREATE TABLE search_results (
    id TEXT PRIMARY KEY,
    search_id TEXT,
    title TEXT NOT NULL,
    authors TEXT,
    abstract TEXT,
    url TEXT,
    source TEXT,
    publication_date DATE,
    citation_count INTEGER,
    metadata TEXT,  -- JSON
    FOREIGN KEY (search_id) REFERENCES search_history(id)
);
```

**bookmarks**
```sql
CREATE TABLE bookmarks (
    id TEXT PRIMARY KEY,
    result_id TEXT,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    notes TEXT,
    tags TEXT,  -- Comma-separated
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (result_id) REFERENCES search_results(id)
);
```

**alerts**
```sql
CREATE TABLE alerts (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    keywords TEXT NOT NULL,  -- JSON array
    author_filter TEXT,
    year_from INTEGER,
    year_to INTEGER,
    sources TEXT,  -- JSON array
    notification_type TEXT,
    last_checked TIMESTAMP,
    last_result_count INTEGER,
    created_at TIMESTAMP NOT NULL
);
```

**downloads**
```sql
CREATE TABLE downloads (
    id TEXT PRIMARY KEY,
    result_id TEXT,
    url TEXT NOT NULL,
    output_path TEXT NOT NULL,
    status TEXT CHECK(status IN ('QUEUED', 'DOWNLOADING', 'COMPLETED', 'FAILED')),
    progress INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (result_id) REFERENCES search_results(id)
);
```

**journal** (activity log)
```sql
CREATE TABLE journal (
    id TEXT PRIMARY KEY,
    session_id TEXT,
    event_type TEXT NOT NULL,
    description TEXT NOT NULL,
    metadata TEXT,  -- Optional JSON data
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE INDEX idx_journal_session ON journal(session_id);
CREATE INDEX idx_journal_timestamp ON journal(timestamp);
```

---

## Design Patterns

### 1. Repository Pattern

**Purpose**: Abstract data access logic from business logic.

**Benefits**:
- Testable (can mock repositories)
- Swappable storage backends
- Centralized query logic

**Example**:
```java
public interface BookmarkRepository {
    void save(Bookmark bookmark) throws SQLException;
    Optional<Bookmark> findById(String id) throws SQLException;
    List<Bookmark> findAll() throws SQLException;
    void delete(String id) throws SQLException;
}

public class SQLiteBookmarkRepository implements BookmarkRepository {
    @Override
    public void save(Bookmark bookmark) throws SQLException {
        String sql = "INSERT OR REPLACE INTO bookmarks (...) VALUES (...)";
        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Execute insert
        }
    }
}
```

### 2. Strategy Pattern

**Purpose**: Encapsulate algorithms (search strategies) behind a common interface.

**Implementation**: `SourceConnector` interface with multiple implementations (ArxivConnector, PubMedConnector, etc.)

**Benefits**:
- Easy to add new sources
- Sources can be enabled/disabled at runtime
- Each source encapsulates its own API logic

### 3. Service Layer Pattern

**Purpose**: Encapsulate business logic separate from presentation and data access.

**Example**:
```
Presentation (SearchController)
    ↓ calls
Service (FederatedSearchService)
    ↓ uses
Repository (SearchHistoryRepository)
    ↓ accesses
Database (SQLite)
```

### 4. Factory Pattern

**Purpose**: Centralized creation of complex objects (HTTP clients).

```java
public class HttpClientFactory {
    public static OkHttpClient createClient(ProxyConfiguration proxyConfig) {
        return new OkHttpClient.Builder()
            .proxy(proxyConfig.createProxy())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .addInterceptor(new LoggingInterceptor())
            .build();
    }
}
```

### 5. Observer Pattern (JavaFX)

**Purpose**: Reactive UI updates via property bindings.

```java
public class SearchController {
    @FXML private ProgressBar progressBar;

    private void executeSearch() {
        Task<List<SearchResult>> searchTask = new Task<>() {
            @Override
            protected List<SearchResult> call() throws Exception {
                updateProgress(0, 100);
                List<SearchResult> results = searchService.search(query);
                updateProgress(100, 100);
                return results;
            }
        };

        // Bind progress bar to task progress
        progressBar.progressProperty().bind(searchTask.progressProperty());

        new Thread(searchTask).start();
    }
}
```

---

## Implementation Details

### JavaFX GUI Architecture

**FXML-based MVC**:
```
View (FXML)                Controller                  Model/Service
  main.fxml  ←→  MainController      ←→  FederatedSearchService
  search.fxml ←→  SearchController    ←→  QueryParserService
  results.fxml ←→  ResultsController   ←→  SearchResult (domain)
```

**Controller Initialization**:
```java
public class MainController implements Initializable {
    @FXML private BorderPane rootPane;
    @FXML private MenuBar menuBar;

    private FederatedSearchService searchService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        this.searchService = new FederatedSearchService();

        // Setup event handlers
        setupMenuActions();
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        // Event handler logic
    }
}
```

**FXML Loading**:
```java
public class HelloApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/main.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(
            getClass().getResource("/css/styles.css").toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.setTitle("LibSearch");
        primaryStage.show();
    }
}
```

### CLI Architecture

**Picocli Command Hierarchy**:
```
libsearch (LibSearchCLI)
├── search (SearchCommand)
├── bookmark (BookmarkCommand)
│   ├── list
│   ├── add
│   └── delete
├── index (IndexCommand)
│   ├── stats
│   ├── optimize
│   └── clear
├── network (NetworkCommand)
│   ├── diag
│   ├── proxy
│   └── test
├── monitor (MonitorCommand)
│   ├── list
│   ├── add
│   ├── delete
│   └── check
├── session (SessionCommand)
│   ├── list
│   └── journal
└── repl (ReplCommand)
```

**Command Implementation**:
```java
@Command(name = "search", description = "Search academic databases")
public class SearchCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Search query")
    private String query;

    @Option(names = {"-s", "--sources"}, split = ",",
            description = "Sources: arxiv,pubmed,crossref")
    private Set<SourceType> sources = new HashSet<>();

    @Option(names = {"-o", "--offline"},
            description = "Use offline index")
    private boolean offline;

    @Option(names = {"-f", "--format"},
            description = "Output format: table,json,simple")
    private String format = "table";

    @Override
    public Integer call() throws Exception {
        // Parse query
        QueryParserService parser = new QueryParserService();
        SearchQuery searchQuery = parser.parse(query);

        // Execute search
        List<SearchResult> results;
        if (offline) {
            LocalSearchService localService = new LocalSearchService();
            results = localService.search(searchQuery, 50);
        } else {
            FederatedSearchService federatedService = new FederatedSearchService();
            results = federatedService.search(searchQuery, sources);
        }

        // Format output
        formatOutput(results, format);

        return 0;  // Success exit code
    }
}
```

**REPL Implementation**:
```java
@Command(name = "repl", description = "Start interactive REPL")
public class ReplCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

            CommandLine rootCmd = new CommandLine(new LibSearchCLI());

            System.out.println("LibSearch REPL - Type 'exit' to quit");

            while (true) {
                try {
                    String line = reader.readLine("libsearch> ");

                    if (line.trim().equalsIgnoreCase("exit")) {
                        break;
                    }

                    // Parse command line (handle quotes)
                    String[] args = parseCommandLine(line);

                    // Execute command
                    int exitCode = rootCmd.execute(args);

                } catch (UserInterruptException e) {
                    // Ctrl+C pressed
                    break;
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        return 0;
    }

    private String[] parseCommandLine(String line) {
        // Simple quoted argument parser
        List<String> args = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)")
            .matcher(line);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                args.add(matcher.group(1));  // Double-quoted
            } else if (matcher.group(2) != null) {
                args.add(matcher.group(2));  // Single-quoted
            } else {
                args.add(matcher.group(3));  // Unquoted
            }
        }

        return args.toArray(new String[0]);
    }
}
```

### Configuration Management

**YAML Configuration**:
```yaml
# application.yaml
sources:
  arxiv:
    enabled: true
    max_results: 50
    rate_limit: 3  # requests per second

  pubmed:
    enabled: true
    api_key: ${PUBMED_API_KEY}
    max_results: 50

search:
  max_concurrent_sources: 5
  timeout_seconds: 30
  auto_index: true

download:
  output_dir: ~/.libsearch/downloads
  max_concurrent: 3
  retry_attempts: 3

database:
  path: ~/.libsearch/data/libsearch.db

index:
  directory: ~/.libsearch/index
  commit_interval: 100

logging:
  level: INFO
  file: ~/.libsearch/logs/libsearch.log
```

**Configuration Loading**:
```java
public class ConfigurationService {
    private final ObjectMapper yamlMapper;

    public ConfigurationService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public AppConfig loadConfig() throws IOException {
        // Load default config
        AppConfig config = yamlMapper.readValue(
            getClass().getResourceAsStream("/config/application.yaml"),
            AppConfig.class
        );

        // Override with user config if exists
        Path userConfig = Paths.get(System.getProperty("user.home"),
                                     ".libsearch", "config.yaml");
        if (Files.exists(userConfig)) {
            AppConfig userOverrides = yamlMapper.readValue(
                Files.newInputStream(userConfig),
                AppConfig.class
            );
            config.merge(userOverrides);
        }

        // Resolve environment variables
        config.resolveEnvVars();

        return config;
    }
}
```

---

## Testing Strategy

### Unit Testing

**Service Layer Tests** (Example):
```java
@ExtendWith(MockitoExtension.class)
class QueryParserServiceTest {

    @InjectMocks
    private QueryParserService parserService;

    @Test
    void testParseSimpleQuery() {
        SearchQuery query = parserService.parse("machine learning");

        assertEquals("machine learning", query.toQueryString());
        assertNull(query.getAuthorFilter());
    }

    @Test
    void testParseAuthorFilter() {
        SearchQuery query = parserService.parse("neural networks author:Hinton");

        assertEquals("Hinton", query.getAuthorFilter());
        assertTrue(query.toQueryString().contains("neural networks"));
    }

    @Test
    void testParseYearRange() {
        SearchQuery query = parserService.parse("AI year:2020..2024");

        assertEquals(2020, query.getYearFrom());
        assertEquals(2024, query.getYearTo());
    }
}
```

**Repository Tests** (Integration):
```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookmarkRepositoryTest {

    private BookmarkRepository repository;
    private Connection testConnection;

    @BeforeAll
    void setup() throws SQLException {
        // Use in-memory SQLite for tests
        testConnection = DriverManager.getConnection("jdbc:sqlite::memory:");

        // Create schema
        String schema = new String(Files.readAllBytes(
            Paths.get("src/main/resources/db/schema.sql")
        ));
        testConnection.createStatement().execute(schema);

        repository = new SQLiteBookmarkRepository(testConnection);
    }

    @Test
    void testSaveAndRetrieve() throws SQLException {
        Bookmark bookmark = new Bookmark();
        bookmark.setId("test-1");
        bookmark.setTitle("Test Paper");
        bookmark.setUrl("http://example.com");

        repository.save(bookmark);

        Optional<Bookmark> retrieved = repository.findById("test-1");
        assertTrue(retrieved.isPresent());
        assertEquals("Test Paper", retrieved.get().getTitle());
    }
}
```

### Integration Testing

**Connector Tests** (requires API access):
```java
@Tag("integration")
class ArxivConnectorTest {

    private ArxivConnector connector;

    @BeforeEach
    void setup() {
        connector = new ArxivConnector();
    }

    @Test
    void testSearch() throws Exception {
        SearchQuery query = new SearchQuery();
        query.setRawQuery("quantum computing");

        List<SearchResult> results = connector.search(query, 10);

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 10);

        // Verify result structure
        SearchResult first = results.get(0);
        assertNotNull(first.getTitle());
        assertNotNull(first.getUrl());
    }
}
```

### GUI Testing (TestFX)

```java
@ExtendWith(ApplicationExtension.class)
class SearchControllerTest {

    @Start
    void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/search.fxml")
        );
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void testSearchExecution(FxRobot robot) {
        // Type in search field
        robot.clickOn("#searchField")
             .write("machine learning");

        // Click search button
        robot.clickOn("#searchButton");

        // Verify results table is populated
        robot.lookup("#resultsTable").queryTableView();
        // Assertions...
    }
}
```

---

## Build & Deployment

### Maven Build Configuration

**pom.xml highlights**:
```xml
<properties>
    <java.version>21</java.version>
    <javafx.version>21.0.6</javafx.version>
    <lucene.version>9.9.1</lucene.version>
</properties>

<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>${javafx.version}</version>
    </dependency>

    <!-- Apache Lucene -->
    <dependency>
        <groupId>org.apache.lucene</groupId>
        <artifactId>lucene-core</artifactId>
        <version>${lucene.version}</version>
    </dependency>

    <!-- OkHttp -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- Picocli -->
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli</artifactId>
        <version>4.7.5</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Maven Compiler -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <source>21</source>
                <target>21</target>
            </configuration>
        </plugin>

        <!-- JavaFX Maven Plugin -->
        <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>0.0.8</version>
            <configuration>
                <mainClass>com.example.teamse1csdchcw.HelloApplication</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Build Commands

**Standard Build**:
```bash
mvn clean install
```

**Skip Tests**:
```bash
mvn clean install -DskipTests
```

**Run GUI**:
```bash
mvn javafx:run
```

**Run CLI**:
```bash
java -jar target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar search "query"
```

### Deployment Considerations

**JAR Packaging**:
- Use Maven Shade Plugin for uber JAR with all dependencies
- Include JavaFX runtime for distribution

**Database Migration**:
- Schema versioning in `src/main/resources/db/`
- Migration scripts for updates

**Configuration**:
- Default config bundled in JAR
- User config in `~/.libsearch/config.yaml`
- Environment variables for sensitive data (API keys)

---

## Extending the Application

### Adding a New Search Source

1. **Create Connector Class**:
```java
public class MySourceConnector implements SourceConnector {
    private static final String API_BASE = "https://api.mysource.com";
    private final OkHttpClient httpClient;

    public MySourceConnector() {
        this.httpClient = HttpClientFactory.createClient();
    }

    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) {
        // Implement API call and parsing
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.MY_SOURCE;
    }

    @Override
    public boolean isAvailable() {
        // Test connectivity
    }
}
```

2. **Add to SourceType Enum**:
```java
public enum SourceType {
    ARXIV, PUBMED, CROSSREF, MY_SOURCE
}
```

3. **Register in Configuration**:
```yaml
sources:
  my_source:
    enabled: true
    api_key: ${MY_SOURCE_API_KEY}
    max_results: 50
```

4. **Register in ConnectorFactory** (if using factory):
```java
public class ConnectorFactory {
    public static List<SourceConnector> getConnectors(Set<SourceType> types) {
        List<SourceConnector> connectors = new ArrayList<>();

        for (SourceType type : types) {
            switch (type) {
                case ARXIV -> connectors.add(new ArxivConnector());
                case MY_SOURCE -> connectors.add(new MySourceConnector());
                // ...
            }
        }

        return connectors;
    }
}
```

### Adding New CLI Commands

1. **Create Command Class**:
```java
@Command(name = "mycommand", description = "My custom command")
public class MyCommand implements Callable<Integer> {
    @Parameters(index = "0")
    private String parameter;

    @Override
    public Integer call() throws Exception {
        // Implementation
        return 0;
    }
}
```

2. **Register in LibSearchCLI**:
```java
@Command(
    subcommands = {
        SearchCommand.class,
        MyCommand.class  // Add here
    }
)
public class LibSearchCLI { }
```

---

## Performance Optimization

### Indexing Performance

- **Batch Commits**: Commit Lucene index in batches (100-1000 docs)
- **RAM Buffer**: Increase IndexWriter RAM buffer size
- **Merge Policy**: Configure merge policy for fewer segments

### Search Performance

- **Connection Pooling**: Reuse HTTP connections with OkHttp
- **Parallel Searches**: Use ExecutorService for concurrent source queries
- **Caching**: Cache search results in database
- **Result Limits**: Enforce max results per source

### Database Performance

- **Indexes**: Add indexes on frequently queried columns
- **Prepared Statements**: Always use prepared statements
- **Batch Inserts**: Use batch operations for multiple inserts
- **Connection Pooling**: Consider HikariCP for connection pooling

---

## Security Considerations

### Input Validation

- Sanitize user queries before sending to APIs
- Validate file paths for downloads
- Prevent SQL injection with prepared statements
- Validate YAML configuration files

### API Keys

- Never hardcode API keys
- Use environment variables
- Encrypt sensitive configuration
- Rotate keys periodically

### Network Security

- Use HTTPS for all API calls
- Validate SSL certificates
- Support proxy authentication
- Implement request rate limiting

---

## Troubleshooting

### Common Issues

**JavaFX Module Errors**:
```
Error: JavaFX runtime components are missing
```
Solution: Ensure JavaFX modules are on module path

**Lucene Index Corruption**:
```
CorruptIndexException
```
Solution: Delete and rebuild index with `index clear` command

**Database Locked**:
```
SQLException: database is locked
```
Solution: Ensure only one connection is open, use connection pooling

**API Rate Limiting**:
```
HTTP 429 Too Many Requests
```
Solution: Implement exponential backoff, respect rate limits

---

## Conclusion

This developer guide provides a comprehensive overview of the LibSearch application architecture, implementation details, and extension points. For usage instructions, see the separate USER_GUIDE.md document.

**Key Takeaways**:
- Modular architecture with clear layer separation
- Extensible connector system for adding new sources
- Dual interface (GUI/CLI) from single codebase
- Full-text indexing for offline capability
- Comprehensive monitoring and session tracking

**Development Team**: qwitch13 (nebulai13) & zahieddo
