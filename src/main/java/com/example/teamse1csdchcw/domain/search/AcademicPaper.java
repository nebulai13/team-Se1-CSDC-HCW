package com.example.teamse1csdchcw.domain.search;

import com.example.teamse1csdchcw.domain.source.SourceType;
// -- java 8+ date (no time component) --
import java.time.LocalDate;
import java.util.List;

/**
 * Extended search result for academic papers with additional metadata.
 */
// -- academicpaper extends searchresult w/ scholarly metadata --
// -- inheritance: is-a relationship (paper is a search result) --
// -- has extra fields: doi, abstract, citations, pdf link, etc --
public class AcademicPaper extends SearchResult {
    // -- digital object identifier: unique id for papers --
    // -- format: "10.1234/abcd.5678" --
    private String doi;
    // -- arxiv preprint id: e.g., "2301.12345" --
    private String arxivId;
    // -- pubmed id: numeric id for biomedical papers --
    private String pmid;
    // -- full paper abstract text --
    private String abstractText;
    // -- when paper was published (date only, no time) --
    private LocalDate publicationDate;
    // -- journal name where published --
    private String journal;
    // -- conference/venue where presented --
    private String venue;
    // -- subject keywords/tags --
    private List<String> keywords;
    // -- how many times cited by other papers --
    private int citationCount;
    // -- direct link to pdf file (if available) --
    private String pdfUrl;

    // -- default constructor calls parent --
    public AcademicPaper() {
        super();
    }

    // -- convenience constructor delegates to parent --
    public AcademicPaper(String id, String title, String authors, String url,
                        String snippet, SourceType source) {
        super(id, title, authors, url, snippet, source);
    }

    /**
     * Generates a BibTeX citation key (e.g., "smith2024machine").
     */
    // -- creates bibtex key for citation: authorYEARfirstword --
    // -- e.g., "smith2024deep" for paper by smith in 2024 about deep learning --
    public String getCitationKey() {
        // -- handle missing author --
        if (getAuthors() == null || getAuthors().isEmpty()) {
            return "unknown" + (publicationDate != null ? publicationDate.getYear() : "");
        }

        // -- extract first author's last name --
        String firstAuthor = getAuthors().split(",")[0].split(" ")[0].toLowerCase();
        // -- get year if available --
        String year = publicationDate != null ? String.valueOf(publicationDate.getYear()) : "";
        // -- get first word of title --
        String titlePart = getTitle() != null ? getTitle().split(" ")[0].toLowerCase() : "";

        return firstAuthor + year + titlePart;
    }

    // Getters and setters
    // -- standard javabean getters/setters for all fields --
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

    // -- toString for debugging: includes key metadata --
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
