package com.example.teamse1csdchcw.service.connector;

import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.exception.ConnectorException;

import java.util.List;

/**
 * Interface for all search source connectors.
 * Each academic database or search engine implements this interface.
 */
public interface SourceConnector {

    /**
     * Executes a search query against this source.
     *
     * @param query      The parsed search query
     * @param maxResults Maximum number of results to return
     * @return List of search results
     * @throws ConnectorException if the search fails
     */
    List<SearchResult> search(SearchQuery query, int maxResults) throws ConnectorException;

    /**
     * Gets the source type this connector handles.
     */
    SourceType getSourceType();

    /**
     * Checks if this source is currently available.
     */
    boolean isAvailable();

    /**
     * Gets the display name of this source.
     */
    default String getName() {
        return getSourceType().getDisplayName();
    }

    /**
     * Gets the base URL of this source.
     */
    default String getBaseUrl() {
        return getSourceType().getBaseUrl();
    }
}
