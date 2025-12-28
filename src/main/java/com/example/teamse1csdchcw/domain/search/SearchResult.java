package com.example.teamse1csdchcw.domain.search;

import com.example.teamse1csdchcw.domain.source.SourceType;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class representing a search result from any source.
 */
public class SearchResult {
    private String id;
    private String title;
    private String authors;
    private String url;
    private String snippet;
    private SourceType source;
    private AccessLevel accessLevel;
    private LocalDateTime timestamp;
    private double relevance;

    public SearchResult() {
        this.timestamp = LocalDateTime.now();
        this.accessLevel = AccessLevel.UNKNOWN;
    }

    public SearchResult(String id, String title, String authors, String url,
                       String snippet, SourceType source) {
        this();
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.url = url;
        this.snippet = snippet;
        this.source = source;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public SourceType getSource() { return source; }
    public void setSource(SourceType source) { this.source = source; }

    public AccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(AccessLevel accessLevel) { this.accessLevel = accessLevel; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getRelevance() { return relevance; }
    public void setRelevance(double relevance) { this.relevance = relevance; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchResult)) return false;
        SearchResult that = (SearchResult) o;
        return Objects.equals(id, that.id) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url);
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "title='" + title + '\'' +
                ", authors='" + authors + '\'' +
                ", source=" + source +
                ", accessLevel=" + accessLevel +
                '}';
    }
}
