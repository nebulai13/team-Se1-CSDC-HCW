package com.example.teamse1csdchcw.service.download;

import com.example.teamse1csdchcw.repository.DownloadRepository;
import com.example.teamse1csdchcw.repository.DownloadRepository.Download;
import com.example.teamse1csdchcw.repository.DownloadRepository.Download.DownloadStatus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class DownloadService {
    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    private static final int BUFFER_SIZE = 8192;

    private final DownloadRepository repository;
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<String, CompletableFuture<Void>> activeDownloads;

    public DownloadService() {
        this.repository = new DownloadRepository();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.activeDownloads = new ConcurrentHashMap<>();
    }

    public String queueDownload(String resultId, String url, String destinationDir) {
        try {
            String filename = extractFilename(url);
            Path destPath = Paths.get(destinationDir, filename);

            Files.createDirectories(destPath.getParent());

            Download download = new Download();
            download.setResultId(resultId);
            download.setUrl(url);
            download.setDestinationPath(destPath.toString());
            download.setStatus(DownloadStatus.PENDING);
            download.setProgress(0.0);

            repository.save(download);
            logger.info("Queued download: {} -> {}", url, destPath);

            startNextDownload();

            return download.getId();

        } catch (Exception e) {
            logger.error("Failed to queue download", e);
            return null;
        }
    }

    public void startDownload(String downloadId, Consumer<Double> progressCallback) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                Download download = repository.findById(downloadId);
                if (download == null) {
                    logger.error("Download not found: {}", downloadId);
                    return;
                }

                download.setStatus(DownloadStatus.IN_PROGRESS);
                download.setStartedAt(LocalDateTime.now());
                download.setProgress(0.0);
                repository.save(download);

                logger.info("Starting download: {}", download.getUrl());

                downloadFile(download, progressCallback);

                download.setStatus(DownloadStatus.COMPLETED);
                download.setProgress(1.0);
                download.setCompletedAt(LocalDateTime.now());
                repository.save(download);

                logger.info("Download completed: {}", download.getDestinationPath());

            } catch (Exception e) {
                logger.error("Download failed", e);
                try {
                    Download download = repository.findById(downloadId);
                    if (download != null) {
                        download.setStatus(DownloadStatus.FAILED);
                        download.setErrorMessage(e.getMessage());
                        download.setCompletedAt(LocalDateTime.now());
                        repository.save(download);
                    }
                } catch (SQLException ex) {
                    logger.error("Failed to update download status", ex);
                }
            } finally {
                activeDownloads.remove(downloadId);
                startNextDownload();
            }
        }, executorService);

        activeDownloads.put(downloadId, future);
    }

    private void downloadFile(Download download, Consumer<Double> progressCallback) throws IOException, SQLException {
        Request request = new Request.Builder()
                .url(download.getUrl())
                .header("User-Agent", "LibSearch/1.0 (Academic Search Tool)")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download failed: HTTP " + response.code());
            }

            // verify response is actually a PDF, not an HTML landing page
            String contentType = response.header("Content-Type", "");
            if (contentType.contains("text/html")) {
                throw new IOException("URL resolved to HTML page, not a PDF. The publisher may not provide direct PDF access.");
            }

            long contentLength = response.body().contentLength();
            download.setFileSize(contentLength);

            try (InputStream input = response.body().byteStream();
                 OutputStream output = new FileOutputStream(download.getDestinationPath())) {

                byte[] buffer = new byte[BUFFER_SIZE];
                long totalBytesRead = 0;
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if (contentLength > 0) {
                        double progress = (double) totalBytesRead / contentLength;
                        download.setProgress(progress);

                        if (progressCallback != null) {
                            progressCallback.accept(progress);
                        }

                        if (totalBytesRead % (BUFFER_SIZE * 100) == 0) {
                            repository.save(download);
                        }
                    }
                }

                output.flush();
            }
        }
    }

    private void startNextDownload() {
        if (activeDownloads.size() >= MAX_CONCURRENT_DOWNLOADS) {
            return;
        }

        try {
            List<Download> pending = repository.findPending();
            if (!pending.isEmpty()) {
                Download next = pending.get(0);
                startDownload(next.getId(), null);
            }
        } catch (SQLException e) {
            logger.error("Failed to start next download", e);
        }
    }

    public void cancelDownload(String downloadId) {
        CompletableFuture<Void> future = activeDownloads.get(downloadId);
        if (future != null) {
            future.cancel(true);
            activeDownloads.remove(downloadId);

            try {
                Download download = repository.findById(downloadId);
                if (download != null) {
                    download.setStatus(DownloadStatus.FAILED);
                    download.setErrorMessage("Cancelled by user");
                    repository.save(download);
                }
            } catch (SQLException e) {
                logger.error("Failed to update cancelled download", e);
            }
        }
    }

    public List<Download> getAllDownloads() throws SQLException {
        return repository.findAll();
    }

    public List<Download> getActiveDownloads() throws SQLException {
        return repository.findInProgress();
    }

    public void retryDownload(String downloadId) {
        try {
            Download download = repository.findById(downloadId);
            if (download != null && download.getStatus() == DownloadStatus.FAILED) {
                download.setStatus(DownloadStatus.PENDING);
                download.setProgress(0.0);
                download.setErrorMessage(null);
                repository.save(download);

                startNextDownload();
            }
        } catch (SQLException e) {
            logger.error("Failed to retry download", e);
        }
    }

    public void shutdown() {
        logger.info("Shutting down DownloadService");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String extractFilename(String url) {
        String filename = url.substring(url.lastIndexOf('/') + 1);

        int queryIndex = filename.indexOf('?');
        if (queryIndex > 0) {
            filename = filename.substring(0, queryIndex);
        }

        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // ensure .pdf extension
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename = filename + ".pdf";
        }

        return filename;
    }
}
