package com.example.teamse1csdchcw.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages application configuration from YAML files.
 * Loads default config from resources and user config from home directory.
 */
public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String DEFAULT_CONFIG = "/config/application.yaml";
    private static final String USER_CONFIG_PATH = System.getProperty("user.home") + "/.libsearch/config.yaml";

    private static ConfigService instance;
    private Map<String, Object> config;
    private final ObjectMapper yamlMapper;

    private ConfigService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.config = new HashMap<>();
        loadConfiguration();
    }

    /**
     * Gets the singleton instance.
     */
    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }

    /**
     * Loads configuration from default and user files.
     */
    @SuppressWarnings("unchecked")
    private void loadConfiguration() {
        try {
            // Load default configuration from resources
            logger.info("Loading default configuration...");
            try (InputStream is = getClass().getResourceAsStream(DEFAULT_CONFIG)) {
                if (is != null) {
                    Map<String, Object> defaultConfig = yamlMapper.readValue(is, Map.class);
                    config.putAll(defaultConfig);
                    logger.info("Default configuration loaded successfully");
                } else {
                    logger.warn("Default configuration file not found");
                }
            }

            // Load user configuration if it exists
            File userConfigFile = new File(USER_CONFIG_PATH);
            if (userConfigFile.exists()) {
                logger.info("Loading user configuration from: {}", USER_CONFIG_PATH);
                Map<String, Object> userConfig = yamlMapper.readValue(userConfigFile, Map.class);
                mergeMaps(config, userConfig);
                logger.info("User configuration loaded and merged");
            } else {
                logger.info("No user configuration file found at: {}", USER_CONFIG_PATH);
            }

            // Expand environment variables
            expandEnvironmentVariables(config);

        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    /**
     * Recursively merges source map into target map.
     */
    @SuppressWarnings("unchecked")
    private void mergeMaps(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() instanceof Map && target.get(entry.getKey()) instanceof Map) {
                mergeMaps((Map<String, Object>) target.get(entry.getKey()),
                         (Map<String, Object>) entry.getValue());
            } else {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Expands environment variables in format ${VAR_NAME:default}.
     */
    @SuppressWarnings("unchecked")
    private void expandEnvironmentVariables(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (value.startsWith("${") && value.endsWith("}")) {
                    String expanded = expandVariable(value);
                    entry.setValue(expanded);
                }
            } else if (entry.getValue() instanceof Map) {
                expandEnvironmentVariables((Map<String, Object>) entry.getValue());
            }
        }
    }

    /**
     * Expands a single environment variable.
     */
    private String expandVariable(String value) {
        String var = value.substring(2, value.length() - 1);
        String[] parts = var.split(":", 2);
        String varName = parts[0];
        String defaultValue = parts.length > 1 ? parts[1] : "";

        String envValue = System.getenv(varName);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Check system properties
        String propValue = System.getProperty(varName);
        if (propValue != null && !propValue.isEmpty()) {
            return propValue;
        }

        return defaultValue;
    }

    /**
     * Gets a configuration value by path (e.g., "app.name").
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path, T defaultValue) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                return defaultValue;
            }
            current = (Map<String, Object>) next;
        }

        Object value = current.get(parts[parts.length - 1]);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            logger.warn("Type mismatch for config key: {}", path);
            return defaultValue;
        }
    }

    /**
     * Gets a configuration value or throws exception if not found.
     */
    public <T> T getRequired(String path) {
        T value = get(path, null);
        if (value == null) {
            throw new IllegalStateException("Required configuration not found: " + path);
        }
        return value;
    }

    /**
     * Gets a string value.
     */
    public String getString(String path, String defaultValue) {
        return get(path, defaultValue);
    }

    /**
     * Gets an integer value.
     */
    public int getInt(String path, int defaultValue) {
        Object value = get(path, defaultValue);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a boolean value.
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path, defaultValue);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * Saves user configuration to file.
     */
    public void saveUserConfig() {
        try {
            Path configPath = Paths.get(USER_CONFIG_PATH);
            Files.createDirectories(configPath.getParent());
            yamlMapper.writeValue(configPath.toFile(), config);
            logger.info("User configuration saved to: {}", USER_CONFIG_PATH);
        } catch (Exception e) {
            logger.error("Failed to save user configuration", e);
        }
    }

    /**
     * Reloads configuration from files.
     */
    public void reload() {
        config.clear();
        loadConfiguration();
        logger.info("Configuration reloaded");
    }
}
