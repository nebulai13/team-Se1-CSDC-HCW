package com.example.teamse1csdchcw.domain.export;

/**
 * Supported citation export formats.
 */
public enum ExportFormat {
    // latex bibliography format - most common in cs/math papers
    BIBTEX("BibTeX", ".bib"),

    // research info systems format - used by refworks, zotero, etc
    RIS("RIS", ".ris"),

    // thomson reuters endnote format - popular in sciences
    ENDNOTE("EndNote", ".enw"),

    // json format - programmatic access, easy parsing
    JSON("JSON", ".json"),

    // markdown format - readable plain text w/ formatting
    MARKDOWN("Markdown", ".md");

    // human-readable format name for ui display
    private final String displayName;

    // file extension for saving exported citations
    private final String fileExtension;

    // enum constructor - associates display name and ext w/ each format
    ExportFormat(String displayName, String fileExtension) {
        this.displayName = displayName;
        this.fileExtension = fileExtension;
    }

    // getter for ui-friendly name
    public String getDisplayName() {
        return displayName;
    }

    // getter for file extension (includes dot prefix)
    public String getFileExtension() {
        return fileExtension;
    }
}
