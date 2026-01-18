package com.example.teamse1csdchcw.util.http;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating configured OkHttp clients.
 */
public class HttpClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);

    private static OkHttpClient defaultClient;
    private static OkHttpClient debugClient;

    /**
     * Gets the default HTTP client with standard configuration.
     */
    public static synchronized OkHttpClient getDefaultClient() {
        if (defaultClient == null) {
            defaultClient = createClient(false);
        }
        return defaultClient;
    }

    /**
     * Gets an HTTP client with debug logging enabled.
     */
    public static synchronized OkHttpClient getDebugClient() {
        if (debugClient == null) {
            debugClient = createClient(true);
        }
        return debugClient;
    }

    /**
     * Creates a new HTTP client with specified configuration.
     */
    private static OkHttpClient createClient(boolean enableLogging) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true);

        // Add logging interceptor if enabled
        if (enableLogging) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                    message -> logger.debug("HTTP: {}", message));
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            builder.addInterceptor(loggingInterceptor);
        }

        // Add user agent interceptor
        builder.addInterceptor(chain -> chain.proceed(
                chain.request().newBuilder()
                        .header("User-Agent", "LibSearch/1.0 (Academic Search Tool)")
                        .build()
        ));

        return builder.build();
    }

    /**
     * Creates a custom client with specified timeout.
     */
    public static OkHttpClient createCustomClient(Duration timeout) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .callTimeout(timeout.multipliedBy(2))
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * Shuts down all clients and cleans up resources.
     */
    public static void shutdown() {
        if (defaultClient != null) {
            defaultClient.dispatcher().executorService().shutdown();
            defaultClient.connectionPool().evictAll();
        }
        if (debugClient != null) {
            debugClient.dispatcher().executorService().shutdown();
            debugClient.connectionPool().evictAll();
        }
        logger.info("HTTP clients shut down");
    }
}
