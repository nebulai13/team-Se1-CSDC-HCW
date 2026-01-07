package com.example.teamse1csdchcw.service.search;

import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.exception.SearchException;
import com.example.teamse1csdchcw.service.connector.ConnectorFactory;
import com.example.teamse1csdchcw.service.connector.SourceConnector;
import com.example.teamse1csdchcw.service.index.IndexService;
import com.example.teamse1csdchcw.util.http.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Orchestrates federated search across multiple sources.
 * Executes searches in parallel with timeout and rate limiting.
 */
public class FederatedSearchService {
    private static final Logger logger = LoggerFactory.getLogger(FederatedSearchService.class);
    private static final int DEFAULT_MAX_RESULTS = 50;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_CONCURRENT = 5;

    private final ConnectorFactory connectorFactory;
    private final ResultAggregator resultAggregator;
    private final RateLimiter rateLimiter;
    private final ExecutorService executorService;
    private IndexService indexService;

    private int maxResultsPerSource = DEFAULT_MAX_RESULTS;
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private int maxConcurrentSources = DEFAULT_MAX_CONCURRENT;
    private boolean autoIndexEnabled = true;

    public FederatedSearchService() {
        this.connectorFactory = ConnectorFactory.getInstance();
        this.resultAggregator = new ResultAggregator();
        this.rateLimiter = RateLimiter.getInstance();
        this.executorService = Executors.newFixedThreadPool(DEFAULT_MAX_CONCURRENT);

        try {
            this.indexService = new IndexService();
        } catch (IOException e) {
            logger.warn("Failed to initialize IndexService, auto-indexing disabled: {}", e.getMessage());
            this.autoIndexEnabled = false;
        }
    }

    public FederatedSearchService(ConnectorFactory connectorFactory, ResultAggregator resultAggregator) {
        this.connectorFactory = connectorFactory;
        this.resultAggregator = resultAggregator;
        this.rateLimiter = RateLimiter.getInstance();
        this.executorService = Executors.newFixedThreadPool(DEFAULT_MAX_CONCURRENT);
    }

    /**
     * Search all available sources.
     *
     * @param query the search query
     * @return aggregated search results
     * @throws SearchException if search fails
     */
    public List<SearchResult> searchAll(SearchQuery query) throws SearchException {
        List<SourceConnector> connectors = connectorFactory.getDefaultAcademicConnectors();
        Set<SourceType> sourceTypes = connectors.stream()
                .map(SourceConnector::getSourceType)
                .collect(Collectors.toSet());
        return search(query, sourceTypes);
    }

    /**
     * Search specific sources.
     *
     * @param query the search query
     * @param sourceTypes the sources to search
     * @return aggregated search results
     * @throws SearchException if search fails
     */
    public List<SearchResult> search(SearchQuery query, Set<SourceType> sourceTypes) throws SearchException {
        logger.info("Starting federated search across {} sources: {}",
                sourceTypes.size(), sourceTypes);

        long startTime = System.currentTimeMillis();

        try {
            // Get connectors for requested sources
            List<SourceConnector> connectors = connectorFactory.getConnectors(sourceTypes);

            if (connectors.isEmpty()) {
                throw new SearchException("No connectors available for requested sources");
            }

            // Execute searches in parallel
            List<CompletableFuture<SearchSourceResult>> futures = new ArrayList<>();

            for (SourceConnector connector : connectors) {
                CompletableFuture<SearchSourceResult> future = CompletableFuture.supplyAsync(
                        () -> searchSource(connector, query),
                        executorService
                );
                futures.add(future);
            }

            // Wait for all searches to complete with timeout
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            try {
                allOf.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Search timeout after {} seconds, using partial results", timeoutSeconds);
                // Cancel remaining tasks
                futures.forEach(f -> f.cancel(true));
            }

            // Collect results
            List<SearchSourceResult> sourceResults = futures.stream()
                    .filter(f -> f.isDone() && !f.isCompletedExceptionally() && !f.isCancelled())
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Log results per source
            for (SearchSourceResult result : sourceResults) {
                logger.info("Source: {} - Found {} results in {}ms",
                        result.sourceType,
                        result.results.size(),
                        result.duration);
            }

            // Aggregate results
            List<SearchResult> allResults = sourceResults.stream()
                    .flatMap(r -> r.results.stream())
                    .collect(Collectors.toList());

            List<SearchResult> aggregatedResults = resultAggregator.aggregate(allResults);

            if (autoIndexEnabled && indexService != null && !aggregatedResults.isEmpty()) {
                try {
                    indexService.indexResults(aggregatedResults);
                    logger.debug("Auto-indexed {} results", aggregatedResults.size());
                } catch (IOException e) {
                    logger.error("Failed to auto-index results: {}", e.getMessage());
                }
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            logger.info("Federated search completed: {} total results from {} sources in {}ms",
                    aggregatedResults.size(),
                    sourceResults.size(),
                    totalDuration);

            return aggregatedResults;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SearchException("Search interrupted", e);
        } catch (ExecutionException e) {
            throw new SearchException("Search execution failed", e);
        } catch (Exception e) {
            throw new SearchException("Federated search failed", e);
        }
    }

    /**
     * Search a single source with error handling and rate limiting.
     */
    private SearchSourceResult searchSource(SourceConnector connector, SearchQuery query) {
        SourceType sourceType = connector.getSourceType();
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Starting search on source: {}", sourceType);

            // Rate limiting
            String domain = extractDomain(connector.getBaseUrl());
            rateLimiter.acquire(domain);

            // Execute search
            List<SearchResult> results = connector.search(query, maxResultsPerSource);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Completed search on source: {} - {} results in {}ms",
                    sourceType, results.size(), duration);

            return new SearchSourceResult(sourceType, results, duration, null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Search failed for source: {} - {}", sourceType, e.getMessage());
            return new SearchSourceResult(sourceType, Collections.emptyList(), duration, e);
        }
    }

    /**
     * Extract domain from URL for rate limiting.
     */
    private String extractDomain(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            return parsedUrl.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Get status of all connectors.
     *
     * @return map of source type to availability status
     */
    public Map<SourceType, Boolean> getSourceStatus() {
        Map<SourceType, Boolean> status = new HashMap<>();

        List<SourceConnector> connectors = connectorFactory.getAllConnectors();
        for (SourceConnector connector : connectors) {
            try {
                boolean available = connector.isAvailable();
                status.put(connector.getSourceType(), available);
            } catch (Exception e) {
                logger.warn("Error checking availability for {}: {}",
                        connector.getSourceType(), e.getMessage());
                status.put(connector.getSourceType(), false);
            }
        }

        return status;
    }

    /**
     * Configuration setters
     */
    public void setMaxResultsPerSource(int maxResultsPerSource) {
        this.maxResultsPerSource = maxResultsPerSource;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setMaxConcurrentSources(int maxConcurrentSources) {
        this.maxConcurrentSources = maxConcurrentSources;
    }

    public void setAutoIndexEnabled(boolean autoIndexEnabled) {
        this.autoIndexEnabled = autoIndexEnabled;
    }

    public boolean isAutoIndexEnabled() {
        return autoIndexEnabled;
    }

    public IndexService getIndexService() {
        return indexService;
    }

    /**
     * Shutdown the service.
     */
    public void shutdown() {
        logger.info("Shutting down FederatedSearchService");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (indexService != null) {
            try {
                indexService.close();
            } catch (IOException e) {
                logger.error("Failed to close IndexService: {}", e.getMessage());
            }
        }
    }

    /**
     * Result holder for individual source search.
     */
    private static class SearchSourceResult {
        final SourceType sourceType;
        final List<SearchResult> results;
        final long duration;
        final Exception error;

        SearchSourceResult(SourceType sourceType, List<SearchResult> results, long duration, Exception error) {
            this.sourceType = sourceType;
            this.results = results;
            this.duration = duration;
            this.error = error;
        }
    }
}
