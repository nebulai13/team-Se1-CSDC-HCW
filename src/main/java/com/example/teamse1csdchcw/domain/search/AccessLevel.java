package com.example.teamse1csdchcw.domain.search;

/**
 * Enum representing the access level of a research paper or document.
 */
public enum AccessLevel {
    OPEN_ACCESS("Open Access", "Freely available to all"),
    LICENSED("Licensed", "Requires institutional access"),
    RESTRICTED("Restricted", "Access denied or requires purchase"),
    UNKNOWN("Unknown", "Access level not determined");

    private final String displayName;
    private final String description;

    AccessLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
