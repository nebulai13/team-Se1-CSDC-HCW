package com.example.teamse1csdchcw.domain.export;

/**
 * Supported citation export formats.
 */
public enum ExportFormat {
    BIBTEX("BibTeX", ".bib"),
    RIS("RIS", ".ris"),
    ENDNOTE("EndNote", ".enw"),
    JSON("JSON", ".json"),
    MARKDOWN("Markdown", ".md");

    private final String displayName;
    private final String fileExtension;

    ExportFormat(String displayName, String fileExtension) {
        this.displayName = displayName;
        this.fileExtension = fileExtension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
