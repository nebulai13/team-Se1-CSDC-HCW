package com.example.teamse1csdchcw.domain.search;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed search query with filters and operators.
 */
public class SearchQuery {
    private String originalQuery;
    private List<String> keywords;
    private List<String> requiredTerms;
    private List<String> optionalTerms;
    private List<String> excludedTerms;
    private List<String> phrases;

    // Filters
    private String authorFilter;
    private Integer yearFrom;
    private Integer yearTo;
    private String typeFilter;
    private String siteFilter;
    private String filetypeFilter;
    private LocalDate dateAfter;
    private LocalDate dateBefore;

    public SearchQuery() {
        this.keywords = new ArrayList<>();
        this.requiredTerms = new ArrayList<>();
        this.optionalTerms = new ArrayList<>();
        this.excludedTerms = new ArrayList<>();
        this.phrases = new ArrayList<>();
    }

    public SearchQuery(String originalQuery) {
        this();
        this.originalQuery = originalQuery;
    }

    /**
     * Validates the query for basic correctness.
     */
    public boolean validate() {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return false;
        }

        // Must have at least one search term
        return !keywords.isEmpty() || !requiredTerms.isEmpty() ||
               !optionalTerms.isEmpty() || !phrases.isEmpty();
    }

    /**
     * Converts the query to a search string for a specific engine.
     */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();

        // Add phrases
        for (String phrase : phrases) {
            sb.append("\"").append(phrase).append("\" ");
        }

        // Add required terms
        for (String term : requiredTerms) {
            sb.append("+").append(term).append(" ");
        }

        // Add optional terms
        for (String term : optionalTerms) {
            sb.append(term).append(" ");
        }

        // Add keywords
        for (String keyword : keywords) {
            sb.append(keyword).append(" ");
        }

        // Add excluded terms
        for (String term : excludedTerms) {
            sb.append("-").append(term).append(" ");
        }

        return sb.toString().trim();
    }

    // Getters and setters
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<String> getRequiredTerms() { return requiredTerms; }
    public void setRequiredTerms(List<String> requiredTerms) { this.requiredTerms = requiredTerms; }

    public List<String> getOptionalTerms() { return optionalTerms; }
    public void setOptionalTerms(List<String> optionalTerms) { this.optionalTerms = optionalTerms; }

    public List<String> getExcludedTerms() { return excludedTerms; }
    public void setExcludedTerms(List<String> excludedTerms) { this.excludedTerms = excludedTerms; }

    public List<String> getPhrases() { return phrases; }
    public void setPhrases(List<String> phrases) { this.phrases = phrases; }

    public String getAuthorFilter() { return authorFilter; }
    public void setAuthorFilter(String authorFilter) { this.authorFilter = authorFilter; }

    public Integer getYearFrom() { return yearFrom; }
    public void setYearFrom(Integer yearFrom) { this.yearFrom = yearFrom; }

    public Integer getYearTo() { return yearTo; }
    public void setYearTo(Integer yearTo) { this.yearTo = yearTo; }

    public String getTypeFilter() { return typeFilter; }
    public void setTypeFilter(String typeFilter) { this.typeFilter = typeFilter; }

    public String getSiteFilter() { return siteFilter; }
    public void setSiteFilter(String siteFilter) { this.siteFilter = siteFilter; }

    public String getFiletypeFilter() { return filetypeFilter; }
    public void setFiletypeFilter(String filetypeFilter) { this.filetypeFilter = filetypeFilter; }

    public LocalDate getDateAfter() { return dateAfter; }
    public void setDateAfter(LocalDate dateAfter) { this.dateAfter = dateAfter; }

    public LocalDate getDateBefore() { return dateBefore; }
    public void setDateBefore(LocalDate dateBefore) { this.dateBefore = dateBefore; }

    @Override
    public String toString() {
        return "SearchQuery{" +
                "originalQuery='" + originalQuery + '\'' +
                ", keywords=" + keywords +
                ", authorFilter='" + authorFilter + '\'' +
                ", yearRange=" + yearFrom + "-" + yearTo +
                '}';
    }
}
