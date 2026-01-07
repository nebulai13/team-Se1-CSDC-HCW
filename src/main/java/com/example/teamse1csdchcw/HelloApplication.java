package com.example.teamse1csdchcw;

import com.example.teamse1csdchcw.repository.sqlite.DatabaseInitializer;
import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import com.example.teamse1csdchcw.ui.controller.MainController;
import com.example.teamse1csdchcw.ui.controller.ResultsController;
import com.example.teamse1csdchcw.ui.controller.SearchController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * LibSearch - Academic Federated Search Application
 *
 * Main JavaFX application class.
 */
public class HelloApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(HelloApplication.class);
    private static HelloApplication instance;
    private SearchController searchController;

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting LibSearch application");
        instance = this;

        // Initialize database
        try {
            if (!SQLiteConnection.getInstance().databaseExists()) {
                logger.info("Initializing database...");
                DatabaseInitializer.initialize();
            } else {
                logger.info("Database already exists");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            showErrorAndExit("Database Error", "Failed to initialize database: " + e.getMessage());
            return;
        }

        // Load main view
        try {
            // Load main FXML (which includes search-panel and results-table via fx:include)
            FXMLLoader mainLoader = new FXMLLoader(
                    getClass().getResource("/com/example/teamse1csdchcw/fxml/main-view.fxml")
            );
            Parent root = mainLoader.load();
            MainController mainController = mainLoader.getController();

            // Get search controller from main controller (injected by fx:include)
            this.searchController = mainController.getSearchController();

            // Create scene
            Scene scene = new Scene(root, 1200, 800);

            // Set up stage
            stage.setTitle("LibSearch - Academic Federated Search");
            stage.setScene(scene);
            stage.setOnCloseRequest(e -> onApplicationClose());
            stage.show();

            logger.info("LibSearch application started successfully");

        } catch (IOException e) {
            logger.error("Failed to load UI", e);
            showErrorAndExit("UI Error", "Failed to load user interface: " + e.getMessage());
        }
    }

    /**
     * Handle application close.
     */
    private void onApplicationClose() {
        logger.info("Closing LibSearch application");

        // Shutdown search service and index
        if (searchController != null) {
            searchController.shutdown();
        }

        try {
            SQLiteConnection.getInstance().close();
        } catch (Exception e) {
            logger.error("Error closing database", e);
        }

        Platform.exit();
    }

    /**
     * Show error and exit application.
     */
    private void showErrorAndExit(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        Platform.exit();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping LibSearch application");
        super.stop();

        // Shutdown search service and index
        if (searchController != null) {
            searchController.shutdown();
        }

        // Clean up resources
        try {
            SQLiteConnection.getInstance().close();
        } catch (Exception e) {
            logger.error("Error closing resources", e);
        }
    }

    public static void main(String[] args) {
        // Add shutdown hook for proper cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            if (instance != null && instance.searchController != null) {
                instance.searchController.shutdown();
            }
        }));

        if (args.length > 0) {
            int exitCode = new picocli.CommandLine(
                    new com.example.teamse1csdchcw.cli.LibSearchCLI()
            ).execute(args);
            System.exit(exitCode);
        } else {
            launch(args);
        }
    }
}
