package com.example.teamse1csdchcw.ui.controller;

import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.exception.SearchException;
import com.example.teamse1csdchcw.repository.SearchHistoryRepository;
import com.example.teamse1csdchcw.repository.SearchResultRepository;
import com.example.teamse1csdchcw.service.search.FederatedSearchService;
import com.example.teamse1csdchcw.service.search.QueryParserService;
import com.example.teamse1csdchcw.service.index.LocalSearchService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for the search panel.
 * Handles user input and executes federated searches.
 */
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    // FXML injected components
    @FXML private TextField searchTextField;
    @FXML private Button searchButton;
    @FXML private CheckBox arxivCheckBox;
    @FXML private CheckBox pubmedCheckBox;
    @FXML private CheckBox crossrefCheckBox;
    @FXML private CheckBox scholarCheckBox;
    @FXML private TextField yearFromField;
    @FXML private TextField yearToField;
    @FXML private ComboBox<String> maxResultsComboBox;
    @FXML private CheckBox offlineModeCheckBox;
    @FXML private TitledPane tipsPane;

    // Services
    private final QueryParserService queryParser;
    private final FederatedSearchService searchService;
    private final LocalSearchService localSearchService;
    private final SearchHistoryRepository historyRepository;
    private final SearchResultRepository resultRepository;

    // Parent controller
    private MainController mainController;
    private ResultsController resultsController;

    // State
    private boolean searchInProgress = false;

    public SearchController() {
        this.queryParser = new QueryParserService();
        this.searchService = new FederatedSearchService();
        this.localSearchService = new LocalSearchService(this.searchService.getIndexService());
        this.historyRepository = new SearchHistoryRepository();
        this.resultRepository = new SearchResultRepository();
    }

    @FXML
    public void initialize() {
        logger.info("Initializing SearchController");

        // Set default values
        maxResultsComboBox.setValue("50");

        // Set up event listeners
        searchTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Enable/disable search button based on input
            searchButton.setDisable(newVal == null || newVal.trim().isEmpty());
        });

        // Initially disable search button
        searchButton.setDisable(true);
    }

    /**
     * Execute search when user presses Enter or clicks Search button.
     */
    @FXML
    private void onSearch() {
        String queryText = searchTextField.getText().trim();

        if (queryText.isEmpty()) {
            return;
        }

        if (searchInProgress) {
            logger.warn("Search already in progress");
            return;
        }

        // Execute search in background thread
        searchInProgress = true;
        searchButton.setDisable(true);
        mainController.setStatus("Searching...");
        mainController.showProgress(true);

        long startTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                // Parse query
                SearchQuery query = queryParser.parse(queryText);
                logger.info("Parsed query: {}", query.getOriginalQuery());

                // Get selected sources
                Set<SourceType> sources = getSelectedSources();

                if (sources.isEmpty()) {
                    Platform.runLater(() -> {
                        showError("No Sources Selected", "Please select at least one search source.");
                        resetSearchUI();
                    });
                    return;
                }

                // Get max results
                int maxResults = Integer.parseInt(maxResultsComboBox.getValue());

                // Apply year filters from UI if not in query
                if (query.getYearFrom() == null && !yearFromField.getText().trim().isEmpty()) {
                    try {
                        query.setYearFrom(Integer.parseInt(yearFromField.getText().trim()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid year from: {}", yearFromField.getText());
                    }
                }

                if (query.getYearTo() == null && !yearToField.getText().trim().isEmpty()) {
                    try {
                        query.setYearTo(Integer.parseInt(yearToField.getText().trim()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid year to: {}", yearToField.getText());
                    }
                }

                // Execute search (offline or federated)
                List<SearchResult> results;
                if (offlineModeCheckBox.isSelected()) {
                    logger.info("Executing local offline search");
                    results = localSearchService.search(query, maxResults);
                    Platform.runLater(() -> {
                        mainController.setStatus("Searching local index...");
                    });
                } else {
                    logger.info("Executing federated online search");
                    searchService.setMaxResultsPerSource(maxResults);
                    results = searchService.search(query, sources);
                }

                long searchTime = System.currentTimeMillis() - startTime;

                // Save to database
                String sessionId = mainController.getCurrentSessionId();
                try {
                    resultRepository.saveAll(results, sessionId);
                    historyRepository.save(query, sessionId, results.size());
                } catch (SQLException e) {
                    logger.error("Failed to save search results", e);
                }

                // Update UI
                final boolean wasOffline = offlineModeCheckBox.isSelected();
                Platform.runLater(() -> {
                    resultsController.setResults(results);
                    mainController.updateResultsCount(results.size());

                    String statusMsg = wasOffline
                            ? "Search completed: " + results.size() + " results from local index"
                            : "Search completed: " + results.size() + " results from " + sources.size() + " sources";
                    mainController.setStatus(statusMsg);
                    mainController.setSearchTime(searchTime);

                    logger.info("Search completed: {} results in {}ms", results.size(), searchTime);
                });

            } catch (SearchException e) {
                logger.error("Search failed", e);
                Platform.runLater(() -> {
                    showError("Search Failed", "Search failed: " + e.getMessage());
                    mainController.setStatus("Search failed");
                });

            } catch (IOException e) {
                logger.error("Local search failed", e);
                Platform.runLater(() -> {
                    showError("Local Search Failed",
                            "Failed to search local index: " + e.getMessage() +
                            "\n\nTry searching online or rebuild the index.");
                    mainController.setStatus("Local search failed");
                });

            } catch (Exception e) {
                logger.error("Unexpected error during search", e);
                Platform.runLater(() -> {
                    showError("Error", "An unexpected error occurred: " + e.getMessage());
                    mainController.setStatus("Error");
                });

            } finally {
                Platform.runLater(this::resetSearchUI);
            }

        }, "SearchThread").start();
    }

    /**
     * Get selected search sources from checkboxes.
     */
    private Set<SourceType> getSelectedSources() {
        Set<SourceType> sources = new HashSet<>();

        if (arxivCheckBox.isSelected()) {
            sources.add(SourceType.ARXIV);
        }
        if (pubmedCheckBox.isSelected()) {
            sources.add(SourceType.PUBMED);
        }
        if (crossrefCheckBox.isSelected()) {
            sources.add(SourceType.CROSSREF);
        }
        if (scholarCheckBox.isSelected()) {
            sources.add(SourceType.SEMANTIC_SCHOLAR);
        }

        return sources;
    }

    /**
     * Reset search UI state after search completes.
     */
    private void resetSearchUI() {
        searchInProgress = false;
        searchButton.setDisable(searchTextField.getText().trim().isEmpty());
        mainController.showProgress(false);
    }

    /**
     * Show advanced search dialog.
     */
    @FXML
    private void onAdvancedSearch() {
        // TODO: Implement advanced search dialog
        mainController.setStatus("Advanced search not yet implemented");
    }

    /**
     * Focus the search text field.
     */
    public void focusSearchField() {
        searchTextField.requestFocus();
    }

    /**
     * Show advanced search dialog (called from main controller).
     */
    public void showAdvancedSearch() {
        onAdvancedSearch();
    }

    /**
     * Set parent controllers.
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setResultsController(ResultsController resultsController) {
        this.resultsController = resultsController;
    }

    /**
     * Show error dialog.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
