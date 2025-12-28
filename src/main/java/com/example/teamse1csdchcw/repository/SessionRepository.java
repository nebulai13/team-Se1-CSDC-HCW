package com.example.teamse1csdchcw.repository;

import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for session management.
 */
public class SessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(SessionRepository.class);
    private final ObjectMapper objectMapper;

    public SessionRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Create a new session.
     */
    public String createSession(String name, Map<String, String> metadata) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO sessions
            (id, name, created_at, last_accessed_at, metadata)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now();

            stmt.setString(1, id);
            stmt.setString(2, name != null ? name : "Session " + now.toString());
            stmt.setString(3, now.toString());
            stmt.setString(4, now.toString());

            if (metadata != null && !metadata.isEmpty()) {
                stmt.setString(5, objectMapper.writeValueAsString(metadata));
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }

            stmt.executeUpdate();
            logger.info("Created session: {} ({})", name, id);

            return id;
        } catch (Exception e) {
            logger.error("Failed to create session", e);
            throw new SQLException("Failed to create session", e);
        }
    }

    /**
     * Find a session by ID.
     */
    public Session findById(String id) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }

            return null;
        } catch (Exception e) {
            logger.error("Failed to find session", e);
            throw new SQLException("Failed to find session", e);
        }
    }

    /**
     * Get all sessions (most recent first).
     */
    public List<Session> findAll(int limit) throws SQLException {
        String sql = """
            SELECT * FROM sessions
            ORDER BY last_accessed_at DESC
            LIMIT ?
            """;

        List<Session> sessions = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                sessions.add(mapResultSet(rs));
            }

            logger.debug("Found {} sessions", sessions.size());
        } catch (Exception e) {
            logger.error("Failed to find sessions", e);
            throw new SQLException("Failed to find sessions", e);
        }

        return sessions;
    }

    /**
     * Get the most recent session.
     */
    public Session findMostRecent() throws SQLException {
        String sql = """
            SELECT * FROM sessions
            ORDER BY last_accessed_at DESC
            LIMIT 1
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return mapResultSet(rs);
            }

            return null;
        } catch (Exception e) {
            logger.error("Failed to find most recent session", e);
            throw new SQLException("Failed to find most recent session", e);
        }
    }

    /**
     * Update session's last accessed time.
     */
    public void updateLastAccessed(String id) throws SQLException {
        String sql = "UPDATE sessions SET last_accessed_at = ? WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, id);

            stmt.executeUpdate();
            logger.debug("Updated last accessed time for session: {}", id);
        }
    }

    /**
     * Update session metadata.
     */
    public void updateMetadata(String id, Map<String, String> metadata) throws SQLException {
        String sql = "UPDATE sessions SET metadata = ? WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (metadata != null && !metadata.isEmpty()) {
                stmt.setString(1, objectMapper.writeValueAsString(metadata));
            } else {
                stmt.setNull(1, Types.VARCHAR);
            }

            stmt.setString(2, id);

            stmt.executeUpdate();
            logger.debug("Updated metadata for session: {}", id);
        } catch (Exception e) {
            logger.error("Failed to update session metadata", e);
            throw new SQLException("Failed to update session metadata", e);
        }
    }

    /**
     * Delete a session and all related data.
     */
    public boolean deleteById(String id) throws SQLException {
        Connection conn = null;
        try {
            conn = SQLiteConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Delete related data first (due to foreign keys)
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM search_results WHERE session_id = ?")) {
                stmt.setString(1, id);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM search_history WHERE session_id = ?")) {
                stmt.setString(1, id);
                stmt.executeUpdate();
            }

            // Delete session
            int deleted;
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM sessions WHERE id = ?")) {
                stmt.setString(1, id);
                deleted = stmt.executeUpdate();
            }

            conn.commit();
            logger.info("Deleted session: {}", id);

            return deleted > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Failed to rollback transaction", ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Delete sessions older than the specified days.
     */
    public int deleteOldSessions(int daysOld) throws SQLException {
        String sql = """
            DELETE FROM sessions
            WHERE last_accessed_at < datetime('now', '-' || ? || ' days')
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, daysOld);
            int deleted = stmt.executeUpdate();

            logger.info("Deleted {} sessions older than {} days", deleted, daysOld);
            return deleted;
        }
    }

    /**
     * Get count of sessions.
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sessions";

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
     * Map ResultSet to Session.
     */
    private Session mapResultSet(ResultSet rs) throws SQLException {
        try {
            Session session = new Session();
            session.setId(rs.getString("id"));
            session.setName(rs.getString("name"));

            String createdStr = rs.getString("created_at");
            if (createdStr != null) {
                session.setCreatedAt(LocalDateTime.parse(createdStr));
            }

            String accessedStr = rs.getString("last_accessed_at");
            if (accessedStr != null) {
                session.setLastAccessedAt(LocalDateTime.parse(accessedStr));
            }

            String metadataJson = rs.getString("metadata");
            if (metadataJson != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> metadata = objectMapper.readValue(metadataJson, Map.class);
                session.setMetadata(metadata);
            }

            return session;
        } catch (Exception e) {
            logger.error("Failed to map result set", e);
            throw new SQLException("Failed to map result", e);
        }
    }

    /**
     * Data class for session.
     */
    public static class Session {
        private String id;
        private String name;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessedAt;
        private Map<String, String> metadata;

        public Session() {
            this.metadata = new HashMap<>();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getLastAccessedAt() {
            return lastAccessedAt;
        }

        public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }

        @Override
        public String toString() {
            return "Session{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", createdAt=" + createdAt +
                    ", lastAccessedAt=" + lastAccessedAt +
                    '}';
        }
    }
}
