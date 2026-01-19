package com.example.teamse1csdchcw.domain.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a saved bookmark for later reference.
 */
public class Bookmark {
    // unique id for this bookmark record in db
    private String id;

    // foreign key - links to search result this bookmarks
    private String resultId;

    // cached paper title - avoids extra lookups
    private String title;

    // link to full paper/resource
    private String url;

    // user's personal notes about this paper
    private String notes;

    // user-defined tags for organization/categorization
    private List<String> tags;

    // timestamp when bookmark was created - useful for sorting/filtering
    private LocalDateTime createdAt;

    // default constructor - auto-set creation time and init tags list
    public Bookmark() {
        this.createdAt = LocalDateTime.now();  // capture current moment
        this.tags = new ArrayList<>();          // prevent null ptr errors
    }

    // convenience constructor - common bookmark creation scenario
    public Bookmark(String id, String title, String url) {
        this();  // call default to set timestamp/tags
        this.id = id;
        this.title = title;
        this.url = url;
    }

    // getters/setters - standard java bean pattern
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
