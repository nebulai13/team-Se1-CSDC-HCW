package com.example.teamse1csdchcw;

import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.exception.SearchException;
import com.example.teamse1csdchcw.service.search.FederatedSearchService;
import com.example.teamse1csdchcw.service.search.QueryParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for federated search functionality.
 * These tests require internet connectivity and may take longer to run.
 *
 * Run with: mvn test -Dgroups=integration
 */
@Tag("integration")
public class FederatedSearchIntegrationTest {

    private FederatedSearchService searchService;
    private QueryParserService queryParser;

    @BeforeEach
    public void setUp() {
        searchService = new FederatedSearchService();
        queryParser = new QueryParserService();
    }

    @Test
    public void testSimpleSearch() throws SearchException {
        // Parse query
        SearchQuery query = queryParser.parse("machine learning");

        // Execute search on arXiv only (fastest for testing)
        Set<SourceType> sources = Set.of(SourceType.ARXIV);
        List<SearchResult> results = searchService.search(query, sources);

        // Verify results
        assertNotNull(results, "Results should not be null");
        assertFalse(results.isEmpty(), "Results should not be empty");

        System.out.println("Found " + results.size() + " results for 'machine learning'");

        // Verify result structure
        SearchResult firstResult = results.get(0);
        assertNotNull(firstResult.getTitle(), "Result should have a title");
        assertNotNull(firstResult.getSource(), "Result should have a source");
        assertEquals(SourceType.ARXIV, firstResult.getSource());

        System.out.println("First result: " + firstResult.getTitle());
    }

    @Test
    public void testAuthorFilter() throws SearchException {
        // Parse query with author filter
        SearchQuery query = queryParser.parse("neural networks author:Hinton");

        // Execute search
        Set<SourceType> sources = Set.of(SourceType.ARXIV);
        List<SearchResult> results = searchService.search(query, sources);

        // Verify
        assertNotNull(results);
        System.out.println("Found " + results.size() + " results for 'neural networks author:Hinton'");

        if (!results.isEmpty()) {
            SearchResult firstResult = results.get(0);
            System.out.println("First result: " + firstResult.getTitle());
            System.out.println("Authors: " + firstResult.getAuthors());
        }
    }

    @Test
    public void testMultipleSources() throws SearchException {
        // Parse query
        SearchQuery query = queryParser.parse("quantum computing");

        // Execute search on multiple sources
        Set<SourceType> sources = Set.of(
                SourceType.ARXIV,
                SourceType.CROSSREF
        );

        List<SearchResult> results = searchService.search(query, sources);

        // Verify results from multiple sources
        assertNotNull(results);
        System.out.println("Found " + results.size() + " results from multiple sources");

        // Count results by source
        long arxivCount = results.stream()
                .filter(r -> r.getSource() == SourceType.ARXIV)
                .count();
        long crossrefCount = results.stream()
                .filter(r -> r.getSource() == SourceType.CROSSREF)
                .count();

        System.out.println("arXiv: " + arxivCount + ", CrossRef: " + crossrefCount);
    }

    @Test
    public void testYearFilter() throws SearchException {
        // Parse query with year filter
        SearchQuery query = queryParser.parse("deep learning year:>2020");

        // Execute search
        Set<SourceType> sources = Set.of(SourceType.ARXIV);
        List<SearchResult> results = searchService.search(query, sources);

        // Verify
        assertNotNull(results);
        System.out.println("Found " + results.size() + " results for 'deep learning year:>2020'");

        // Check that year filter was parsed
        assertNotNull(query.getYearFrom());
        assertEquals(2020, query.getYearFrom());
    }
}
