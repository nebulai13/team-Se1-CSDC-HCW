package com.example.teamse1csdchcw.service.connector;

import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.exception.ConnectorException;
import com.example.teamse1csdchcw.util.http.HttpClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Connector for CrossRef API - scholarly metadata database.
 * API Documentation: https://api.crossref.org/swagger-ui/index.html
 *
 * CrossRef provides metadata for millions of scholarly works including:
 * - DOIs, titles, authors
 * - Publication dates, journals
 * - Citation counts
 * - Links to full text (when available)
 */
public class CrossRefConnector implements SourceConnector {
    private static final Logger logger = LoggerFactory.getLogger(CrossRefConnector.class);
    private static final String API_URL = "https://api.crossref.org/works";
    private static final String USER_AGENT = "LibSearch/1.0 (https://github.com/libsearch; mailto:support@example.com)";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrossRefConnector() {
        this.httpClient = HttpClientFactory.getDefaultClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) throws ConnectorException {
        try {
            String searchQuery = buildCrossRefQuery(query);

            StringBuilder url = new StringBuilder(API_URL);
            url.append("?query=").append(URLEncoder.encode(searchQuery, StandardCharsets.UTF_8));
            url.append("&rows=").append(maxResults);
            url.append("&select=DOI,title,author,published,publisher,container-title,abstract,type,is-referenced-by-count,link");

            // Add filters if available
            if (query.getAuthorFilter() != null) {
                url.append("&query.author=").append(URLEncoder.encode(query.getAuthorFilter(), StandardCharsets.UTF_8));
            }

            if (query.getYearFrom() != null || query.getYearTo() != null) {
                if (query.getYearFrom() != null) {
                    url.append("&filter=from-pub-date:").append(query.getYearFrom());
                }
                if (query.getYearTo() != null) {
                    url.append(",until-pub-date:").append(query.getYearTo());
                }
            }

            logger.debug("CrossRef search URL: {}", url);

            Request request = new Request.Builder()
                    .url(url.toString())
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new ConnectorException("CrossRef API returned: " + response.code());
                }

                String jsonContent = response.body().string();
                return parseJsonResponse(jsonContent);
            }

        } catch (IOException e) {
            logger.error("CrossRef search failed", e);
            throw new ConnectorException("CrossRef search failed", e);
        }
    }

    /**
     * Build CrossRef query from SearchQuery object.
     */
    private String buildCrossRefQuery(SearchQuery query) {
        StringBuilder sb = new StringBuilder();

        // Add keywords
        if (!query.getKeywords().isEmpty()) {
            sb.append(String.join(" ", query.getKeywords()));
        }

        // Add phrases
        for (String phrase : query.getPhrases()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("\"").append(phrase).append("\"");
        }

        // If query is still empty, search for everything
        if (sb.length() == 0) {
            sb.append("*");
        }

        return sb.toString();
    }

    /**
     * Parse CrossRef JSON response into search results.
     */
    private List<SearchResult> parseJsonResponse(String json) {
        List<SearchResult> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode message = root.get("message");

            if (message == null) {
                logger.warn("No 'message' field in CrossRef response");
                return results;
            }

            JsonNode items = message.get("items");
            if (items == null || !items.isArray()) {
                logger.warn("No items found in CrossRef response");
                return results;
            }

            for (JsonNode item : items) {
                AcademicPaper paper = new AcademicPaper();
                paper.setId(UUID.randomUUID().toString());
                paper.setSource(SourceType.CROSSREF);

                // Extract DOI
                JsonNode doiNode = item.get("DOI");
                if (doiNode != null) {
                    String doi = doiNode.asText();
                    paper.setDoi(doi);
                    paper.setUrl("https://doi.org/" + doi);
                    // Try to construct PDF URL
                    paper.setPdfUrl("https://doi.org/" + doi);
                }

                // Extract title
                JsonNode titleNode = item.get("title");
                if (titleNode != null && titleNode.isArray() && titleNode.size() > 0) {
                    paper.setTitle(titleNode.get(0).asText());
                }

                // Extract authors
                JsonNode authorNode = item.get("author");
                if (authorNode != null && authorNode.isArray()) {
                    List<String> authors = new ArrayList<>();
                    for (JsonNode author : authorNode) {
                        String given = author.has("given") ? author.get("given").asText() : "";
                        String family = author.has("family") ? author.get("family").asText() : "";

                        if (!family.isEmpty()) {
                            authors.add(given.isEmpty() ? family : given + " " + family);
                        }
                    }
                    paper.setAuthors(String.join(", ", authors));
                }

                // Extract abstract (if available)
                JsonNode abstractNode = item.get("abstract");
                if (abstractNode != null) {
                    String abstractText = abstractNode.asText();
                    // CrossRef abstracts sometimes contain XML tags, remove them
                    abstractText = abstractText.replaceAll("<[^>]+>", "").trim();
                    paper.setAbstractText(abstractText);
                    paper.setSnippet(abstractText.length() > 200 ?
                            abstractText.substring(0, 200) + "..." : abstractText);
                } else {
                    // Use title as snippet if no abstract
                    String title = paper.getTitle();
                    if (title != null) {
                        paper.setSnippet(title.length() > 200 ?
                                title.substring(0, 200) + "..." : title);
                    }
                }

                // Extract publication date
                JsonNode publishedNode = item.get("published");
                if (publishedNode == null) {
                    publishedNode = item.get("published-print");
                }
                if (publishedNode == null) {
                    publishedNode = item.get("published-online");
                }

                if (publishedNode != null) {
                    JsonNode dateParts = publishedNode.get("date-parts");
                    if (dateParts != null && dateParts.isArray() && dateParts.size() > 0) {
                        JsonNode dateArray = dateParts.get(0);
                        if (dateArray != null && dateArray.isArray() && dateArray.size() > 0) {
                            try {
                                int year = dateArray.get(0).asInt();
                                int month = dateArray.size() > 1 ? dateArray.get(1).asInt() : 1;
                                int day = dateArray.size() > 2 ? dateArray.get(2).asInt() : 1;

                                LocalDate date = LocalDate.of(year, month, day);
                                paper.setPublicationDate(date);
                            } catch (Exception e) {
                                logger.warn("Failed to parse CrossRef date: {}", dateParts);
                            }
                        }
                    }
                }

                // Extract journal/container title
                JsonNode containerNode = item.get("container-title");
                if (containerNode != null && containerNode.isArray() && containerNode.size() > 0) {
                    paper.setJournal(containerNode.get(0).asText());
                }

                // Extract publisher
                JsonNode publisherNode = item.get("publisher");
                if (publisherNode != null) {
                    // Store publisher in venue field if journal is not set
                    if (paper.getJournal() == null || paper.getJournal().isEmpty()) {
                        paper.setVenue(publisherNode.asText());
                    }
                }

                // Extract citation count
                JsonNode citationNode = item.get("is-referenced-by-count");
                if (citationNode != null) {
                    paper.setCitationCount(citationNode.asInt());
                }

                // Extract type
                JsonNode typeNode = item.get("type");
                if (typeNode != null) {
                    String type = typeNode.asText();
                    // Normalize type
                    String normalizedType = normalizeType(type);
                    // Store in keywords for filtering
                    List<String> keywords = new ArrayList<>();
                    keywords.add(normalizedType);
                    paper.setKeywords(keywords);
                }

                // Extract links (for PDF if available)
                JsonNode linkNode = item.get("link");
                if (linkNode != null && linkNode.isArray()) {
                    for (JsonNode link : linkNode) {
                        String contentType = link.has("content-type") ? link.get("content-type").asText() : "";
                        String linkUrl = link.has("URL") ? link.get("URL").asText() : "";

                        if (contentType.contains("pdf") || contentType.contains("application/pdf")) {
                            paper.setPdfUrl(linkUrl);
                            break;
                        }
                    }
                }

                results.add(paper);
            }

            logger.info("Parsed {} results from CrossRef", results.size());

        } catch (Exception e) {
            logger.error("Failed to parse CrossRef JSON", e);
        }

        return results;
    }

    /**
     * Normalize CrossRef document type to standard categories.
     */
    private String normalizeType(String type) {
        if (type == null) return "Unknown";

        return switch (type.toLowerCase()) {
            case "journal-article" -> "Article";
            case "book-chapter" -> "Book Chapter";
            case "book" -> "Book";
            case "proceedings-article" -> "Conference Paper";
            case "dissertation" -> "Thesis";
            case "posted-content" -> "Preprint";
            case "report" -> "Report";
            default -> type;
        };
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.CROSSREF;
    }

    @Override
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                    .url("https://api.crossref.org/works?rows=1")
                    .header("User-Agent", USER_AGENT)
                    .head()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            logger.warn("CrossRef availability check failed", e);
            return false;
        }
    }
}
