package com.example.teamse1csdchcw.domain.search;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed search query with filters and operators.
 */
public class SearchQuery {
    // orig user input - stored for ref/logging purposes
    private String originalQuery;

    // basic search terms - standard words w/o special operators
    private List<String> keywords;

    // must appear in results - marked w/ '+' prefix
    private List<String> requiredTerms;

    // nice to have - boost relevance score if present
    private List<String> optionalTerms;

    // must NOT appear - marked w/ '-' prefix to filter out unwanted results
    private List<String> excludedTerms;

    // exact phrase matches - wrapped in quotes "like this"
    private List<String> phrases;

    // filters applied to narrow down results
    private String authorFilter;      // search by specific author name
    private Integer yearFrom;         // earliest pub year to include
    private Integer yearTo;           // latest pub year to include
    private String typeFilter;        // doc type filter (article/book/etc)
    private String siteFilter;        // limit to specific domain/site
    private String filetypeFilter;    // restrict to file ext (pdf/doc/etc)
    private LocalDate dateAfter;      // indexed after this date
    private LocalDate dateBefore;     // indexed before this date

    // default constructor - init all lists to prevent null ptr exceptions
    public SearchQuery() {
        this.keywords = new ArrayList<>();
        this.requiredTerms = new ArrayList<>();
        this.optionalTerms = new ArrayList<>();
        this.excludedTerms = new ArrayList<>();
        this.phrases = new ArrayList<>();
    }

    // convenience constructor - accepts raw user query str
    public SearchQuery(String originalQuery) {
        this();  // call default to init lists
        this.originalQuery = originalQuery;
    }

    /**
     * Validates the query for basic correctness.
     */
    public boolean validate() {
        // reject null/empty queries
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return false;
        }

        // must have at least one search criterion - otherwise nothing to search for
        return !keywords.isEmpty() || !requiredTerms.isEmpty() ||
               !optionalTerms.isEmpty() || !phrases.isEmpty();
    }

    /**
     * Converts the query to a search string for a specific engine.
     */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();

        // add phrases first - highest precedence w/ quotes
        for (String phrase : phrases) {
            sb.append("\"").append(phrase).append("\" ");
        }

        // add required terms - prefix w/ + to enforce presence
        for (String term : requiredTerms) {
            sb.append("+").append(term).append(" ");
        }

        // add optional terms - no operator, treated as should-match
        for (String term : optionalTerms) {
            sb.append(term).append(" ");
        }

        // add basic keywords - similar to optional but diff semantic meaning
        for (String keyword : keywords) {
            sb.append(keyword).append(" ");
        }

        // add excluded terms last - prefix w/ - to filter out
        for (String term : excludedTerms) {
            sb.append("-").append(term).append(" ");
        }

        // trim trailing space and return formatted query str
        return sb.toString().trim();
    }

    // getters/setters - standard java bean pattern for property access
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

    // toString for debug/logging - shows key props of query obj
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
