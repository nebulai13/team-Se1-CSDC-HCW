package com.example.teamse1csdchcw.ui.controller;

import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.domain.user.Bookmark;
import com.example.teamse1csdchcw.repository.BookmarkRepository;
import com.example.teamse1csdchcw.repository.DownloadRepository;
import com.example.teamse1csdchcw.repository.SearchHistoryRepository;
import com.example.teamse1csdchcw.repository.SessionRepository;
import com.example.teamse1csdchcw.service.connector.ConnectorFactory;
import com.example.teamse1csdchcw.service.connector.SourceConnector;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.time.format.DateTimeFormatter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main controller for the LibSearch application.
 * Orchestrates the overall UI and manages the application state.
 */
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // FXML injected components
    @FXML private ListView<SourceStatus> sourceListView;
    @FXML private Label sessionNameLabel;
    @FXML private Label resultsCountLabel;
    @FXML private Label resultsSummaryLabel;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label searchTimeLabel;

    // Controllers (injected by FXMLLoader via fx:include fx:id)
    @FXML private SearchController searchPanelController;
    @FXML private ResultsController resultsTableController;

    // Services
    private final SessionRepository sessionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final DownloadRepository downloadRepository;
    private final ConnectorFactory connectorFactory;

    // State
    private String currentSessionId;
    private int totalResults = 0;

    public MainController() {
        this.sessionRepository = new SessionRepository();
        this.bookmarkRepository = new BookmarkRepository();
        this.searchHistoryRepository = new SearchHistoryRepository();
        this.downloadRepository = new DownloadRepository();
        this.connectorFactory = ConnectorFactory.getInstance();
    }

    @FXML
    public void initialize() {
        logger.info("Initializing MainController");

        // Wire up included controllers
        if (searchPanelController != null) {
            searchPanelController.setMainController(this);
        }
        if (resultsTableController != null) {
            resultsTableController.setMainController(this);
        }
        if (searchPanelController != null && resultsTableController != null) {
            searchPanelController.setResultsController(resultsTableController);
        }

        // Initialize session
        initializeSession();

        // Initialize source status list
        initializeSource();

        // Initialize combo boxes
        sortComboBox.setValue("Relevance");
        typeFilterComboBox.setValue("All Types");

        // Set up event listeners
        sortComboBox.setOnAction(e -> onSortChanged());
        typeFilterComboBox.setOnAction(e -> onFilterChanged());

        // Update status
        setStatus("Ready");
    }

    /**
     * Initialize or resume session.
     */
    private void initializeSession() {
        try {
            // Try to resume most recent session
            SessionRepository.Session session = sessionRepository.findMostRecent();

            if (session != null) {
                currentSessionId = session.getId();
                sessionNameLabel.setText("Session: " + session.getName());
                logger.info("Resumed session: {}", session.getName());
            } else {
                // Create new session
                currentSessionId = sessionRepository.createSession("Default Session", null);
                sessionNameLabel.setText("Session: Default Session");
                logger.info("Created new session: {}", currentSessionId);
            }

        } catch (SQLException e) {
            logger.error("Failed to initialize session", e);
            showError("Session Error", "Failed to initialize session: " + e.getMessage());

            // Create fallback session ID
            currentSessionId = java.util.UUID.randomUUID().toString();
        }
    }

    /**
     * Initialize source status list.
     */
    private void initializeSource() {
        ObservableList<SourceStatus> sourceStatuses = FXCollections.observableArrayList();

        // Get all connectors
        List<SourceConnector> connectors = connectorFactory.getAllConnectors();

        // Check availability in background
        new Thread(() -> {
            for (SourceConnector connector : connectors) {
                SourceType type = connector.getSourceType();
                boolean available = false;

                try {
                    available = connector.isAvailable();
                } catch (Exception e) {
                    logger.warn("Failed to check availability for {}", type, e);
                }

                boolean finalAvailable = available;
                Platform.runLater(() -> {
                    SourceStatus status = new SourceStatus(type, finalAvailable);
                    sourceStatuses.add(status);
                });
            }
        }).start();

        sourceListView.setItems(sourceStatuses);
        sourceListView.setCellFactory(lv -> new SourceStatusCell());
    }

    /**
     * Update results count.
     */
    public void updateResultsCount(int count) {
        this.totalResults = count;
        resultsCountLabel.setText("Results: " + count);
        resultsSummaryLabel.setText(count + " paper" + (count != 1 ? "s" : ""));
    }

    /**
     * Set status message.
     */
    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Show progress indicator.
     */
    public void showProgress(boolean show) {
        progressIndicator.setVisible(show);
    }

    /**
     * Set search time.
     */
    public void setSearchTime(long milliseconds) {
        double seconds = milliseconds / 1000.0;
        searchTimeLabel.setText(String.format("%.2f seconds", seconds));
    }

    /**
     * Get current session ID.
     */
    public String getCurrentSessionId() {
        return currentSessionId;
    }

    // Menu actions
    @FXML
    private void onNewSession() {
        TextInputDialog dialog = new TextInputDialog("New Session");
        dialog.setTitle("New Session");
        dialog.setHeaderText("Create a new search session");
        dialog.setContentText("Session name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                currentSessionId = sessionRepository.createSession(name, null);
                sessionNameLabel.setText("Session: " + name);
                setStatus("Created new session: " + name);
                logger.info("Created new session: {}", name);

                // Clear results
                if (resultsTableController != null) {
                    resultsTableController.clearResults();
                }
                updateResultsCount(0);

            } catch (SQLException e) {
                logger.error("Failed to create session", e);
                showError("Session Error", "Failed to create session: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onOpenSession() {
        try {
            List<SessionRepository.Session> sessions = sessionRepository.findAll(50);

            if (sessions.isEmpty()) {
                showError("No Sessions", "No saved sessions found.");
                return;
            }

            ChoiceDialog<SessionRepository.Session> dialog = new ChoiceDialog<>(sessions.get(0), sessions);
            dialog.setTitle("Open Session");
            dialog.setHeaderText("Select a session to resume");
            dialog.setContentText("Session:");

            dialog.showAndWait().ifPresent(session -> {
                currentSessionId = session.getId();
                sessionNameLabel.setText("Session: " + session.getName());
                setStatus("Switched to session: " + session.getName());
                logger.info("Switched to session: {}", session.getName());

                // Update last accessed time
                try {
                    sessionRepository.updateLastAccessed(session.getId());
                } catch (SQLException e) {
                    logger.error("Failed to update session access time", e);
                }

                // Clear results for new session
                if (resultsTableController != null) {
                    resultsTableController.clearResults();
                }
                updateResultsCount(0);
            });

        } catch (SQLException e) {
            logger.error("Failed to load sessions", e);
            showError("Error", "Failed to load sessions: " + e.getMessage());
        }
    }

    @FXML
    private void onExportResults() {
        if (resultsTableController == null || totalResults == 0) {
            setStatus("No results to export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Results");
        fileChooser.setInitialFileName("search_results.csv");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(resultsCountLabel.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Title,Authors,Year,Source,URL\n");
                for (var result : resultsTableController.getResults()) {
                    String title = result.getTitle() != null ? result.getTitle().replace(",", ";") : "";
                    String authors = result.getAuthors() != null ? result.getAuthors().replace(",", ";") : "";
                    String url = result.getUrl() != null ? result.getUrl() : "";
                    String source = result.getSource() != null ? result.getSource().getDisplayName() : "";
                    writer.write(String.format("\"%s\",\"%s\",\"\",\"%s\",\"%s\"\n", title, authors, source, url));
                }
                setStatus("Exported " + totalResults + " results to " + file.getName());
                logger.info("Exported results to {}", file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Failed to export results", e);
                showError("Export Error", "Failed to export results: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onNewSearch() {
        if (searchPanelController != null) {
            searchPanelController.focusSearchField();
        }
    }

    @FXML
    private void onAdvancedSearch() {
        if (searchPanelController != null) {
            searchPanelController.showAdvancedSearch();
        }
    }

    @FXML
    private void onClearResults() {
        if (resultsTableController != null) {
            resultsTableController.clearResults();
            updateResultsCount(0);
            setStatus("Results cleared");
        }
    }

    @FXML
    private void onShowHistory() {
        try {
            List<SearchHistoryRepository.SearchHistoryEntry> history = searchHistoryRepository.findAll(100);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Search History");
            dialog.setHeaderText("Your recent searches (" + history.size() + " entries)");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            TableView<SearchHistoryRepository.SearchHistoryEntry> table = new TableView<>();
            table.setPrefWidth(600);
            table.setPrefHeight(400);

            TableColumn<SearchHistoryRepository.SearchHistoryEntry, String> queryCol = new TableColumn<>("Query");
            queryCol.setCellValueFactory(new PropertyValueFactory<>("queryText"));
            queryCol.setPrefWidth(300);

            TableColumn<SearchHistoryRepository.SearchHistoryEntry, Integer> countCol = new TableColumn<>("Results");
            countCol.setCellValueFactory(new PropertyValueFactory<>("resultCount"));
            countCol.setPrefWidth(80);

            TableColumn<SearchHistoryRepository.SearchHistoryEntry, String> timeCol = new TableColumn<>("Time");
            timeCol.setCellValueFactory(cellData -> {
                var timestamp = cellData.getValue().getTimestamp();
                String formatted = timestamp != null ?
                        timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
                return new javafx.beans.property.SimpleStringProperty(formatted);
            });
            timeCol.setPrefWidth(120);

            TableColumn<SearchHistoryRepository.SearchHistoryEntry, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setPrefWidth(100);
            actionsCol.setCellFactory(col -> new TableCell<>() {
                private final Button rerunBtn = new Button("Re-run");
                private final Button deleteBtn = new Button("Delete");
                private final HBox buttons = new HBox(5, rerunBtn, deleteBtn);

                {
                    rerunBtn.setOnAction(e -> {
                        var entry = getTableView().getItems().get(getIndex());
                        if (searchPanelController != null && entry.getQueryText() != null) {
                            searchPanelController.setSearchQuery(entry.getQueryText());
                            dialog.close();
                            searchPanelController.executeSearch();
                        }
                    });

                    deleteBtn.setOnAction(e -> {
                        var entry = getTableView().getItems().get(getIndex());
                        try {
                            searchHistoryRepository.deleteById(entry.getId());
                            getTableView().getItems().remove(entry);
                            setStatus("Deleted history entry");
                        } catch (Exception ex) {
                            logger.error("Failed to delete history entry", ex);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : buttons);
                }
            });

            table.getColumns().addAll(queryCol, countCol, timeCol, actionsCol);
            table.getItems().addAll(history);

            VBox content = new VBox(10, table);
            content.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();

        } catch (Exception e) {
            logger.error("Failed to load search history", e);
            showError("Error", "Failed to load search history: " + e.getMessage());
        }
    }

    @FXML
    private void onBookmarkSelected() {
        if (resultsTableController != null) {
            resultsTableController.bookmarkSelected();
        }
    }

    @FXML
    private void onManageBookmarks() {
        try {
            List<Bookmark> bookmarks = bookmarkRepository.findAll();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Bookmark Manager");
            dialog.setHeaderText("Manage your bookmarks (" + bookmarks.size() + " total)");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            TableView<Bookmark> table = new TableView<>();
            table.setPrefWidth(750);
            table.setPrefHeight(400);

            TableColumn<Bookmark, String> titleCol = new TableColumn<>("Title");
            titleCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle()));
            titleCol.setPrefWidth(280);

            TableColumn<Bookmark, String> urlCol = new TableColumn<>("URL");
            urlCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUrl()));
            urlCol.setPrefWidth(200);

            TableColumn<Bookmark, String> notesCol = new TableColumn<>("Notes");
            notesCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(
                            cellData.getValue().getNotes() != null ? cellData.getValue().getNotes() : ""));
            notesCol.setPrefWidth(120);

            TableColumn<Bookmark, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setPrefWidth(140);
            actionsCol.setCellFactory(col -> new TableCell<Bookmark, Void>() {
                private final Button openBtn = new Button("Open");
                private final Button deleteBtn = new Button("Delete");
                private final HBox buttons = new HBox(5, openBtn, deleteBtn);

                {
                    openBtn.setStyle("-fx-font-size: 11px;");
                    deleteBtn.setStyle("-fx-font-size: 11px;");

                    openBtn.setOnAction(e -> {
                        Bookmark b = getTableView().getItems().get(getIndex());
                        if (b.getUrl() != null) {
                            try {
                                Desktop.getDesktop().browse(new URI(b.getUrl()));
                            } catch (Exception ex) {
                                logger.error("Failed to open URL", ex);
                                showError("Error", "Failed to open URL: " + ex.getMessage());
                            }
                        }
                    });

                    deleteBtn.setOnAction(e -> {
                        Bookmark b = getTableView().getItems().get(getIndex());
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Delete Bookmark");
                        confirm.setHeaderText("Delete bookmark?");
                        confirm.setContentText("Are you sure you want to delete: " + b.getTitle());
                        confirm.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                try {
                                    bookmarkRepository.delete(b.getId());
                                    getTableView().getItems().remove(b);
                                    setStatus("Deleted bookmark: " + b.getTitle());
                                } catch (Exception ex) {
                                    logger.error("Failed to delete bookmark", ex);
                                    showError("Error", "Failed to delete: " + ex.getMessage());
                                }
                            }
                        });
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : buttons);
                }
            });

            table.getColumns().add(titleCol);
            table.getColumns().add(urlCol);
            table.getColumns().add(notesCol);
            table.getColumns().add(actionsCol);
            table.getItems().addAll(bookmarks);

            // Search/filter field
            TextField filterField = new TextField();
            filterField.setPromptText("Filter by tag...");
            filterField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.isEmpty()) {
                    try {
                        table.getItems().setAll(bookmarkRepository.findAll());
                    } catch (Exception e) {
                        logger.error("Failed to reload bookmarks", e);
                    }
                } else {
                    try {
                        table.getItems().setAll(bookmarkRepository.findByTag(newVal));
                    } catch (Exception e) {
                        logger.error("Failed to filter bookmarks", e);
                    }
                }
            });

            Label infoLabel = new Label("Double-click a bookmark to open it in browser");
            infoLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");

            table.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Bookmark selected = table.getSelectionModel().getSelectedItem();
                    if (selected != null && selected.getUrl() != null) {
                        try {
                            Desktop.getDesktop().browse(new URI(selected.getUrl()));
                        } catch (Exception ex) {
                            logger.error("Failed to open URL", ex);
                        }
                    }
                }
            });

            VBox content = new VBox(10, filterField, table, infoLabel);
            content.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(780);
            dialog.showAndWait();

            // Refresh results table bookmark state
            if (resultsTableController != null) {
                resultsTableController.refreshBookmarkCache();
                Platform.runLater(() -> resultsTableController.getResultsTable().refresh());
            }
        } catch (Exception e) {
            logger.error("Failed to open bookmark manager", e);
            showError("Error", "Failed to load bookmarks: " + e.getMessage());
        }
    }

    @FXML
    private void onDownloadManager() {
        try {
            List<DownloadRepository.Download> downloads = downloadRepository.findAll();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Download Manager");
            dialog.setHeaderText("Manage your downloads (" + downloads.size() + " total)");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            TableView<DownloadRepository.Download> table = new TableView<>();
            table.setPrefWidth(750);
            table.setPrefHeight(400);

            TableColumn<DownloadRepository.Download, String> urlCol = new TableColumn<>("URL");
            urlCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(
                            cellData.getValue().getUrl() != null ? cellData.getValue().getUrl() : ""));
            urlCol.setPrefWidth(280);

            TableColumn<DownloadRepository.Download, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(
                            cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().name() : ""));
            statusCol.setPrefWidth(100);

            TableColumn<DownloadRepository.Download, String> progressCol = new TableColumn<>("Progress");
            progressCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(
                            String.format("%.0f%%", cellData.getValue().getProgress() * 100)));
            progressCol.setPrefWidth(80);

            TableColumn<DownloadRepository.Download, String> pathCol = new TableColumn<>("Destination");
            pathCol.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(
                            cellData.getValue().getDestinationPath() != null ? cellData.getValue().getDestinationPath() : ""));
            pathCol.setPrefWidth(180);

            TableColumn<DownloadRepository.Download, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setPrefWidth(100);
            actionsCol.setCellFactory(col -> new TableCell<DownloadRepository.Download, Void>() {
                private final Button deleteBtn = new Button("Delete");
                private final Button openBtn = new Button("Open");
                private final HBox buttons = new HBox(5, openBtn, deleteBtn);

                {
                    openBtn.setStyle("-fx-font-size: 11px;");
                    deleteBtn.setStyle("-fx-font-size: 11px;");

                    openBtn.setOnAction(e -> {
                        DownloadRepository.Download d = getTableView().getItems().get(getIndex());
                        if (d.getDestinationPath() != null) {
                            try {
                                File file = new File(d.getDestinationPath());
                                if (file.exists()) {
                                    Desktop.getDesktop().open(file.getParentFile());
                                }
                            } catch (Exception ex) {
                                logger.error("Failed to open folder", ex);
                            }
                        }
                    });

                    deleteBtn.setOnAction(e -> {
                        DownloadRepository.Download d = getTableView().getItems().get(getIndex());
                        try {
                            downloadRepository.delete(d.getId());
                            getTableView().getItems().remove(d);
                            setStatus("Deleted download record");
                        } catch (Exception ex) {
                            logger.error("Failed to delete download", ex);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : buttons);
                }
            });

            table.getColumns().add(urlCol);
            table.getColumns().add(statusCol);
            table.getColumns().add(progressCol);
            table.getColumns().add(pathCol);
            table.getColumns().add(actionsCol);
            table.getItems().addAll(downloads);

            if (downloads.isEmpty()) {
                table.setPlaceholder(new Label("No downloads yet. Download PDFs from search results."));
            }

            VBox content = new VBox(10, table);
            content.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(780);
            dialog.showAndWait();

        } catch (Exception e) {
            logger.error("Failed to open download manager", e);
            showError("Error", "Failed to load downloads: " + e.getMessage());
        }
    }

    @FXML
    private void onIndexManager() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Index Manager");
        alert.setHeaderText("Local Search Index");

        StringBuilder content = new StringBuilder();
        content.append("The local search index allows offline searching of previously downloaded papers.\n\n");
        content.append("CLI Commands:\n");
        content.append("  libsearch index build    - Build/rebuild the search index\n");
        content.append("  libsearch index status   - Show index status\n");
        content.append("  libsearch index clear    - Clear the index\n\n");
        content.append("Enable 'Offline Mode' in the search panel to search the local index.");

        alert.setContentText(content.toString());
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    @FXML
    private void onSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Application Settings");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Max results setting
        HBox maxResultsBox = new HBox(10);
        maxResultsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label maxResultsLabel = new Label("Default max results:");
        ComboBox<String> maxResultsCombo = new ComboBox<>();
        maxResultsCombo.getItems().addAll("10", "25", "50", "100", "200");
        maxResultsCombo.setValue("50");
        maxResultsBox.getChildren().addAll(maxResultsLabel, maxResultsCombo);

        // Download directory
        HBox downloadDirBox = new HBox(10);
        downloadDirBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label downloadDirLabel = new Label("Download directory:");
        TextField downloadDirField = new TextField(System.getProperty("user.home") + "/Downloads/LibSearch");
        downloadDirField.setPrefWidth(300);
        downloadDirBox.getChildren().addAll(downloadDirLabel, downloadDirField);

        // Auto-save results
        CheckBox autoSaveCheck = new CheckBox("Auto-save search results to database");
        autoSaveCheck.setSelected(true);

        content.getChildren().addAll(maxResultsBox, downloadDirBox, autoSaveCheck);
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                setStatus("Settings saved");
            }
        });
    }

    @FXML
    private void onUserGuide() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Guide");
        alert.setHeaderText("LibSearch User Guide");

        String guide = """
            LibSearch - Academic Federated Search Application

            SEARCH SYNTAX:
              Simple search:     machine learning
              Phrase search:     "deep learning"
              Author search:     author:Smith OR author:"John Smith"
              Year filter:       year:2023 OR year:2020-2024
              Boolean operators: neural AND networks, AI OR ML

            KEYBOARD SHORTCUTS:
              Ctrl+N  - New search
              Ctrl+F  - Focus search field
              Ctrl+B  - Bookmark selected
              Ctrl+E  - Export results

            CLI COMMANDS:
              libsearch search "query"     - Search from command line
              libsearch bookmark list      - List all bookmarks
              libsearch bookmark find tag  - Find bookmarks by tag
              libsearch session list       - List sessions
              libsearch repl               - Start interactive mode

            For more help, visit the project documentation.
            """;

        TextArea textArea = new TextArea(guide);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        textArea.setPrefWidth(500);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(550);
        alert.showAndWait();
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About LibSearch");
        alert.setHeaderText("LibSearch - Academic Federated Search Application");
        alert.setContentText("Version: 1.0.0\n" +
                "Java: 21 (Corretto)\n" +
                "JavaFX: 21.0.6\n\n" +
                "LibSearch provides federated search across multiple academic databases\n" +
                "including arXiv, PubMed, CrossRef, Semantic Scholar, and more.");
        alert.showAndWait();
    }

    private void onSortChanged() {
        if (resultsTableController != null) {
            String sort = sortComboBox.getValue();
            resultsTableController.setSortMode(sort);
            setStatus("Sorted by: " + sort);
        }
    }

    private void onFilterChanged() {
        if (resultsTableController != null) {
            String filter = typeFilterComboBox.getValue();
            resultsTableController.setTypeFilter(filter);
            setStatus("Filtered by type: " + filter);
        }
    }

    /**
     * Show error dialog.
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Set child controllers (called by parent loader).
     */
    public void setSearchController(SearchController searchController) {
        this.searchPanelController = searchController;
        searchController.setMainController(this);
    }

    public void setResultsController(ResultsController resultsController) {
        this.resultsTableController = resultsController;
        resultsController.setMainController(this);
    }

    public SearchController getSearchController() {
        return searchPanelController;
    }

    public ResultsController getResultsController() {
        return resultsTableController;
    }

    /**
     * Source status data class.
     */
    public static class SourceStatus {
        private final SourceType type;
        private final boolean available;

        public SourceStatus(SourceType type, boolean available) {
            this.type = type;
            this.available = available;
        }

        public SourceType getType() {
            return type;
        }

        public boolean isAvailable() {
            return available;
        }
    }

    /**
     * Custom cell for source status list.
     */
    private static class SourceStatusCell extends ListCell<SourceStatus> {
        @Override
        protected void updateItem(SourceStatus item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                // Status indicator
                Label indicator = new Label("●");
                indicator.setStyle("-fx-text-fill: " +
                    (item.isAvailable() ? "#4CAF50" : "#F44336") + ";");

                // Source name
                Label name = new Label(item.getType().getDisplayName());

                hbox.getChildren().addAll(indicator, name);
                setGraphic(hbox);
            }
        }
    }
}
