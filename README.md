# LibSearch - Academic Federated Search Application

**Version:** 1.0.0
**Java:** 21 (Corretto)
**JavaFX:** 21.0.6

## Overview

LibSearch is a comprehensive academic search application that provides federated search across multiple academic databases and search engines. It features both a JavaFX GUI and a command-line interface (CLI) with REPL mode, full-text indexing, PDF downloads, bookmarks, citation export (BibTeX), and keyword monitoring.

## Features

### Core Functionality
- **Federated Search** across multiple sources:
  - arXiv, PubMed, CrossRef, Google Scholar, Semantic Scholar
  - Primo, OBV Network (institutional)
  - DuckDuckGo, Bing, Brave
  - Wikipedia, GitHub, StackOverflow, Reddit

- **Advanced Query Syntax** (grep-like):
  - `author:Smith` - Filter by author
  - `year:>2020` or `year:2020..2024` - Year ranges
  - `type:article` - Document type filter
  - `site:arxiv.org` - Limit to specific site
  - `filetype:pdf` - File type filter
  - Boolean operators: `AND`, `OR`, `NOT`, `+`, `-`
  - Phrases: `"machine learning"`

- **Full-Text Indexing** (Apache Lucene):
  - Offline search capability
  - Auto-indexing of search results
  - Fast local queries

- **PDF Downloads**:
  - Single and bulk downloads
  - Queue-based with progress tracking
  - Rate limiting

- **Bookmarks**:
  - Save results for later
  - Tags and notes
  - Export to JSON/Markdown

- **Citation Export**:
  - BibTeX format
  - RIS format
  - EndNote format

- **Keyword Alerts**:
  - Monitor for specific keywords
  - Email/console/log notifications

- **Dual Interfaces**:
  - Rich JavaFX GUI
  - Interactive CLI (REPL mode)

## Project Structure

```
team-Se1-CSDC-HCW/
├── src/main/java/com/example/teamse1csdchcw/
│   ├── domain/           # POJOs and enums
│   ├── service/          # Business logic
│   ├── connector/        # API connectors
│   ├── repository/       # Data access (SQLite, Lucene)
│   ├── ui/               # JavaFX controllers
│   ├── cli/              # CLI commands and REPL
│   ├── util/             # Utilities
│   ├── config/           # Configuration management
│   └── exception/        # Custom exceptions
├── src/main/resources/
│   ├── config/           # YAML configuration
│   ├── db/               # Database schema
│   ├── fxml/             # JavaFX layouts
│   └── css/              # Stylesheets
└── pom.xml               # Maven build file
```

## Implementation Status

### ✅ Phase 1: Foundation (COMPLETE)
- [x] Maven dependencies configured
- [x] Java modules configured
- [x] Package structure created
- [x] Domain models implemented
- [x] SQLite database setup
- [x] Configuration service (YAML)
- [x] HTTP client factory (OkHttp)
- [x] Rate limiter
- [x] Logging configuration

### 🚧 Phase 2: Core Search (IN PROGRESS)
- [x] QueryParserService (grep-like syntax)
- [x] SourceConnector interface
- [x] ArxivConnector implemented
- [ ] PubMedConnector
- [ ] CrossRefConnector
- [ ] FederatedSearchService
- [ ] ResultAggregator
- [ ] Repository implementations

### 📋 Upcoming Phases
- Phase 3: JavaFX GUI
- Phase 4: Advanced Features (downloads, bookmarks, citations)
- Phase 5: Local Indexing (Lucene)
- Phase 6: CLI/REPL
- Phase 7: Networking (VPN support)
- Phase 8: Monitoring & Alerts
- Phase 9: Journaling & Session Management
- Phase 10: Polish & Documentation

## Building and Running

### Prerequisites
- Java 21 (Amazon Corretto recommended)
- Maven 3.8+

### Build
```bash
mvn clean install
```

### Run GUI
```bash
mvn javafx:run
```

### Run CLI
```bash
java -jar target/libsearch.jar search "machine learning"
java -jar target/libsearch.jar --interactive  # REPL mode
```

## Configuration

Configuration is loaded from:
1. Default: `src/main/resources/config/application.yaml`
2. User overrides: `~/.libsearch/config.yaml`

### Key Configuration Options

```yaml
sources:
  arxiv:
    enabled: true
    max_results: 50

  pubmed:
    enabled: true
    api_key: ${PUBMED_API_KEY}  # From environment variable

search:
  max_concurrent_sources: 5
  timeout_seconds: 30

download:
  output_dir: ~/.libsearch/downloads
  max_concurrent: 3

database:
  path: ~/.libsearch/data/libsearch.db
```

## Database Schema

SQLite database with the following tables:
- `sessions` - Session tracking
- `search_history` - Query history
- `search_results` - Cached results
- `bookmarks` - Saved bookmarks
- `alerts` - Keyword monitoring
- `downloads` - Download queue
- `journal` - Audit log

## Dependencies

### Core Libraries
- **OkHttp 4.12.0** - HTTP client
- **Jsoup 1.17.2** - HTML parsing
- **Apache Lucene 9.9.1** - Full-text search
- **SQLite JDBC 3.45.0** - Database
- **Jackson 2.16.1** - JSON/YAML
- **Picocli 4.7.5** - CLI framework
- **JLine3 3.25.1** - Terminal UI
- **SLF4J + Logback** - Logging
- **JBibTeX 1.0.18** - BibTeX support

### JavaFX
- JavaFX 21.0.6 (controls, fxml, web, swing, media)
- ControlsFX, TilesFX, FormsFX, ValidatorFX
- Ikonli, BootstrapFX

## Architecture

### Design Patterns
- **Repository Pattern** - Data access abstraction
- **Strategy Pattern** - Pluggable connectors
- **Service Layer** - Business logic encapsulation
- **Observer Pattern** - Reactive UI updates
- **Factory Pattern** - HTTP client creation

### Key Classes

**Domain Layer:**
- `SearchResult` - Base search result
- `AcademicPaper` - Academic paper with metadata
- `SearchQuery` - Parsed query with filters
- `SourceType` - Enum of sources

**Service Layer:**
- `QueryParserService` - Query parsing
- `FederatedSearchService` - Multi-source search orchestration
- `DownloadService` - Download management
- `IndexService` - Lucene indexing

**Connector Layer:**
- `SourceConnector` - Interface for all connectors
- `ArxivConnector` - arXiv.org implementation
- `PubMedConnector` - PubMed/NCBI
- `CrossRefConnector` - CrossRef API

## Query Examples

```
# Simple search
machine learning

# Author filter
machine learning author:Hinton

# Year range
neural networks year:2020..2024

# Multiple filters
"deep learning" author:Goodfellow year:>2018 type:article

# Boolean operators
(AI OR "artificial intelligence") AND ethics -military

# Site-specific
quantum computing site:arxiv.org filetype:pdf
```

## Logging

Logs are written to:
- Console: INFO level
- File: `~/.libsearch/logs/libsearch.log` (DEBUG level)

Logging levels by package:
- Service layer: INFO
- Connectors: DEBUG
- Repository: TRACE
- UI: ERROR

## Development

### Adding a New Connector

1. Create class implementing `SourceConnector`
2. Add source to `SourceType` enum
3. Implement `search()`, `getSourceType()`, `isAvailable()`
4. Register in `ConnectorFactory`
5. Add configuration to `application.yaml`

Example:
```java
public class MyConnector implements SourceConnector {
    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) {
        // Implementation
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.MY_SOURCE;
    }

    @Override
    public boolean isAvailable() {
        // Check connectivity
    }
}
```

## Testing

```bash
# Run all tests
mvn test

# Run integration tests (requires API access)
mvn test -Dgroups=integration

# Run with coverage
mvn clean test jacoco:report
```

## License

See LICENSE file for details.

## Contributing

This project was developed as part of the team-Se1-CSDC-HCW coursework.

## Support

For issues or questions, please refer to the documentation in the `docs/` directory:
- `docs/USER_GUIDE.md` - End user documentation
- `docs/DEVELOPER.md` - Developer guide
- `docs/ONBOARDING.md` - Quick start for new developers

---

Coders qwitch13(nebulai13) & zahieddo