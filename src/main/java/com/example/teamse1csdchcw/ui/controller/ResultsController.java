package com.example.teamse1csdchcw.ui.controller;

import com.example.teamse1csdchcw.domain.export.ExportFormat;
import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.AccessLevel;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.user.Bookmark;
import com.example.teamse1csdchcw.repository.BookmarkRepository;
import com.example.teamse1csdchcw.service.download.DownloadService;
import com.example.teamse1csdchcw.service.export.CitationExportService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the results table.
 * Displays search results and handles user interactions.
 */
public class ResultsController {
    private static final Logger logger = LoggerFactory.getLogger(ResultsController.class);

    // FXML injected components
    @FXML private TableView<SearchResult> resultsTable;
    @FXML private TableColumn<SearchResult, CheckBox> selectColumn;
    @FXML private TableColumn<SearchResult, String> titleColumn;
    @FXML private TableColumn<SearchResult, String> authorsColumn;
    @FXML private TableColumn<SearchResult, String> yearColumn;
    @FXML private TableColumn<SearchResult, String> sourceColumn;
    @FXML private TableColumn<SearchResult, String> accessColumn;
    @FXML private TableColumn<SearchResult, Integer> citationsColumn;
    @FXML private TableColumn<SearchResult, Void> actionsColumn;
    @FXML private ContextMenu rowContextMenu;

    // Data
    private final ObservableList<SearchResult> results = FXCollections.observableArrayList();
    private final ObservableList<SearchResult> filteredResults = FXCollections.observableArrayList();

    // Parent controller
    private MainController mainController;

    // Services
    private final BookmarkRepository bookmarkRepository;
    private final DownloadService downloadService;
    private final CitationExportService citationService;

    // State
    private String currentSortMode = "Relevance";
    private String currentTypeFilter = "All Types";

    public ResultsController() {
        this.bookmarkRepository = new BookmarkRepository();
        this.downloadService = new DownloadService();
        this.citationService = new CitationExportService();
    }

    @FXML
    public void initialize() {
        logger.info("Initializing ResultsController");

        // Set up table columns
        setupColumns();

        // Set up table properties
        resultsTable.setItems(filteredResults);
        resultsTable.setRowFactory(tv -> {
            TableRow<SearchResult> row = new TableRow<>();

            // Double-click to open URL
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openUrl(row.getItem());
                }
            });

            // Context menu
            row.setContextMenu(rowContextMenu);

            return row;
        });
    }

    /**
     * Set up table columns with cell value factories and custom cells.
     */
    private void setupColumns() {
        // Selection column
        selectColumn.setCellFactory(col -> new CheckBoxTableCell());

        // Title column
        titleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTitle()));
        titleColumn.setCellFactory(col -> new TitleCell());

        // Authors column
        authorsColumn.setCellValueFactory(cellData -> {
            String authors = cellData.getValue().getAuthors();
            if (authors != null && authors.length() > 50) {
                authors = authors.substring(0, 47) + "...";
            }
            return new SimpleStringProperty(authors);
        });

        // Year column
        yearColumn.setCellValueFactory(cellData -> {
            SearchResult result = cellData.getValue();
            if (result instanceof AcademicPaper paper && paper.getPublicationDate() != null) {
                return new SimpleStringProperty(String.valueOf(paper.getPublicationDate().getYear()));
            }
            return new SimpleStringProperty("");
        });
        yearColumn.setStyle("-fx-alignment: CENTER;");

        // Source column
        sourceColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getSource().getDisplayName()));
        sourceColumn.setCellFactory(col -> new SourceCell());

        // Access level column
        accessColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getAccessLevel().name()));
        accessColumn.setCellFactory(col -> new AccessLevelCell());

        // Citations column
        citationsColumn.setCellValueFactory(cellData -> {
            SearchResult result = cellData.getValue();
            if (result instanceof AcademicPaper paper) {
                return new javafx.beans.property.SimpleObjectProperty<>(paper.getCitationCount());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(0);
        });
        citationsColumn.setStyle("-fx-alignment: CENTER;");

        // Actions column
        actionsColumn.setCellFactory(col -> new ActionsCell());
    }

    /**
     * Set search results.
     */
    public void setResults(List<SearchResult> newResults) {
        results.setAll(newResults);
        applyFiltersAndSort();
    }

    /**
     * Clear all results.
     */
    public void clearResults() {
        results.clear();
        filteredResults.clear();
    }

    /**
     * Set sort mode.
     */
    public void setSortMode(String sortMode) {
        this.currentSortMode = sortMode;
        applyFiltersAndSort();
    }

    /**
     * Set type filter.
     */
    public void setTypeFilter(String typeFilter) {
        this.currentTypeFilter = typeFilter;
        applyFiltersAndSort();
    }

    /**
     * Apply filters and sorting to results.
     */
    private void applyFiltersAndSort() {
        List<SearchResult> filtered = results.stream()
                .filter(this::matchesTypeFilter)
                .sorted(getComparator())
                .collect(Collectors.toList());

        filteredResults.setAll(filtered);
    }

    /**
     * Check if result matches current type filter.
     */
    private boolean matchesTypeFilter(SearchResult result) {
        if ("All Types".equals(currentTypeFilter)) {
            return true;
        }

        if (!(result instanceof AcademicPaper paper)) {
            return true;
        }

        String filter = currentTypeFilter.toLowerCase();

        switch (filter) {
            case "article":
                return matchesArticle(paper);
            case "conference":
                return matchesConference(paper);
            case "thesis":
                return matchesThesis(paper);
            case "book":
                return matchesBook(paper);
            default:
                return true;
        }
    }

    private boolean matchesArticle(AcademicPaper paper) {
        String venue = (paper.getVenue() != null ? paper.getVenue() : "").toLowerCase();
        String journal = (paper.getJournal() != null ? paper.getJournal() : "").toLowerCase();
        return journal.contains("journal") || journal.contains("transactions") ||
               venue.contains("journal") || venue.contains("article") || venue.contains("letter");
    }

    private boolean matchesConference(AcademicPaper paper) {
        String venue = (paper.getVenue() != null ? paper.getVenue() : "").toLowerCase();
        return venue.contains("conference") || venue.contains("proceedings") ||
               venue.contains("symposium") || venue.contains("workshop");
    }

    private boolean matchesThesis(AcademicPaper paper) {
        String title = (paper.getTitle() != null ? paper.getTitle() : "").toLowerCase();
        String venue = (paper.getVenue() != null ? paper.getVenue() : "").toLowerCase();
        return title.contains("thesis") || title.contains("dissertation") ||
               venue.contains("thesis") || venue.contains("dissertation");
    }

    private boolean matchesBook(AcademicPaper paper) {
        String venue = (paper.getVenue() != null ? paper.getVenue() : "").toLowerCase();
        String journal = (paper.getJournal() != null ? paper.getJournal() : "").toLowerCase();
        return venue.contains("book") || journal.contains("book") ||
               venue.contains("chapter") || venue.contains("textbook");
    }

    /**
     * Get comparator for current sort mode.
     */
    private Comparator<SearchResult> getComparator() {
        if ("Citation Count".equals(currentSortMode)) {
            return Comparator.comparingInt((SearchResult r) -> {
                if (r instanceof AcademicPaper p) return p.getCitationCount();
                return 0;
            }).reversed();
        } else if ("Publication Date".equals(currentSortMode)) {
            return Comparator.comparing((SearchResult r) -> {
                if (r instanceof AcademicPaper p && p.getPublicationDate() != null) {
                    return p.getPublicationDate();
                }
                return java.time.LocalDate.MIN;
            }).reversed();
        } else if ("Title".equals(currentSortMode)) {
            return Comparator.comparing(SearchResult::getTitle);
        } else {
            // Default: sort by relevance
            return Comparator.comparingDouble(SearchResult::getRelevance).reversed();
        }
    }

    /**
     * Bookmark selected results.
     */
    public void bookmarkSelected() {
        List<SearchResult> selected = resultsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            mainController.setStatus("No results selected");
            return;
        }

        int bookmarked = 0;
        for (SearchResult result : selected) {
            try {
                if (!bookmarkRepository.exists(result.getId())) {
                    Bookmark bookmark = new Bookmark();
                    bookmark.setResultId(result.getId());
                    bookmark.setTitle(result.getTitle());
                    bookmark.setUrl(result.getUrl());
                    bookmarkRepository.save(bookmark);
                    bookmarked++;
                }
            } catch (Exception e) {
                logger.error("Failed to bookmark result", e);
            }
        }

        mainController.setStatus("Bookmarked " + bookmarked + " result(s)");
    }

    /**
     * Set parent controller.
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // Context menu actions
    @FXML
    private void onOpenUrl() {
        SearchResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openUrl(selected);
        }
    }

    @FXML
    private void onDownloadPdf() {
        SearchResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected instanceof AcademicPaper paper && paper.getPdfUrl() != null) {
            String homeDir = System.getProperty("user.home");
            String downloadDir = homeDir + "/Downloads/LibSearch";

            String downloadId = downloadService.queueDownload(
                selected.getId(),
                paper.getPdfUrl(),
                downloadDir
            );

            if (downloadId != null) {
                mainController.setStatus("Download queued: " + paper.getTitle());
                logger.info("Queued PDF download: {}", paper.getPdfUrl());
            } else {
                mainController.setStatus("Failed to queue download");
            }
        } else {
            mainController.setStatus("No PDF available");
        }
    }

    @FXML
    private void onBookmark() {
        SearchResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                if (bookmarkRepository.exists(selected.getId())) {
                    mainController.setStatus("Already bookmarked");
                } else {
                    Bookmark bookmark = new Bookmark();
                    bookmark.setResultId(selected.getId());
                    bookmark.setTitle(selected.getTitle());
                    bookmark.setUrl(selected.getUrl());
                    bookmarkRepository.save(bookmark);
                    mainController.setStatus("Added to bookmarks");
                }
            } catch (Exception e) {
                logger.error("Failed to bookmark", e);
                mainController.setStatus("Failed to bookmark");
            }
        }
    }

    @FXML
    private void onExportCitation() {
        SearchResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showExportDialog(selected);
        }
    }

    private void showExportDialog(SearchResult result) {
        javafx.scene.control.ChoiceDialog<ExportFormat> dialog =
            new javafx.scene.control.ChoiceDialog<>(ExportFormat.BIBTEX,
                ExportFormat.BIBTEX, ExportFormat.RIS, ExportFormat.ENDNOTE);

        dialog.setTitle("Export Citation");
        dialog.setHeaderText("Choose export format");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(format -> {
            try {
                String citation = citationService.exportSingle(result, format);

                TextArea textArea = new TextArea(citation);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefRowCount(20);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Citation");
                alert.setHeaderText(format + " Format");
                alert.getDialogPane().setContent(textArea);
                alert.getDialogPane().setPrefWidth(600);

                ButtonType copyButton = new ButtonType("Copy to Clipboard");
                ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(copyButton, closeButton);

                alert.showAndWait().ifPresent(button -> {
                    if (button == copyButton) {
                        ClipboardContent content = new ClipboardContent();
                        content.putString(citation);
                        Clipboard.getSystemClipboard().setContent(content);
                        mainController.setStatus("Citation copied to clipboard");
                    }
                });

            } catch (Exception e) {
                logger.error("Failed to export citation", e);
                mainController.setStatus("Failed to export citation");
            }
        });
    }

    @FXML
    private void onViewDetails() {
        SearchResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showDetailsDialog(selected);
        }
    }

    @FXML
    private void onCopyDoi() {
        SearchResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected instanceof AcademicPaper paper && paper.getDoi() != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(paper.getDoi());
            Clipboard.getSystemClipboard().setContent(content);
            mainController.setStatus("Copied DOI to clipboard");
        } else {
            mainController.setStatus("No DOI available");
        }
    }

    /**
     * Open URL in browser.
     */
    private void openUrl(SearchResult result) {
        if (result.getUrl() == null) {
            mainController.setStatus("No URL available");
            return;
        }

        try {
            Desktop.getDesktop().browse(new URI(result.getUrl()));
            mainController.setStatus("Opened: " + result.getUrl());
        } catch (Exception e) {
            logger.error("Failed to open URL", e);
            mainController.setStatus("Failed to open URL");
        }
    }

    /**
     * Show details dialog for a result.
     */
    private void showDetailsDialog(SearchResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Paper Details");
        alert.setHeaderText(result.getTitle());

        StringBuilder content = new StringBuilder();
        content.append("Authors: ").append(result.getAuthors()).append("\n\n");

        if (result instanceof AcademicPaper paper) {
            if (paper.getDoi() != null) {
                content.append("DOI: ").append(paper.getDoi()).append("\n");
            }
            if (paper.getArxivId() != null) {
                content.append("arXiv ID: ").append(paper.getArxivId()).append("\n");
            }
            if (paper.getPublicationDate() != null) {
                content.append("Publication Date: ").append(paper.getPublicationDate()).append("\n");
            }
            if (paper.getJournal() != null) {
                content.append("Journal: ").append(paper.getJournal()).append("\n");
            }
            content.append("Citations: ").append(paper.getCitationCount()).append("\n");
            content.append("\nAbstract:\n").append(paper.getAbstractText() != null ? paper.getAbstractText() : "N/A");
        }

        content.append("\n\nSource: ").append(result.getSource().getDisplayName());
        content.append("\nAccess: ").append(result.getAccessLevel());
        content.append("\nURL: ").append(result.getUrl());

        alert.setContentText(content.toString());
        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }

    // Custom table cells

    private static class CheckBoxTableCell extends TableCell<SearchResult, CheckBox> {
        private final CheckBox checkBox = new CheckBox();

        @Override
        protected void updateItem(CheckBox item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(checkBox);
            }
        }
    }

    private static class TitleCell extends TableCell<SearchResult, String> {
        @Override
        protected void updateItem(String title, boolean empty) {
            super.updateItem(title, empty);
            if (empty || title == null) {
                setText(null);
                setStyle("");
            } else {
                setText(title);
                setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
            }
        }
    }

    private static class SourceCell extends TableCell<SearchResult, String> {
        @Override
        protected void updateItem(String source, boolean empty) {
            super.updateItem(source, empty);
            if (empty || source == null) {
                setText(null);
                setGraphic(null);
            } else {
                Label label = new Label(source);
                label.getStyleClass().add("source-badge");
                label.getStyleClass().add("source-" + source.toLowerCase().replace(" ", "-"));
                setGraphic(label);
            }
        }
    }

    private static class AccessLevelCell extends TableCell<SearchResult, String> {
        @Override
        protected void updateItem(String access, boolean empty) {
            super.updateItem(access, empty);
            if (empty || access == null) {
                setText(null);
                setGraphic(null);
            } else {
                Label label = new Label(access.replace("_", " "));
                label.getStyleClass().add("access-" + access.toLowerCase());
                setGraphic(label);
            }
        }
    }

    private class ActionsCell extends TableCell<SearchResult, Void> {
        private final Button viewButton = new Button("View");
        private final Button downloadButton = new Button("PDF");
        private final HBox buttons = new HBox(5, viewButton, downloadButton);

        public ActionsCell() {
            buttons.setAlignment(Pos.CENTER);
            viewButton.getStyleClass().add("action-button");
            downloadButton.getStyleClass().add("action-button");

            viewButton.setOnAction(e -> {
                SearchResult result = getTableView().getItems().get(getIndex());
                openUrl(result);
            });

            downloadButton.setOnAction(e -> {
                SearchResult result = getTableView().getItems().get(getIndex());
                if (result instanceof AcademicPaper paper && paper.getPdfUrl() != null) {
                    String homeDir = System.getProperty("user.home");
                    String downloadDir = homeDir + "/Downloads/LibSearch";
                    downloadService.queueDownload(result.getId(), paper.getPdfUrl(), downloadDir);
                    mainController.setStatus("Download queued");
                } else {
                    mainController.setStatus("No PDF available");
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(buttons);
            }
        }
    }
}
