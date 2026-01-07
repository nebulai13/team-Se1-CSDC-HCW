package com.example.teamse1csdchcw.service.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetworkDiagnostics {
    private static final Logger logger = LoggerFactory.getLogger(NetworkDiagnostics.class);

    private static final String[] TEST_HOSTS = {
            "arxiv.org",
            "www.ncbi.nlm.nih.gov",
            "api.crossref.org",
            "api.semanticscholar.org"
    };

    public static class DiagnosticResult {
        public String host;
        public boolean reachable;
        public Long responseTimeMs;
        public String error;
        public String ipAddress;

        public DiagnosticResult(String host) {
            this.host = host;
            this.reachable = false;
        }

        @Override
        public String toString() {
            if (reachable) {
                return String.format("%s: OK (%dms) [%s]", host, responseTimeMs, ipAddress);
            } else {
                return String.format("%s: FAILED - %s", host, error);
            }
        }
    }

    public static List<DiagnosticResult> runDiagnostics() {
        List<DiagnosticResult> results = new ArrayList<>();

        for (String host : TEST_HOSTS) {
            results.add(testHost(host));
        }

        return results;
    }

    public static DiagnosticResult testHost(String host) {
        DiagnosticResult result = new DiagnosticResult(host);

        try {
            InetAddress address = InetAddress.getByName(host);
            result.ipAddress = address.getHostAddress();

            long startTime = System.currentTimeMillis();
            boolean reachable = address.isReachable(5000);
            long endTime = System.currentTimeMillis();

            if (reachable) {
                result.reachable = true;
                result.responseTimeMs = endTime - startTime;
            } else {
                result.error = "Host not reachable (ICMP)";
            }

        } catch (UnknownHostException e) {
            result.error = "Unknown host: " + e.getMessage();
            logger.debug("Unknown host: {}", host, e);
        } catch (IOException e) {
            result.error = "I/O error: " + e.getMessage();
            logger.debug("I/O error for host: {}", host, e);
        }

        return result;
    }

    public static DiagnosticResult testHttpConnection(String url) {
        String host = extractHost(url);
        DiagnosticResult result = new DiagnosticResult(host);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        try {
            long startTime = System.currentTimeMillis();
            Response response = client.newCall(request).execute();
            long endTime = System.currentTimeMillis();

            result.reachable = response.isSuccessful();
            result.responseTimeMs = endTime - startTime;

            if (!response.isSuccessful()) {
                result.error = "HTTP " + response.code() + ": " + response.message();
            }

            response.close();

        } catch (IOException e) {
            result.error = "Connection failed: " + e.getMessage();
            logger.debug("HTTP connection failed for URL: {}", url, e);
        }

        return result;
    }

    public static boolean testInternetConnectivity() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            return address.isReachable(3000);
        } catch (IOException e) {
            logger.debug("Internet connectivity test failed", e);
            return false;
        }
    }

    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.debug("Failed to get local host name", e);
            return "unknown";
        }
    }

    public static String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.debug("Failed to get local host address", e);
            return "unknown";
        }
    }

    private static String extractHost(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    public static class NetworkInfo {
        public String localHostName;
        public String localHostAddress;
        public boolean internetConnected;
        public ProxyConfiguration proxyConfig;
        public List<DiagnosticResult> sourceTests;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Network Information:\n");
            sb.append("  Local Host: ").append(localHostName).append("\n");
            sb.append("  Local IP: ").append(localHostAddress).append("\n");
            sb.append("  Internet: ").append(internetConnected ? "Connected" : "Disconnected").append("\n");
            sb.append("  Proxy: ").append(proxyConfig != null && proxyConfig.isEnabled() ? "Enabled" : "Disabled").append("\n");

            if (sourceTests != null && !sourceTests.isEmpty()) {
                sb.append("\nSource Connectivity:\n");
                for (DiagnosticResult result : sourceTests) {
                    sb.append("  ").append(result).append("\n");
                }
            }

            return sb.toString();
        }
    }

    public static NetworkInfo getNetworkInfo() {
        NetworkInfo info = new NetworkInfo();
        info.localHostName = getLocalHostName();
        info.localHostAddress = getLocalHostAddress();
        info.internetConnected = testInternetConnectivity();
        info.proxyConfig = new ProxyConfiguration();
        info.sourceTests = runDiagnostics();

        return info;
    }
}
