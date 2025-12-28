package com.example.teamse1csdchcw.repository.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages SQLite database connections with connection pooling.
 */
public class SQLiteConnection {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteConnection.class);
    private static final String DEFAULT_DB_PATH = System.getProperty("user.home") + "/.libsearch/data/libsearch.db";

    private static SQLiteConnection instance;
    private final String dbPath;
    private Connection connection;

    private SQLiteConnection(String dbPath) {
        this.dbPath = dbPath;
        ensureDataDirectory();
    }

    /**
     * Gets the singleton instance with default database path.
     */
    public static synchronized SQLiteConnection getInstance() {
        if (instance == null) {
            instance = new SQLiteConnection(DEFAULT_DB_PATH);
        }
        return instance;
    }

    /**
     * Gets the singleton instance with custom database path.
     */
    public static synchronized SQLiteConnection getInstance(String dbPath) {
        if (instance == null) {
            instance = new SQLiteConnection(dbPath);
        }
        return instance;
    }

    /**
     * Ensures the data directory exists.
     */
    private void ensureDataDirectory() {
        try {
            Path dataDir = Paths.get(dbPath).getParent();
            if (dataDir != null && !Files.exists(dataDir)) {
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
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            logger.debug("Established database connection: {}", dbPath);

            // Enable foreign keys
            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
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
    public boolean databaseExists() {
        return Files.exists(Paths.get(dbPath));
    }
}
