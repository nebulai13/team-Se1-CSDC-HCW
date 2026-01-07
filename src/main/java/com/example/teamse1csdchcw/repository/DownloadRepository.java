package com.example.teamse1csdchcw.repository;

import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DownloadRepository {
    private static final Logger logger = LoggerFactory.getLogger(DownloadRepository.class);

    public static class Download {
        private String id;
        private String resultId;
        private String url;
        private String destinationPath;
        private DownloadStatus status;
        private double progress;
        private Long fileSize;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String errorMessage;

        public enum DownloadStatus {
            PENDING, IN_PROGRESS, COMPLETED, FAILED
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getResultId() { return resultId; }
        public void setResultId(String resultId) { this.resultId = resultId; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getDestinationPath() { return destinationPath; }
        public void setDestinationPath(String destinationPath) { this.destinationPath = destinationPath; }
        public DownloadStatus getStatus() { return status; }
        public void setStatus(DownloadStatus status) { this.status = status; }
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public void save(Download download) throws SQLException {
        String sql = """
            INSERT INTO downloads (id, result_id, url, destination_path, status, progress,
                                   file_size, started_at, completed_at, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                status = excluded.status,
                progress = excluded.progress,
                file_size = excluded.file_size,
                completed_at = excluded.completed_at,
                error_message = excluded.error_message
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (download.getId() == null) {
                download.setId(UUID.randomUUID().toString());
            }

            stmt.setString(1, download.getId());
            stmt.setString(2, download.getResultId());
            stmt.setString(3, download.getUrl());
            stmt.setString(4, download.getDestinationPath());
            stmt.setString(5, download.getStatus().name());
            stmt.setDouble(6, download.getProgress());
            stmt.setObject(7, download.getFileSize());
            stmt.setTimestamp(8, download.getStartedAt() != null ?
                    Timestamp.valueOf(download.getStartedAt()) : null);
            stmt.setTimestamp(9, download.getCompletedAt() != null ?
                    Timestamp.valueOf(download.getCompletedAt()) : null);
            stmt.setString(10, download.getErrorMessage());

            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Failed to save download", e);
            throw e;
        }
    }

    public Download findById(String id) throws SQLException {
        String sql = "SELECT * FROM downloads WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            logger.error("Failed to find download by id", e);
            throw e;
        }
    }

    public List<Download> findPending() throws SQLException {
        String sql = "SELECT * FROM downloads WHERE status = 'PENDING' ORDER BY started_at ASC";
        return findByQuery(sql);
    }

    public List<Download> findInProgress() throws SQLException {
        String sql = "SELECT * FROM downloads WHERE status = 'IN_PROGRESS'";
        return findByQuery(sql);
    }

    public List<Download> findAll() throws SQLException {
        String sql = "SELECT * FROM downloads ORDER BY started_at DESC";
        return findByQuery(sql);
    }

    private List<Download> findByQuery(String sql) throws SQLException {
        List<Download> downloads = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                downloads.add(mapResultSet(rs));
            }

            return downloads;

        } catch (SQLException e) {
            logger.error("Failed to execute query", e);
            throw e;
        }
    }

    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM downloads WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Failed to delete download", e);
            throw e;
        }
    }

    private Download mapResultSet(ResultSet rs) throws SQLException {
        Download download = new Download();
        download.setId(rs.getString("id"));
        download.setResultId(rs.getString("result_id"));
        download.setUrl(rs.getString("url"));
        download.setDestinationPath(rs.getString("destination_path"));
        download.setStatus(Download.DownloadStatus.valueOf(rs.getString("status")));
        download.setProgress(rs.getDouble("progress"));

        Long fileSize = rs.getObject("file_size", Long.class);
        download.setFileSize(fileSize);

        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            download.setStartedAt(startedAt.toLocalDateTime());
        }

        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            download.setCompletedAt(completedAt.toLocalDateTime());
        }

        download.setErrorMessage(rs.getString("error_message"));

        return download;
    }
}
