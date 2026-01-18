package com.example.teamse1csdchcw.domain.source;

/**
 * Enum representing different academic and research source types.
 */
public enum SourceType {
    // Primary academic sources
    ARXIV("arXiv", "https://arxiv.org"),
    PUBMED("PubMed", "https://pubmed.ncbi.nlm.nih.gov"),
    CROSSREF("CrossRef", "https://www.crossref.org"),
    GOOGLE_SCHOLAR("Google Scholar", "https://scholar.google.com"),
    SEMANTIC_SCHOLAR("Semantic Scholar", "https://www.semanticscholar.org"),

    // Institutional sources
    PRIMO("Primo", ""),
    OBV("OBV Network", ""),

    // General search engines
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com"),
    BING("Bing", "https://www.bing.com"),
    BRAVE("Brave Search", "https://search.brave.com"),

    // Specialized sources
    IEEE("IEEE Xplore", "https://ieeexplore.ieee.org"),
    WIKIPEDIA("Wikipedia", "https://www.wikipedia.org"),
    GITHUB("GitHub", "https://github.com"),
    STACKOVERFLOW("StackOverflow", "https://stackoverflow.com"),
    REDDIT("Reddit", "https://www.reddit.com");

    private final String displayName;
    private final String baseUrl;

    SourceType(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
