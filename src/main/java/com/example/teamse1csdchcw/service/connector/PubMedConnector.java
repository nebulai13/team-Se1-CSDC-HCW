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
 * Connector for PubMed (NCBI) academic database.
 * Uses NCBI E-utilities API: https://www.ncbi.nlm.nih.gov/books/NBK25501/
 *
 * API workflow:
 * 1. ESearch - search and retrieve list of PMIDs
 * 2. EFetch - fetch full records for PMIDs
 */
public class PubMedConnector implements SourceConnector {
    private static final Logger logger = LoggerFactory.getLogger(PubMedConnector.class);
    private static final String ESEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    private static final String EFETCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    private static final String DATABASE = "pubmed";
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy/MM"),
            DateTimeFormatter.ofPattern("yyyy")
    };

    private final OkHttpClient httpClient;
    private final String apiKey;

    public PubMedConnector() {
        this.httpClient = HttpClientFactory.getDefaultClient();
        // API key should be loaded from config/environment
        this.apiKey = System.getenv("PUBMED_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("PUBMED_API_KEY not set. Rate limits will be lower (3 req/sec vs 10 req/sec)");
        }
    }

    public PubMedConnector(String apiKey) {
        this.httpClient = HttpClientFactory.getDefaultClient();
        this.apiKey = apiKey;
    }

    @Override
    public List<SearchResult> search(SearchQuery query, int maxResults) throws ConnectorException {
        try {
            // Step 1: Search for PMIDs
            List<String> pmids = searchPMIDs(query, maxResults);

            if (pmids.isEmpty()) {
                logger.info("No PubMed results found for query: {}", query.getOriginalQuery());
                return new ArrayList<>();
            }

            // Step 2: Fetch full records
            return fetchRecords(pmids);

        } catch (IOException e) {
            logger.error("PubMed search failed", e);
            throw new ConnectorException("PubMed search failed", e);
        }
    }

    /**
     * Step 1: Search PubMed and retrieve list of PMIDs.
     */
    private List<String> searchPMIDs(SearchQuery query, int maxResults) throws IOException, ConnectorException {
        String searchTerm = buildPubMedQuery(query);

        StringBuilder url = new StringBuilder(ESEARCH_URL);
        url.append("?db=").append(DATABASE);
        url.append("&term=").append(URLEncoder.encode(searchTerm, StandardCharsets.UTF_8));
        url.append("&retmax=").append(maxResults);
        url.append("&retmode=xml");
        url.append("&usehistory=y");

        if (apiKey != null && !apiKey.isEmpty()) {
            url.append("&api_key=").append(apiKey);
        }

        logger.debug("PubMed ESearch URL: {}", url);

        Request request = new Request.Builder()
                .url(url.toString())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ConnectorException("PubMed ESearch API returned: " + response.code());
            }

            String xmlContent = response.body().string();
            return parsePMIDs(xmlContent);
        }
    }

    /**
     * Parse PMIDs from ESearch XML response.
     */
    private List<String> parsePMIDs(String xml) {
        List<String> pmids = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
            Elements idElements = doc.select("IdList > Id");

            for (Element idElement : idElements) {
                pmids.add(idElement.text());
            }

            logger.debug("Found {} PMIDs", pmids.size());

        } catch (Exception e) {
            logger.error("Failed to parse PubMed PMIDs", e);
        }

        return pmids;
    }

    /**
     * Step 2: Fetch full records for the given PMIDs.
     */
    private List<SearchResult> fetchRecords(List<String> pmids) throws IOException, ConnectorException {
        String pmidList = String.join(",", pmids);

        StringBuilder url = new StringBuilder(EFETCH_URL);
        url.append("?db=").append(DATABASE);
        url.append("&id=").append(pmidList);
        url.append("&retmode=xml");

        if (apiKey != null && !apiKey.isEmpty()) {
            url.append("&api_key=").append(apiKey);
        }

        logger.debug("PubMed EFetch URL: {}", url);

        Request request = new Request.Builder()
                .url(url.toString())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ConnectorException("PubMed EFetch API returned: " + response.code());
            }

            String xmlContent = response.body().string();
            return parseRecords(xmlContent);
        }
    }

    /**
     * Parse full PubMed records from EFetch XML response.
     */
    private List<SearchResult> parseRecords(String xml) {
        List<SearchResult> results = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
            Elements articles = doc.select("PubmedArticle");

            for (Element article : articles) {
                AcademicPaper paper = new AcademicPaper();
                paper.setId(UUID.randomUUID().toString());
                paper.setSource(SourceType.PUBMED);

                // Extract PMID
                String pmid = article.select("MedlineCitation > PMID").text();
                paper.setPmid(pmid);
                paper.setUrl("https://pubmed.ncbi.nlm.nih.gov/" + pmid + "/");

                // Extract title
                String title = article.select("ArticleTitle").text();
                paper.setTitle(title);

                // Extract abstract
                Elements abstractTexts = article.select("Abstract > AbstractText");
                StringBuilder abstractBuilder = new StringBuilder();
                for (Element abstractText : abstractTexts) {
                    String label = abstractText.attr("Label");
                    if (!label.isEmpty()) {
                        abstractBuilder.append(label).append(": ");
                    }
                    abstractBuilder.append(abstractText.text()).append(" ");
                }
                String abstractContent = abstractBuilder.toString().trim();
                paper.setAbstractText(abstractContent);
                paper.setSnippet(abstractContent.length() > 200 ?
                        abstractContent.substring(0, 200) + "..." : abstractContent);

                // Extract authors
                Elements authorElements = article.select("AuthorList > Author");
                List<String> authors = new ArrayList<>();
                for (Element author : authorElements) {
                    String lastName = author.select("LastName").text();
                    String foreName = author.select("ForeName").text();
                    if (!lastName.isEmpty()) {
                        authors.add(foreName.isEmpty() ? lastName : foreName + " " + lastName);
                    }
                }
                paper.setAuthors(String.join(", ", authors));

                // Extract journal
                String journal = article.select("Journal > Title").text();
                if (journal.isEmpty()) {
                    journal = article.select("Journal > ISOAbbreviation").text();
                }
                paper.setJournal(journal);

                // Extract publication date
                String year = article.select("PubDate > Year").text();
                String month = article.select("PubDate > Month").text();
                String day = article.select("PubDate > Day").text();

                if (!year.isEmpty()) {
                    try {
                        String dateString = year;
                        if (!month.isEmpty()) {
                            // Convert month name to number
                            int monthNum = parseMonth(month);
                            if (monthNum > 0) {
                                dateString += "/" + String.format("%02d", monthNum);
                                if (!day.isEmpty()) {
                                    dateString += "/" + String.format("%02d", Integer.parseInt(day));
                                }
                            }
                        }

                        LocalDate date = parseDate(dateString);
                        if (date != null) {
                            paper.setPublicationDate(date);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to parse date: {}/{}/{}", year, month, day);
                    }
                }

                // Extract DOI
                Elements articleIds = article.select("ArticleIdList > ArticleId");
                for (Element articleId : articleIds) {
                    if ("doi".equals(articleId.attr("IdType"))) {
                        paper.setDoi(articleId.text());
                        break;
                    }
                }

                // Extract keywords/MeSH terms
                Elements meshHeadings = article.select("MeshHeadingList > MeshHeading > DescriptorName");
                List<String> keywords = new ArrayList<>();
                for (Element mesh : meshHeadings) {
                    keywords.add(mesh.text());
                }
                paper.setKeywords(keywords);

                // Try to construct PDF URL if DOI exists
                if (paper.getDoi() != null && !paper.getDoi().isEmpty()) {
                    // Many publishers provide PDF at doi.org redirect
                    paper.setPdfUrl("https://doi.org/" + paper.getDoi());
                }

                results.add(paper);
            }

            logger.info("Parsed {} records from PubMed", results.size());

        } catch (Exception e) {
            logger.error("Failed to parse PubMed XML", e);
        }

        return results;
    }

    /**
     * Build PubMed query from SearchQuery object.
     */
    private String buildPubMedQuery(SearchQuery query) {
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

        // Add author filter
        if (query.getAuthorFilter() != null) {
            if (sb.length() > 0) sb.append(" AND ");
            sb.append(query.getAuthorFilter()).append("[Author]");
        }

        // Add year filter
        if (query.getYearFrom() != null || query.getYearTo() != null) {
            if (sb.length() > 0) sb.append(" AND ");

            int yearFrom = query.getYearFrom() != null ? query.getYearFrom() : 1900;
            int yearTo = query.getYearTo() != null ? query.getYearTo() : LocalDate.now().getYear();

            sb.append(yearFrom).append(":").append(yearTo).append("[pdat]");
        }

        // If query is still empty, search for everything
        if (sb.length() == 0) {
            sb.append("*");
        }

        return sb.toString();
    }

    /**
     * Parse month name or number to numeric month.
     */
    private int parseMonth(String month) {
        try {
            // Try parsing as number first
            return Integer.parseInt(month);
        } catch (NumberFormatException e) {
            // Try parsing as month name
            String monthLower = month.toLowerCase();
            return switch (monthLower) {
                case "jan", "january" -> 1;
                case "feb", "february" -> 2;
                case "mar", "march" -> 3;
                case "apr", "april" -> 4;
                case "may" -> 5;
                case "jun", "june" -> 6;
                case "jul", "july" -> 7;
                case "aug", "august" -> 8;
                case "sep", "september" -> 9;
                case "oct", "october" -> 10;
                case "nov", "november" -> 11;
                case "dec", "december" -> 12;
                default -> 0;
            };
        }
    }

    /**
     * Try parsing date with multiple formats.
     */
    private LocalDate parseDate(String dateString) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateString, formatter);
            } catch (Exception e) {
                // Try next format
            }
        }
        return null;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.PUBMED;
    }

    @Override
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                    .url("https://pubmed.ncbi.nlm.nih.gov/")
                    .head()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            logger.warn("PubMed availability check failed", e);
            return false;
        }
    }
}
