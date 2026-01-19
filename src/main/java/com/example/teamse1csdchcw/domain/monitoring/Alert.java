package com.example.teamse1csdchcw.domain.monitoring;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// represents saved search alert - monitors for new papers matching criteria
public class Alert {
    // unique identifier for this alert in db
    private String id;

    // user-friendly name to identify alert ("machine learning papers")
    private String name;

    // search terms to watch for - matches if any appear
    private List<String> keywords;

    // optional - only alert for specific author's work
    private String authorFilter;

    // optional year range - filter alerts by pub date
    private Integer yearFrom;
    private Integer yearTo;

    // whether alert is active - can pause w/o deleting
    private boolean enabled;

    // how to notify user when matches found
    private NotificationType notificationType;

    // where to send notification (email addr, log file, etc)
    private String notificationTarget;

    // when alert was created
    private LocalDateTime createdAt;

    // last time system checked for new matches
    private LocalDateTime lastChecked;

    // total count of papers matched since creation
    private int matchCount;

    // notification delivery methods
    public enum NotificationType {
        CONSOLE,  // print to stdout
        LOG,      // write to log file
        EMAIL     // send email notification
    }

    // default constructor - set sensible defaults
    public Alert() {
        this.keywords = new ArrayList<>();              // init empty list
        this.enabled = true;                            // alerts start active
        this.notificationType = NotificationType.CONSOLE;  // default output
        this.createdAt = LocalDateTime.now();           // timestamp creation
        this.matchCount = 0;                            // start w/ zero matches
    }

    // getters/setters - standard bean pattern for all props
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

    // debug/logging output - show key alert properties
    @Override
    public String toString() {
        return String.format("Alert{id=%s, name=%s, keywords=%s, enabled=%s}",
                id, name, keywords, enabled);
    }
}
