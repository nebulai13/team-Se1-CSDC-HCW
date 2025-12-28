package com.example.teamse1csdchcw.domain.search;

import com.example.teamse1csdchcw.domain.source.SourceType;
import java.time.LocalDate;
import java.util.List;

/**
 * Extended search result for academic papers with additional metadata.
 */
public class AcademicPaper extends SearchResult {
    private String doi;
    private String arxivId;
    private String pmid;
    private String abstractText;
    private LocalDate publicationDate;
    private String journal;
    private String venue;
    private List<String> keywords;
    private int citationCount;
    private String pdfUrl;

    public AcademicPaper() {
        super();
    }

    public AcademicPaper(String id, String title, String authors, String url,
                        String snippet, SourceType source) {
        super(id, title, authors, url, snippet, source);
    }

    /**
     * Generates a BibTeX citation key (e.g., "smith2024machine").
     */
    public String getCitationKey() {
        if (getAuthors() == null || getAuthors().isEmpty()) {
            return "unknown" + (publicationDate != null ? publicationDate.getYear() : "");
        }

        String firstAuthor = getAuthors().split(",")[0].split(" ")[0].toLowerCase();
        String year = publicationDate != null ? String.valueOf(publicationDate.getYear()) : "";
        String titlePart = getTitle() != null ? getTitle().split(" ")[0].toLowerCase() : "";

        return firstAuthor + year + titlePart;
    }

    // Getters and setters
    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }

    public String getArxivId() { return arxivId; }
    public void setArxivId(String arxivId) { this.arxivId = arxivId; }

    public String getPmid() { return pmid; }
    public void setPmid(String pmid) { this.pmid = pmid; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }

    public String getJournal() { return journal; }
    public void setJournal(String journal) { this.journal = journal; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public int getCitationCount() { return citationCount; }
    public void setCitationCount(int citationCount) { this.citationCount = citationCount; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    @Override
    public String toString() {
        return "AcademicPaper{" +
                "title='" + getTitle() + '\'' +
                ", authors='" + getAuthors() + '\'' +
                ", doi='" + doi + '\'' +
                ", journal='" + journal + '\'' +
                ", publicationDate=" + publicationDate +
                ", citations=" + citationCount +
                '}';
    }
}
