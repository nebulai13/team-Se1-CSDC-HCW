package com.example.teamse1csdchcw.service.session;

import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private String currentSessionId;

    public String startSession() throws SQLException {
        currentSessionId = UUID.randomUUID().toString();

        String sql = """
                INSERT INTO sessions (id, started_at, status)
                VALUES (?, ?, 'ACTIVE')
                """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, currentSessionId);
            stmt.setObject(2, LocalDateTime.now());

            stmt.executeUpdate();
            logger.info("Started new session: {}", currentSessionId);
        }

        return currentSessionId;
    }

    public void endSession(String sessionId) throws SQLException {
        String sql = """
                UPDATE sessions
                SET ended_at = ?, status = 'COMPLETED'
                WHERE id = ?
                """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, LocalDateTime.now());
            stmt.setString(2, sessionId);

            stmt.executeUpdate();
            logger.info("Ended session: {}", sessionId);
        }

        if (sessionId.equals(currentSessionId)) {
            currentSessionId = null;
        }
    }

    public String getCurrentSessionId() {
        if (currentSessionId == null) {
            try {
                currentSessionId = startSession();
            } catch (SQLException e) {
                logger.error("Failed to start session", e);
                currentSessionId = "default-session";
            }
        }
        return currentSessionId;
    }

    public List<SessionInfo> getRecentSessions(int limit) throws SQLException {
        String sql = """
                SELECT s.id, s.started_at, s.ended_at, s.status,
                       COUNT(DISTINCT sh.id) as search_count
                FROM sessions s
                LEFT JOIN search_history sh ON s.id = sh.session_id
                GROUP BY s.id
                ORDER BY s.started_at DESC
                LIMIT ?
                """;

        List<SessionInfo> sessions = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SessionInfo info = new SessionInfo();
                    info.id = rs.getString("id");
                    info.startedAt = (LocalDateTime) rs.getObject("started_at");
                    info.endedAt = (LocalDateTime) rs.getObject("ended_at");
                    info.status = rs.getString("status");
                    info.searchCount = rs.getInt("search_count");

                    sessions.add(info);
                }
            }
        }

        return sessions;
    }

    public static class SessionInfo {
        public String id;
        public LocalDateTime startedAt;
        public LocalDateTime endedAt;
        public String status;
        public int searchCount;

        @Override
        public String toString() {
            String duration = "";
            if (endedAt != null) {
                long minutes = java.time.Duration.between(startedAt, endedAt).toMinutes();
                duration = String.format(" (%.0f min)", (double) minutes);
            }

            return String.format("%s: %s - %s [%s]%s - %d searches",
                    id.substring(0, 8),
                    startedAt,
                    endedAt != null ? endedAt : "ongoing",
                    status,
                    duration,
                    searchCount);
        }
    }
}
