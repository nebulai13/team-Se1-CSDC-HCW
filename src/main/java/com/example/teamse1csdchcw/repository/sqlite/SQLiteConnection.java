package com.example.teamse1csdchcw.repository.sqlite;

// -- logging --
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// -- java nio for file/path operations --
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// -- jdbc (java database connectivity) api --
import java.sql.Connection;      // -- represents db connection --
import java.sql.DriverManager;   // -- factory for connections --
import java.sql.SQLException;

/**
 * Manages SQLite database connections with connection pooling.
 */
// -- singleton class: only one instance exists in app --
// -- manages single sqlite connection for all repos --
// -- sqlite = lightweight embedded db (single file, no server) --
public class SQLiteConnection {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteConnection.class);
    // -- default db path: ~/.libsearch/data/libsearch.db --
    private static final String DEFAULT_DB_PATH = System.getProperty("user.home") + "/.libsearch/data/libsearch.db";

    // -- singleton instance: shared across whole app --
    private static SQLiteConnection instance;
    // -- path to sqlite db file --
    private final String dbPath;
    // -- jdbc connection to db --
    private Connection connection;

    // -- private constructor: can only be called by getInstance() --
    // -- singleton pattern enforces single instance --
    private SQLiteConnection(String dbPath) {
        this.dbPath = dbPath;
        // -- make sure parent dir exists --
        ensureDataDirectory();
    }

    /**
     * Gets the singleton instance with default database path.
     */
    // -- synchronized = thread-safe (prevents race conditions) --
    // -- lazy init: only creates instance when first called --
    public static synchronized SQLiteConnection getInstance() {
        if (instance == null) {
            instance = new SQLiteConnection(DEFAULT_DB_PATH);
        }
        return instance;
    }

    /**
     * Gets the singleton instance with custom database path.
     */
    // -- overload for custom db path (e.g., testing) --
    public static synchronized SQLiteConnection getInstance(String dbPath) {
        if (instance == null) {
            instance = new SQLiteConnection(dbPath);
        }
        return instance;
    }

    /**
     * Ensures the data directory exists.
     */
    // -- creates ~/.libsearch/data/ if missing --
    private void ensureDataDirectory() {
        try {
            // -- get parent dir of db file --
            Path dataDir = Paths.get(dbPath).getParent();
            if (dataDir != null && !Files.exists(dataDir)) {
                // -- create dir and all parents --
                Files.createDirectories(dataDir);
                logger.info("Created data directory: {}", dataDir);
            }
        } catch (Exception e) {
            logger.error("Failed to create data directory", e);
        }
    }

    /**
     * Gets an active database connection.
     */
    // -- returns existing connection or creates new one --
    // -- lazy connection: only connects when needed --
    public Connection getConnection() throws SQLException {
        // -- check if need to (re)connect --
        if (connection == null || connection.isClosed()) {
            // -- jdbc url format: jdbc:sqlite:/path/to/file.db --
            String url = "jdbc:sqlite:" + dbPath;
            // -- drivermanager.getConnection creates actual connection --
            connection = DriverManager.getConnection(url);
            // -- autocommit = each statement is its own transaction --
            connection.setAutoCommit(true);
            logger.info("SQLite DB path in use: {}", dbPath);

            logger.info("Established database connection: {}", dbPath);

            // Enable foreign keys
            // -- sqlite foreign keys are off by default --
            // -- PRAGMA = sqlite-specific command --
            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    // -- should be called on app shutdown --
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.debug("Closed database connection");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }

    /**
     * Gets the database file path.
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * Checks if database file exists.
     */
    // -- used to determine if schema needs to be created --
    public boolean databaseExists() {
        return Files.exists(Paths.get(dbPath));
    }
}
