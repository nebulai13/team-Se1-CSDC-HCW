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
// converts user query strings into structured objs - supports advanced syntax
public class QueryParserService {
    private static final Logger logger = LoggerFactory.getLogger(QueryParserService.class);

    // regex patterns for extracting special syntax from query strings
    // matches text in quotes: "exact phrase" -> captures phrase w/o quotes
    private static final Pattern PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");

    // matches author:name or author:first last -> captures author name
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("author:([\\w\\s]+)");

    // matches year:2020, year:>2020, year:<2024, year:2020..2024
    private static final Pattern YEAR_PATTERN = Pattern.compile("year:([><]?\\d{4}(?:\\.\\.\\.?\\d{4})?)");

    // matches type:article, type:book, etc -> captures doc type
    private static final Pattern TYPE_PATTERN = Pattern.compile("type:(\\w+)");

    // matches site:arxiv.org, site:pubmed.gov -> captures domain
    private static final Pattern SITE_PATTERN = Pattern.compile("site:([\\w.-]+)");

    // matches filetype:pdf, filetype:doc -> captures file extension
    private static final Pattern FILETYPE_PATTERN = Pattern.compile("filetype:(\\w+)");

    // matches after:2024-01-01 -> captures iso date
    private static final Pattern AFTER_PATTERN = Pattern.compile("after:(\\d{4}-\\d{2}-\\d{2})");

    // matches before:2024-12-31 -> captures iso date
    private static final Pattern BEFORE_PATTERN = Pattern.compile("before:(\\d{4}-\\d{2}-\\d{2})");

    /**
     * Parses a query string into a SearchQuery object.
     */
    public SearchQuery parse(String queryString) {
        // validate input - reject null/empty strings
        if (queryString == null || queryString.trim().isEmpty()) {
            throw new IllegalArgumentException("Query string cannot be empty");
        }

        // create new query obj w/ original str stored
        SearchQuery query = new SearchQuery(queryString);

        // working copy - gets progressively stripped as we extract parts
        String remaining = queryString;

        // extract in specific order - phrases first to avoid false matches
        remaining = extractPhrases(remaining, query);

        // then extract all filter syntax before parsing basic terms
        remaining = extractAuthor(remaining, query);
        remaining = extractYear(remaining, query);
        remaining = extractType(remaining, query);
        remaining = extractSite(remaining, query);
        remaining = extractFiletype(remaining, query);
        remaining = extractDateFilters(remaining, query);

        // finally parse remaining text for keywords w/ boolean ops
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

        // find all quoted strings - exact phrase matching
        while (matcher.find()) {
            phrases.add(matcher.group(1));  // group(1) = text inside quotes
        }

        searchQuery.setPhrases(phrases);
        // return query w/ phrases removed - prevents them being parsed as keywords
        return matcher.replaceAll("");
    }

    /**
     * Extracts author filter.
     */
    private String extractAuthor(String query, SearchQuery searchQuery) {
        Matcher matcher = AUTHOR_PATTERN.matcher(query);
        if (matcher.find()) {
            // trim whitespace from author name
            searchQuery.setAuthorFilter(matcher.group(1).trim());
            // remove author: syntax from query string
            return matcher.replaceAll("");
        }
        return query;  // no change if pattern not found
    }

    /**
     * Extracts year filter (supports >, <, and ranges like 2020..2024).
     */
    private String extractYear(String query, SearchQuery searchQuery) {
        Matcher matcher = YEAR_PATTERN.matcher(query);
        if (matcher.find()) {
            String yearExpr = matcher.group(1);

            // year:>2020 -> papers from 2020 onwards
            if (yearExpr.startsWith(">")) {
                searchQuery.setYearFrom(Integer.parseInt(yearExpr.substring(1)));
            }
            // year:<2024 -> papers before 2024
            else if (yearExpr.startsWith("<")) {
                searchQuery.setYearTo(Integer.parseInt(yearExpr.substring(1)));
            }
            // year:2020..2024 -> range from 2020 to 2024
            else if (yearExpr.contains("..")) {
                String[] parts = yearExpr.split("\\.\\.");
                searchQuery.setYearFrom(Integer.parseInt(parts[0]));
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    searchQuery.setYearTo(Integer.parseInt(parts[1]));
                }
            }
            // year:2020 -> exact year match
            else {
                int year = Integer.parseInt(yearExpr);
                searchQuery.setYearFrom(year);
                searchQuery.setYearTo(year);  // same start/end = exact match
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
            // type:article, type:book, type:thesis, etc
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
            // site:arxiv.org -> only search within that domain
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
            // filetype:pdf -> only pdf results
            searchQuery.setFiletypeFilter(matcher.group(1));
            return matcher.replaceAll("");
        }
        return query;
    }

    /**
     * Extracts date filters (after: and before:).
     */
    private String extractDateFilters(String query, SearchQuery searchQuery) {
        // after:YYYY-MM-DD -> indexed after this date
        Matcher afterMatcher = AFTER_PATTERN.matcher(query);
        if (afterMatcher.find()) {
            searchQuery.setDateAfter(LocalDate.parse(afterMatcher.group(1)));
            query = afterMatcher.replaceAll("");
        }

        // before:YYYY-MM-DD -> indexed before this date
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
        // normalize various boolean operator syntaxes to consistent format
        // AND/&& -> + prefix (required term)
        remaining = remaining.replaceAll("\\s+AND\\s+", " +")
                          .replaceAll("\\s+&&\\s+", " +")
                          // OR/|| -> | separator (optional term)
                          .replaceAll("\\s+OR\\s+", " | ")
                          .replaceAll("\\s+\\|\\|\\s+", " | ")
                          // NOT/! -> - prefix (excluded term)
                          .replaceAll("\\s+NOT\\s+", " -")
                          .replaceAll("\\s+!\\s+", " -");

        // split on whitespace to get individual tokens
        String[] tokens = remaining.trim().split("\\s+");

        // categorize tokens by prefix
        List<String> keywords = new ArrayList<>();   // no prefix = regular keyword
        List<String> required = new ArrayList<>();   // + prefix = must appear
        List<String> optional = new ArrayList<>();   // after | = nice to have
        List<String> excluded = new ArrayList<>();   // - prefix = must not appear

        for (String token : tokens) {
            if (token.isEmpty()) continue;  // skip empty tokens from double spaces

            // categorize based on prefix char
            if (token.startsWith("+")) {
                required.add(token.substring(1));  // strip prefix, add term
            } else if (token.startsWith("-")) {
                excluded.add(token.substring(1));
            } else if (!token.equals("|")) {  // skip pipe separators
                keywords.add(token);
            }
        }

        // populate query obj w/ categorized terms
        searchQuery.setKeywords(keywords);
        searchQuery.setRequiredTerms(required);
        searchQuery.setExcludedTerms(excluded);
    }

    /**
     * Validates a query string without fully parsing it.
     */
    public boolean validateQuery(String queryString) {
        try {
            // attempt full parse - if succeeds and validates, query is good
            SearchQuery query = parse(queryString);
            return query.validate();
        } catch (Exception e) {
            // any exception during parsing means invalid syntax
            logger.warn("Invalid query: {}", queryString, e);
            return false;
        }
    }
}
