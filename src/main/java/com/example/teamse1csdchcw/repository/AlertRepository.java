package com.example.teamse1csdchcw.repository;

import com.example.teamse1csdchcw.domain.monitoring.Alert;
import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlertRepository {
    private static final Logger logger = LoggerFactory.getLogger(AlertRepository.class);
    private final ObjectMapper objectMapper;

    public AlertRepository() {
        this.objectMapper = new ObjectMapper();
    }

    public void save(Alert alert) throws SQLException {
        if (alert.getId() == null) {
            alert.setId(UUID.randomUUID().toString());
        }

        String sql = """
                INSERT INTO alerts (id, name, keywords, author_filter, year_from, year_to,
                                    enabled, notification_type, notification_target,
                                    created_at, last_checked, match_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name = excluded.name,
                    keywords = excluded.keywords,
                    author_filter = excluded.author_filter,
                    year_from = excluded.year_from,
                    year_to = excluded.year_to,
                    enabled = excluded.enabled,
                    notification_type = excluded.notification_type,
                    notification_target = excluded.notification_target,
                    last_checked = excluded.last_checked,
                    match_count = excluded.match_count
                """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, alert.getId());
            stmt.setString(2, alert.getName());
            stmt.setString(3, serializeKeywords(alert.getKeywords()));
            stmt.setString(4, alert.getAuthorFilter());
            stmt.setObject(5, alert.getYearFrom());
            stmt.setObject(6, alert.getYearTo());
            stmt.setBoolean(7, alert.isEnabled());
            stmt.setString(8, alert.getNotificationType().name());
            stmt.setString(9, alert.getNotificationTarget());
            stmt.setObject(10, alert.getCreatedAt());
            stmt.setObject(11, alert.getLastChecked());
            stmt.setInt(12, alert.getMatchCount());

            stmt.executeUpdate();
            logger.debug("Saved alert: {}", alert.getId());
        }
    }

    public List<Alert> findAll() throws SQLException {
        String sql = "SELECT * FROM alerts ORDER BY created_at DESC";
        List<Alert> alerts = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                alerts.add(mapResultSet(rs));
            }
        }

        logger.debug("Found {} alerts", alerts.size());
        return alerts;
    }

    public List<Alert> findEnabled() throws SQLException {
        String sql = "SELECT * FROM alerts WHERE enabled = 1 ORDER BY created_at DESC";
        List<Alert> alerts = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                alerts.add(mapResultSet(rs));
            }
        }

        logger.debug("Found {} enabled alerts", alerts.size());
        return alerts;
    }

    public Alert findById(String id) throws SQLException {
        String sql = "SELECT * FROM alerts WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }

        return null;
    }

    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM alerts WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int rows = stmt.executeUpdate();

            logger.debug("Deleted {} alert(s) with id: {}", rows, id);
        }
    }

    public void updateLastChecked(String id, LocalDateTime timestamp, int matchCount) throws SQLException {
        String sql = "UPDATE alerts SET last_checked = ?, match_count = match_count + ? WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, timestamp);
            stmt.setInt(2, matchCount);
            stmt.setString(3, id);

            stmt.executeUpdate();
            logger.debug("Updated last_checked for alert: {}", id);
        }
    }

    private Alert mapResultSet(ResultSet rs) throws SQLException {
        Alert alert = new Alert();

        alert.setId(rs.getString("id"));
        alert.setName(rs.getString("name"));
        alert.setKeywords(deserializeKeywords(rs.getString("keywords")));
        alert.setAuthorFilter(rs.getString("author_filter"));

        Object yearFrom = rs.getObject("year_from");
        if (yearFrom != null) {
            alert.setYearFrom((Integer) yearFrom);
        }

        Object yearTo = rs.getObject("year_to");
        if (yearTo != null) {
            alert.setYearTo((Integer) yearTo);
        }

        alert.setEnabled(rs.getBoolean("enabled"));
        alert.setNotificationType(Alert.NotificationType.valueOf(rs.getString("notification_type")));
        alert.setNotificationTarget(rs.getString("notification_target"));

        Object createdAt = rs.getObject("created_at");
        if (createdAt != null) {
            alert.setCreatedAt((LocalDateTime) createdAt);
        }

        Object lastChecked = rs.getObject("last_checked");
        if (lastChecked != null) {
            alert.setLastChecked((LocalDateTime) lastChecked);
        }

        alert.setMatchCount(rs.getInt("match_count"));

        return alert;
    }

    private String serializeKeywords(List<String> keywords) {
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (Exception e) {
            logger.error("Failed to serialize keywords", e);
            return "[]";
        }
    }

    private List<String> deserializeKeywords(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            logger.error("Failed to deserialize keywords", e);
            return new ArrayList<>();
        }
    }
}
