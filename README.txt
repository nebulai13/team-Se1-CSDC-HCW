// README (commented TODO version)
// Project: team-Se1-CSDC-HCW — Library Search (LibSearch)
// Repository: nebulai13/team-Se1-CSDC-HCW
// Date: 2026-01-16
// still to update: refine details after verification

// Purpose
// still to update: Federated academic search client & toolkit for research workflows.
// todo: Confirm supported sources and capabilities.

// Key Features
// still to update: Unified search across multiple sources (ArXiv, CrossRef, PubMed, Semantic Scholar).
// still to update: Search history, bookmarking, sessions, monitoring alerts.
// still to update: JavaFX desktop UI + interactive CLI.
// todo: Add export/format and download handling notes.

// Architecture
// still to update: Java 17+ with Gradle; modularized under com.example.teamse1csdchcw.
// still to update: Connectors in service/connector/ (ArxivConnector, CrossRefConnector, PubMedConnector, SemanticScholarConnector).
// still to update: Domain models under domain/search, domain/user, domain/monitoring.
// still to update: Persistence via repository/sqlite/ (DatabaseInitializer, SQLiteConnection).
// todo: Document config service and application.yaml settings.

// UI (JavaFX)
// still to update: FXML views under resources/com/example/teamse1csdchcw/fxml (main-view.fxml, search-panel.fxml, results-table.fxml).
// still to update: Entry points in HelloApplication and Launcher.
// todo: Add screenshots and usage flow.

// CLI
// still to update: LibSearchCLI with commands (SearchCommand, BookmarkCommand, IndexCommand, MonitorCommand, NetworkCommand, SessionCommand, ReplCommand).
// todo: Document flags and examples for each command.

// Build & Run
// still to update: Requires JDK 17+. Build with Gradle.
// todo: Commands
// todo: ./gradlew clean build
// todo: ./gradlew run (if JavaFX app is configured)
// todo: ./libsearch.sh or libsearch.bat for CLI (verify entrypoint).

// Configuration
// still to update: resources/config/application.yaml for settings (API keys, database path, logging).
// todo: Describe logback.xml levels and outputs.

// Testing
// still to update: Integration tests under src/test/java/... (FederatedSearchIntegrationTest).
// todo: ./gradlew test and coverage reporting.

// Deployment
// still to update: Packaging options (fat JAR/native image if applicable).
// todo: Document release process and versioning.

// Contribution Guidelines
// still to update: Preferred branching model, PR review process, issue templates.
// todo: Code style conventions and static analysis tooling.

// License
// still to update: See LICENSE at repo root.
// todo: Clarify third-party dependencies and their licenses.

// Acknowledgements
// still to update: Core developers and contributors.
// todo: Add names and roles of the project team.
// todo: Owner: nebulai13. Team: Se1 CSDC HCW.
// todo: Recognize course mentors and community resources.

// Changelog / Roadmap
// still to update: High-level milestones and features by phase.
// todo: Link to project outlook or roadmap (HTML file present).

// Notes
// still to update: Known limitations and future enhancements.
// todo: Security considerations, rate limits, retry/backoff in connectors.
