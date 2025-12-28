package com.example.teamse1csdchcw.service.connector;

import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.exception.ConnectorException;
import com.example.teamse1csdchcw.util.http.HttpClientFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
public class ArxivConnector implements SourceConnector {
    private static final Logger logger = LoggerFactory.getLogger(ArxivConnector.class);
    private static final String API_URL = "http://export.arxiv.org/api/query";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    private final OkHttpClient httpClient;

    public ArxivConnector() {
        this.httpClient = HttpClientFactory.getDefaultClient();
    }

    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) throws ConnectorException {
        try {
            String searchQuery = buildArxivQuery(query);
            String url = String.format("%s?search_query=%s&start=0&max_results=%d",
                    API_URL,
                    URLEncoder.encode(searchQuery, StandardCharsets.UTF_8),
                    maxResults);

            logger.debug("arXiv search URL: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new ConnectorException("arXiv API returned: " + response.code());
                }

                String xmlContent = response.body().string();
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
    private String buildArxivQuery(SearchQuery query) {
        StringBuilder sb = new StringBuilder();

        // Add keywords
        if (!query.getKeywords().isEmpty()) {
            sb.append("all:").append(String.join(" ", query.getKeywords()));
        }

        // Add phrases
        for (String phrase : query.getPhrases()) {
            if (sb.length() > 0) sb.append(" AND ");
            sb.append("all:\"").append(phrase).append("\"");
        }

        // Add author filter
        if (query.getAuthorFilter() != null) {
            if (sb.length() > 0) sb.append(" AND ");
            sb.append("au:").append(query.getAuthorFilter());
        }

        // If query is still empty, search for everything
        if (sb.length() == 0) {
            sb.append("all:*");
        }

        return sb.toString();
    }

    /**
     * Parses arXiv Atom/XML feed into search results.
     */
    private List<SearchResult> parseAtomFeed(String xml) {
        List<SearchResult> results = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
            Elements entries = doc.select("entry");

            for (Element entry : entries) {
                AcademicPaper paper = new AcademicPaper();
                paper.setId(UUID.randomUUID().toString());
                paper.setSource(SourceType.ARXIV);

                // Extract basic fields
                String title = entry.select("title").text();
                paper.setTitle(title.replaceAll("\\s+", " ").trim());

                String summary = entry.select("summary").text();
                paper.setAbstractText(summary.replaceAll("\\s+", " ").trim());
                paper.setSnippet(summary.length() > 200 ?
                        summary.substring(0, 200) + "..." : summary);

                // Extract authors
                Elements authorElements = entry.select("author > name");
                List<String> authors = authorElements.eachText();
                paper.setAuthors(String.join(", ", authors));

                // Extract URLs
                Elements links = entry.select("link");
                for (Element link : links) {
                    String rel = link.attr("rel");
                    String href = link.attr("href");

                    if ("alternate".equals(rel)) {
                        paper.setUrl(href);
                    } else if ("related".equals(rel) && link.attr("title").equals("pdf")) {
                        paper.setPdfUrl(href);
                    }
                }

                // Extract arXiv ID
                String id = entry.select("id").text();
                if (id.contains("arxiv.org/abs/")) {
                    String arxivId = id.substring(id.lastIndexOf("/") + 1);
                    paper.setArxivId(arxivId);
                }

                // Extract publication date
                String published = entry.select("published").text();
                if (!published.isEmpty()) {
                    try {
                        LocalDate date = LocalDate.parse(published.substring(0, 10));
                        paper.setPublicationDate(date);
                    } catch (Exception e) {
                        logger.warn("Failed to parse date: {}", published);
                    }
                }

                // Extract categories as keywords
                Elements categories = entry.select("category");
                List<String> keywords = new ArrayList<>();
                for (Element cat : categories) {
                    keywords.add(cat.attr("term"));
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
