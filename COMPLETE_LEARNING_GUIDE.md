# Complete Learning Guide: LibSearch Project

This guide explains every technology, concept, and architectural decision in this project. Perfect for learning Java development, JavaFX, Maven, and software architecture.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Maven: Build Tool](#maven-build-tool)
4. [JavaFX: UI Framework](#javafx-ui-framework)
5. [Project Architecture](#project-architecture)
6. [Key Concepts](#key-concepts)
7. [How Everything Connects](#how-everything-connects)
8. [Development Workflow](#development-workflow)

---

## Project Overview

### What is LibSearch?

LibSearch is an **academic search application** that searches multiple academic databases simultaneously (federated search). Think of it as a "Google for academic papers" that queries arXiv, PubMed, CrossRef, and more at once.

### Key Features

1. **Federated Search**: Query multiple sources in parallel
2. **Advanced Query Syntax**: Filter by author, year, type, etc.
3. **JavaFX GUI**: Rich desktop interface
4. **Local Caching**: SQLite database for offline access
5. **Full-Text Search**: Apache Lucene for fast local queries
6. **Export Citations**: BibTeX, RIS, EndNote formats

---

## Technology Stack

### Core Technologies

```
┌─────────────────────────────────────┐
│         Java 21 (LTS)               │  ← Programming Language
├─────────────────────────────────────┤
│         JavaFX 21.0.6               │  ← UI Framework
├─────────────────────────────────────┤
│         Maven 3.8+                  │  ← Build Tool
├─────────────────────────────────────┤
│         SQLite 3.45                 │  ← Local Database
├─────────────────────────────────────┤
│         Apache Lucene 9.9           │  ← Search Engine
├─────────────────────────────────────┤
│         OkHttp 4.12                 │  ← HTTP Client
├─────────────────────────────────────┤
│         Jackson 2.16                │  ← JSON/YAML Parser
├─────────────────────────────────────┤
│         JUnit 5 + Mockito           │  ← Testing
└─────────────────────────────────────┘
```

### Why Each Technology?

| Technology | Purpose | Alternative |
|------------|---------|-------------|
| **Java 21** | Modern Java with latest features | Java 17 (older LTS) |
| **JavaFX** | Rich desktop UI framework | Swing (old), Electron (web-based) |
| **Maven** | Dependency management & build | Gradle (more flexible) |
| **SQLite** | Lightweight embedded database | H2, Derby |
| **Lucene** | Full-text search engine | Elasticsearch (overkill) |
| **OkHttp** | Modern HTTP client | Java HttpClient, Apache HttpClient |
| **Jackson** | JSON/YAML parsing | Gson (Google's library) |

---

## Maven: Build Tool

### What is Maven?

Maven is a **build automation tool** that:
1. Manages dependencies (libraries)
2. Compiles your code
3. Runs tests
4. Packages your application
5. Enforces project structure

### The `pom.xml` File

**POM** = **P**roject **O**bject **M**odel

This file is Maven's configuration. Let's break it down:

```xml
<project>
    <!-- Who you are -->
    <groupId>com.example</groupId>           <!-- Company/org -->
    <artifactId>team-Se1-CSDC-HCW</artifactId>  <!-- Project name -->
    <version>1.0-SNAPSHOT</version>          <!-- Version -->

    <!-- Settings -->
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.12.1</junit.version>
    </properties>

    <!-- Dependencies (libraries you need) -->
    <dependencies>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>21.0.6</version>
        </dependency>
        <!-- ... more dependencies ... -->
    </dependencies>

    <!-- Build configuration -->
    <build>
        <plugins>
            <!-- Compiler plugin -->
            <!-- JavaFX plugin -->
        </plugins>
    </build>
</project>
```

### Maven Coordinates

Every library has three parts (GAV):

```
groupId:artifactId:version

Example:
org.openjfx:javafx-controls:21.0.6
   │           │              │
   Group       Artifact       Version
```

**Group**: Organization (reverse domain, like `com.google`, `org.apache`)
**Artifact**: Library name
**Version**: Specific version

### Maven Lifecycle

Maven has **phases** that run in order:

```
validate → compile → test → package → install → deploy
    ↓         ↓        ↓        ↓         ↓        ↓
  Check    Compile   Run    Create    Install  Upload
  config    .java   tests    JAR     to local to repo
                                      Maven
```

**Commands:**
```bash
mvn clean         # Delete target/ directory
mvn compile       # Compile code to .class files
mvn test          # Run JUnit tests
mvn package       # Create JAR file
mvn install       # Install JAR to local Maven repo (~/.m2)
mvn clean install # Clean + compile + test + package + install
```

### Maven Wrapper (`mvnw`)

The `mvnw` script ensures everyone uses the **same Maven version**:

```bash
./mvnw clean install   # Uses Maven 3.8.5 (configured in wrapper)
```

Benefits:
- No need to install Maven globally
- Version consistency across team
- Works on CI/CD servers

**Files:**
- `mvnw` - Unix/Mac script
- `mvnw.cmd` - Windows script
- `.mvn/wrapper/` - Wrapper JAR and config

### Maven Repository

When you add a dependency, Maven downloads it from **Maven Central** (https://repo.maven.apache.org/maven2/):

```
Dependencies are stored in: ~/.m2/repository/

Example:
~/.m2/repository/org/openjfx/javafx-controls/21.0.6/javafx-controls-21.0.6.jar
```

### Dependency Scopes

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.12.1</version>
    <scope>test</scope>  <!-- Only available in tests -->
</dependency>
```

**Scopes:**
- `compile` (default) - Available everywhere
- `test` - Only in test code
- `provided` - Provided by runtime (e.g., servlet container)
- `runtime` - Not needed for compilation, only runtime

### Maven Plugins

Plugins extend Maven's functionality:

#### Compiler Plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <source>21</source>  <!-- Use Java 21 syntax -->
        <target>21</target>  <!-- Compile to Java 21 bytecode -->
    </configuration>
</plugin>
```

#### JavaFX Plugin
```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.teamse1csdchcw.HelloApplication</mainClass>
    </configuration>
</plugin>
```

**Run with:** `mvn javafx:run`

---

## JavaFX: UI Framework

### What is JavaFX?

JavaFX is a **modern Java framework** for building desktop applications with rich UIs.

**History:**
- 2008: JavaFX 1.0 (separate language)
- 2011: JavaFX 2.0 (pure Java)
- 2014: Included in Java 8
- 2018: Removed from JDK, now separate (modular)
- 2024: JavaFX 21 (aligned with Java 21 LTS)

### JavaFX Architecture

```
┌─────────────────────────────────────────┐
│  Your Application (Java Code)          │
├─────────────────────────────────────────┤
│  JavaFX Application Thread             │  ← UI updates must happen here
├─────────────────────────────────────────┤
│  Scene Graph                            │  ← Tree of UI nodes
├─────────────────────────────────────────┤
│  Rendering Engine (Prism)              │  ← Hardware-accelerated
├─────────────────────────────────────────┤
│  Native Platform (Quantum)             │  ← OS integration
└─────────────────────────────────────────┘
```

### Key Concepts

#### 1. Stage and Scene

```java
Stage (Window)
  └── Scene (Content)
        └── Root Node (Layout)
              └── Children (Buttons, TextFields, etc.)
```

**Analogy:** Think of a theater
- **Stage** = The theater building (window)
- **Scene** = The play being performed (content)
- **Nodes** = Actors and props (UI elements)

#### 2. Scene Graph

JavaFX UI is a **tree** of nodes:

```
BorderPane (root)
  ├── VBox (top)
  │     ├── MenuBar
  │     └── ToolBar
  ├── TableView (center)
  ├── VBox (left)
  │     └── ListView
  └── HBox (bottom)
        └── Label (status)
```

#### 3. FXML (UI Markup)

Instead of creating UI in Java code:

```java
// Java code (verbose)
VBox vbox = new VBox();
Button btn = new Button("Click me");
vbox.getChildren().add(btn);
```

Use FXML (declarative):

```xml
<!-- FXML (declarative) -->
<VBox>
    <Button text="Click me"/>
</VBox>
```

**Benefits:**
- Separation of UI and logic
- Easier to visualize
- Can use Scene Builder (visual editor)

#### 4. Controller Pattern (MVC)

JavaFX uses **Model-View-Controller**:

```
FXML File (View)  ←→  Controller (Java)  ←→  Model (Data)
    │                       │                      │
  Defines UI          Handles events         Business logic
  appearance          Updates UI             Data access
```

**Example:**

**FXML (main-view.fxml):**
```xml
<TextField fx:id="searchTextField"/>
<Button text="Search" onAction="#onSearch"/>
```

**Controller (MainController.java):**
```java
public class MainController {
    @FXML
    private TextField searchTextField;  // Linked to fx:id

    @FXML
    private void onSearch(ActionEvent event) {  // Linked to onAction
        String query = searchTextField.getText();
        // Perform search...
    }
}
```

#### 5. Properties and Binding

JavaFX has **observable properties**:

```java
// Traditional Java
private String name;
public String getName() { return name; }
public void setName(String name) { this.name = name; }

// JavaFX Property
private StringProperty name = new SimpleStringProperty();
public StringProperty nameProperty() { return name; }
public String getName() { return name.get(); }
public void setName(String name) { this.name.set(name); }
```

**Why?** You can **bind** properties:

```java
label.textProperty().bind(textField.textProperty());
// Now label always shows whatever is in textField!
```

#### 6. Collections

JavaFX has **observable collections**:

```java
ObservableList<String> items = FXCollections.observableArrayList();
items.add("Item 1");
items.add("Item 2");

ListView<String> listView = new ListView<>(items);
// ListView automatically updates when items change!
```

### JavaFX Components in This Project

#### Layouts

| Layout | Purpose | Example |
|--------|---------|---------|
| **BorderPane** | Top, Bottom, Left, Right, Center | Main window |
| **VBox** | Vertical stack | Sidebar, forms |
| **HBox** | Horizontal row | Toolbars, buttons |
| **GridPane** | Grid (rows × columns) | Forms with labels |
| **StackPane** | Layers (z-axis) | Overlays |

#### Controls

| Control | Purpose |
|---------|---------|
| **TextField** | Single-line text input |
| **TextArea** | Multi-line text input |
| **Button** | Clickable button |
| **CheckBox** | On/off toggle |
| **ComboBox** | Dropdown menu |
| **ListView** | Scrollable list |
| **TableView** | Data table (like Excel) |
| **MenuBar** | Application menu (File, Edit, etc.) |
| **ProgressBar** | Show progress |
| **Label** | Display text |

### CSS Styling

JavaFX uses **CSS** for styling:

```css
/* main.css */
.button {
    -fx-background-color: #2196F3;
    -fx-text-fill: white;
    -fx-font-size: 14px;
}

.button:hover {
    -fx-background-color: #1976D2;
}
```

**In FXML:**
```xml
<BorderPane stylesheets="@../css/main.css">
    <Button styleClass="primary-button" text="Search"/>
</BorderPane>
```

### JavaFX Threading

**CRITICAL RULE:** All UI updates must happen on the **JavaFX Application Thread**.

```java
// ❌ WRONG - updating UI from background thread
new Thread(() -> {
    label.setText("Done!");  // CRASH!
}).start();

// ✅ CORRECT - use Platform.runLater()
new Thread(() -> {
    // Do background work...
    String result = doWork();

    // Update UI on JavaFX thread
    Platform.runLater(() -> {
        label.setText(result);
    });
}).start();
```

---

## Project Architecture

### Package Structure

```
com.example.teamse1csdchcw/
├── domain/                    # Data models (POJOs)
│   ├── search/
│   │   ├── SearchResult.java       # Base result class
│   │   ├── AcademicPaper.java      # Academic paper with metadata
│   │   └── SearchQuery.java        # Parsed search query
│   ├── source/
│   │   └── SourceType.java         # Enum of sources (arXiv, PubMed, etc.)
│   └── user/
│       └── Bookmark.java           # User bookmarks
│
├── service/                   # Business logic
│   ├── search/
│   │   ├── QueryParserService.java       # Parse search queries
│   │   ├── FederatedSearchService.java   # Coordinate searches
│   │   └── ResultAggregator.java         # Merge & deduplicate results
│   └── connector/
│       ├── SourceConnector.java          # Interface for all connectors
│       ├── ConnectorFactory.java         # Create connector instances
│       ├── ArxivConnector.java           # arXiv implementation
│       ├── PubMedConnector.java          # PubMed implementation
│       └── CrossRefConnector.java        # CrossRef implementation
│
├── repository/                # Data access layer
│   ├── SearchResultRepository.java
│   ├── SearchHistoryRepository.java
│   └── sqlite/
│       ├── SQLiteConnection.java
│       └── DatabaseInitializer.java
│
├── ui/                        # User interface
│   └── controller/
│       ├── MainController.java       # Main window controller
│       ├── SearchController.java     # Search panel controller
│       └── ResultsController.java    # Results table controller
│
├── util/                      # Utilities
│   └── http/
│       ├── HttpClientFactory.java    # Create HTTP clients
│       └── RateLimiter.java          # Prevent API overload
│
├── config/
│   └── ConfigService.java     # Load YAML configuration
│
├── exception/
│   ├── SearchException.java
│   └── ConnectorException.java
│
└── HelloApplication.java      # Main entry point
```

### Architectural Layers

```
┌─────────────────────────────────────┐
│   Presentation Layer (UI)           │  ← JavaFX, FXML, Controllers
├─────────────────────────────────────┤
│   Service Layer (Business Logic)    │  ← Search, Parsing, Aggregation
├─────────────────────────────────────┤
│   Connector Layer (External APIs)   │  ← arXiv, PubMed, CrossRef
├─────────────────────────────────────┤
│   Repository Layer (Data Access)    │  ← SQLite, Lucene
├─────────────────────────────────────┤
│   Domain Layer (Data Models)        │  ← POJOs, Entities
└─────────────────────────────────────┘
```

### Design Patterns Used

#### 1. Repository Pattern

**Purpose:** Abstract data access

```java
public interface SearchResultRepository {
    void save(SearchResult result);
    List<SearchResult> findByQuery(String query);
    SearchResult findById(String id);
}
```

**Benefits:**
- Swap SQLite for PostgreSQL easily
- Easy to mock for testing
- Cleaner service layer code

#### 2. Strategy Pattern

**Purpose:** Pluggable connectors

```java
public interface SourceConnector {
    List<SearchResult> search(SearchQuery query, int maxResults);
    SourceType getSourceType();
    boolean isAvailable();
}
```

**Benefits:**
- Add new sources easily
- Each connector is independent
- Easy to enable/disable sources

#### 3. Factory Pattern

**Purpose:** Create connector instances

```java
public class ConnectorFactory {
    public SourceConnector getConnector(SourceType type) {
        return connectors.get(type);
    }
}
```

**Benefits:**
- Centralized connector creation
- Singleton pattern for efficiency
- Lazy initialization

#### 4. Service Layer Pattern

**Purpose:** Encapsulate business logic

```java
public class FederatedSearchService {
    public List<SearchResult> search(SearchQuery query, Set<SourceType> sources) {
        // 1. Get connectors
        // 2. Execute parallel searches
        // 3. Aggregate results
        // 4. Return deduplicated list
    }
}
```

**Benefits:**
- Controllers stay thin
- Business logic is testable
- Reusable across UI and CLI

#### 5. MVC (Model-View-Controller)

**Purpose:** Separate UI from logic

```
Model (SearchResult)  ←→  Controller (SearchController)  ←→  View (search-panel.fxml)
```

**Benefits:**
- Easier to test
- Designers work on FXML, developers on Java
- Multiple views for same model

---

## Key Concepts

### 1. Federated Search

**Problem:** Users need to search multiple academic databases

**Traditional Approach:**
1. Search arXiv → get results
2. Search PubMed → get results
3. Search CrossRef → get results
4. Manually combine and compare

**Federated Search:**
1. Send query to all sources **in parallel**
2. Automatically merge results
3. Remove duplicates (same paper from different sources)
4. Sort by relevance

**Implementation:**

```java
// Execute searches in parallel using CompletableFuture
List<CompletableFuture<SearchSourceResult>> futures = new ArrayList<>();

for (SourceConnector connector : connectors) {
    CompletableFuture<SearchSourceResult> future =
        CompletableFuture.supplyAsync(() -> searchSource(connector, query));
    futures.add(future);
}

// Wait for all to complete (with timeout)
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .get(30, TimeUnit.SECONDS);

// Collect results
List<SearchResult> allResults = futures.stream()
    .map(f -> f.get())
    .flatMap(r -> r.results.stream())
    .collect(Collectors.toList());
```

### 2. Query Parsing

**Problem:** Users want Google-like search syntax

**Input:**
```
"machine learning" author:Hinton year:>2020 type:article
```

**Output (SearchQuery object):**
```java
SearchQuery {
    phrases: ["machine learning"]
    authorFilter: "Hinton"
    yearFrom: 2020
    yearTo: null
    typeFilter: "article"
    keywords: []
}
```

**Implementation:**

Uses **regular expressions** to extract filters:

```java
private static final Pattern AUTHOR_PATTERN = Pattern.compile("author:([\\w\\s]+)");
private static final Pattern YEAR_PATTERN = Pattern.compile("year:([><]?\\d{4}(?:\\.\\.\\.?\\d{4})?)");

public SearchQuery parse(String queryString) {
    SearchQuery query = new SearchQuery(queryString);

    // Extract author
    Matcher matcher = AUTHOR_PATTERN.matcher(queryString);
    if (matcher.find()) {
        query.setAuthorFilter(matcher.group(1));
    }

    // Extract year
    // ... etc

    return query;
}
```

### 3. Result Aggregation & Deduplication

**Problem:** Same paper appears from multiple sources

**Example:**
- arXiv: "Deep Learning" (arxiv:2012.12345)
- CrossRef: "Deep Learning" (DOI: 10.1234/xyz)
- Semantic Scholar: "Deep Learning" (DOI: 10.1234/xyz)

**Solution:** Deduplicate by DOI, arXiv ID, or normalized title

```java
private String generateDeduplicationKey(SearchResult result) {
    if (result instanceof AcademicPaper paper) {
        // Priority: DOI > arXiv ID > PMID > URL > title hash
        if (paper.getDoi() != null) {
            return "doi:" + paper.getDoi().toLowerCase();
        }
        if (paper.getArxivId() != null) {
            return "arxiv:" + paper.getArxivId().toLowerCase();
        }
        // ...
    }
    return "title:" + normalizeTitle(result.getTitle()).hashCode();
}
```

### 4. Rate Limiting

**Problem:** APIs have rate limits (e.g., 3 requests/second)

**Solution:** Token bucket algorithm

```java
public class RateLimiter {
    private final Map<String, Semaphore> limiters = new ConcurrentHashMap<>();

    public void acquire(String domain) {
        Semaphore semaphore = limiters.computeIfAbsent(domain,
            k -> new Semaphore(3)); // 3 requests

        semaphore.acquire(); // Wait if limit reached

        // Release after 1 second
        scheduler.schedule(() -> semaphore.release(), 1, TimeUnit.SECONDS);
    }
}
```

### 5. HTTP Client

**Why OkHttp?**
- Modern, efficient
- Built-in connection pooling
- Interceptors for logging
- Timeout handling

**Usage:**

```java
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(new LoggingInterceptor())
    .build();

Request request = new Request.Builder()
    .url("https://export.arxiv.org/api/query?search_query=machine+learning")
    .build();

Response response = client.newCall(request).execute();
String body = response.body().string();
```

### 6. JSON Parsing

**Jackson** converts JSON ↔ Java objects:

```java
// JSON to Java
ObjectMapper mapper = new ObjectMapper();
SearchResult result = mapper.readValue(jsonString, SearchResult.class);

// Java to JSON
String json = mapper.writeValueAsString(result);
```

**With annotations:**

```java
public class AcademicPaper {
    @JsonProperty("title")
    private String title;

    @JsonProperty("publication_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate publicationDate;

    @JsonIgnore  // Don't serialize this field
    private transient String cachedData;
}
```

### 7. SQLite Database

**Why SQLite?**
- Embedded (no server needed)
- Single file database
- Zero configuration
- Perfect for desktop apps

**Schema:**

```sql
CREATE TABLE search_results (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    authors TEXT,
    abstract TEXT,
    url TEXT,
    source TEXT,
    publication_date TEXT,
    doi TEXT,
    arxiv_id TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doi ON search_results(doi);
CREATE INDEX idx_arxiv ON search_results(arxiv_id);
```

**Usage:**

```java
Connection conn = DriverManager.getConnection("jdbc:sqlite:libsearch.db");
PreparedStatement stmt = conn.prepareStatement(
    "INSERT INTO search_results (id, title, authors) VALUES (?, ?, ?)"
);
stmt.setString(1, result.getId());
stmt.setString(2, result.getTitle());
stmt.setString(3, String.join(", ", result.getAuthors()));
stmt.executeUpdate();
```

### 8. Apache Lucene

**What is Lucene?**
Full-text search engine (used by Elasticsearch, Solr)

**Use Case:** Search cached papers offline

**How it works:**

```
1. Indexing:
   Paper → Analyzer → Tokens → Inverted Index
   "Machine Learning is awesome"
   → ["machine", "learn", "awesom"]  (stemming)

2. Searching:
   Query → Tokens → Match in Index → Ranked Results
```

**Implementation:**

```java
// Create index
Directory directory = FSDirectory.open(Paths.get("index"));
IndexWriter writer = new IndexWriter(directory, config);

// Add document
Document doc = new Document();
doc.add(new TextField("title", paper.getTitle(), Field.Store.YES));
doc.add(new TextField("abstract", paper.getAbstractText(), Field.Store.YES));
writer.addDocument(doc);

// Search
IndexSearcher searcher = new IndexSearcher(reader);
QueryParser parser = new QueryParser("title", analyzer);
Query query = parser.parse("machine learning");
TopDocs results = searcher.search(query, 10);
```

---

## How Everything Connects

### Full Search Flow

```
1. User enters query in SearchController
        ↓
2. SearchController calls QueryParserService
        ↓
3. QueryParserService parses query into SearchQuery object
        ↓
4. SearchController calls FederatedSearchService.search()
        ↓
5. FederatedSearchService gets connectors from ConnectorFactory
        ↓
6. FederatedSearchService executes parallel searches
        ↓ ↓ ↓
   ArxivConnector  PubMedConnector  CrossRefConnector
        ↓ ↓ ↓
   HTTP requests to external APIs
        ↓ ↓ ↓
7. Connectors parse responses, return SearchResult objects
        ↓
8. ResultAggregator merges and deduplicates results
        ↓
9. Results cached in SearchResultRepository (SQLite)
        ↓
10. Results returned to SearchController
        ↓
11. SearchController updates ResultsController (TableView)
        ↓
12. User sees results in GUI
```

### Startup Sequence

```
1. HelloApplication.main()
        ↓
2. JavaFX Application.launch()
        ↓
3. HelloApplication.start(Stage primaryStage)
        ↓
4. Load main-view.fxml
        ↓
5. FXMLLoader creates MainController
        ↓
6. FXML includes search-panel.fxml and results-table.fxml
        ↓
7. FXMLLoader creates SearchController and ResultsController
        ↓
8. Controllers' @FXML fields injected
        ↓
9. Controllers' initialize() methods called
        ↓
10. DatabaseInitializer.createTables()
        ↓
11. ConnectorFactory.getInstance() (lazy init)
        ↓
12. Stage.show() - GUI appears
```

---

## Development Workflow

### 1. Setting Up

```bash
# Clone repository
git clone <repo-url>
cd team-Se1-CSDC-HCW

# Build project
./mvnw clean install

# Run application
./mvnw javafx:run
```

### 2. Making Changes

#### Adding a New Feature

1. **Create branch**
   ```bash
   git checkout -b feature/my-new-feature
   ```

2. **Write code**
   - Add new classes in appropriate packages
   - Follow existing patterns

3. **Write tests**
   ```java
   @Test
   public void testMyFeature() {
       // Arrange
       MyClass obj = new MyClass();

       // Act
       String result = obj.doSomething();

       // Assert
       assertEquals("expected", result);
   }
   ```

4. **Run tests**
   ```bash
   ./mvnw test
   ```

5. **Build**
   ```bash
   ./mvnw clean install
   ```

#### Modifying the UI

1. **Open FXML file** in IntelliJ or Scene Builder

2. **Make changes**
   - Add new components
   - Adjust layout
   - Update styles

3. **Update controller**
   ```java
   @FXML
   private Button newButton;  // Match fx:id in FXML

   @FXML
   private void onNewButtonClick(ActionEvent event) {
       // Handle click
   }
   ```

4. **Run application**
   ```bash
   ./mvnw javafx:run
   ```

### 3. Common Tasks

#### Adding a Dependency

1. Find on Maven Central: https://search.maven.org/

2. Add to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.example</groupId>
       <artifactId>cool-library</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

3. Reload Maven in IntelliJ:
   - Maven panel → Reload button
   - Or: `./mvnw dependency:resolve`

#### Debugging

**In IntelliJ:**
1. Set breakpoint (click left gutter)
2. Debug → Run 'HelloApplication' with Debug
3. Step through code (F8 = step over, F7 = step into)

**In Maven:**
```bash
mvn javafx:run -X  # Debug output
```

**JavaFX Debugging:**
```java
// Print scene graph
printSceneGraph(root, 0);

private void printSceneGraph(Node node, int depth) {
    System.out.println("  ".repeat(depth) + node.getClass().getSimpleName());
    if (node instanceof Parent parent) {
        parent.getChildrenUnmodifiable()
              .forEach(child -> printSceneGraph(child, depth + 1));
    }
}
```

#### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=FederatedSearchIntegrationTest

# Specific test method
./mvnw test -Dtest=FederatedSearchIntegrationTest#testSimpleSearch

# Skip tests
./mvnw install -DskipTests
```

### 4. IntelliJ Tips

#### Shortcuts

| Action | Mac | Windows/Linux |
|--------|-----|---------------|
| Search everywhere | ⌘+Shift+A | Ctrl+Shift+A |
| Find class | ⌘+O | Ctrl+N |
| Find file | ⌘+Shift+O | Ctrl+Shift+N |
| Auto-import | ⌘+Option+O | Ctrl+Alt+O |
| Format code | ⌘+Option+L | Ctrl+Alt+L |
| Refactor/Rename | Shift+F6 | Shift+F6 |
| Quick fix | Option+Enter | Alt+Enter |
| Run | Ctrl+R | Shift+F10 |
| Debug | Ctrl+D | Shift+F9 |

#### Live Templates

Type abbreviation + Tab:

- `sout` → `System.out.println();`
- `psvm` → `public static void main(String[] args) {}`
- `fori` → `for (int i = 0; i < ; i++) {}`

#### Code Generation

- `Cmd+N` / `Alt+Insert` in class:
  - Generate constructor
  - Generate getters/setters
  - Generate equals/hashCode
  - Generate toString

---

## Learning Resources

### JavaFX
- **Official Docs**: https://openjfx.io/
- **Oracle Tutorial**: https://docs.oracle.com/javafx/2/get_started/jfxpub-get_started.htm
- **Scene Builder**: https://gluonhq.com/products/scene-builder/

### Maven
- **Official Guide**: https://maven.apache.org/guides/getting-started/
- **Intro to POM**: https://maven.apache.org/guides/introduction/introduction-to-the-pom.html
- **Dependency Mechanism**: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html

### Java
- **Official Tutorial**: https://docs.oracle.com/javase/tutorial/
- **Effective Java** (book) by Joshua Bloch
- **Modern Java in Action** (book)

### Design Patterns
- **Refactoring Guru**: https://refactoring.guru/design-patterns
- **Design Patterns** (book) by Gang of Four
- **Head First Design Patterns** (book)

### SQL & Databases
- **SQLite Tutorial**: https://www.sqlitetutorial.net/
- **JDBC Tutorial**: https://docs.oracle.com/javase/tutorial/jdbc/

### HTTP & APIs
- **OkHttp**: https://square.github.io/okhttp/
- **Jackson**: https://github.com/FasterXML/jackson-docs
- **REST API Design**: https://restfulapi.net/

---

## Practice Exercises

### Beginner

1. **Add a status bar**
   - Show "Ready", "Searching...", "Found X results"
   - Update during search

2. **Add keyboard shortcuts**
   - Ctrl+F to focus search field
   - Ctrl+Enter to search

3. **Save window size**
   - Remember window dimensions
   - Restore on next launch

### Intermediate

4. **Add export to CSV**
   - New menu item: File → Export Results
   - Write results to CSV file

5. **Add search history**
   - Store last 10 searches
   - Dropdown with recent searches

6. **Add bookmarking**
   - Star button on each result
   - Saved to SQLite
   - Show bookmarks in sidebar

### Advanced

7. **Add PDF download**
   - Queue downloads
   - Show progress bar
   - Save to configured directory

8. **Add local indexing**
   - Index all search results with Lucene
   - Offline search capability

9. **Add a new connector**
   - Implement IEEE Xplore or Google Scholar
   - Follow SourceConnector interface
   - Register in ConnectorFactory

---

## Glossary

**Artifact**: A file (usually JAR) produced by Maven build

**Bytecode**: Compiled Java code (.class files) run by JVM

**Controller**: Java class that handles UI events

**Dependency**: External library your project needs

**FXML**: JavaFX Markup Language (XML for UI)

**fx:id**: Attribute linking FXML elements to controller fields

**GroupId**: Organization identifier in Maven (e.g., org.apache)

**JavaFX Application Thread**: Special thread for UI updates

**JAR**: Java Archive (ZIP file with .class files)

**Lifecycle**: Sequence of Maven build phases

**MVC**: Model-View-Controller design pattern

**Node**: Any element in JavaFX scene graph

**Observable**: JavaFX object that notifies listeners of changes

**Plugin**: Maven extension for additional tasks

**POM**: Project Object Model (pom.xml file)

**Repository**: Storage for Maven artifacts (local or remote)

**Scene**: Content displayed in a JavaFX Stage

**Scope**: When a dependency is available (compile, test, etc.)

**Stage**: Top-level JavaFX container (window)

**Wrapper**: mvnw script that downloads correct Maven version

---

## Next Steps

1. **Read through existing code**
   - Start with `HelloApplication.java`
   - Follow the flow to controllers
   - Examine one connector implementation

2. **Make small changes**
   - Modify button text
   - Add a label
   - Change colors in CSS

3. **Experiment**
   - Break something, see what happens
   - Fix the error
   - Learn from the process

4. **Build something new**
   - Add a feature you want
   - Follow existing patterns
   - Ask for help when stuck

5. **Read documentation**
   - JavaFX docs for UI questions
   - Maven docs for build questions
   - Library-specific docs for dependencies

---

Remember: This is a learning journey. Don't try to understand everything at once. Pick one area, master it, then move to the next. Happy coding!
