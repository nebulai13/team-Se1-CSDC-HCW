package com.example.teamse1csdchcw.service.index;

import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalSearchService {
    private static final Logger logger = LoggerFactory.getLogger(LocalSearchService.class);
    private static final int DEFAULT_MAX_RESULTS = 100;

    private final IndexService indexService;
    private final StandardAnalyzer analyzer;

    public LocalSearchService(IndexService indexService) {
        this.indexService = indexService;
        this.analyzer = new StandardAnalyzer();
    }

    public List<SearchResult> search(SearchQuery query, int maxResults) throws IOException {
        if (maxResults <= 0) {
            maxResults = DEFAULT_MAX_RESULTS;
        }

        try (IndexReader reader = indexService.getReader()) {
            IndexSearcher searcher = new IndexSearcher(reader);

            Query luceneQuery = buildQuery(query);

            TopDocs topDocs = searcher.search(luceneQuery, maxResults);

            logger.info("Local search found {} results for query: {}",
                    topDocs.totalHits.value, query.getOriginalQuery());

            return convertToSearchResults(searcher, topDocs);

        } catch (Exception e) {
            logger.error("Local search failed", e);
            throw new IOException("Failed to search local index", e);
        }
    }

    private Query buildQuery(SearchQuery query) throws ParseException {
        BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

        String[] searchFields = {"title", "abstract", "authors", "keywords", "journal", "venue"};
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(searchFields, analyzer);
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        String queryText = query.toQueryString();
        if (queryText != null && !queryText.trim().isEmpty()) {
            Query textQuery = queryParser.parse(QueryParser.escape(queryText));
            booleanQueryBuilder.add(textQuery, BooleanClause.Occur.MUST);
        }

        if (query.getAuthorFilter() != null && !query.getAuthorFilter().isEmpty()) {
            QueryParser authorParser = new QueryParser("authors", analyzer);
            Query authorQuery = authorParser.parse(QueryParser.escape(query.getAuthorFilter()));
            booleanQueryBuilder.add(authorQuery, BooleanClause.Occur.MUST);
        }

        if (query.getYearFrom() != null || query.getYearTo() != null) {
            int fromYear = query.getYearFrom() != null ? query.getYearFrom() : 1900;
            int toYear = query.getYearTo() != null ? query.getYearTo() : LocalDate.now().getYear();

            Query yearQuery = org.apache.lucene.document.IntPoint.newRangeQuery("year", fromYear, toYear);
            booleanQueryBuilder.add(yearQuery, BooleanClause.Occur.MUST);
        }

        if (query.getTypeFilter() != null && !query.getTypeFilter().isEmpty()) {
            BooleanQuery.Builder typeBuilder = new BooleanQuery.Builder();
            QueryParser venueParser = new QueryParser("venue", analyzer);
            QueryParser journalParser = new QueryParser("journal", analyzer);

            String typeKeyword = query.getTypeFilter().toLowerCase();
            try {
                Query venueQuery = venueParser.parse(QueryParser.escape(typeKeyword));
                Query journalQuery = journalParser.parse(QueryParser.escape(typeKeyword));

                typeBuilder.add(venueQuery, BooleanClause.Occur.SHOULD);
                typeBuilder.add(journalQuery, BooleanClause.Occur.SHOULD);

                booleanQueryBuilder.add(typeBuilder.build(), BooleanClause.Occur.MUST);
            } catch (ParseException e) {
                logger.warn("Failed to parse document type filter: {}", typeKeyword);
            }
        }

        BooleanQuery finalQuery = booleanQueryBuilder.build();

        if (finalQuery.clauses().isEmpty()) {
            return new MatchAllDocsQuery();
        }

        return finalQuery;
    }

    private List<SearchResult> convertToSearchResults(IndexSearcher searcher, TopDocs topDocs) throws IOException {
        List<SearchResult> results = new ArrayList<>();

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            SearchResult result = convertDocument(doc, scoreDoc.score);
            if (result != null) {
                results.add(result);
            }
        }

        return results;
    }

    private SearchResult convertDocument(Document doc, float score) {
        try {
            String id = doc.get("id");
            String title = doc.get("title");
            String url = doc.get("url");
            String sourceStr = doc.get("source");

            if (id == null || title == null) {
                logger.warn("Document missing required fields (id or title)");
                return null;
            }

            SourceType source = SourceType.valueOf(sourceStr != null ? sourceStr : "UNKNOWN");

            String authors = doc.get("authors");
            String abstractText = doc.get("abstract");

            if (authors != null || abstractText != null) {
                AcademicPaper paper = new AcademicPaper();
                paper.setId(id);
                paper.setTitle(title);
                paper.setUrl(url);
                paper.setSource(source);
                paper.setAuthors(authors);
                paper.setAbstractText(abstractText);

                String dateStr = doc.get("publication_date");
                if (dateStr != null) {
                    try {
                        paper.setPublicationDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE));
                    } catch (Exception e) {
                        logger.debug("Failed to parse publication date: {}", dateStr);
                    }
                }

                paper.setJournal(doc.get("journal"));
                paper.setVenue(doc.get("venue"));
                paper.setDoi(doc.get("doi"));
                paper.setArxivId(doc.get("arxiv_id"));
                paper.setPdfUrl(doc.get("pdf_url"));

                String keywords = doc.get("keywords");
                if (keywords != null && !keywords.isEmpty()) {
                    paper.setKeywords(Arrays.asList(keywords.split("\\s+")));
                }

                Number citationCount = doc.getField("citation_count_stored") != null
                        ? doc.getField("citation_count_stored").numericValue()
                        : null;
                if (citationCount != null) {
                    paper.setCitationCount(citationCount.intValue());
                }

                return paper;
            } else {
                SearchResult result = new SearchResult();
                result.setId(id);
                result.setTitle(title);
                result.setUrl(url);
                result.setSource(source);
                return result;
            }

        } catch (Exception e) {
            logger.error("Failed to convert document to SearchResult", e);
            return null;
        }
    }

    public List<SearchResult> searchByAuthor(String author, int maxResults) throws IOException {
        SearchQuery query = new SearchQuery("");
        query.setAuthorFilter(author);
        return search(query, maxResults);
    }

    public List<SearchResult> searchByYear(int fromYear, int toYear, int maxResults) throws IOException {
        SearchQuery query = new SearchQuery("");
        query.setYearFrom(fromYear);
        query.setYearTo(toYear);
        return search(query, maxResults);
    }

    public List<SearchResult> searchByKeyword(String keyword, int maxResults) throws IOException {
        SearchQuery query = new SearchQuery(keyword);
        return search(query, maxResults);
    }

    public List<SearchResult> searchAll(int maxResults) throws IOException {
        try (IndexReader reader = indexService.getReader()) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new MatchAllDocsQuery();

            Sort sort = new Sort(SortField.FIELD_SCORE,
                    new SortField("indexed_at", SortField.Type.LONG, true));

            TopDocs topDocs = searcher.search(query, maxResults, sort);

            logger.info("Retrieved {} documents from local index", topDocs.totalHits.value);

            return convertToSearchResults(searcher, topDocs);
        }
    }

    public void close() throws IOException {
        if (analyzer != null) {
            analyzer.close();
        }
    }
}
