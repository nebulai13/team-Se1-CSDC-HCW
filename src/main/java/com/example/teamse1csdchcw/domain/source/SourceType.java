package com.example.teamse1csdchcw.domain.source;

/**
 * Enum representing different academic and research source types.
 */
// -- enum: java's type-safe constant collection --
// -- each constant has display name & base url --
// -- enums can have fields, methods, constructors --
public enum SourceType {
    // Primary academic sources
    // -- arxiv: physics, math, cs preprints (free) --
    ARXIV("arXiv", "https://arxiv.org"),
    // -- pubmed: biomedical & life sciences literature --
    PUBMED("PubMed", "https://pubmed.ncbi.nlm.nih.gov"),
    // -- crossref: doi metadata registry for journals --
    CROSSREF("CrossRef", "https://www.crossref.org"),
    // -- google scholar: broad academic search (no api) --
    GOOGLE_SCHOLAR("Google Scholar", "https://scholar.google.com"),
    // -- semantic scholar: ai-powered academic search --
    SEMANTIC_SCHOLAR("Semantic Scholar", "https://www.semanticscholar.org"),

    // Institutional sources
    // -- primo: library discovery system --
    PRIMO("Primo", ""),
    // -- obv: austrian library network --
    OBV("OBV Network", ""),

    // General search engines
    // -- non-academic sources for broader searches --
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com"),
    BING("Bing", "https://www.bing.com"),
    BRAVE("Brave Search", "https://search.brave.com"),

    // Specialized sources
    // -- ieee: electrical engineering papers --
    IEEE("IEEE Xplore", "https://ieeexplore.ieee.org"),
    // -- reference sources --
    WIKIPEDIA("Wikipedia", "https://www.wikipedia.org"),
    GITHUB("GitHub", "https://github.com"),
    STACKOVERFLOW("StackOverflow", "https://stackoverflow.com"),
    REDDIT("Reddit", "https://www.reddit.com");

    // -- enum fields: each constant has these values --
    private final String displayName;  // -- human-readable name for ui --
    private final String baseUrl;      // -- api base url --

    // -- enum constructor: private by default --
    // -- called for each constant declaration above --
    SourceType(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }

    // -- getter for ui display --
    public String getDisplayName() {
        return displayName;
    }

    // -- getter for api calls --
    public String getBaseUrl() {
        return baseUrl;
    }
}
