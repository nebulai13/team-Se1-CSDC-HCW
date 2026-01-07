package com.example.teamse1csdchcw.service.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.function.Supplier;

public class RetryHandler {
    private static final Logger logger = LoggerFactory.getLogger(RetryHandler.class);

    private int maxRetries;
    private long initialDelayMs;
    private double backoffMultiplier;

    public RetryHandler() {
        this(3, 1000, 2.0);
    }

    public RetryHandler(int maxRetries, long initialDelayMs, double backoffMultiplier) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    public <T> T executeWithRetry(Supplier<T> operation) throws IOException {
        return executeWithRetry(operation, IOException.class);
    }

    public <T, E extends Exception> T executeWithRetry(Supplier<T> operation, Class<E> exceptionType) throws E {
        int attempt = 0;
        long delay = initialDelayMs;
        Exception lastException = null;

        while (attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    logger.debug("Retry attempt {} of {}", attempt, maxRetries);
                }

                return operation.get();

            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt > maxRetries) {
                    logger.warn("Max retries ({}) exceeded", maxRetries);
                    break;
                }

                if (isRetriable(e)) {
                    logger.debug("Retriable error on attempt {}: {}", attempt, e.getMessage());

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw createException(exceptionType, "Retry interrupted", ie);
                    }

                    delay = (long) (delay * backoffMultiplier);
                } else {
                    logger.debug("Non-retriable error: {}", e.getMessage());
                    break;
                }
            }
        }

        throw createException(exceptionType, "Operation failed after " + attempt + " attempts", lastException);
    }

    private boolean isRetriable(Exception e) {
        if (e instanceof SocketTimeoutException) {
            return true;
        }

        if (e instanceof IOException) {
            String message = e.getMessage();
            if (message != null) {
                return message.contains("timeout") ||
                       message.contains("Connection reset") ||
                       message.contains("Connection refused") ||
                       message.contains("Temporary failure");
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private <E extends Exception> E createException(Class<E> exceptionType, String message, Exception cause) {
        try {
            if (exceptionType == IOException.class) {
                return (E) new IOException(message, cause);
            } else if (exceptionType == RuntimeException.class) {
                return (E) new RuntimeException(message, cause);
            } else {
                return exceptionType.getConstructor(String.class, Throwable.class)
                        .newInstance(message, cause);
            }
        } catch (Exception e) {
            return (E) new RuntimeException(message, cause);
        }
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
}
