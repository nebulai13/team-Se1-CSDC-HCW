package com.example.teamse1csdchcw.domain.search;

/**
 * Enum representing the access level of a research paper or document.
 */
public enum AccessLevel {
    // free to read/download - no paywall or restrictions
    OPEN_ACCESS("Open Access", "Freely available to all"),

    // requires institutional login - uni/library subscription needed
    LICENSED("Licensed", "Requires institutional access"),

    // paywalled or unavailable - must purchase or request access
    RESTRICTED("Restricted", "Access denied or requires purchase"),

    // couldn't determine - api didn't return access info
    UNKNOWN("Unknown", "Access level not determined");

    // human-readable label for ui display
    private final String displayName;

    // longer explanation of what this access level means
    private final String description;

    // enum constructor - sets display props for each constant
    AccessLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    // getter for ui-friendly name
    public String getDisplayName() {
        return displayName;
    }

    // getter for detailed explanation
    public String getDescription() {
        return description;
    }
}
