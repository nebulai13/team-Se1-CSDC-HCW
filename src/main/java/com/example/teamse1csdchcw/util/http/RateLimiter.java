package com.example.teamse1csdchcw.util.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiter for API calls to prevent overwhelming servers.
 * Implements singleton pattern for global rate limiting.
 */
public class RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    private static RateLimiter instance;

    private final ConcurrentMap<String, Instant> lastRequestTime;
    private final Duration minInterval;

    /**
     * Creates a rate limiter with specified minimum interval between requests.
     *
     * @param minInterval Minimum time between requests to the same domain
     */
    public RateLimiter(Duration minInterval) {
        this.minInterval = minInterval;
        this.lastRequestTime = new ConcurrentHashMap<>();
    }

    /**
     * Creates a rate limiter with 1 second interval (default).
     */
    public RateLimiter() {
        this(Duration.ofSeconds(1));
    }

    /**
     * Get singleton instance with default 1 second interval.
     */
    public static synchronized RateLimiter getInstance() {
        if (instance == null) {
            instance = new RateLimiter();
        }
        return instance;
    }

    /**
     * Acquires permission to make a request to the specified domain.
     * Blocks if necessary to maintain rate limit.
     *
     * @param domain The domain to rate limit
     */
    public void acquire(String domain) {
        Instant now = Instant.now();
        Instant lastRequest = lastRequestTime.get(domain);

        if (lastRequest != null) {
            Duration elapsed = Duration.between(lastRequest, now);
            if (elapsed.compareTo(minInterval) < 0) {
                Duration waitTime = minInterval.minus(elapsed);
                try {
                    logger.debug("Rate limiting {} - waiting {} ms",
                                domain, waitTime.toMillis());
                    Thread.sleep(waitTime.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Rate limiter interrupted for domain: {}", domain);
                }
            }
        }

        lastRequestTime.put(domain, Instant.now());
    }

    /**
     * Resets the rate limiter for a specific domain.
     */
    public void reset(String domain) {
        lastRequestTime.remove(domain);
    }

    /**
     * Resets the rate limiter for all domains.
     */
    public void resetAll() {
        lastRequestTime.clear();
    }

    /**
     * Gets the minimum interval between requests.
     */
    public Duration getMinInterval() {
        return minInterval;
    }
}
