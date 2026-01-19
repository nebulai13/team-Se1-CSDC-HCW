package com.example.teamse1csdchcw.service.connector;

import com.example.teamse1csdchcw.domain.source.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Factory for creating SourceConnector instances.
 * Uses singleton pattern to cache connector instances.
 */
// factory pattern: centralize obj creation - easy to add new connectors
// singleton pattern: single shared instance w/ cached connectors
public class ConnectorFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConnectorFactory.class);

    // singleton instance - shared across entire app
    private static ConnectorFactory instance;

    // cache of connector instances - one per source type
    // avoids recreating expensive http clients repeatedly
    private final Map<SourceType, SourceConnector> connectors;

    // private constructor - prevents external instantiation (singleton pattern)
    private ConnectorFactory() {
        this.connectors = new HashMap<>();
        initializeConnectors();  // eager init all connectors at startup
    }

    /**
     * Get singleton instance of ConnectorFactory.
     */
    // synchronized prevents race conditions w/ multiple threads
    public static synchronized ConnectorFactory getInstance() {
        // lazy init - create instance on first access
        if (instance == null) {
            instance = new ConnectorFactory();
        }
        return instance;
    }

    /**
     * Initialize all available connectors.
     */
    private void initializeConnectors() {
        try {
            // register all implemented academic source connectors
            registerConnector(new ArxivConnector());
            registerConnector(new PubMedConnector());
            registerConnector(new CrossRefConnector());
            registerConnector(new SemanticScholarConnector());

            // future connectors can be added here:
            // registerConnector(new GoogleScholarConnector());
            // registerConnector(new PrimoConnector());
            // registerConnector(new OBVConnector());
            // registerConnector(new IEEEConnector());
            // registerConnector(new DuckDuckGoConnector());
            // registerConnector(new BingConnector());
            // registerConnector(new BraveConnector());

            logger.info("Initialized {} connectors", connectors.size());
        } catch (Exception e) {
            // log but don't crash - app can work w/ partial connector set
            logger.error("Failed to initialize connectors", e);
        }
    }

    /**
     * Register a connector instance.
     */
    private void registerConnector(SourceConnector connector) {
        // use source type as map key for O(1) lookup
        SourceType type = connector.getSourceType();
        connectors.put(type, connector);
        logger.debug("Registered connector: {}", type.getDisplayName());
    }

    /**
     * Get a connector for the specified source type.
     *
     * @param type the source type
     * @return the connector, or null if not available
     */
    public SourceConnector getConnector(SourceType type) {
        // simple lookup - O(1) access to cached connector
        return connectors.get(type);
    }

    /**
     * Get connectors for multiple source types.
     *
     * @param types the source types
     * @return list of available connectors (may be fewer than requested if some are unavailable)
     */
    public List<SourceConnector> getConnectors(Set<SourceType> types) {
        List<SourceConnector> result = new ArrayList<>();

        // fetch each requested connector - skip if not registered
        for (SourceType type : types) {
            SourceConnector connector = getConnector(type);
            if (connector != null) {
                result.add(connector);
            } else {
                // warn but continue - partial results better than none
                logger.warn("No connector available for source type: {}", type);
            }
        }

        return result;
    }

    /**
     * Get all registered connectors.
     *
     * @return list of all connectors
     */
    public List<SourceConnector> getAllConnectors() {
        // return copy to prevent external modification of cache
        return new ArrayList<>(connectors.values());
    }

    /**
     * Get all available (working) connectors.
     * This checks each connector's isAvailable() method.
     *
     * @return list of available connectors
     */
    public List<SourceConnector> getAvailableConnectors() {
        List<SourceConnector> available = new ArrayList<>();

        // health check each connector - api may be down
        for (SourceConnector connector : connectors.values()) {
            try {
                // isAvailable() typically does lightweight connectivity test
                if (connector.isAvailable()) {
                    available.add(connector);
                } else {
                    logger.debug("Connector not available: {}", connector.getSourceType());
                }
            } catch (Exception e) {
                // catch exceptions from health checks - don't crash entire app
                logger.warn("Error checking availability for {}: {}",
                        connector.getSourceType(), e.getMessage());
            }
        }

        return available;
    }

    /**
     * Check if a connector is registered for the given source type.
     *
     * @param type the source type
     * @return true if a connector is registered
     */
    public boolean hasConnector(SourceType type) {
        // simple presence check - doesn't test if actually working
        return connectors.containsKey(type);
    }

    /**
     * Get the number of registered connectors.
     *
     * @return count of registered connectors
     */
    public int getConnectorCount() {
        return connectors.size();
    }

    /**
     * Get all registered source types.
     *
     * @return set of source types
     */
    public Set<SourceType> getRegisteredSourceTypes() {
        // return copy of keyset to prevent modification
        return new HashSet<>(connectors.keySet());
    }

    /**
     * Get default academic sources (arXiv, PubMed, CrossRef, Semantic Scholar).
     *
     * @return list of default academic connectors
     */
    public List<SourceConnector> getDefaultAcademicConnectors() {
        List<SourceConnector> academic = new ArrayList<>();

        // predefined set of reliable academic sources
        SourceType[] defaultTypes = {
                SourceType.ARXIV,
                SourceType.PUBMED,
                SourceType.CROSSREF,
                SourceType.SEMANTIC_SCHOLAR
        };

        // fetch each default connector - skip if not registered
        for (SourceType type : defaultTypes) {
            SourceConnector connector = getConnector(type);
            if (connector != null) {
                academic.add(connector);
            }
        }

        return academic;
    }

    /**
     * Reset the factory (mainly for testing).
     */
    public void reset() {
        // clear cache and reinit - useful for testing different configs
        connectors.clear();
        initializeConnectors();
    }
}
