package com.example.teamse1csdchcw.repository;

import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.user.Bookmark;
import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookmarkRepository {
    private static final Logger logger = LoggerFactory.getLogger(BookmarkRepository.class);
    private final ObjectMapper objectMapper;

    public BookmarkRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public void save(Bookmark bookmark) throws SQLException {
        String sql = """
            INSERT INTO bookmarks (id, result_id, title, url, notes, tags, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                notes = excluded.notes,
                tags = excluded.tags
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (bookmark.getId() == null) {
                bookmark.setId(UUID.randomUUID().toString());
            }

            stmt.setString(1, bookmark.getId());
            stmt.setString(2, bookmark.getResultId());
            stmt.setString(3, bookmark.getTitle());
            stmt.setString(4, bookmark.getUrl());
            stmt.setString(5, bookmark.getNotes());
            stmt.setString(6, serializeTags(bookmark.getTags()));
            stmt.setTimestamp(7, Timestamp.valueOf(bookmark.getCreatedAt() != null ?
                    bookmark.getCreatedAt() : LocalDateTime.now()));

            stmt.executeUpdate();
            logger.info("Saved bookmark: {}", bookmark.getTitle());

        } catch (Exception e) {
            logger.error("Failed to save bookmark", e);
            throw new SQLException("Failed to save bookmark", e);
        }
    }

    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM bookmarks WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();
            logger.info("Deleted bookmark: {}", id);

        } catch (SQLException e) {
            logger.error("Failed to delete bookmark", e);
            throw e;
        }
    }

    public List<Bookmark> findAll() throws SQLException {
        String sql = "SELECT * FROM bookmarks ORDER BY created_at DESC";
        List<Bookmark> bookmarks = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                bookmarks.add(mapResultSet(rs));
            }

            logger.debug("Found {} bookmarks", bookmarks.size());
            return bookmarks;

        } catch (SQLException e) {
            logger.error("Failed to find bookmarks", e);
            throw e;
        }
    }

    public Bookmark findById(String id) throws SQLException {
        String sql = "SELECT * FROM bookmarks WHERE id = ?";

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
            logger.error("Failed to find bookmark by id", e);
            throw e;
        }
    }

    public boolean exists(String resultId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookmarks WHERE result_id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, resultId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            logger.error("Failed to check bookmark existence", e);
            throw e;
        }
    }

    public List<Bookmark> findByTag(String tag) throws SQLException {
        String sql = "SELECT * FROM bookmarks WHERE tags LIKE ? ORDER BY created_at DESC";
        List<Bookmark> bookmarks = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + tag + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bookmark bookmark = mapResultSet(rs);
                    if (bookmark.getTags() != null && bookmark.getTags().contains(tag)) {
                        bookmarks.add(bookmark);
                    }
                }
            }

            logger.debug("Found {} bookmarks with tag '{}'", bookmarks.size(), tag);
            return bookmarks;

        } catch (SQLException e) {
            logger.error("Failed to find bookmarks by tag", e);
            throw e;
        }
    }

    private Bookmark mapResultSet(ResultSet rs) throws SQLException {
        Bookmark bookmark = new Bookmark();
        bookmark.setId(rs.getString("id"));
        bookmark.setResultId(rs.getString("result_id"));
        bookmark.setTitle(rs.getString("title"));
        bookmark.setUrl(rs.getString("url"));
        bookmark.setNotes(rs.getString("notes"));
        bookmark.setTags(deserializeTags(rs.getString("tags")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            bookmark.setCreatedAt(createdAt.toLocalDateTime());
        }

        return bookmark;
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize tags", e);
            return "[]";
        }
    }

    private List<String> deserializeTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize tags", e);
            return new ArrayList<>();
        }
    }
}
