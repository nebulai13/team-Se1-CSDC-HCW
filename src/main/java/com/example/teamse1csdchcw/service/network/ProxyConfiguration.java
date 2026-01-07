package com.example.teamse1csdchcw.service.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.prefs.Preferences;

public class ProxyConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ProxyConfiguration.class);
    private static final String PROXY_ENABLED_KEY = "proxy.enabled";
    private static final String PROXY_TYPE_KEY = "proxy.type";
    private static final String PROXY_HOST_KEY = "proxy.host";
    private static final String PROXY_PORT_KEY = "proxy.port";
    private static final String PROXY_USERNAME_KEY = "proxy.username";
    private static final String PROXY_PASSWORD_KEY = "proxy.password";

    private final Preferences prefs;

    private boolean enabled;
    private Proxy.Type type;
    private String host;
    private int port;
    private String username;
    private String password;

    public ProxyConfiguration() {
        this.prefs = Preferences.userNodeForPackage(ProxyConfiguration.class);
        loadFromPreferences();
    }

    private void loadFromPreferences() {
        this.enabled = prefs.getBoolean(PROXY_ENABLED_KEY, false);
        String typeStr = prefs.get(PROXY_TYPE_KEY, "HTTP");
        this.type = Proxy.Type.valueOf(typeStr);
        this.host = prefs.get(PROXY_HOST_KEY, "");
        this.port = prefs.getInt(PROXY_PORT_KEY, 8080);
        this.username = prefs.get(PROXY_USERNAME_KEY, "");
        this.password = prefs.get(PROXY_PASSWORD_KEY, "");

        logger.info("Loaded proxy configuration: enabled={}, type={}, host={}, port={}",
                enabled, type, host, port);
    }

    public void save() {
        prefs.putBoolean(PROXY_ENABLED_KEY, enabled);
        prefs.put(PROXY_TYPE_KEY, type.name());
        prefs.put(PROXY_HOST_KEY, host);
        prefs.putInt(PROXY_PORT_KEY, port);
        prefs.put(PROXY_USERNAME_KEY, username != null ? username : "");
        prefs.put(PROXY_PASSWORD_KEY, password != null ? password : "");

        logger.info("Saved proxy configuration");
    }

    public Proxy createProxy() {
        if (!enabled || host == null || host.isEmpty()) {
            return Proxy.NO_PROXY;
        }

        InetSocketAddress address = new InetSocketAddress(host, port);
        return new Proxy(type, address);
    }

    public boolean isAuthenticationRequired() {
        return username != null && !username.isEmpty();
    }

    public static ProxyConfiguration fromSystemProperties() {
        ProxyConfiguration config = new ProxyConfiguration();

        String httpProxyHost = System.getProperty("http.proxyHost");
        String httpProxyPort = System.getProperty("http.proxyPort");
        String httpsProxyHost = System.getProperty("https.proxyHost");
        String httpsProxyPort = System.getProperty("https.proxyPort");
        String socksProxyHost = System.getProperty("socksProxyHost");
        String socksProxyPort = System.getProperty("socksProxyPort");

        if (httpsProxyHost != null && !httpsProxyHost.isEmpty()) {
            config.setEnabled(true);
            config.setType(Proxy.Type.HTTP);
            config.setHost(httpsProxyHost);
            config.setPort(httpsProxyPort != null ? Integer.parseInt(httpsProxyPort) : 443);
        } else if (httpProxyHost != null && !httpProxyHost.isEmpty()) {
            config.setEnabled(true);
            config.setType(Proxy.Type.HTTP);
            config.setHost(httpProxyHost);
            config.setPort(httpProxyPort != null ? Integer.parseInt(httpProxyPort) : 80);
        } else if (socksProxyHost != null && !socksProxyHost.isEmpty()) {
            config.setEnabled(true);
            config.setType(Proxy.Type.SOCKS);
            config.setHost(socksProxyHost);
            config.setPort(socksProxyPort != null ? Integer.parseInt(socksProxyPort) : 1080);
        }

        return config;
    }

    public void applyToSystemProperties() {
        if (enabled && host != null && !host.isEmpty()) {
            if (type == Proxy.Type.HTTP) {
                System.setProperty("http.proxyHost", host);
                System.setProperty("http.proxyPort", String.valueOf(port));
                System.setProperty("https.proxyHost", host);
                System.setProperty("https.proxyPort", String.valueOf(port));
            } else if (type == Proxy.Type.SOCKS) {
                System.setProperty("socksProxyHost", host);
                System.setProperty("socksProxyPort", String.valueOf(port));
            }

            if (username != null && !username.isEmpty()) {
                System.setProperty("http.proxyUser", username);
                System.setProperty("http.proxyPassword", password != null ? password : "");
            }

            logger.info("Applied proxy configuration to system properties");
        } else {
            clearSystemProperties();
        }
    }

    public void clearSystemProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");

        logger.info("Cleared proxy system properties");
    }

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Proxy.Type getType() { return type; }
    public void setType(Proxy.Type type) { this.type = type; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return String.format("ProxyConfiguration{enabled=%s, type=%s, host=%s, port=%d, auth=%s}",
                enabled, type, host, port, isAuthenticationRequired());
    }
}
