package com.example.teamse1csdchcw.repository.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;


/**
 * Initializes the SQLite database with the required schema.
 */
public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String SCHEMA_FILE = "/db/schema.sql";

    private final SQLiteConnection sqliteConnection;

    public DatabaseInitializer(SQLiteConnection sqliteConnection) {
        this.sqliteConnection = sqliteConnection;
    }

    /**
     * Static method to initialize database (convenience method).
     */
    public static void initialize() {
        DatabaseInitializer initializer = new DatabaseInitializer(SQLiteConnection.getInstance());
        initializer.initializeSchema();
    }

    /**
     * Initializes the database schema if it doesn't exist.
     */
    public void initializeSchema() {
        try {
            logger.info("Initializing database schema...");
            String schema = loadSchemaFromResource();

            try (Connection conn = sqliteConnection.getConnection();
                 Statement stmt = conn.createStatement()) {

                // WICHTIG: jedes SQL-Statement einzeln ausführen
                String[] statements = schema.split(";");

                for (String s : statements) {
                    String sql = s.trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }

                logger.info("Database schema initialized successfully");

            }
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }


    /**
     * Loads the SQL schema from resources.
     */
    private String loadSchemaFromResource() {
        try (InputStream is = getClass().getResourceAsStream(SCHEMA_FILE)) {
            if (is == null) {
                throw new RuntimeException("Schema file not found: " + SCHEMA_FILE);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            logger.error("Failed to load schema file", e);
            throw new RuntimeException("Failed to load schema", e);
        }
    }

    /**
     * Drops all tables (use with caution!).
     */
    public void dropAllTables() {
        try (Connection conn = sqliteConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            String[] tables = {"journal", "downloads", "alert_matches", "alerts",
                             "bookmarks", "search_results", "search_history",
                             "sessions", "config"};

            for (String table : tables) {
                stmt.execute("DROP TABLE IF EXISTS " + table);
            }

            logger.warn("All tables dropped");

        } catch (Exception e) {
            logger.error("Failed to drop tables", e);
        }
    }

    /**
     * Checks if the database is initialized.
     */
    public boolean isInitialized() {
        try (Connection conn = sqliteConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            var rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='sessions'");
            return rs.next();

        } catch (Exception e) {
            logger.error("Failed to check database initialization", e);
            return false;
        }
    }
}
