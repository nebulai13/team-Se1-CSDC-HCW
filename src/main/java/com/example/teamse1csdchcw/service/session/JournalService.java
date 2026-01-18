package com.example.teamse1csdchcw.service.session;

import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JournalService {
    private static final Logger logger = LoggerFactory.getLogger(JournalService.class);

    public enum EventType {
        SEARCH,
        DOWNLOAD,
        BOOKMARK,
        EXPORT,
        INDEX,
        ALERT,
        SESSION
    }

    public void logEvent(String sessionId, EventType eventType, String description) {
        logEvent(sessionId, eventType, description, null);
    }

    public void logEvent(String sessionId, EventType eventType, String description, String metadata) {
        try {
            String sql = """
                    INSERT INTO journal (id, session_id, event_type, description, metadata, timestamp)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (Connection conn = SQLiteConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, sessionId);
                stmt.setString(3, eventType.name());
                stmt.setString(4, description);
                stmt.setString(5, metadata);
                stmt.setObject(6, LocalDateTime.now());

                stmt.executeUpdate();
                logger.debug("Logged event: {} - {}", eventType, description);
            }

        } catch (SQLException e) {
            logger.error("Failed to log journal event", e);
        }
    }

    public List<JournalEntry> getRecentEntries(int limit) throws SQLException {
        String sql = """
                SELECT * FROM journal
                ORDER BY timestamp DESC
                LIMIT ?
                """;

        List<JournalEntry> entries = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JournalEntry entry = new JournalEntry();
                    entry.id = rs.getString("id");
                    entry.sessionId = rs.getString("session_id");
                    entry.eventType = EventType.valueOf(rs.getString("event_type"));
                    entry.description = rs.getString("description");
                    entry.metadata = rs.getString("metadata");
                    entry.timestamp = (LocalDateTime) rs.getObject("timestamp");

                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    public List<JournalEntry> getSessionEntries(String sessionId) throws SQLException {
        String sql = """
                SELECT * FROM journal
                WHERE session_id = ?
                ORDER BY timestamp ASC
                """;

        List<JournalEntry> entries = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JournalEntry entry = new JournalEntry();
                    entry.id = rs.getString("id");
                    entry.sessionId = rs.getString("session_id");
                    entry.eventType = EventType.valueOf(rs.getString("event_type"));
                    entry.description = rs.getString("description");
                    entry.metadata = rs.getString("metadata");
                    entry.timestamp = (LocalDateTime) rs.getObject("timestamp");

                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    public static class JournalEntry {
        public String id;
        public String sessionId;
        public EventType eventType;
        public String description;
        public String metadata;
        public LocalDateTime timestamp;

        @Override
        public String toString() {
            return String.format("[%s] %s: %s",
                    timestamp,
                    eventType,
                    description);
        }
    }
}
