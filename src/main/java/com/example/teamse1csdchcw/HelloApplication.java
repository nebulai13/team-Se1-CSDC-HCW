package com.example.teamse1csdchcw;

// -- db init & connection mgmt --
import com.example.teamse1csdchcw.repository.sqlite.DatabaseInitializer;
import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
// -- ui controller imports --
import com.example.teamse1csdchcw.ui.controller.MainController;
import com.example.teamse1csdchcw.ui.controller.ResultsController;
import com.example.teamse1csdchcw.ui.controller.SearchController;
// -- javafx core classes --
import javafx.application.Application;  // -- base class for fx apps --
import javafx.application.Platform;     // -- fx thread & exit utils --
import javafx.fxml.FXMLLoader;          // -- loads .fxml files into scene graph --
import javafx.scene.Parent;             // -- root node type for scenes --
import javafx.scene.Scene;              // -- container for all scene content --
import javafx.stage.Stage;              // -- top-level window (like jframe) --
// -- slf4j logging facade --
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * LibSearch - Academic Federated Search Application
 *
 * Main JavaFX application class.
 */
// -- main javafx app class: extends Application for lifecycle mgmt --
// -- javafx apps must extend Application and override start() --
public class HelloApplication extends Application {
    // -- logger instance: uses slf4j facade w/ logback impl --
    private static final Logger logger = LoggerFactory.getLogger(HelloApplication.class);
    // -- singleton ref for shutdown hook access --
    private static HelloApplication instance;
    // -- ref to search controller for cleanup on exit --
    private SearchController searchController;

    // -- start() is javafx entry point, called after init() --
    // -- stage param is the primary window created by javafx --
    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting LibSearch application");
        // -- store instance for shutdown hook --
        instance = this;

        // Initialize database
        // -- check if sqlite db exists, create if not --
        try {
            if (!SQLiteConnection.getInstance().databaseExists()) {
                logger.info("Initializing database...");
                // -- runs schema.sql to create all tables --
                DatabaseInitializer.initialize();
            } else {
                logger.info("Database already exists");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            // -- show alert dialog & exit on db failure --
            showErrorAndExit("Database Error", "Failed to initialize database: " + e.getMessage());
            return;
        }

        // Load main view
        // -- fxml = xml-based ui markup, separates ui from logic --
        try {
            // Load main FXML (which includes search-panel and results-table via fx:include)
            // -- fxmlloader parses xml & creates javafx node tree --
            FXMLLoader mainLoader = new FXMLLoader(
                    // -- load from resources/ on classpath --
                    getClass().getResource("/com/example/teamse1csdchcw/fxml/main-view.fxml")
            );
            // -- load() parses fxml & instantiates controller --
            Parent root = mainLoader.load();
            // -- get controller instance that was created by fxmlloader --
            MainController mainController = mainLoader.getController();

            // Get search controller from main controller (injected by fx:include)
            // -- fx:include nests fxml files, each w/ own controller --
            this.searchController = mainController.getSearchController();

            // Create scene
            // -- scene = container for scene graph, 1200x800 px --
            Scene scene = new Scene(root, 1200, 800);

            // Set up stage
            // -- stage = top-level window (like swing jframe) --
            stage.setTitle("LibSearch - Academic Federated Search");
            stage.setScene(scene);
            // -- register close handler for cleanup --
            stage.setOnCloseRequest(e -> onApplicationClose());
            // -- display the window --
            stage.show();

            logger.info("LibSearch application started successfully");

        } catch (IOException e) {
            // -- fxml parse error or missing file --
            logger.error("Failed to load UI", e);
            showErrorAndExit("UI Error", "Failed to load user interface: " + e.getMessage());
        }
    }

    /**
     * Handle application close.
     */
    // -- cleanup when user closes window (x button) --
    private void onApplicationClose() {
        logger.info("Closing LibSearch application");

        // Shutdown search service and index
        // -- close lucene index & stop any bg threads --
        if (searchController != null) {
            searchController.shutdown();
        }

        // -- close sqlite connection pool --
        try {
            SQLiteConnection.getInstance().close();
        } catch (Exception e) {
            logger.error("Error closing database", e);
        }

        // -- terminate javafx app thread --
        Platform.exit();
    }

    /**
     * Show error and exit application.
     */
    // -- displays modal error dialog then exits --
    private void showErrorAndExit(String title, String message) {
        // -- javafx alert = modal popup dialog --
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);  // -- no header text, just content --
        alert.setContentText(message);
        alert.showAndWait();  // -- blocks until user dismisses --
        Platform.exit();      // -- terminate app --
    }

    // -- stop() called by javafx when app is shutting down --
    // -- called after start() returns or Platform.exit() --
    @Override
    public void stop() throws Exception {
        logger.info("Stopping LibSearch application");
        super.stop();  // -- call parent impl --

        // Shutdown search service and index
        // -- ensure lucene index is closed properly --
        if (searchController != null) {
            searchController.shutdown();
        }

        // Clean up resources
        // -- release db connections --
        try {
            SQLiteConnection.getInstance().close();
        } catch (Exception e) {
            logger.error("Error closing resources", e);
        }

        // Force JVM exit - needed because thread pools (e.g., DownloadService)
        // use non-daemon threads that would otherwise keep the JVM alive
        System.exit(0);
    }

    // -- alt entry point: normally Launcher.main() is used instead --
    // -- kept for direct javafx launch (e.g., from ide) --
    public static void main(String[] args) {
        // Add shutdown hook for proper cleanup
        // -- jvm shutdown hook: runs on ctrl+c, kill, etc --
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            // -- ensure search resources cleaned up --
            if (instance != null && instance.searchController != null) {
                instance.searchController.shutdown();
            }
        }));

        // -- if args provided, run cli mode --
        if (args.length > 0) {
            int exitCode = new picocli.CommandLine(
                    new com.example.teamse1csdchcw.cli.LibSearchCLI()
            ).execute(args);
            System.exit(exitCode);
        } else {
            // -- no args = launch gui --
            // -- Application.launch() starts javafx runtime --
            launch(args);
        }
    }
}
