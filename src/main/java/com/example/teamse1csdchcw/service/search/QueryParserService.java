package com.example.teamse1csdchcw.service.search;

import com.example.teamse1csdchcw.domain.search.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses grep-like search query syntax into structured SearchQuery objects.
 * Supports: author:, year:, type:, site:, filetype:, AND, OR, NOT, phrases, etc.
 */
public class QueryParserService {
    private static final Logger logger = LoggerFactory.getLogger(QueryParserService.class);

    // Regex patterns for filters
    private static final Pattern PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("author:([\\w\\s]+)");
    private static final Pattern YEAR_PATTERN = Pattern.compile("year:([><]?\\d{4}(?:\\.\\.\\.?\\d{4})?)");
    private static final Pattern TYPE_PATTERN = Pattern.compile("type:(\\w+)");
    private static final Pattern SITE_PATTERN = Pattern.compile("site:([\\w.-]+)");
    private static final Pattern FILETYPE_PATTERN = Pattern.compile("filetype:(\\w+)");
    private static final Pattern AFTER_PATTERN = Pattern.compile("after:(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern BEFORE_PATTERN = Pattern.compile("before:(\\d{4}-\\d{2}-\\d{2})");

    /**
     * Parses a query string into a SearchQuery object.
     */
    public SearchQuery parse(String queryString) {
        if (queryString == null || queryString.trim().isEmpty()) {
            throw new IllegalArgumentException("Query string cannot be empty");
        }

        SearchQuery query = new SearchQuery(queryString);
        String remaining = queryString;

        // Extract phrases first
        remaining = extractPhrases(remaining, query);

        // Extract filters
        remaining = extractAuthor(remaining, query);
        remaining = extractYear(remaining, query);
        remaining = extractType(remaining, query);
        remaining = extractSite(remaining, query);
        remaining = extractFiletype(remaining, query);
        remaining = extractDateFilters(remaining, query);

        // Parse remaining terms with boolean operators
        parseTerms(remaining, query);

        logger.debug("Parsed query: {}", query);
        return query;
    }

    /**
     * Extracts quoted phrases from the query.
     */
    private String extractPhrases(String query, SearchQuery searchQuery) {
        Matcher matcher = PHRASE_PATTERN.matcher(query);
        List<String> phrases = new ArrayList<>();

        while (matcher.find()) {
            phrases.add(matcher.group(1));
        }

        searchQuery.setPhrases(phrases);
        return matcher.replaceAll("");
    }

    /**
     * Extracts author filter.
     */
    private String extractAuthor(String query, SearchQuery searchQuery) {
        Matcher matcher = AUTHOR_PATTERN.matcher(query);
        if (matcher.find()) {
            searchQuery.setAuthorFilter(matcher.group(1).trim());
            return matcher.replaceAll("");
        }
        return query;
    }

    /**
     * Extracts year filter (supports >, <, and ranges like 2020..2024).
     */
    private String extractYear(String query, SearchQuery searchQuery) {
        Matcher matcher = YEAR_PATTERN.matcher(query);
        if (matcher.find()) {
            String yearExpr = matcher.group(1);

            if (yearExpr.startsWith(">")) {
                searchQuery.setYearFrom(Integer.parseInt(yearExpr.substring(1)));
            } else if (yearExpr.startsWith("<")) {
                searchQuery.setYearTo(Integer.parseInt(yearExpr.substring(1)));
            } else if (yearExpr.contains("..")) {
                String[] parts = yearExpr.split("\\.\\.");
                searchQuery.setYearFrom(Integer.parseInt(parts[0]));
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    searchQuery.setYearTo(Integer.parseInt(parts[1]));
                }
            } else {
                // Single year
                int year = Integer.parseInt(yearExpr);
                searchQuery.setYearFrom(year);
                searchQuery.setYearTo(year);
            }

            return matcher.replaceAll("");
        }
        return query;
    }

    /**
     * Extracts document type filter.
     */
    private String extractType(String query, SearchQuery searchQuery) {
        Matcher matcher = TYPE_PATTERN.matcher(query);
        if (matcher.find()) {
            searchQuery.setTypeFilter(matcher.group(1));
            return matcher.replaceAll("");
        }
        return query;
    }

    /**
     * Extracts site filter.
     */
    private String extractSite(String query, SearchQuery searchQuery) {
        Matcher matcher = SITE_PATTERN.matcher(query);
        if (matcher.find()) {
            searchQuery.setSiteFilter(matcher.group(1));
            return matcher.replaceAll("");
        }
        return query;
    }

    /**
     * Extracts filetype filter.
     */
    private String extractFiletype(String query, SearchQuery searchQuery) {
        Matcher matcher = FILETYPE_PATTERN.matcher(query);
        if (matcher.find()) {
            searchQuery.setFiletypeFilter(matcher.group(1));
            return matcher.replaceAll("");
        }
        return query;
    }

    /**
     * Extracts date filters (after: and before:).
     */
    private String extractDateFilters(String query, SearchQuery searchQuery) {
        Matcher afterMatcher = AFTER_PATTERN.matcher(query);
        if (afterMatcher.find()) {
            searchQuery.setDateAfter(LocalDate.parse(afterMatcher.group(1)));
            query = afterMatcher.replaceAll("");
        }

        Matcher beforeMatcher = BEFORE_PATTERN.matcher(query);
        if (beforeMatcher.find()) {
            searchQuery.setDateBefore(LocalDate.parse(beforeMatcher.group(1)));
            query = beforeMatcher.replaceAll("");
        }

        return query;
    }

    /**
     * Parses remaining terms with boolean operators.
     */
    private void parseTerms(String remaining, SearchQuery searchQuery) {
        // Normalize operators
        remaining = remaining.replaceAll("\\s+AND\\s+", " +")
                          .replaceAll("\\s+&&\\s+", " +")
                          .replaceAll("\\s+OR\\s+", " | ")
                          .replaceAll("\\s+\\|\\|\\s+", " | ")
                          .replaceAll("\\s+NOT\\s+", " -")
                          .replaceAll("\\s+!\\s+", " -");

        // Split by whitespace
        String[] tokens = remaining.trim().split("\\s+");

        List<String> keywords = new ArrayList<>();
        List<String> required = new ArrayList<>();
        List<String> optional = new ArrayList<>();
        List<String> excluded = new ArrayList<>();

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            if (token.startsWith("+")) {
                required.add(token.substring(1));
            } else if (token.startsWith("-")) {
                excluded.add(token.substring(1));
            } else if (!token.equals("|")) {
                keywords.add(token);
            }
        }

        searchQuery.setKeywords(keywords);
        searchQuery.setRequiredTerms(required);
        searchQuery.setExcludedTerms(excluded);
    }

    /**
     * Validates a query string without fully parsing it.
     */
    public boolean validateQuery(String queryString) {
        try {
            SearchQuery query = parse(queryString);
            return query.validate();
        } catch (Exception e) {
            logger.warn("Invalid query: {}", queryString, e);
            return false;
        }
    }
}
