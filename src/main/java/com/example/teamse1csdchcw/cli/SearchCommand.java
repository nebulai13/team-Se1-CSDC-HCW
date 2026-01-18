package com.example.teamse1csdchcw.cli;

import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.service.index.LocalSearchService;
import com.example.teamse1csdchcw.service.search.FederatedSearchService;
import com.example.teamse1csdchcw.service.search.QueryParserService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
        name = "search",
        description = "Search for academic papers across multiple sources"
)
public class SearchCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Search query")
    private String query;

    @Option(names = {"-s", "--sources"},
            description = "Sources to search (ARXIV, PUBMED, CROSSREF, SEMANTIC_SCHOLAR)",
            split = ",")
    private Set<SourceType> sources = new HashSet<>();

    @Option(names = {"-m", "--max-results"},
            description = "Maximum number of results per source (default: 10)")
    private int maxResults = 10;

    @Option(names = {"-o", "--offline"},
            description = "Search offline index only")
    private boolean offline;

    @Option(names = {"-a", "--author"},
            description = "Filter by author")
    private String author;

    @Option(names = {"-y", "--year"},
            description = "Filter by year (e.g., 2020 or 2018-2024)")
    private String year;

    @Option(names = {"-f", "--format"},
            description = "Output format: table (default), json, simple")
    private String format = "table";

    @Option(names = {"--save"},
            description = "Save results to database")
    private boolean save = true;

    @Override
    public Integer call() throws Exception {
        if (query == null || query.trim().isEmpty()) {
            System.err.println("Error: Search query is required");
            return 1;
        }

        try {
            List<SearchResult> results;

            if (offline) {
                results = searchOffline();
            } else {
                results = searchOnline();
            }

            if (results.isEmpty()) {
                System.out.println("No results found.");
                return 0;
            }

            printResults(results);

            System.out.println();
            System.out.println("Found " + results.size() + " results");

            return 0;

        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            return 1;
        }
    }

    private List<SearchResult> searchOffline() throws IOException {
        System.out.println("Searching offline index...");

        FederatedSearchService fedService = new FederatedSearchService();
        LocalSearchService localSearch = new LocalSearchService(fedService.getIndexService());

        QueryParserService parser = new QueryParserService();
        SearchQuery searchQuery = parser.parse(query);

        if (author != null) {
            searchQuery.setAuthorFilter(author);
        }

        parseYearFilter(searchQuery);

        List<SearchResult> results = localSearch.search(searchQuery, maxResults);

        try {
            fedService.shutdown();
            localSearch.close();
        } catch (IOException e) {
            // Ignore cleanup errors
        }

        return results;
    }

    private List<SearchResult> searchOnline() throws Exception {
        if (sources.isEmpty()) {
            sources.add(SourceType.ARXIV);
            sources.add(SourceType.PUBMED);
            sources.add(SourceType.CROSSREF);
            sources.add(SourceType.SEMANTIC_SCHOLAR);
        }

        System.out.println("Searching " + sources.size() + " sources...");

        FederatedSearchService searchService = new FederatedSearchService();
        QueryParserService parser = new QueryParserService();

        SearchQuery searchQuery = parser.parse(query);

        if (author != null) {
            searchQuery.setAuthorFilter(author);
        }

        parseYearFilter(searchQuery);

        searchService.setMaxResultsPerSource(maxResults);
        List<SearchResult> results = searchService.search(searchQuery, sources);

        searchService.shutdown();

        return results;
    }

    private void parseYearFilter(SearchQuery searchQuery) {
        if (year != null) {
            if (year.contains("-")) {
                String[] parts = year.split("-");
                try {
                    searchQuery.setYearFrom(Integer.parseInt(parts[0].trim()));
                    searchQuery.setYearTo(Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid year range format: " + year);
                }
            } else {
                try {
                    int yearInt = Integer.parseInt(year.trim());
                    searchQuery.setYearFrom(yearInt);
                    searchQuery.setYearTo(yearInt);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid year format: " + year);
                }
            }
        }
    }

    private void printResults(List<SearchResult> results) {
        switch (format.toLowerCase()) {
            case "json":
                printJsonFormat(results);
                break;
            case "simple":
                printSimpleFormat(results);
                break;
            case "table":
            default:
                printTableFormat(results);
                break;
        }
    }

    private void printTableFormat(List<SearchResult> results) {
        System.out.println();
        System.out.println("─".repeat(120));
        System.out.printf("%-60s %-20s %-10s %-30s%n", "TITLE", "AUTHORS", "YEAR", "SOURCE");
        System.out.println("─".repeat(120));

        for (SearchResult result : results) {
            String title = truncate(result.getTitle(), 60);
            String authors = "";
            String year = "";
            String source = result.getSource().getDisplayName();

            if (result instanceof AcademicPaper paper) {
                authors = truncate(paper.getAuthors(), 20);
                if (paper.getPublicationDate() != null) {
                    year = String.valueOf(paper.getPublicationDate().getYear());
                }
            }

            System.out.printf("%-60s %-20s %-10s %-30s%n", title, authors, year, source);
        }

        System.out.println("─".repeat(120));
    }

    private void printSimpleFormat(List<SearchResult> results) {
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.println((i + 1) + ". " + result.getTitle());

            if (result instanceof AcademicPaper paper) {
                if (paper.getAuthors() != null) {
                    System.out.println("   Authors: " + paper.getAuthors());
                }
                if (paper.getPublicationDate() != null) {
                    System.out.println("   Year: " + paper.getPublicationDate().getYear());
                }
                if (paper.getUrl() != null) {
                    System.out.println("   URL: " + paper.getUrl());
                }
            }

            System.out.println();
        }
    }

    private void printJsonFormat(List<SearchResult> results) {
        System.out.println("[");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.println("  {");
            System.out.println("    \"title\": \"" + escapeJson(result.getTitle()) + "\",");
            System.out.println("    \"source\": \"" + result.getSource().name() + "\",");
            System.out.println("    \"url\": \"" + escapeJson(result.getUrl()) + "\"");

            if (result instanceof AcademicPaper paper) {
                if (paper.getAuthors() != null) {
                    System.out.println("    ,\"authors\": \"" + escapeJson(paper.getAuthors()) + "\"");
                }
                if (paper.getPublicationDate() != null) {
                    System.out.println("    ,\"year\": " + paper.getPublicationDate().getYear());
                }
            }

            System.out.print("  }");
            if (i < results.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
        System.out.println("]");
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
