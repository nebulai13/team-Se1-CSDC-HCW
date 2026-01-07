package com.example.teamse1csdchcw.service.export;

import com.example.teamse1csdchcw.domain.export.ExportFormat;
import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class CitationExportService {
    private static final Logger logger = LoggerFactory.getLogger(CitationExportService.class);

    public String export(List<SearchResult> results, ExportFormat format) {
        logger.info("Exporting {} results to {}", results.size(), format);

        if (format == null) {
            format = ExportFormat.BIBTEX;
        }

        return switch (format) {
            case BIBTEX -> exportBibTeX(results);
            case RIS -> exportRIS(results);
            case ENDNOTE -> exportEndNote(results);
            case JSON -> exportJSON(results);
            case MARKDOWN -> exportMarkdown(results);
        };
    }

    public String exportSingle(SearchResult result, ExportFormat format) {
        return export(List.of(result), format);
    }

    private String exportBibTeX(List<SearchResult> results) {
        StringBuilder bibtex = new StringBuilder();

        for (SearchResult result : results) {
            if (result instanceof AcademicPaper paper) {
                bibtex.append(toBibTeX(paper)).append("\n\n");
            }
        }

        return bibtex.toString();
    }

    private String toBibTeX(AcademicPaper paper) {
        StringBuilder entry = new StringBuilder();

        String citationKey = generateCitationKey(paper);
        String type = inferBibTeXType(paper);

        entry.append("@").append(type).append("{").append(citationKey).append(",\n");

        if (paper.getTitle() != null) {
            entry.append("  title = {").append(escapeBibTeX(paper.getTitle())).append("},\n");
        }

        if (paper.getAuthors() != null && !paper.getAuthors().isEmpty()) {
            entry.append("  author = {").append(escapeBibTeX(paper.getAuthors())).append("},\n");
        }

        if (paper.getPublicationDate() != null) {
            entry.append("  year = {").append(paper.getPublicationDate().getYear()).append("},\n");
            entry.append("  month = {").append(paper.getPublicationDate().getMonth().toString().toLowerCase()).append("},\n");
        }

        if (paper.getJournal() != null) {
            entry.append("  journal = {").append(escapeBibTeX(paper.getJournal())).append("},\n");
        }

        if (paper.getVenue() != null) {
            entry.append("  booktitle = {").append(escapeBibTeX(paper.getVenue())).append("},\n");
        }

        if (paper.getDoi() != null) {
            entry.append("  doi = {").append(paper.getDoi()).append("},\n");
        }

        if (paper.getUrl() != null) {
            entry.append("  url = {").append(paper.getUrl()).append("},\n");
        }

        if (paper.getAbstractText() != null) {
            entry.append("  abstract = {").append(escapeBibTeX(paper.getAbstractText())).append("},\n");
        }

        if (paper.getArxivId() != null) {
            entry.append("  eprint = {").append(paper.getArxivId()).append("},\n");
            entry.append("  archivePrefix = {arXiv},\n");
        }

        entry.append("}");

        return entry.toString();
    }

    private String exportRIS(List<SearchResult> results) {
        StringBuilder ris = new StringBuilder();

        for (SearchResult result : results) {
            if (result instanceof AcademicPaper paper) {
                ris.append(toRIS(paper)).append("\n\n");
            }
        }

        return ris.toString();
    }

    private String toRIS(AcademicPaper paper) {
        StringBuilder entry = new StringBuilder();

        String type = inferRISType(paper);
        entry.append("TY  - ").append(type).append("\n");

        if (paper.getTitle() != null) {
            entry.append("TI  - ").append(paper.getTitle()).append("\n");
        }

        if (paper.getAuthors() != null) {
            String[] authors = paper.getAuthors().split(",|;");
            for (String author : authors) {
                entry.append("AU  - ").append(author.trim()).append("\n");
            }
        }

        if (paper.getPublicationDate() != null) {
            entry.append("PY  - ").append(paper.getPublicationDate().getYear()).append("\n");
            entry.append("DA  - ").append(paper.getPublicationDate().format(DateTimeFormatter.ISO_DATE)).append("\n");
        }

        if (paper.getJournal() != null) {
            entry.append("JO  - ").append(paper.getJournal()).append("\n");
        }

        if (paper.getVenue() != null) {
            entry.append("T2  - ").append(paper.getVenue()).append("\n");
        }

        if (paper.getDoi() != null) {
            entry.append("DO  - ").append(paper.getDoi()).append("\n");
        }

        if (paper.getUrl() != null) {
            entry.append("UR  - ").append(paper.getUrl()).append("\n");
        }

        if (paper.getAbstractText() != null) {
            entry.append("AB  - ").append(paper.getAbstractText()).append("\n");
        }

        if (paper.getKeywords() != null && !paper.getKeywords().isEmpty()) {
            for (String keyword : paper.getKeywords()) {
                entry.append("KW  - ").append(keyword).append("\n");
            }
        }

        entry.append("ER  - \n");

        return entry.toString();
    }

    private String exportEndNote(List<SearchResult> results) {
        StringBuilder endnote = new StringBuilder();

        for (SearchResult result : results) {
            if (result instanceof AcademicPaper paper) {
                endnote.append(toEndNote(paper)).append("\n\n");
            }
        }

        return endnote.toString();
    }

    private String toEndNote(AcademicPaper paper) {
        StringBuilder entry = new StringBuilder();

        String type = inferEndNoteType(paper);
        entry.append("%0 ").append(type).append("\n");

        if (paper.getTitle() != null) {
            entry.append("%T ").append(paper.getTitle()).append("\n");
        }

        if (paper.getAuthors() != null) {
            String[] authors = paper.getAuthors().split(",|;");
            for (String author : authors) {
                entry.append("%A ").append(author.trim()).append("\n");
            }
        }

        if (paper.getPublicationDate() != null) {
            entry.append("%D ").append(paper.getPublicationDate().getYear()).append("\n");
        }

        if (paper.getJournal() != null) {
            entry.append("%J ").append(paper.getJournal()).append("\n");
        }

        if (paper.getVenue() != null) {
            entry.append("%B ").append(paper.getVenue()).append("\n");
        }

        if (paper.getDoi() != null) {
            entry.append("%R ").append(paper.getDoi()).append("\n");
        }

        if (paper.getUrl() != null) {
            entry.append("%U ").append(paper.getUrl()).append("\n");
        }

        if (paper.getAbstractText() != null) {
            entry.append("%X ").append(paper.getAbstractText()).append("\n");
        }

        if (paper.getKeywords() != null && !paper.getKeywords().isEmpty()) {
            entry.append("%K ").append(String.join("; ", paper.getKeywords())).append("\n");
        }

        return entry.toString();
    }

    private String generateCitationKey(AcademicPaper paper) {
        StringBuilder key = new StringBuilder();

        if (paper.getAuthors() != null && !paper.getAuthors().isEmpty()) {
            String firstAuthor = paper.getAuthors().split(",")[0].trim();
            String[] parts = firstAuthor.split("\\s+");
            key.append(parts[parts.length - 1].toLowerCase());
        } else {
            key.append("unknown");
        }

        if (paper.getPublicationDate() != null) {
            key.append(paper.getPublicationDate().getYear());
        }

        if (paper.getTitle() != null && paper.getTitle().length() > 0) {
            String[] words = paper.getTitle().toLowerCase().split("\\s+");
            for (String word : words) {
                if (word.length() > 3 && !isStopWord(word)) {
                    key.append(word, 0, Math.min(3, word.length()));
                    break;
                }
            }
        }

        return key.toString().replaceAll("[^a-z0-9]", "");
    }

    private String inferBibTeXType(AcademicPaper paper) {
        String venue = (paper.getVenue() != null ? paper.getVenue() : "").toLowerCase();
        String journal = (paper.getJournal() != null ? paper.getJournal() : "").toLowerCase();

        if (venue.contains("conference") || venue.contains("proceedings") || venue.contains("workshop")) {
            return "inproceedings";
        } else if (journal.contains("journal") || journal.contains("transactions")) {
            return "article";
        } else if (venue.contains("book") || venue.contains("chapter")) {
            return "inbook";
        } else if (paper.getArxivId() != null) {
            return "misc";
        } else {
            return "article";
        }
    }

    private String inferRISType(AcademicPaper paper) {
        String venue = (paper.getVenue() != null ? paper.getVenue() : "").toLowerCase();
        String journal = (paper.getJournal() != null ? paper.getJournal() : "").toLowerCase();

        if (venue.contains("conference") || venue.contains("proceedings")) {
            return "CONF";
        } else if (journal.contains("journal")) {
            return "JOUR";
        } else if (venue.contains("thesis")) {
            return "THES";
        } else if (venue.contains("book")) {
            return "BOOK";
        } else {
            return "JOUR";
        }
    }

    private String inferEndNoteType(AcademicPaper paper) {
        String venue = (paper.getVenue() != null ? paper.getVenue() : "").toLowerCase();
        String journal = (paper.getJournal() != null ? paper.getJournal() : "").toLowerCase();

        if (venue.contains("conference") || venue.contains("proceedings")) {
            return "Conference Proceedings";
        } else if (journal.contains("journal")) {
            return "Journal Article";
        } else if (venue.contains("thesis")) {
            return "Thesis";
        } else if (venue.contains("book")) {
            return "Book";
        } else {
            return "Journal Article";
        }
    }

    private String escapeBibTeX(String text) {
        if (text == null) return "";
        return text.replace("&", "\\&")
                   .replace("%", "\\%")
                   .replace("$", "\\$")
                   .replace("#", "\\#")
                   .replace("_", "\\_")
                   .replace("{", "\\{")
                   .replace("}", "\\}");
    }

    private boolean isStopWord(String word) {
        String[] stopWords = {"the", "and", "for", "with", "from", "into", "during", "including"};
        for (String stop : stopWords) {
            if (word.equals(stop)) return true;
        }
        return false;
    }

    private String exportJSON(List<SearchResult> results) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            if (result instanceof AcademicPaper paper) {
                json.append("  {\n");
                json.append("    \"title\": \"").append(escapeJSON(paper.getTitle())).append("\",\n");
                json.append("    \"authors\": \"").append(escapeJSON(paper.getAuthors())).append("\",\n");
                if (paper.getPublicationDate() != null) {
                    json.append("    \"year\": ").append(paper.getPublicationDate().getYear()).append(",\n");
                }
                if (paper.getDoi() != null) {
                    json.append("    \"doi\": \"").append(paper.getDoi()).append("\",\n");
                }
                if (paper.getUrl() != null) {
                    json.append("    \"url\": \"").append(escapeJSON(paper.getUrl())).append("\",\n");
                }
                if (paper.getAbstractText() != null) {
                    json.append("    \"abstract\": \"").append(escapeJSON(paper.getAbstractText())).append("\",\n");
                }
                json.append("    \"source\": \"").append(paper.getSource().getDisplayName()).append("\"\n");
                json.append("  }");
                if (i < results.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
        }

        json.append("]\n");
        return json.toString();
    }

    private String exportMarkdown(List<SearchResult> results) {
        StringBuilder md = new StringBuilder();
        md.append("# Academic Papers\n\n");

        for (SearchResult result : results) {
            if (result instanceof AcademicPaper paper) {
                md.append("## ").append(paper.getTitle()).append("\n\n");

                if (paper.getAuthors() != null) {
                    md.append("**Authors:** ").append(paper.getAuthors()).append("\n\n");
                }

                if (paper.getPublicationDate() != null) {
                    md.append("**Year:** ").append(paper.getPublicationDate().getYear()).append("\n\n");
                }

                if (paper.getJournal() != null) {
                    md.append("**Journal:** ").append(paper.getJournal()).append("\n\n");
                }

                if (paper.getDoi() != null) {
                    md.append("**DOI:** ").append(paper.getDoi()).append("\n\n");
                }

                if (paper.getUrl() != null) {
                    md.append("**URL:** [Link](").append(paper.getUrl()).append(")\n\n");
                }

                if (paper.getAbstractText() != null) {
                    md.append("**Abstract:** ").append(paper.getAbstractText()).append("\n\n");
                }

                md.append("**Source:** ").append(paper.getSource().getDisplayName()).append("\n\n");
                md.append("---\n\n");
            }
        }

        return md.toString();
    }

    private String escapeJSON(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
