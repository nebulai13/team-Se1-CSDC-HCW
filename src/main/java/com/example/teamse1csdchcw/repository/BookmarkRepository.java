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

/**
 * repository layer - manages user bookmarks in sqlite
 * handles crud operations for saved papers/search results
 * uses jackson for json serialization of tag lists
 * follows repository pattern - separates data access from business logic
 */
public class BookmarkRepository {
    private static final Logger logger = LoggerFactory.getLogger(BookmarkRepository.class);

    // jackson mapper for converting java objs to/from json
    // needed for storing tag lists as json in sqlite
    private final ObjectMapper objectMapper;

    /** constructor - init jackson w/ module support for java 8 time types */
    public BookmarkRepository() {
        this.objectMapper = new ObjectMapper();
        // auto-detect and register modules (e.g., JavaTimeModule for LocalDateTime)
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * save or update bookmark in db
     * uses upsert - insert if new, update notes/tags if exists
     * auto-generates uuid for new bookmarks
     */
    public void save(Bookmark bookmark) throws SQLException {
        // upsert sql - ON CONFLICT updates only mutable fields (notes, tags)
        // title/url/created_at remain unchanged on conflict
        String sql = """
            INSERT INTO bookmarks (id, result_id, title, url, notes, tags, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                notes = excluded.notes,
                tags = excluded.tags
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // generate uuid if bookmark doesn't have id yet
            if (bookmark.getId() == null) {
                bookmark.setId(UUID.randomUUID().toString());
            }

            // bind all parameters - prepared statement prevents sql injection
            stmt.setString(1, bookmark.getId());
            stmt.setString(2, bookmark.getResultId());
            stmt.setString(3, bookmark.getTitle());
            stmt.setString(4, bookmark.getUrl());
            stmt.setString(5, bookmark.getNotes());
            stmt.setString(6, serializeTags(bookmark.getTags()));  // list -> json string
            stmt.setTimestamp(7, Timestamp.valueOf(bookmark.getCreatedAt() != null ?
                    bookmark.getCreatedAt() : LocalDateTime.now()));

            stmt.executeUpdate();
            logger.info("Saved bookmark: {}", bookmark.getTitle());

        } catch (Exception e) {
            logger.error("Failed to save bookmark", e);
            throw new SQLException("Failed to save bookmark", e);
        }
    }

    /**
     * delete bookmark by id
     * permanent removal from db
     */
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

    /**
     * delete all bookmarks for a specific search result
     * useful when user unbookmarks from search results list
     * returns count of deleted rows
     */
    public int deleteByResultId(String resultId) throws SQLException {
        String sql = "DELETE FROM bookmarks WHERE result_id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, resultId);
            int deleted = stmt.executeUpdate();
            logger.info("Deleted {} bookmark(s) for result_id={}", deleted, resultId);
            return deleted;

        } catch (SQLException e) {
            logger.error("Failed to delete bookmark by result_id", e);
            throw e;
        }
    }

    /**
     * retrieve all bookmarks from db
     * sorted newest first by creation timestamp
     */
    public List<Bookmark> findAll() throws SQLException {
        String sql = "SELECT * FROM bookmarks ORDER BY created_at DESC";
        List<Bookmark> bookmarks = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // iterate rows and convert to bookmark objs
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

    /**
     * find single bookmark by unique id
     * returns null if not found
     */
    public Bookmark findById(String id) throws SQLException {
        String sql = "SELECT * FROM bookmarks WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
                return null;  // not found
            }

        } catch (SQLException e) {
            logger.error("Failed to find bookmark by id", e);
            throw e;
        }
    }

    /**
     * check if bookmark exists for given result id
     * fast check w/o loading full bookmark obj
     */
    public boolean exists(String resultId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookmarks WHERE result_id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, resultId);

            try (ResultSet rs = stmt.executeQuery()) {
                // return true if count > 0
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            logger.error("Failed to check bookmark existence", e);
            throw e;
        }
    }

    /**
     * find bookmarks containing specific tag
     * uses like query then filters in-memory for exact match
     * (sqlite stores tags as json string, not separate table)
     */
    public List<Bookmark> findByTag(String tag) throws SQLException {
        // LIKE query finds candidates - may have false positives
        String sql = "SELECT * FROM bookmarks WHERE tags LIKE ? ORDER BY created_at DESC";
        List<Bookmark> bookmarks = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // %tag% matches if tag appears anywhere in json array
            stmt.setString(1, "%" + tag + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bookmark bookmark = mapResultSet(rs);
                    // verify exact tag match after deserializing json
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

    /**
     * convert jdbc ResultSet row to Bookmark object
     * handles type conversions and json deserialization
     */
    private Bookmark mapResultSet(ResultSet rs) throws SQLException {
        Bookmark bookmark = new Bookmark();

        // extract all columns from result row
        bookmark.setId(rs.getString("id"));
        bookmark.setResultId(rs.getString("result_id"));
        bookmark.setTitle(rs.getString("title"));
        bookmark.setUrl(rs.getString("url"));
        bookmark.setNotes(rs.getString("notes"));
        bookmark.setTags(deserializeTags(rs.getString("tags")));  // json -> list

        // convert sql Timestamp to java LocalDateTime
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            bookmark.setCreatedAt(createdAt.toLocalDateTime());
        }

        return bookmark;
    }

    /**
     * convert tag list to json string for storage
     * returns empty array json if list is null/empty
     */
    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            // jackson converts list to json array string
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize tags", e);
            return "[]";  // fallback to empty array on error
        }
    }

    /**
     * parse json string back to tag list
     * returns empty list if json is null/invalid
     */
    private List<String> deserializeTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // jackson parses json array to List<String>
            // TypeReference needed for generic type info
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize tags", e);
            return new ArrayList<>();  // fallback to empty list on error
        }
    }
}
