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
 * Connector for Semantic Scholar API - AI-powered research tool.
 * API Documentation: https://api.semanticscholar.org/
 *
 * Semantic Scholar provides:
 * - Comprehensive paper metadata
 * - Citation counts and influential citations
 * - Author information
 * - Links to open access PDFs
 */
public class SemanticScholarConnector implements SourceConnector {
    private static final Logger logger = LoggerFactory.getLogger(SemanticScholarConnector.class);
    private static final String API_URL = "https://api.semanticscholar.org/graph/v1/paper/search";
    private static final String FIELDS = "paperId,externalIds,title,abstract,year,authors,publicationDate,venue,citationCount,openAccessPdf,fieldsOfStudy";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 1000; // 1 request per second for free tier

    public SemanticScholarConnector() {
        this.httpClient = HttpClientFactory.getDefaultClient();
        this.objectMapper = new ObjectMapper();
        // API key is optional but recommended for higher rate limits
        this.apiKey = System.getenv("SEMANTIC_SCHOLAR_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("SEMANTIC_SCHOLAR_API_KEY not set. Rate limits will be lower.");
        }
    }

    public SemanticScholarConnector(String apiKey) {
        this.httpClient = HttpClientFactory.getDefaultClient();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
    }

    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) throws ConnectorException {
        try {
            String searchQuery = buildSemanticScholarQuery(query);

            StringBuilder url = new StringBuilder(API_URL);
            url.append("?query=").append(URLEncoder.encode(searchQuery, StandardCharsets.UTF_8));
            url.append("&fields=").append(FIELDS);
            url.append("&limit=").append(Math.min(maxResults, 100)); // API max is 100

            // Add year filter if available
            if (query.getYearFrom() != null) {
                url.append("&year=").append(query.getYearFrom()).append("-");
            }
            if (query.getYearTo() != null) {
                if (query.getYearFrom() == null) {
                    url.append("&year=-");
                }
                url.append(query.getYearTo());
            }

            logger.debug("Semantic Scholar search URL: {}", url);

            // Implement rate limiting
            synchronized (this) {
                long now = System.currentTimeMillis();
                long timeSinceLastRequest = now - lastRequestTime;
                if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                    long sleepTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest;
                    logger.debug("Rate limiting: sleeping for {}ms", sleepTime);
                    Thread.sleep(sleepTime);
                }
                lastRequestTime = System.currentTimeMillis();
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url.toString())
                    .get();

            // Add API key if available
            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("x-api-key", apiKey);
            }

            Request request = requestBuilder.build();

            // Retry logic with exponential backoff for 429 errors
            int maxRetries = 3;
            int retryCount = 0;
            long backoffMs = 2000; // Start with 2 seconds

            while (retryCount < maxRetries) {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.code() == 429) {
                        // Rate limited - backoff and retry
                        retryCount++;
                        if (retryCount >= maxRetries) {
                            logger.warn("Semantic Scholar rate limit exceeded after {} retries", maxRetries);
                            throw new ConnectorException("Semantic Scholar API returned: " + response.code());
                        }

                        logger.info("Rate limited by Semantic Scholar, backing off for {}ms (attempt {}/{})",
                                backoffMs, retryCount, maxRetries);
                        Thread.sleep(backoffMs);
                        backoffMs *= 2; // Exponential backoff
                        continue;
                    }

                    if (!response.isSuccessful()) {
                        throw new ConnectorException("Semantic Scholar API returned: " + response.code());
                    }

                    String jsonContent = response.body().string();
                    return parseJsonResponse(jsonContent);
                }
            }

            // If we exhausted retries
            throw new ConnectorException("Semantic Scholar rate limit exceeded after retries");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Semantic Scholar search interrupted", e);
            throw new ConnectorException("Semantic Scholar search interrupted", e);
        } catch (IOException e) {
            logger.error("Semantic Scholar search failed", e);
            throw new ConnectorException("Semantic Scholar search failed", e);
        }
    }

    /**
     * Build Semantic Scholar query from SearchQuery object.
     */
    private String buildSemanticScholarQuery(SearchQuery query) {
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
            sb.append("machine learning"); // Default query since API doesn't support "*"
        }

        return sb.toString();
    }

    /**
     * Parse Semantic Scholar JSON response into search results.
     */
    private List<SearchResult> parseJsonResponse(String json) {
        List<SearchResult> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");

            if (data == null || !data.isArray()) {
                logger.warn("No data found in Semantic Scholar response");
                return results;
            }

            for (JsonNode item : data) {
                AcademicPaper paper = new AcademicPaper();
                paper.setId(UUID.randomUUID().toString());
                paper.setSource(SourceType.SEMANTIC_SCHOLAR);

                // Extract paper ID
                JsonNode paperIdNode = item.get("paperId");
                if (paperIdNode != null) {
                    String paperId = paperIdNode.asText();
                    paper.setUrl("https://www.semanticscholar.org/paper/" + paperId);
                }

                // Extract external IDs (DOI, ArXiv, etc.)
                JsonNode externalIds = item.get("externalIds");
                if (externalIds != null) {
                    if (externalIds.has("DOI")) {
                        paper.setDoi(externalIds.get("DOI").asText());
                    }
                    if (externalIds.has("ArXiv")) {
                        paper.setArxivId(externalIds.get("ArXiv").asText());
                    }
                    if (externalIds.has("PubMed")) {
                        paper.setPmid(externalIds.get("PubMed").asText());
                    }
                }

                // Extract title
                JsonNode titleNode = item.get("title");
                if (titleNode != null) {
                    paper.setTitle(titleNode.asText());
                }

                // Extract abstract
                JsonNode abstractNode = item.get("abstract");
                if (abstractNode != null && !abstractNode.isNull()) {
                    String abstractText = abstractNode.asText();
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

                // Extract authors
                JsonNode authorsNode = item.get("authors");
                if (authorsNode != null && authorsNode.isArray()) {
                    List<String> authors = new ArrayList<>();
                    for (JsonNode author : authorsNode) {
                        if (author.has("name")) {
                            authors.add(author.get("name").asText());
                        }
                    }
                    paper.setAuthors(String.join(", ", authors));
                }

                // Extract publication date
                JsonNode dateNode = item.get("publicationDate");
                if (dateNode != null && !dateNode.isNull()) {
                    try {
                        String dateStr = dateNode.asText();
                        LocalDate date = LocalDate.parse(dateStr);
                        paper.setPublicationDate(date);
                    } catch (Exception e) {
                        // Try year only
                        JsonNode yearNode = item.get("year");
                        if (yearNode != null && !yearNode.isNull()) {
                            try {
                                int year = yearNode.asInt();
                                paper.setPublicationDate(LocalDate.of(year, 1, 1));
                            } catch (Exception ex) {
                                logger.warn("Failed to parse Semantic Scholar date");
                            }
                        }
                    }
                } else {
                    // Use year field
                    JsonNode yearNode = item.get("year");
                    if (yearNode != null && !yearNode.isNull()) {
                        try {
                            int year = yearNode.asInt();
                            paper.setPublicationDate(LocalDate.of(year, 1, 1));
                        } catch (Exception e) {
                            logger.warn("Failed to parse Semantic Scholar year");
                        }
                    }
                }

                // Extract venue
                JsonNode venueNode = item.get("venue");
                if (venueNode != null && !venueNode.isNull()) {
                    String venue = venueNode.asText();
                    if (!venue.isEmpty()) {
                        paper.setVenue(venue);
                        // Also set as journal if it looks like a journal
                        if (venue.toLowerCase().contains("journal") ||
                            venue.toLowerCase().contains("proceedings")) {
                            paper.setJournal(venue);
                        }
                    }
                }

                // Extract citation count
                JsonNode citationNode = item.get("citationCount");
                if (citationNode != null && !citationNode.isNull()) {
                    paper.setCitationCount(citationNode.asInt());
                }

                // Extract fields of study (keywords)
                JsonNode fieldsNode = item.get("fieldsOfStudy");
                if (fieldsNode != null && fieldsNode.isArray()) {
                    List<String> keywords = new ArrayList<>();
                    for (JsonNode field : fieldsNode) {
                        keywords.add(field.asText());
                    }
                    paper.setKeywords(keywords);
                }

                // Extract open access PDF
                JsonNode openAccessNode = item.get("openAccessPdf");
                if (openAccessNode != null && !openAccessNode.isNull()) {
                    if (openAccessNode.has("url")) {
                        String pdfUrl = openAccessNode.get("url").asText();
                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            paper.setPdfUrl(pdfUrl);
                        }
                    }
                }

                results.add(paper);
            }

            logger.info("Parsed {} results from Semantic Scholar", results.size());

        } catch (Exception e) {
            logger.error("Failed to parse Semantic Scholar JSON", e);
        }

        return results;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.SEMANTIC_SCHOLAR;
    }

    @Override
    public boolean isAvailable() {
        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .url("https://api.semanticscholar.org/graph/v1/paper/search?query=test&limit=1")
                    .get();

            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("x-api-key", apiKey);
            }

            Request request = requestBuilder.build();

            try (Response response = httpClient.newCall(request).execute()) {
                // Consider available if we get 200 OK or 429 Rate Limited
                // 429 means the API is working, just rate limited
                return response.isSuccessful() || response.code() == 429;
            }
        } catch (Exception e) {
            logger.debug("Semantic Scholar availability check failed: {}", e.getMessage());
            // Return true by default - assume available unless proven otherwise
            return true;
        }
    }
}
