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
 * initializes sqlite db schema from sql file
 * creates all tables, indexes, constraints on first run
 * reads schema.sql from resources folder
 */
public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    // path to schema sql file in resources folder
    // loaded from classpath during runtime
    private static final String SCHEMA_FILE = "/db/schema.sql";

    // reference to singleton db connection
    private final SQLiteConnection sqliteConnection;

    /** constructor - accepts db connection to use */
    public DatabaseInitializer(SQLiteConnection sqliteConnection) {
        this.sqliteConnection = sqliteConnection;
    }

    /**
     * static convenience method - uses default singleton connection
     * called from Launcher.main() at app startup
     */
    public static void initialize() {
        DatabaseInitializer initializer = new DatabaseInitializer(SQLiteConnection.getInstance());
        initializer.initializeSchema();
    }

    /**
     * loads and executes sql schema from resources
     * idempotent - safe to call multiple times (CREATE TABLE IF NOT EXISTS)
     */
    public void initializeSchema() {
        try {
            logger.info("Initializing database schema...");

            // load schema.sql content as string
            String schema = loadSchemaFromResource();

            try (Connection conn = sqliteConnection.getConnection();
                 Statement stmt = conn.createStatement()) {

                // split on semicolons - each statement must execute separately
                // sqlite doesn't support multi-statement execution in one call
                String[] statements = schema.split(";");

                for (String s : statements) {
                    String sql = s.trim();
                    // skip empty statements (trailing semicolons, blank lines)
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }

                logger.info("Database schema initialized successfully");

            }
        } catch (Exception e) {
            // fatal error - can't proceed w/o db
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }


    /**
     * reads schema sql file from jar resources
     * uses classloader to access embedded resource
     */
    private String loadSchemaFromResource() {
        // try-with-resources ensures stream closed even on exception
        try (InputStream is = getClass().getResourceAsStream(SCHEMA_FILE)) {
            // check if resource exists in jar/classpath
            if (is == null) {
                throw new RuntimeException("Schema file not found: " + SCHEMA_FILE);
            }

            // wrap in bufferedreader for efficient line-by-line reading
            // specify utf-8 explicitly to avoid platform encoding issues
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                // read all lines and join w/ newlines
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            logger.error("Failed to load schema file", e);
            throw new RuntimeException("Failed to load schema", e);
        }
    }

    /**
     * drops all tables - use for testing or reset
     * careful: deletes all data!
     */
    public void dropAllTables() {
        try (Connection conn = sqliteConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // list all app tables in dependency order (reverse of creation)
            // drop in reverse to avoid foreign key constraint errors
            String[] tables = {"journal", "downloads", "alert_matches", "alerts",
                             "bookmarks", "search_results", "search_history",
                             "sessions", "config"};

            // IF EXISTS prevents error if table doesn't exist
            for (String table : tables) {
                stmt.execute("DROP TABLE IF EXISTS " + table);
            }

            logger.warn("All tables dropped");

        } catch (Exception e) {
            logger.error("Failed to drop tables", e);
        }
    }

    /**
     * checks if db schema is initialized
     * looks for sessions table as indicator
     */
    public boolean isInitialized() {
        try (Connection conn = sqliteConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // query sqlite_master - special table w/ schema metadata
            // returns row if 'sessions' table exists
            var rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='sessions'");
            return rs.next();  // true if at least one result

        } catch (Exception e) {
            logger.error("Failed to check database initialization", e);
            return false;
        }
    }
}
