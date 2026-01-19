package com.example.teamse1csdchcw.cli;

// -- domain models for search results --
import com.example.teamse1csdchcw.domain.search.AcademicPaper;   // -- paper w/ authors, date, etc --
import com.example.teamse1csdchcw.domain.search.SearchQuery;     // -- parsed query w/ filters --
import com.example.teamse1csdchcw.domain.search.SearchResult;    // -- base result class --
import com.example.teamse1csdchcw.domain.source.SourceType;      // -- enum: arxiv, pubmed, etc --
// -- service layer for searching --
import com.example.teamse1csdchcw.service.index.LocalSearchService;     // -- lucene local search --
import com.example.teamse1csdchcw.service.search.FederatedSearchService; // -- multi-source search --
import com.example.teamse1csdchcw.service.search.QueryParserService;     // -- parse grep-like syntax --
// -- picocli annotations --
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;  // -- positional args --

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

// -- search subcommand: main feature of the app --
// -- usage: libsearch search "machine learning" -s ARXIV,PUBMED --
@Command(
        name = "search",
        description = "Search for academic papers across multiple sources"
)
public class SearchCommand implements Callable<Integer> {

    // -- @Parameters: positional arg (not a flag) --
    // -- index = position, so "search ml" → query = "ml" --
    @Parameters(index = "0", description = "Search query")
    private String query;

    // -- -s flag: which apis to search --
    // -- split="," allows -s ARXIV,PUBMED syntax --
    @Option(names = {"-s", "--sources"},
            description = "Sources to search (ARXIV, PUBMED, CROSSREF, SEMANTIC_SCHOLAR)",
            split = ",")
    private Set<SourceType> sources = new HashSet<>();  // -- default empty = all sources --

    // -- limit results per source --
    @Option(names = {"-m", "--max-results"},
            description = "Maximum number of results per source (default: 10)")
    private int maxResults = 10;

    // -- offline mode: search local lucene index only --
    @Option(names = {"-o", "--offline"},
            description = "Search offline index only")
    private boolean offline;

    // -- author filter: narrows results by author name --
    @Option(names = {"-a", "--author"},
            description = "Filter by author")
    private String author;

    // -- year filter: single year or range (2020 or 2018-2024) --
    @Option(names = {"-y", "--year"},
            description = "Filter by year (e.g., 2020 or 2018-2024)")
    private String year;

    // -- output format: table (ascii), json, or simple list --
    @Option(names = {"-f", "--format"},
            description = "Output format: table (default), json, simple")
    private String format = "table";

    // -- auto-save results to sqlite for later access --
    @Option(names = {"--save"},
            description = "Save results to database")
    private boolean save = true;

    // -- main execution method called by picocli --
    @Override
    public Integer call() throws Exception {
        // -- validate query is present --
        if (query == null || query.trim().isEmpty()) {
            System.err.println("Error: Search query is required");
            return 1;  // -- error exit code --
        }

        try {
            List<SearchResult> results;

            // -- choose online vs offline search based on flag --
            if (offline) {
                // -- search local lucene index (no network) --
                results = searchOffline();
            } else {
                // -- search remote apis (arxiv, pubmed, etc) --
                results = searchOnline();
            }

            // -- handle empty results --
            if (results.isEmpty()) {
                System.out.println("No results found.");
                return 0;  // -- not an error, just no matches --
            }

            // -- format & print results based on -f flag --
            printResults(results);

            System.out.println();
            System.out.println("Found " + results.size() + " results");

            return 0;  // -- success --

        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            return 1;  // -- error exit code --
        }
    }

    // -- search local lucene index (works w/o internet) --
    // -- papers must have been indexed previously --
    private List<SearchResult> searchOffline() throws IOException {
        System.out.println("Searching offline index...");

        // -- fedService gives us access to index --
        FederatedSearchService fedService = new FederatedSearchService();
        // -- localSearch queries the lucene index --
        LocalSearchService localSearch = new LocalSearchService(fedService.getIndexService());

        // -- parse user's query string into structured query --
        QueryParserService parser = new QueryParserService();
        SearchQuery searchQuery = parser.parse(query);

        // -- apply author filter if provided --
        if (author != null) {
            searchQuery.setAuthorFilter(author);
        }

        // -- apply year filter if provided --
        parseYearFilter(searchQuery);

        // -- execute search against lucene index --
        List<SearchResult> results = localSearch.search(searchQuery, maxResults);

        // -- cleanup resources --
        try {
            fedService.shutdown();
            localSearch.close();
        } catch (IOException e) {
            // Ignore cleanup errors
            // -- non-critical: index already searched --
        }

        return results;
    }

    // -- search remote apis in parallel --
    // -- federated = combines results from multiple sources --
    private List<SearchResult> searchOnline() throws Exception {
        // -- default to all sources if none specified --
        if (sources.isEmpty()) {
            sources.add(SourceType.ARXIV);           // -- physics/cs preprints --
            sources.add(SourceType.PUBMED);          // -- biomedical literature --
            sources.add(SourceType.CROSSREF);        // -- doi metadata --
            sources.add(SourceType.SEMANTIC_SCHOLAR); // -- ai-powered academic search --
        }

        System.out.println("Searching " + sources.size() + " sources...");

        // -- federated service handles parallel api calls --
        FederatedSearchService searchService = new FederatedSearchService();
        QueryParserService parser = new QueryParserService();

        // -- parse query text into structured query object --
        SearchQuery searchQuery = parser.parse(query);

        // -- apply filters from cli options --
        if (author != null) {
            searchQuery.setAuthorFilter(author);
        }

        parseYearFilter(searchQuery);

        // -- set limit per source (total = sources * maxResults) --
        searchService.setMaxResultsPerSource(maxResults);
        // -- execute parallel search across all sources --
        List<SearchResult> results = searchService.search(searchQuery, sources);

        // -- cleanup (close http clients, executors) --
        searchService.shutdown();

        return results;
    }

    // -- parse -y flag: handles "2020" or "2018-2024" format --
    private void parseYearFilter(SearchQuery searchQuery) {
        if (year != null) {
            if (year.contains("-")) {
                // -- range format: "2018-2024" --
                String[] parts = year.split("-");
                try {
                    searchQuery.setYearFrom(Integer.parseInt(parts[0].trim()));
                    searchQuery.setYearTo(Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid year range format: " + year);
                }
            } else {
                // -- single year: "2020" -> from=2020, to=2020 --
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

    // -- dispatch to correct formatter based on -f flag --
    private void printResults(List<SearchResult> results) {
        switch (format.toLowerCase()) {
            case "json":
                printJsonFormat(results);   // -- machine-readable --
                break;
            case "simple":
                printSimpleFormat(results); // -- readable list --
                break;
            case "table":
            default:
                printTableFormat(results);  // -- ascii table --
                break;
        }
    }

    // -- ascii table format w/ columns --
    private void printTableFormat(List<SearchResult> results) {
        System.out.println();
        // -- print header row --
        System.out.println("─".repeat(120));  // -- unicode box drawing char --
        System.out.printf("%-60s %-20s %-10s %-30s%n", "TITLE", "AUTHORS", "YEAR", "SOURCE");
        System.out.println("─".repeat(120));

        // -- print each result as a row --
        for (SearchResult result : results) {
            String title = truncate(result.getTitle(), 60);
            String authors = "";
            String year = "";
            String source = result.getSource().getDisplayName();

            // -- academicpaper has extra fields (authors, date) --
            // -- pattern matching: "instanceof Type var" --
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

    // -- simple numbered list format --
    private void printSimpleFormat(List<SearchResult> results) {
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.println((i + 1) + ". " + result.getTitle());

            // -- print extra details for academic papers --
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

    // -- json array format for scripting/piping --
    private void printJsonFormat(List<SearchResult> results) {
        System.out.println("[");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.println("  {");
            System.out.println("    \"title\": \"" + escapeJson(result.getTitle()) + "\",");
            System.out.println("    \"source\": \"" + result.getSource().name() + "\",");
            System.out.println("    \"url\": \"" + escapeJson(result.getUrl()) + "\"");

            // -- add extra fields for academic papers --
            if (result instanceof AcademicPaper paper) {
                if (paper.getAuthors() != null) {
                    System.out.println("    ,\"authors\": \"" + escapeJson(paper.getAuthors()) + "\"");
                }
                if (paper.getPublicationDate() != null) {
                    System.out.println("    ,\"year\": " + paper.getPublicationDate().getYear());
                }
            }

            System.out.print("  }");
            // -- comma between objects, not after last one --
            if (i < results.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
        System.out.println("]");
    }

    // -- truncate string w/ "..." if too long --
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    // -- escape special chars for valid json output --
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")   // -- backslash --
                .replace("\"", "\\\"")      // -- quote --
                .replace("\n", "\\n")       // -- newline --
                .replace("\r", "\\r")       // -- carriage return --
                .replace("\t", "\\t");      // -- tab --
    }
}
