package com.example.teamse1csdchcw.repository;

import com.example.teamse1csdchcw.domain.search.AccessLevel;
import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.repository.sqlite.SQLiteConnection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repository for search results persistence.
 */
public class SearchResultRepository {
    private static final Logger logger = LoggerFactory.getLogger(SearchResultRepository.class);
    private final ObjectMapper objectMapper;

    public SearchResultRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // For Java 8 date/time
    }

    /**
     * Save a search result.
     */
    public void save(SearchResult result, String sessionId) throws SQLException {
        String sql = """
            INSERT INTO search_results
            (id, session_id, title, authors, url, snippet, source, access_level,
             relevance, doi, arxiv_id, pmid, abstract_text, publication_date,
             journal, venue, keywords, citation_count, pdf_url, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                relevance = excluded.relevance,
                access_level = excluded.access_level,
                citation_count = excluded.citation_count,
                pdf_url = excluded.pdf_url
            """;

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, result.getId());
            stmt.setString(2, sessionId);
            stmt.setString(3, result.getTitle());
            stmt.setString(4, result.getAuthors());
            stmt.setString(5, result.getUrl());
            stmt.setString(6, result.getSnippet());
            stmt.setString(7, result.getSource().name());
            stmt.setString(8, result.getAccessLevel().name());
            stmt.setDouble(9, result.getRelevance());

            // Academic paper specific fields
            if (result instanceof AcademicPaper paper) {
                stmt.setString(10, paper.getDoi());
                stmt.setString(11, paper.getArxivId());
                stmt.setString(12, paper.getPmid());
                stmt.setString(13, paper.getAbstractText());

                if (paper.getPublicationDate() != null) {
                    stmt.setString(14, paper.getPublicationDate().toString());
                } else {
                    stmt.setNull(14, Types.VARCHAR);
                }

                stmt.setString(15, paper.getJournal());
                stmt.setString(16, paper.getVenue());

                // Serialize keywords as JSON
                if (paper.getKeywords() != null && !paper.getKeywords().isEmpty()) {
                    stmt.setString(17, objectMapper.writeValueAsString(paper.getKeywords()));
                } else {
                    stmt.setNull(17, Types.VARCHAR);
                }

                stmt.setInt(18, paper.getCitationCount());
                stmt.setString(19, paper.getPdfUrl());
            } else {
                // Set nulls for academic fields
                for (int i = 10; i <= 19; i++) {
                    stmt.setNull(i, Types.VARCHAR);
                }
            }

            stmt.setString(20, result.getTimestamp().toString());

            stmt.executeUpdate();
            logger.debug("Saved search result: {}", result.getId());
        } catch (Exception e) {
            logger.error("Failed to save search result", e);
            throw new SQLException("Failed to save search result", e);
        }
    }
    private void save(SearchResult result, String sessionId, Connection conn) throws SQLException {
        String sql = """
        INSERT INTO search_results
        (id, session_id, title, authors, url, snippet, source, access_level,
         relevance, doi, arxiv_id, pmid, abstract_text, publication_date,
         journal, venue, keywords, citation_count, pdf_url, timestamp)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            relevance = excluded.relevance,
            access_level = excluded.access_level,
            citation_count = excluded.citation_count,
            pdf_url = excluded.pdf_url
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, result.getId());
            stmt.setString(2, sessionId);
            stmt.setString(3, result.getTitle());
            stmt.setString(4, result.getAuthors());
            stmt.setString(5, result.getUrl());
            stmt.setString(6, result.getSnippet());
            stmt.setString(7, result.getSource().name());
            stmt.setString(8, result.getAccessLevel().name());
            stmt.setDouble(9, result.getRelevance());

            if (result instanceof AcademicPaper paper) {
                stmt.setString(10, paper.getDoi());
                stmt.setString(11, paper.getArxivId());
                stmt.setString(12, paper.getPmid());
                stmt.setString(13, paper.getAbstractText());

                if (paper.getPublicationDate() != null) {
                    stmt.setString(14, paper.getPublicationDate().toString());
                } else {
                    stmt.setNull(14, Types.VARCHAR);
                }

                stmt.setString(15, paper.getJournal());
                stmt.setString(16, paper.getVenue());

                if (paper.getKeywords() != null && !paper.getKeywords().isEmpty()) {
                    try {
                        stmt.setString(17, objectMapper.writeValueAsString(paper.getKeywords()));
                    } catch (Exception ex) {
                        throw new SQLException("Failed to serialize keywords", ex);
                    }
                } else {
                    stmt.setNull(17, Types.VARCHAR);
                }

                stmt.setInt(18, paper.getCitationCount());
                stmt.setString(19, paper.getPdfUrl());
            } else {
                for (int i = 10; i <= 19; i++) {
                    stmt.setNull(i, Types.VARCHAR);
                }
            }

            stmt.setString(20, result.getTimestamp().toString());
            stmt.executeUpdate();
        }
    }



    /**
     * Save multiple search results in a batch.
     */
    public void saveAll(List<SearchResult> results, String sessionId) throws SQLException {
        try (Connection conn = SQLiteConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (SearchResult result : results) {
                    save(result, sessionId, conn); // gleiche Connection!
                }

                conn.commit();
                logger.info("Saved {} search results for session {}", results.size(), sessionId);
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Failed to rollback transaction", ex);
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    /**
     * Find results by session ID.
     */
    public List<SearchResult> findBySessionId(String sessionId) throws SQLException {
        String sql = """
            SELECT * FROM search_results
            WHERE session_id = ?
            ORDER BY relevance DESC, timestamp DESC
            """;

        List<SearchResult> results = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(mapResultSet(rs));
            }

            logger.debug("Found {} results for session {}", results.size(), sessionId);
        } catch (Exception e) {
            logger.error("Failed to find results by session", e);
            throw new SQLException("Failed to find results", e);
        }

        return results;
    }

    /**
     * Find a result by ID.
     */
    public SearchResult findById(String id) throws SQLException {
        String sql = "SELECT * FROM search_results WHERE id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }

            return null;
        } catch (Exception e) {
            logger.error("Failed to find result by ID", e);
            throw new SQLException("Failed to find result", e);
        }
    }

    /**
     * Search results by DOI.
     */
    public SearchResult findByDoi(String doi) throws SQLException {
        String sql = "SELECT * FROM search_results WHERE doi = ? LIMIT 1";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, doi);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }

            return null;
        } catch (Exception e) {
            logger.error("Failed to find result by DOI", e);
            throw new SQLException("Failed to find result", e);
        }
    }

    /**
     * Delete results by session ID.
     */
    public int deleteBySessionId(String sessionId) throws SQLException {
        String sql = "DELETE FROM search_results WHERE session_id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            int deleted = stmt.executeUpdate();

            logger.info("Deleted {} results for session {}", deleted, sessionId);
            return deleted;
        }
    }

    /**
     * Get count of results for a session.
     */
    public int countBySessionId(String sessionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM search_results WHERE session_id = ?";

        try (Connection conn = SQLiteConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;
        }
    }

    /**
     * Map ResultSet to SearchResult object.
     */
    private SearchResult mapResultSet(ResultSet rs) throws SQLException {
        try {
            // Determine if it's an AcademicPaper or basic SearchResult
            String doi = rs.getString("doi");
            String arxivId = rs.getString("arxiv_id");
            String pmid = rs.getString("pmid");

            SearchResult result;

            if (doi != null || arxivId != null || pmid != null) {
                // It's an AcademicPaper
                AcademicPaper paper = new AcademicPaper();
                paper.setDoi(doi);
                paper.setArxivId(arxivId);
                paper.setPmid(pmid);
                paper.setAbstractText(rs.getString("abstract_text"));

                String pubDateStr = rs.getString("publication_date");
                if (pubDateStr != null) {
                    paper.setPublicationDate(LocalDate.parse(pubDateStr));
                }

                paper.setJournal(rs.getString("journal"));
                paper.setVenue(rs.getString("venue"));

                // Deserialize keywords
                String keywordsJson = rs.getString("keywords");
                if (keywordsJson != null) {
                    List<String> keywords = objectMapper.readValue(
                            keywordsJson,
                            new TypeReference<List<String>>() {}
                    );
                    paper.setKeywords(keywords);
                }

                paper.setCitationCount(rs.getInt("citation_count"));
                paper.setPdfUrl(rs.getString("pdf_url"));

                result = paper;
            } else {
                result = new SearchResult();
            }

            // Set common fields
            result.setId(rs.getString("id"));
            result.setTitle(rs.getString("title"));
            result.setAuthors(rs.getString("authors"));
            result.setUrl(rs.getString("url"));
            result.setSnippet(rs.getString("snippet"));
            result.setSource(SourceType.valueOf(rs.getString("source")));
            result.setAccessLevel(AccessLevel.valueOf(rs.getString("access_level")));
            result.setRelevance(rs.getDouble("relevance"));

            String timestampStr = rs.getString("timestamp");
            if (timestampStr != null) {
                result.setTimestamp(LocalDateTime.parse(timestampStr));
            }

            return result;
        } catch (Exception e) {
            logger.error("Failed to map result set", e);
            throw new SQLException("Failed to map result", e);
        }
    }
}
