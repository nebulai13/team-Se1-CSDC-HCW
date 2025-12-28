package com.example.teamse1csdchcw.domain.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a saved bookmark for later reference.
 */
public class Bookmark {
    private String id;
    private String resultId;
    private String title;
    private String url;
    private String notes;
    private List<String> tags;
    private LocalDateTime createdAt;

    public Bookmark() {
        this.createdAt = LocalDateTime.now();
        this.tags = new ArrayList<>();
    }

    public Bookmark(String id, String title, String url) {
        this();
        this.id = id;
        this.title = title;
        this.url = url;
    }

    // Getters and setters
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
