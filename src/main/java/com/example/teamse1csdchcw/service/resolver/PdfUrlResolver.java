package com.example.teamse1csdchcw.service.resolver;

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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfUrlResolver {
    private static final Logger logger = LoggerFactory.getLogger(PdfUrlResolver.class);
    private static final String UNPAYWALL_API = "https://api.unpaywall.org/v2/";
    private static final String UNPAYWALL_EMAIL = "your-email@example.com"; // Required by Unpaywall

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PdfUrlResolver() {
        this.httpClient = HttpClientFactory.getDefaultClient();
        this.objectMapper = new ObjectMapper();
    }

    public PdfUrlResolver(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Attempts to resolve a DOI URL to an actual PDF download link.
     * Returns "UNRESOLVED:" prefix + original URL if resolution fails.
     */
    public String resolvePdfUrl(String url, String doi) {
        if (!isDOILink(url)) {
            return url;
        }

        logger.info("Attempting to resolve DOI to PDF: {}", doi);

        Optional<String> pdfUrl = tryUnpaywall(doi)
            .or(() -> tryPreprints(doi))
            .or(() -> tryPublisherParsing(url));

        if (pdfUrl.isPresent()) {
            logger.info("Resolved DOI to PDF URL: {}", pdfUrl.get());
            return pdfUrl.get();
        }

        logger.warn("Could not resolve DOI to PDF: {}", doi);
        return "UNRESOLVED:" + url;
    }

    private boolean isDOILink(String url) {
        return url != null && url.contains("doi.org/");
    }

    private Optional<String> tryUnpaywall(String doi) {
        try {
            String encodedDoi = URLEncoder.encode(doi, StandardCharsets.UTF_8);
            String url = UNPAYWALL_API + encodedDoi + "?email=" + UNPAYWALL_EMAIL;

            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonNode root = objectMapper.readTree(json);

                    JsonNode bestOa = root.get("best_oa_location");
                    if (bestOa != null) {
                        JsonNode pdfUrlNode = bestOa.get("url_for_pdf");
                        if (pdfUrlNode != null && !pdfUrlNode.isNull()) {
                            String pdfUrl = pdfUrlNode.asText();
                            logger.info("Found PDF via Unpaywall: {}", pdfUrl);
                            return Optional.of(pdfUrl);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Unpaywall lookup failed for DOI: {}", doi, e);
        }

        return Optional.empty();
    }

    private Optional<String> tryPreprints(String doi) {
        if (doi.toLowerCase().contains("arxiv")) {
            Pattern arxivPattern = Pattern.compile("(\\d{4}\\.\\d{4,5})");
            Matcher matcher = arxivPattern.matcher(doi);
            if (matcher.find()) {
                String arxivId = matcher.group(1);
                String pdfUrl = "https://arxiv.org/pdf/" + arxivId + ".pdf";
                logger.info("Found ArXiv PDF: {}", pdfUrl);
                return Optional.of(pdfUrl);
            }
        }

        if (doi.contains("biorxiv") || doi.contains("medrxiv")) {
            String pdfUrl = "https://www.biorxiv.org/content/" + doi + ".full.pdf";
            logger.info("Found bioRxiv/medRxiv PDF: {}", pdfUrl);
            return Optional.of(pdfUrl);
        }

        return Optional.empty();
    }

    private Optional<String> tryPublisherParsing(String doiUrl) {
        try {
            Request request = new Request.Builder()
                .url(doiUrl)
                .header("User-Agent", "Mozilla/5.0 (compatible; LibSearch/1.0)")
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String html = response.body().string();

                    String[] patterns = {
                        "href=\"([^\"]*\\.pdf)\"",
                        "data-article-pdf=\"([^\"]+)\"",
                        "\"pdfUrl\"\\s*:\\s*\"([^\"]+)\"",
                        "class=\"pdf-download\"[^>]*href=\"([^\"]+)\""
                    };

                    for (String patternStr : patterns) {
                        Pattern pattern = Pattern.compile(patternStr);
                        Matcher matcher = pattern.matcher(html);
                        if (matcher.find()) {
                            String pdfUrl = matcher.group(1);

                            if (pdfUrl.startsWith("/")) {
                                String baseUrl = response.request().url().scheme() + "://" +
                                                response.request().url().host();
                                pdfUrl = baseUrl + pdfUrl;
                            }

                            logger.info("Found PDF via publisher parsing: {}", pdfUrl);
                            return Optional.of(pdfUrl);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Publisher page parsing failed", e);
        }

        return Optional.empty();
    }
}