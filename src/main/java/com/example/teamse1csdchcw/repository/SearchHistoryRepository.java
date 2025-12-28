package com.example.teamse1csdchcw.repository;

import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repository for search history persistence.
 */
public class SearchHistoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(SearchHistoryRepository.class);
    private final ObjectMapper objectMapper;

    public SearchHistoryRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Save a search query to history.
     */
    public String save(SearchQuery query, String sessionId, int resultCount) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO search_history
            (id, session_id, query_text, parsed_query, result_count, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.setString(2, sessionId);
            stmt.setString(3, query.getOriginalQuery());

            // Serialize the entire SearchQuery as JSON
            String parsedQueryJson = objectMapper.writeValueAsString(query);
            stmt.setString(4, parsedQueryJson);

            stmt.setInt(5, resultCount);
            stmt.setString(6, LocalDateTime.now().toString());

            stmt.executeUpdate();
            logger.debug("Saved search history: {}", id);

            return id;
        } catch (Exception e) {
            logger.error("Failed to save search history", e);
            throw new SQLException("Failed to save search history", e);
        }
    }

    /**
     * Find search history by session ID.
     */
    public List<SearchHistoryEntry> findBySessionId(String sessionId) throws SQLException {
        String sql = """
            SELECT * FROM search_history
            WHERE session_id = ?
            ORDER BY timestamp DESC
            """;

        List<SearchHistoryEntry> history = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                history.add(mapResultSet(rs));
            }

            logger.debug("Found {} history entries for session {}", history.size(), sessionId);
        } catch (Exception e) {
            logger.error("Failed to find search history", e);
            throw new SQLException("Failed to find search history", e);
        }

        return history;
    }

    /**
     * Find all search history (most recent first).
     */
    public List<SearchHistoryEntry> findAll(int limit) throws SQLException {
        String sql = """
            SELECT * FROM search_history
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<SearchHistoryEntry> history = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                history.add(mapResultSet(rs));
            }

            logger.debug("Found {} history entries", history.size());
        } catch (Exception e) {
            logger.error("Failed to find search history", e);
            throw new SQLException("Failed to find search history", e);
        }

        return history;
    }

    /**
     * Search history by query text.
     */
    public List<SearchHistoryEntry> searchByQueryText(String queryText) throws SQLException {
        String sql = """
            SELECT * FROM search_history
            WHERE query_text LIKE ?
            ORDER BY timestamp DESC
            LIMIT 50
            """;

        List<SearchHistoryEntry> history = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + queryText + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                history.add(mapResultSet(rs));
            }

            logger.debug("Found {} history entries matching '{}'", history.size(), queryText);
        } catch (Exception e) {
            logger.error("Failed to search history", e);
            throw new SQLException("Failed to search history", e);
        }

        return history;
    }

    /**
     * Delete a history entry by ID.
     */
    public boolean deleteById(String id) throws SQLException {
        String sql = "DELETE FROM search_history WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int deleted = stmt.executeUpdate();

            logger.debug("Deleted history entry: {}", id);
            return deleted > 0;
        }
    }

    /**
     * Delete all history for a session.
     */
    public int deleteBySessionId(String sessionId) throws SQLException {
        String sql = "DELETE FROM search_history WHERE session_id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            int deleted = stmt.executeUpdate();

            logger.info("Deleted {} history entries for session {}", deleted, sessionId);
            return deleted;
        }
    }

    /**
     * Clear all history older than the specified days.
     */
    public int clearOldHistory(int daysOld) throws SQLException {
        String sql = """
            DELETE FROM search_history
            WHERE timestamp < datetime('now', '-' || ? || ' days')
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, daysOld);
            int deleted = stmt.executeUpdate();

            logger.info("Deleted {} history entries older than {} days", deleted, daysOld);
            return deleted;
        }
    }

    /**
     * Get count of history entries.
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM search_history";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;
        }
    }

    /**
     * Map ResultSet to SearchHistoryEntry.
     */
    private SearchHistoryEntry mapResultSet(ResultSet rs) throws SQLException {
        try {
            SearchHistoryEntry entry = new SearchHistoryEntry();
            entry.setId(rs.getString("id"));
            entry.setSessionId(rs.getString("session_id"));
            entry.setQueryText(rs.getString("query_text"));

            String parsedQueryJson = rs.getString("parsed_query");
            if (parsedQueryJson != null) {
                SearchQuery query = objectMapper.readValue(parsedQueryJson, SearchQuery.class);
                entry.setParsedQuery(query);
            }

            entry.setResultCount(rs.getInt("result_count"));

            String timestampStr = rs.getString("timestamp");
            if (timestampStr != null) {
                entry.setTimestamp(LocalDateTime.parse(timestampStr));
            }

            return entry;
        } catch (Exception e) {
            logger.error("Failed to map result set", e);
            throw new SQLException("Failed to map result", e);
        }
    }

    /**
     * Data class for search history entry.
     */
    public static class SearchHistoryEntry {
        private String id;
        private String sessionId;
        private String queryText;
        private SearchQuery parsedQuery;
        private int resultCount;
        private LocalDateTime timestamp;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getQueryText() {
            return queryText;
        }

        public void setQueryText(String queryText) {
            this.queryText = queryText;
        }

        public SearchQuery getParsedQuery() {
            return parsedQuery;
        }

        public void setParsedQuery(SearchQuery parsedQuery) {
            this.parsedQuery = parsedQuery;
        }

        public int getResultCount() {
            return resultCount;
        }

        public void setResultCount(int resultCount) {
            this.resultCount = resultCount;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
