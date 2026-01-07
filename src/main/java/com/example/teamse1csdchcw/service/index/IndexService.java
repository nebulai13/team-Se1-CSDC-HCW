package com.example.teamse1csdchcw.service.index;

import com.example.teamse1csdchcw.domain.search.AcademicPaper;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class IndexService {
    private static final Logger logger = LoggerFactory.getLogger(IndexService.class);
    private static final String DEFAULT_INDEX_DIR = System.getProperty("user.home") + "/.libsearch/index";

    private final Path indexPath;
    private final Directory directory;
    private final StandardAnalyzer analyzer;
    private IndexWriter indexWriter;

    public IndexService() throws IOException {
        this(DEFAULT_INDEX_DIR);
    }

    public IndexService(String indexDir) throws IOException {
        this.indexPath = Paths.get(indexDir);
        Files.createDirectories(indexPath);

        this.directory = FSDirectory.open(indexPath);
        this.analyzer = new StandardAnalyzer();

        initializeWriter();

        logger.info("IndexService initialized with index directory: {}", indexPath);
    }

    private void initializeWriter() throws IOException {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            config.setRAMBufferSizeMB(256.0);

            this.indexWriter = new IndexWriter(directory, config);
        } catch (LockObtainFailedException e) {
            String errorMessage = e.getMessage();

            // Check if lock is held by another process or this JVM
            if (errorMessage != null && errorMessage.contains("this virtual machine")) {
                logger.error("Index lock is held by this JVM - another IndexWriter may be open for this directory");
                logger.error("This usually means multiple IndexService instances are trying to use the same index");
                throw e;
            }

            logger.warn("Index is locked by another process, attempting to clear stale lock...");

            // Try to clear stale lock by deleting lock file (only for external locks)
            try {
                Path lockFile = indexPath.resolve("write.lock");
                if (Files.exists(lockFile)) {
                    Files.delete(lockFile);
                    logger.info("Deleted stale lock file");
                }

                // Retry creating the writer with a NEW config object
                IndexWriterConfig newConfig = new IndexWriterConfig(analyzer);
                newConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                newConfig.setRAMBufferSizeMB(256.0);

                this.indexWriter = new IndexWriter(directory, newConfig);
                logger.info("Successfully created IndexWriter after clearing lock");
            } catch (IOException retryException) {
                logger.error("Failed to create IndexWriter after clearing lock", retryException);
                throw retryException;
            }
        }
    }

    public void indexResult(SearchResult result) throws IOException {
        if (result == null) {
            logger.warn("Attempted to index null result");
            return;
        }

        Document doc = createDocument(result);

        Term idTerm = new Term("id", result.getId());
        indexWriter.updateDocument(idTerm, doc);

        logger.debug("Indexed result: {} (ID: {})", result.getTitle(), result.getId());
    }

    public void indexResults(List<SearchResult> results) throws IOException {
        if (results == null || results.isEmpty()) {
            logger.warn("Attempted to index empty result list");
            return;
        }

        for (SearchResult result : results) {
            indexResult(result);
        }

        commit();
        logger.info("Indexed {} results", results.size());
    }

    private Document createDocument(SearchResult result) {
        Document doc = new Document();

        doc.add(new StringField("id", result.getId(), Field.Store.YES));
        doc.add(new TextField("title", result.getTitle() != null ? result.getTitle() : "", Field.Store.YES));
        doc.add(new TextField("url", result.getUrl() != null ? result.getUrl() : "", Field.Store.YES));
        doc.add(new StringField("source", result.getSource().name(), Field.Store.YES));

        if (result instanceof AcademicPaper paper) {
            if (paper.getAuthors() != null) {
                doc.add(new TextField("authors", paper.getAuthors(), Field.Store.YES));
            }

            if (paper.getAbstractText() != null) {
                doc.add(new TextField("abstract", paper.getAbstractText(), Field.Store.YES));
            }

            if (paper.getPublicationDate() != null) {
                String dateStr = paper.getPublicationDate().format(DateTimeFormatter.ISO_DATE);
                doc.add(new StringField("publication_date", dateStr, Field.Store.YES));
                doc.add(new IntPoint("year", paper.getPublicationDate().getYear()));
                doc.add(new StoredField("year_stored", paper.getPublicationDate().getYear()));
            }

            if (paper.getJournal() != null) {
                doc.add(new TextField("journal", paper.getJournal(), Field.Store.YES));
            }

            if (paper.getVenue() != null) {
                doc.add(new TextField("venue", paper.getVenue(), Field.Store.YES));
            }

            if (paper.getDoi() != null) {
                doc.add(new StringField("doi", paper.getDoi(), Field.Store.YES));
            }

            if (paper.getArxivId() != null) {
                doc.add(new StringField("arxiv_id", paper.getArxivId(), Field.Store.YES));
            }

            if (paper.getKeywords() != null && !paper.getKeywords().isEmpty()) {
                doc.add(new TextField("keywords", String.join(" ", paper.getKeywords()), Field.Store.YES));
            }

            if (paper.getPdfUrl() != null) {
                doc.add(new StringField("pdf_url", paper.getPdfUrl(), Field.Store.YES));
            }

            int citationCount = paper.getCitationCount();
            if (citationCount >= 0) {
                doc.add(new IntPoint("citation_count", citationCount));
                doc.add(new StoredField("citation_count_stored", citationCount));
            }
        }

        doc.add(new LongPoint("indexed_at", System.currentTimeMillis()));
        doc.add(new StoredField("indexed_at_stored", System.currentTimeMillis()));

        return doc;
    }

    public void deleteResult(String resultId) throws IOException {
        Term idTerm = new Term("id", resultId);
        indexWriter.deleteDocuments(idTerm);
        commit();

        logger.info("Deleted result from index: {}", resultId);
    }

    public void deleteAll() throws IOException {
        indexWriter.deleteAll();
        commit();

        logger.info("Deleted all documents from index");
    }

    public void commit() throws IOException {
        indexWriter.commit();
        logger.debug("Index committed");
    }

    public void optimize() throws IOException {
        indexWriter.forceMerge(1);
        commit();

        logger.info("Index optimized");
    }

    public IndexReader getReader() throws IOException {
        return DirectoryReader.open(directory);
    }

    public long getDocumentCount() throws IOException {
        try (IndexReader reader = getReader()) {
            return reader.numDocs();
        }
    }

    public IndexStats getStats() throws IOException {
        try (IndexReader reader = getReader()) {
            IndexStats stats = new IndexStats();
            stats.documentCount = reader.numDocs();
            stats.deletedDocCount = reader.numDeletedDocs();
            stats.totalDocCount = reader.maxDoc();
            stats.indexSizeBytes = getIndexSizeBytes();
            return stats;
        }
    }

    private long getIndexSizeBytes() throws IOException {
        return Files.walk(indexPath)
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
    }

    public void close() throws IOException {
        if (indexWriter != null && indexWriter.isOpen()) {
            indexWriter.close();
        }

        if (directory != null) {
            directory.close();
        }

        if (analyzer != null) {
            analyzer.close();
        }

        logger.info("IndexService closed");
    }

    public void shutdown() {
        try {
            close();
        } catch (IOException e) {
            logger.error("Error during shutdown", e);
        }
    }

    public static class IndexStats {
        public long documentCount;
        public long deletedDocCount;
        public long totalDocCount;
        public long indexSizeBytes;

        public String getFormattedSize() {
            if (indexSizeBytes < 1024) {
                return indexSizeBytes + " B";
            } else if (indexSizeBytes < 1024 * 1024) {
                return String.format("%.2f KB", indexSizeBytes / 1024.0);
            } else if (indexSizeBytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", indexSizeBytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", indexSizeBytes / (1024.0 * 1024.0 * 1024.0));
            }
        }

        @Override
        public String toString() {
            return String.format("Documents: %d (deleted: %d), Size: %s",
                    documentCount, deletedDocCount, getFormattedSize());
        }
    }
}
