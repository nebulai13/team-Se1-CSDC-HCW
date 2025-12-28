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
public class ResultAggregator {
    private static final Logger logger = LoggerFactory.getLogger(ResultAggregator.class);

    /**
     * Aggregate results: merge, deduplicate, and sort.
     *
     * @param results list of all results from multiple sources
     * @return aggregated and sorted list
     */
    public List<SearchResult> aggregate(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        logger.debug("Aggregating {} results", results.size());

        // Step 1: Deduplicate
        List<SearchResult> deduplicated = deduplicate(results);

        // Step 2: Sort by relevance
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
        Map<String, SearchResult> uniqueResults = new LinkedHashMap<>();

        for (SearchResult result : results) {
            String key = generateDeduplicationKey(result);

            if (uniqueResults.containsKey(key)) {
                // Merge with existing result
                SearchResult existing = uniqueResults.get(key);
                SearchResult merged = merge(existing, result);
                uniqueResults.put(key, merged);
                logger.debug("Merged duplicate result: {}", key);
            } else {
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
        // For AcademicPaper, try DOI, arXiv ID, or PMID
        if (result instanceof AcademicPaper paper) {
            if (paper.getDoi() != null && !paper.getDoi().isEmpty()) {
                return "doi:" + paper.getDoi().toLowerCase().trim();
            }
            if (paper.getArxivId() != null && !paper.getArxivId().isEmpty()) {
                return "arxiv:" + paper.getArxivId().toLowerCase().trim();
            }
            if (paper.getPmid() != null && !paper.getPmid().isEmpty()) {
                return "pmid:" + paper.getPmid().toLowerCase().trim();
            }
        }

        // Fall back to URL
        if (result.getUrl() != null && !result.getUrl().isEmpty()) {
            String normalizedUrl = normalizeUrl(result.getUrl());
            return "url:" + normalizedUrl;
        }

        // Last resort: title hash
        if (result.getTitle() != null && !result.getTitle().isEmpty()) {
            String normalizedTitle = normalizeTitle(result.getTitle());
            return "title:" + normalizedTitle.hashCode();
        }

        // Use object ID if nothing else available
        return "id:" + result.getId();
    }

    /**
     * Normalize URL for comparison (remove protocol, www, trailing slash, etc.)
     */
    private String normalizeUrl(String url) {
        return url.toLowerCase()
                .replaceAll("^https?://", "")
                .replaceAll("^www\\.", "")
                .replaceAll("/$", "")
                .trim();
    }

    /**
     * Normalize title for comparison (lowercase, remove punctuation, extra spaces)
     */
    private String normalizeTitle(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Merge two search results (duplicate entries from different sources).
     * Keep the most complete information from both.
     */
    private SearchResult merge(SearchResult existing, SearchResult incoming) {
        // If one is an AcademicPaper and the other isn't, prefer AcademicPaper
        if (existing instanceof AcademicPaper && !(incoming instanceof AcademicPaper)) {
            return mergeMetadata(existing, incoming);
        }
        if (!(existing instanceof AcademicPaper) && incoming instanceof AcademicPaper) {
            return mergeMetadata(incoming, existing);
        }

        // Both are AcademicPaper or both are SearchResult
        return mergeMetadata(existing, incoming);
    }

    /**
     * Merge metadata from two results, keeping the most complete information.
     */
    private SearchResult mergeMetadata(SearchResult primary, SearchResult secondary) {
        // Use primary as base, fill in missing fields from secondary

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

        // For AcademicPaper, merge additional fields
        if (primary instanceof AcademicPaper primaryPaper && secondary instanceof AcademicPaper secondaryPaper) {
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

            // Merge keywords (combine unique keywords)
            if (secondaryPaper.getKeywords() != null && !secondaryPaper.getKeywords().isEmpty()) {
                Set<String> combinedKeywords = new HashSet<>();
                if (primaryPaper.getKeywords() != null) {
                    combinedKeywords.addAll(primaryPaper.getKeywords());
                }
                combinedKeywords.addAll(secondaryPaper.getKeywords());
                primaryPaper.setKeywords(new ArrayList<>(combinedKeywords));
            }

            // Use higher citation count
            if (secondaryPaper.getCitationCount() > primaryPaper.getCitationCount()) {
                primaryPaper.setCitationCount(secondaryPaper.getCitationCount());
            }
        }

        // Update relevance score (average if both have scores)
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
                    // Sort by relevance score first (higher is better)
                    int relevanceCompare = Double.compare(r2.getRelevance(), r1.getRelevance());
                    if (relevanceCompare != 0) {
                        return relevanceCompare;
                    }

                    // Then by citation count (higher is better) for AcademicPapers
                    if (r1 instanceof AcademicPaper p1 && r2 instanceof AcademicPaper p2) {
                        int citationCompare = Integer.compare(p2.getCitationCount(), p1.getCitationCount());
                        if (citationCompare != 0) {
                            return citationCompare;
                        }

                        // Then by publication date (newer is better)
                        if (p1.getPublicationDate() != null && p2.getPublicationDate() != null) {
                            return p2.getPublicationDate().compareTo(p1.getPublicationDate());
                        }
                    }

                    // Finally by timestamp (newer is better)
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
                    return true; // Keep non-academic results
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
                    if (r instanceof AcademicPaper paper && paper.getPublicationDate() != null) {
                        int year = paper.getPublicationDate().getYear();
                        if (yearFrom != null && year < yearFrom) {
                            return false;
                        }
                        if (yearTo != null && year > yearTo) {
                            return false;
                        }
                    }
                    return true;
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
        return results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSource().getDisplayName(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
