package com.example.teamse1csdchcw.domain.monitoring;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Alert {
    private String id;
    private String name;
    private List<String> keywords;
    private String authorFilter;
    private Integer yearFrom;
    private Integer yearTo;
    private boolean enabled;
    private NotificationType notificationType;
    private String notificationTarget;
    private LocalDateTime createdAt;
    private LocalDateTime lastChecked;
    private int matchCount;

    public enum NotificationType {
        CONSOLE,
        LOG,
        EMAIL
    }

    public Alert() {
        this.keywords = new ArrayList<>();
        this.enabled = true;
        this.notificationType = NotificationType.CONSOLE;
        this.createdAt = LocalDateTime.now();
        this.matchCount = 0;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getAuthorFilter() { return authorFilter; }
    public void setAuthorFilter(String authorFilter) { this.authorFilter = authorFilter; }

    public Integer getYearFrom() { return yearFrom; }
    public void setYearFrom(Integer yearFrom) { this.yearFrom = yearFrom; }

    public Integer getYearTo() { return yearTo; }
    public void setYearTo(Integer yearTo) { this.yearTo = yearTo; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }

    public String getNotificationTarget() { return notificationTarget; }
    public void setNotificationTarget(String notificationTarget) { this.notificationTarget = notificationTarget; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastChecked() { return lastChecked; }
    public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }

    public int getMatchCount() { return matchCount; }
    public void setMatchCount(int matchCount) { this.matchCount = matchCount; }

    @Override
    public String toString() {
        return String.format("Alert{id=%s, name=%s, keywords=%s, enabled=%s}",
                id, name, keywords, enabled);
    }
}
