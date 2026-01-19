package com.example.teamse1csdchcw.domain.search;

// -- enum defining which api source result came from --
import com.example.teamse1csdchcw.domain.source.SourceType;
// -- java 8+ date/time api --
import java.time.LocalDateTime;
// -- for equals/hashCode impl --
import java.util.Objects;

/**
 * Base class representing a search result from any source.
 */
// -- pojo (plain old java object) for search results --
// -- base class: academicpaper extends this with more fields --
// -- follows javabean pattern: private fields + getters/setters --
public class SearchResult {
    // -- unique id (uuid) for db storage & deduplication --
    private String id;
    // -- paper/article title --
    private String title;
    // -- author names (comma-separated string) --
    private String authors;
    // -- link to full paper --
    private String url;
    // -- short preview text (abstract excerpt) --
    private String snippet;
    // -- which api this came from: arxiv, pubmed, etc --
    private SourceType source;
    // -- open access vs licensed vs restricted --
    private AccessLevel accessLevel;
    // -- when result was retrieved --
    private LocalDateTime timestamp;
    // -- search relevance score 0.0-1.0 --
    private double relevance;

    // -- default constructor sets timestamp & access level --
    public SearchResult() {
        this.timestamp = LocalDateTime.now();
        this.accessLevel = AccessLevel.UNKNOWN;
    }

    // -- convenience constructor for common fields --
    public SearchResult(String id, String title, String authors, String url,
                       String snippet, SourceType source) {
        this();  // -- call default constructor first --
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.url = url;
        this.snippet = snippet;
        this.source = source;
    }

    // Getters and setters
    // -- javabean pattern: private field + public getter/setter --
    // -- required for javafx property binding & orm frameworks --
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

    // -- equals/hashCode for collections & deduplication --
    // -- two results are equal if same id OR same url --
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;  // -- same object ref --
        if (!(o instanceof SearchResult)) return false;
        SearchResult that = (SearchResult) o;
        return Objects.equals(id, that.id) && Objects.equals(url, that.url);
    }

    // -- hashCode must be consistent w/ equals --
    @Override
    public int hashCode() {
        return Objects.hash(id, url);
    }

    // -- toString for debugging/logging --
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
