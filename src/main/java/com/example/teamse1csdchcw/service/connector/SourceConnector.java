package com.example.teamse1csdchcw.service.connector;

// -- domain models --
import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
// -- custom exception for connector failures --
import com.example.teamse1csdchcw.exception.ConnectorException;

import java.util.List;

/**
 * Interface for all search source connectors.
 * Each academic database or search engine implements this interface.
 */
// -- interface = contract that implementing classes must follow --
// -- strategy pattern: different connectors, same interface --
// -- enables pluggable architecture: easy to add new sources --
// -- implementations: ArxivConnector, PubMedConnector, etc --
public interface SourceConnector {

    /**
     * Executes a search query against this source.
     *
     * @param query      The parsed search query
     * @param maxResults Maximum number of results to return
     * @return List of search results
     * @throws ConnectorException if the search fails
     */
    // -- main method: sends query to api, returns results --
    // -- each impl translates query to api-specific format --
    List<SearchResult> search(SearchQuery query, int maxResults) throws ConnectorException;

    /**
     * Gets the source type this connector handles.
     */
    // -- identifies which api this connector handles --
    SourceType getSourceType();

    /**
     * Checks if this source is currently available.
     */
    // -- health check: is the api reachable? --
    // -- used to skip unavailable sources during search --
    boolean isAvailable();

    /**
     * Gets the display name of this source.
     */
    // -- default method: interface provides impl --
    // -- java 8+ feature: default methods in interfaces --
    default String getName() {
        return getSourceType().getDisplayName();
    }

    /**
     * Gets the base URL of this source.
     */
    // -- another default method using SourceType data --
    default String getBaseUrl() {
        return getSourceType().getBaseUrl();
    }
}
