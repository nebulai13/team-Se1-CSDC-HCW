package com.example.teamse1csdchcw.service.connector;

// -- domain models --
import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
// -- custom exception --
import com.example.teamse1csdchcw.exception.ConnectorException;
// -- http client factory --
import com.example.teamse1csdchcw.util.http.HttpClientFactory;
// -- okhttp: modern http client library --
import okhttp3.OkHttpClient;   // -- http client instance --
import okhttp3.Request;        // -- http request builder --
import okhttp3.Response;       // -- http response wrapper --
// -- jsoup: html/xml parser library --
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;   // -- parsed document --
import org.jsoup.nodes.Element;    // -- single xml element --
import org.jsoup.select.Elements;  // -- collection of elements --
// -- logging --
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Connector for arXiv.org academic preprint repository.
 * Uses arXiv API v1: https://arxiv.org/help/api/
 */
// -- arxiv = preprint server for physics, math, cs, etc --
// -- free access, no api key required --
// -- api returns atom/xml format --
public class ArxivConnector implements SourceConnector {
    private static final Logger logger = LoggerFactory.getLogger(ArxivConnector.class);
    // -- arxiv api base url --
    private static final String API_URL = "http://export.arxiv.org/api/query";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    // -- okhttp client for making http requests --
    private final OkHttpClient httpClient;

    // -- constructor: get shared http client from factory --
    public ArxivConnector() {
        this.httpClient = HttpClientFactory.getDefaultClient();
    }

    // -- implements SourceConnector.search() --
    // -- sends query to arxiv api, parses response --
    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) throws ConnectorException {
        try {
            // -- convert SearchQuery to arxiv api format --
            String searchQuery = buildArxivQuery(query);
            // -- build full api url with query params --
            // -- format: /api/query?search_query=xxx&max_results=N --
            String url = String.format("%s?search_query=%s&start=0&max_results=%d",
                    API_URL,
                    URLEncoder.encode(searchQuery, StandardCharsets.UTF_8),  // -- url-encode query --
                    maxResults);

            logger.debug("arXiv search URL: {}", url);

            // -- build http get request using okhttp builder pattern --
            Request request = new Request.Builder()
                    .url(url)
                    .get()   // -- http GET method --
                    .build();

            // -- try-with-resources: auto-closes response --
            try (Response response = httpClient.newCall(request).execute()) {
                // -- check for http errors (4xx, 5xx) --
                if (!response.isSuccessful()) {
                    throw new ConnectorException("arXiv API returned: " + response.code());
                }

                // -- read response body as string (xml) --
                String xmlContent = response.body().string();
                // -- parse xml into SearchResult objects --
                return parseAtomFeed(xmlContent);
            }

        } catch (IOException e) {
            logger.error("arXiv search failed", e);
            throw new ConnectorException("arXiv search failed", e);
        }
    }

    /**
     * Builds an arXiv API query string from the parsed query.
     */
    // -- converts SearchQuery to arxiv-specific query syntax --
    // -- arxiv uses prefix-based search: all:keywords, au:author, ti:title --
    private String buildArxivQuery(SearchQuery query) {
        StringBuilder sb = new StringBuilder();

        // Add keywords
        // -- "all:" searches all fields (title, abstract, authors) --
        if (!query.getKeywords().isEmpty()) {
            sb.append("all:").append(String.join(" ", query.getKeywords()));
        }

        // Add phrases
        // -- exact phrase search with quotes --
        for (String phrase : query.getPhrases()) {
            if (sb.length() > 0) sb.append(" AND ");
            sb.append("all:\"").append(phrase).append("\"");
        }

        // Add author filter
        // -- "au:" prefix for author search --
        if (query.getAuthorFilter() != null) {
            if (sb.length() > 0) sb.append(" AND ");
            sb.append("au:").append(query.getAuthorFilter());
        }

        // If query is still empty, search for everything
        // -- wildcard search to get recent papers --
        if (sb.length() == 0) {
            sb.append("all:*");
        }

        return sb.toString();
    }

    /**
     * Parses arXiv Atom/XML feed into search results.
     */
    // -- parses atom xml response from arxiv api --
    // -- atom = xml-based feed format (like rss) --
    private List<SearchResult> parseAtomFeed(String xml) {
        List<SearchResult> results = new ArrayList<>();

        try {
            // -- jsoup with xml parser (not html parser) --
            Document doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
            // -- select all <entry> elements (one per paper) --
            Elements entries = doc.select("entry");

            // -- iterate through each entry --
            for (Element entry : entries) {
                // -- create AcademicPaper to hold extracted data --
                AcademicPaper paper = new AcademicPaper();
                paper.setId(UUID.randomUUID().toString());  // -- generate unique id --
                paper.setSource(SourceType.ARXIV);

                // Extract basic fields
                // -- css selector syntax: select("tagname") --
                String title = entry.select("title").text();
                // -- normalize whitespace (arxiv titles often have linebreaks) --
                paper.setTitle(title.replaceAll("\\s+", " ").trim());

                // -- <summary> contains abstract --
                String summary = entry.select("summary").text();
                paper.setAbstractText(summary.replaceAll("\\s+", " ").trim());
                // -- snippet = truncated preview --
                paper.setSnippet(summary.length() > 200 ?
                        summary.substring(0, 200) + "..." : summary);

                // Extract authors
                // -- selector "author > name" = <name> inside <author> --
                Elements authorElements = entry.select("author > name");
                // -- eachText() gets text content of each element --
                List<String> authors = authorElements.eachText();
                paper.setAuthors(String.join(", ", authors));

                // Extract URLs
                // -- arxiv has multiple <link> elements --
                Elements links = entry.select("link");
                for (Element link : links) {
                    String rel = link.attr("rel");    // -- link type --
                    String href = link.attr("href");  // -- actual url --

                    if ("alternate".equals(rel)) {
                        // -- main paper url --
                        paper.setUrl(href);
                    } else if ("related".equals(rel) && link.attr("title").equals("pdf")) {
                        // -- direct pdf link --
                        paper.setPdfUrl(href);
                    }
                }

                // Extract arXiv ID
                // -- id looks like: http://arxiv.org/abs/2301.12345v1 --
                String id = entry.select("id").text();
                if (id.contains("arxiv.org/abs/")) {
                    // -- extract just the id part --
                    String arxivId = id.substring(id.lastIndexOf("/") + 1);
                    paper.setArxivId(arxivId);
                }

                // Extract publication date
                // -- <published> contains iso timestamp --
                String published = entry.select("published").text();
                if (!published.isEmpty()) {
                    try {
                        // -- parse first 10 chars (yyyy-mm-dd) --
                        LocalDate date = LocalDate.parse(published.substring(0, 10));
                        paper.setPublicationDate(date);
                    } catch (Exception e) {
                        logger.warn("Failed to parse date: {}", published);
                    }
                }

                // Extract categories as keywords
                // -- arxiv categories: cs.AI, physics.hep-th, etc --
                Elements categories = entry.select("category");
                List<String> keywords = new ArrayList<>();
                for (Element cat : categories) {
                    keywords.add(cat.attr("term"));  // -- term attr has category name --
                }
                paper.setKeywords(keywords);

                results.add(paper);
            }

            logger.info("Parsed {} results from arXiv", results.size());

        } catch (Exception e) {
            logger.error("Failed to parse arXiv XML", e);
        }

        return results;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.ARXIV;
    }

    @Override
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                    .url("https://arxiv.org")
                    .head()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            logger.warn("arXiv availability check failed", e);
            return false;
        }
    }
}
