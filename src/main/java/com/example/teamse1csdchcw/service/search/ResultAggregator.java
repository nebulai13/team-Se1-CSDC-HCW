package com.example.teamse1csdchcw.service.search;

import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates and deduplicates search results from multiple sources.
 * Uses DOI, arXiv ID, PMID, and URL for deduplication.
 * Sorts results by relevance score and citation count.
 */
// merges results from multiple apis - removes duplicates, combines metadata
public class ResultAggregator {
    private static final Logger logger = LoggerFactory.getLogger(ResultAggregator.class);

    /**
     * Aggregate results: merge, deduplicate, and sort.
     *
     * @param results list of all results from multiple sources
     * @return aggregated and sorted list
     */
    public List<SearchResult> aggregate(List<SearchResult> results) {
        // handle empty input gracefully
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        logger.debug("Aggregating {} results", results.size());

        // step 1: remove duplicates and merge metadata from diff sources
        List<SearchResult> deduplicated = deduplicate(results);

        // step 2: sort by relevance score & citation count
        List<SearchResult> sorted = sort(deduplicated);

        logger.info("Aggregation complete: {} unique results (from {} total)",
                sorted.size(), results.size());

        return sorted;
    }

    /**
     * Deduplicate results based on DOI, arXiv ID, PMID, and URL.
     * When duplicates are found, merge metadata from multiple sources.
     */
    private List<SearchResult> deduplicate(List<SearchResult> results) {
        // linkedhashmap preserves insertion order
        Map<String, SearchResult> uniqueResults = new LinkedHashMap<>();

        for (SearchResult result : results) {
            // generate unique key based on doi/arxiv/pmid/url
            String key = generateDeduplicationKey(result);

            if (uniqueResults.containsKey(key)) {
                // duplicate found - merge metadata from both sources
                SearchResult existing = uniqueResults.get(key);
                SearchResult merged = merge(existing, result);
                uniqueResults.put(key, merged);  // replace w/ merged version
                logger.debug("Merged duplicate result: {}", key);
            } else {
                // first occurrence - add to map
                uniqueResults.put(key, result);
            }
        }

        logger.debug("Deduplicated {} results to {}", results.size(), uniqueResults.size());
        return new ArrayList<>(uniqueResults.values());
    }

    /**
     * Generate a unique key for deduplication.
     * Priority: DOI > arXiv ID > PMID > normalized URL > title hash
     */
    private String generateDeduplicationKey(SearchResult result) {
        // academic papers have structured identifiers - use them first
        if (result instanceof AcademicPaper paper) {
            // doi is most reliable - globally unique identifier
            if (paper.getDoi() != null && !paper.getDoi().isEmpty()) {
                return "doi:" + paper.getDoi().toLowerCase().trim();
            }
            // arxiv id is unique within arxiv
            if (paper.getArxivId() != null && !paper.getArxivId().isEmpty()) {
                return "arxiv:" + paper.getArxivId().toLowerCase().trim();
            }
            // pubmed id is unique within pubmed
            if (paper.getPmid() != null && !paper.getPmid().isEmpty()) {
                return "pmid:" + paper.getPmid().toLowerCase().trim();
            }
        }

        // fallback to url - normalize to handle minor differences
        if (result.getUrl() != null && !result.getUrl().isEmpty()) {
            String normalizedUrl = normalizeUrl(result.getUrl());
            return "url:" + normalizedUrl;
        }

        // last resort: hash title - catches papers w/o identifiers
        if (result.getTitle() != null && !result.getTitle().isEmpty()) {
            String normalizedTitle = normalizeTitle(result.getTitle());
            return "title:" + normalizedTitle.hashCode();
        }

        // extremely rare: use object id if nothing else available
        return "id:" + result.getId();
    }

    /**
     * Normalize URL for comparison (remove protocol, www, trailing slash, etc.)
     */
    private String normalizeUrl(String url) {
        // lowercase & strip common url variations
        return url.toLowerCase()
                .replaceAll("^https?://", "")     // remove http:// or https://
                .replaceAll("^www\\.", "")        // remove www. prefix
                .replaceAll("/$", "")             // remove trailing slash
                .trim();
    }

    /**
     * Normalize title for comparison (lowercase, remove punctuation, extra spaces)
     */
    private String normalizeTitle(String title) {
        // lowercase & strip punctuation/whitespace for fuzzy matching
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")   // keep only alphanumeric & spaces
                .replaceAll("\\s+", " ")          // collapse multiple spaces
                .trim();
    }

    /**
     * Merge two search results (duplicate entries from different sources).
     * Keep the most complete information from both.
     */
    private SearchResult merge(SearchResult existing, SearchResult incoming) {
        // prefer academic paper objs over generic search results
        if (existing instanceof AcademicPaper && !(incoming instanceof AcademicPaper)) {
            return mergeMetadata(existing, incoming);
        }
        if (!(existing instanceof AcademicPaper) && incoming instanceof AcademicPaper) {
            return mergeMetadata(incoming, existing);  // swap order
        }

        // both same type - merge metadata
        return mergeMetadata(existing, incoming);
    }

    /**
     * Merge metadata from two results, keeping the most complete information.
     */
    private SearchResult mergeMetadata(SearchResult primary, SearchResult secondary) {
        // use primary as base, fill in missing fields from secondary

        // fill null/empty string fields w/ secondary's data
        if (primary.getTitle() == null || primary.getTitle().isEmpty()) {
            primary.setTitle(secondary.getTitle());
        }

        if (primary.getAuthors() == null || primary.getAuthors().isEmpty()) {
            primary.setAuthors(secondary.getAuthors());
        }

        if (primary.getUrl() == null || primary.getUrl().isEmpty()) {
            primary.setUrl(secondary.getUrl());
        }

        if (primary.getSnippet() == null || primary.getSnippet().isEmpty()) {
            primary.setSnippet(secondary.getSnippet());
        }

        // for academic papers, merge additional fields
        if (primary instanceof AcademicPaper primaryPaper && secondary instanceof AcademicPaper secondaryPaper) {
            // merge all academic-specific fields
            if (primaryPaper.getDoi() == null || primaryPaper.getDoi().isEmpty()) {
                primaryPaper.setDoi(secondaryPaper.getDoi());
            }

            if (primaryPaper.getArxivId() == null || primaryPaper.getArxivId().isEmpty()) {
                primaryPaper.setArxivId(secondaryPaper.getArxivId());
            }

            if (primaryPaper.getPmid() == null || primaryPaper.getPmid().isEmpty()) {
                primaryPaper.setPmid(secondaryPaper.getPmid());
            }

            if (primaryPaper.getAbstractText() == null || primaryPaper.getAbstractText().isEmpty()) {
                primaryPaper.setAbstractText(secondaryPaper.getAbstractText());
            }

            if (primaryPaper.getPdfUrl() == null || primaryPaper.getPdfUrl().isEmpty()) {
                primaryPaper.setPdfUrl(secondaryPaper.getPdfUrl());
            }

            if (primaryPaper.getPublicationDate() == null) {
                primaryPaper.setPublicationDate(secondaryPaper.getPublicationDate());
            }

            if (primaryPaper.getJournal() == null || primaryPaper.getJournal().isEmpty()) {
                primaryPaper.setJournal(secondaryPaper.getJournal());
            }

            if (primaryPaper.getVenue() == null || primaryPaper.getVenue().isEmpty()) {
                primaryPaper.setVenue(secondaryPaper.getVenue());
            }

            // merge keywords - combine both lists & remove duplicates
            if (secondaryPaper.getKeywords() != null && !secondaryPaper.getKeywords().isEmpty()) {
                Set<String> combinedKeywords = new HashSet<>();
                if (primaryPaper.getKeywords() != null) {
                    combinedKeywords.addAll(primaryPaper.getKeywords());
                }
                combinedKeywords.addAll(secondaryPaper.getKeywords());
                primaryPaper.setKeywords(new ArrayList<>(combinedKeywords));
            }

            // use higher citation count - diff sources may have diff counts
            if (secondaryPaper.getCitationCount() > primaryPaper.getCitationCount()) {
                primaryPaper.setCitationCount(secondaryPaper.getCitationCount());
            }
        }

        // update relevance score - average if both have scores
        if (primary.getRelevance() > 0 && secondary.getRelevance() > 0) {
            primary.setRelevance((primary.getRelevance() + secondary.getRelevance()) / 2.0);
        } else if (secondary.getRelevance() > 0) {
            primary.setRelevance(secondary.getRelevance());
        }

        return primary;
    }

    /**
     * Sort results by relevance score and citation count.
     */
    private List<SearchResult> sort(List<SearchResult> results) {
        return results.stream()
                .sorted((r1, r2) -> {
                    // primary sort: relevance score (higher = better)
                    int relevanceCompare = Double.compare(r2.getRelevance(), r1.getRelevance());
                    if (relevanceCompare != 0) {
                        return relevanceCompare;
                    }

                    // secondary sort: citation count for academic papers (higher = better)
                    if (r1 instanceof AcademicPaper p1 && r2 instanceof AcademicPaper p2) {
                        int citationCompare = Integer.compare(p2.getCitationCount(), p1.getCitationCount());
                        if (citationCompare != 0) {
                            return citationCompare;
                        }

                        // tertiary sort: publication date (newer = better)
                        if (p1.getPublicationDate() != null && p2.getPublicationDate() != null) {
                            return p2.getPublicationDate().compareTo(p1.getPublicationDate());
                        }
                    }

                    // final tiebreaker: timestamp (newer = better)
                    return r2.getTimestamp().compareTo(r1.getTimestamp());
                })
                .collect(Collectors.toList());
    }

    /**
     * Filter results by minimum citation count (for AcademicPapers).
     *
     * @param results the results to filter
     * @param minCitations minimum citation count
     * @return filtered results
     */
    public List<SearchResult> filterByCitations(List<SearchResult> results, int minCitations) {
        return results.stream()
                .filter(r -> {
                    if (r instanceof AcademicPaper paper) {
                        return paper.getCitationCount() >= minCitations;
                    }
                    return true; // keep non-academic results (no citation data)
                })
                .collect(Collectors.toList());
    }

    /**
     * Filter results by year range (for AcademicPapers).
     *
     * @param results the results to filter
     * @param yearFrom start year (inclusive, null for no lower bound)
     * @param yearTo end year (inclusive, null for no upper bound)
     * @return filtered results
     */
    public List<SearchResult> filterByYear(List<SearchResult> results, Integer yearFrom, Integer yearTo) {
        return results.stream()
                .filter(r -> {
                    // only filter academic papers w/ pub dates
                    if (r instanceof AcademicPaper paper && paper.getPublicationDate() != null) {
                        int year = paper.getPublicationDate().getYear();
                        // check lower bound
                        if (yearFrom != null && year < yearFrom) {
                            return false;
                        }
                        // check upper bound
                        if (yearTo != null && year > yearTo) {
                            return false;
                        }
                    }
                    return true;  // keep if no date or within range
                })
                .collect(Collectors.toList());
    }

    /**
     * Group results by source.
     *
     * @param results the results to group
     * @return map of source type to results
     */
    public Map<String, List<SearchResult>> groupBySource(List<SearchResult> results) {
        // group by source display name, preserving order
        return results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSource().getDisplayName(),
                        LinkedHashMap::new,  // preserve insertion order
                        Collectors.toList()
                ));
    }
}
