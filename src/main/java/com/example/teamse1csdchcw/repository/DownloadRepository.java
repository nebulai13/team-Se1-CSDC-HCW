package com.example.teamse1csdchcw.repository;

import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * repository layer - manages pdf download queue persistence in sqlite
 * tracks download state, progress, and errors for async pdf downloads
 * follows repository pattern - abstracts sql from business logic
 */
public class DownloadRepository {
    private static final Logger logger = LoggerFactory.getLogger(DownloadRepository.class);

    /**
     * inner class representing a download record
     * tracks metadata for queued/in-progress/completed pdf downloads
     */
    public static class Download {
        // unique id for this download record
        private String id;

        // foreign key - links to search result being downloaded
        private String resultId;

        // source url for pdf file
        private String url;

        // local filesystem path where file will be saved
        private String destinationPath;

        // current state in download lifecycle
        private DownloadStatus status;

        // completion percentage 0.0 to 1.0 (100%)
        private double progress;

        // file size in bytes - null until headers received
        private Long fileSize;

        // when download was queued/started
        private LocalDateTime startedAt;

        // when download finished (success or failure)
        private LocalDateTime completedAt;

        // error details if download failed
        private String errorMessage;

        /** download lifecycle states */
        public enum DownloadStatus {
            PENDING,       // queued, not started yet
            IN_PROGRESS,   // actively downloading
            COMPLETED,     // successfully finished
            FAILED         // errored out
        }

        // standard getters/setters - java bean pattern for property access
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

    /**
     * insert or update download record in db
     * uses upsert pattern - insert if new, update if exists
     * auto-generates uuid if id is null
     */
    public void save(Download download) throws SQLException {
        // upsert sql - inserts or updates on conflict
        // ON CONFLICT handles race conditions gracefully
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

        // try-with-resources ensures conn/stmt closed even on exception
        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // generate uuid if new download w/o id
            if (download.getId() == null) {
                download.setId(UUID.randomUUID().toString());
            }

            // bind params to prepared stmt - prevents sql injection
            stmt.setString(1, download.getId());
            stmt.setString(2, download.getResultId());
            stmt.setString(3, download.getUrl());
            stmt.setString(4, download.getDestinationPath());
            stmt.setString(5, download.getStatus().name());  // enum to string
            stmt.setDouble(6, download.getProgress());
            stmt.setObject(7, download.getFileSize());  // handles null w/ setObject

            // convert LocalDateTime to sql Timestamp - null-safe
            stmt.setTimestamp(8, download.getStartedAt() != null ?
                    Timestamp.valueOf(download.getStartedAt()) : null);
            stmt.setTimestamp(9, download.getCompletedAt() != null ?
                    Timestamp.valueOf(download.getCompletedAt()) : null);
            stmt.setString(10, download.getErrorMessage());

            // execute insert/update
            stmt.executeUpdate();

        } catch (SQLException e) {
            // log and rethrow - caller handles error
            logger.error("Failed to save download", e);
            throw e;
        }
    }

    /**
     * find single download by unique id
     * returns null if not found
     */
    public Download findById(String id) throws SQLException {
        String sql = "SELECT * FROM downloads WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            // nested try-with-resources for resultset
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);  // convert row to obj
                }
                return null;  // not found
            }

        } catch (SQLException e) {
            logger.error("Failed to find download by id", e);
            throw e;
        }
    }

    /**
     * find all downloads waiting to start
     * ordered by start time - fifo queue
     */
    public List<Download> findPending() throws SQLException {
        String sql = "SELECT * FROM downloads WHERE status = 'PENDING' ORDER BY started_at ASC";
        return findByQuery(sql);
    }

    /**
     * find downloads currently in progress
     * used for resume/monitoring
     */
    public List<Download> findInProgress() throws SQLException {
        String sql = "SELECT * FROM downloads WHERE status = 'IN_PROGRESS'";
        return findByQuery(sql);
    }

    /**
     * get complete download history
     * newest first
     */
    public List<Download> findAll() throws SQLException {
        String sql = "SELECT * FROM downloads ORDER BY started_at DESC";
        return findByQuery(sql);
    }

    /**
     * helper method - executes query and maps results to list
     * reduces code duplication for similar queries
     */
    private List<Download> findByQuery(String sql) throws SQLException {
        List<Download> downloads = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // iterate all rows, convert each to Download obj
            while (rs.next()) {
                downloads.add(mapResultSet(rs));
            }

            return downloads;

        } catch (SQLException e) {
            logger.error("Failed to execute query", e);
            throw e;
        }
    }

    /**
     * remove download record from db
     * typically called after file downloaded or on cancel
     */
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

    /**
     * maps jdbc ResultSet row to Download object
     * handles type conversions and null values
     */
    private Download mapResultSet(ResultSet rs) throws SQLException {
        Download download = new Download();

        // extract all columns, convert types as needed
        download.setId(rs.getString("id"));
        download.setResultId(rs.getString("result_id"));
        download.setUrl(rs.getString("url"));
        download.setDestinationPath(rs.getString("destination_path"));

        // convert string back to enum
        download.setStatus(Download.DownloadStatus.valueOf(rs.getString("status")));
        download.setProgress(rs.getDouble("progress"));

        // handle nullable Long - use getObject instead of getLong
        Long fileSize = rs.getObject("file_size", Long.class);
        download.setFileSize(fileSize);

        // convert sql Timestamp back to LocalDateTime - null-safe
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
