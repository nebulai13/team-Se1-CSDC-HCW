package com.example.teamse1csdchcw.ui.controller;

import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.repository.SessionRepository;
import com.example.teamse1csdchcw.service.connector.ConnectorFactory;
import com.example.teamse1csdchcw.service.connector.SourceConnector;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // Controllers (injected by FXMLLoader)
    private SearchController searchController;
    private ResultsController resultsController;

    // Services
    private final SessionRepository sessionRepository;
    private final ConnectorFactory connectorFactory;

    // State
    private String currentSessionId;
    private int totalResults = 0;

    public MainController() {
        this.sessionRepository = new SessionRepository();
        this.connectorFactory = ConnectorFactory.getInstance();
    }

    @FXML
    public void initialize() {
        logger.info("Initializing MainController");

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
                if (resultsController != null) {
                    resultsController.clearResults();
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
        // TODO: Implement session selection dialog
        setStatus("Open session not yet implemented");
    }

    @FXML
    private void onExportResults() {
        // TODO: Implement export dialog
        setStatus("Export not yet implemented");
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onNewSearch() {
        if (searchController != null) {
            searchController.focusSearchField();
        }
    }

    @FXML
    private void onAdvancedSearch() {
        if (searchController != null) {
            searchController.showAdvancedSearch();
        }
    }

    @FXML
    private void onClearResults() {
        if (resultsController != null) {
            resultsController.clearResults();
            updateResultsCount(0);
            setStatus("Results cleared");
        }
    }

    @FXML
    private void onShowHistory() {
        setStatus("History not yet implemented");
    }

    @FXML
    private void onBookmarkSelected() {
        if (resultsController != null) {
            resultsController.bookmarkSelected();
        }
    }

    @FXML
    private void onManageBookmarks() {
        setStatus("Bookmark manager not yet implemented");
    }

    @FXML
    private void onDownloadManager() {
        setStatus("Download manager not yet implemented");
    }

    @FXML
    private void onIndexManager() {
        setStatus("Index manager not yet implemented");
    }

    @FXML
    private void onSettings() {
        setStatus("Settings not yet implemented");
    }

    @FXML
    private void onUserGuide() {
        setStatus("User guide not yet implemented");
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
        if (resultsController != null) {
            String sort = sortComboBox.getValue();
            resultsController.setSortMode(sort);
            setStatus("Sorted by: " + sort);
        }
    }

    private void onFilterChanged() {
        if (resultsController != null) {
            String filter = typeFilterComboBox.getValue();
            resultsController.setTypeFilter(filter);
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
        this.searchController = searchController;
        searchController.setMainController(this);
    }

    public void setResultsController(ResultsController resultsController) {
        this.resultsController = resultsController;
        resultsController.setMainController(this);
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
