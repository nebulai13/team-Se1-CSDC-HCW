package com.example.teamse1csdchcw.service.connector;

import com.example.teamse1csdchcw.domain.source.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Factory for creating SourceConnector instances.
 * Uses singleton pattern to cache connector instances.
 */
public class ConnectorFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConnectorFactory.class);
    private static ConnectorFactory instance;

    private final Map<SourceType, SourceConnector> connectors;

    private ConnectorFactory() {
        this.connectors = new HashMap<>();
        initializeConnectors();
    }

    /**
     * Get singleton instance of ConnectorFactory.
     */
    public static synchronized ConnectorFactory getInstance() {
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
            // Academic sources
            registerConnector(new ArxivConnector());
            registerConnector(new PubMedConnector());
            registerConnector(new CrossRefConnector());
            registerConnector(new SemanticScholarConnector());

            // TODO: Add more connectors as they're implemented:
            // registerConnector(new GoogleScholarConnector());
            // registerConnector(new PrimoConnector());
            // registerConnector(new OBVConnector());
            // registerConnector(new IEEEConnector());
            // registerConnector(new DuckDuckGoConnector());
            // registerConnector(new BingConnector());
            // registerConnector(new BraveConnector());

            logger.info("Initialized {} connectors", connectors.size());
        } catch (Exception e) {
            logger.error("Failed to initialize connectors", e);
        }
    }

    /**
     * Register a connector instance.
     */
    private void registerConnector(SourceConnector connector) {
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

        for (SourceType type : types) {
            SourceConnector connector = getConnector(type);
            if (connector != null) {
                result.add(connector);
            } else {
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

        for (SourceConnector connector : connectors.values()) {
            try {
                if (connector.isAvailable()) {
                    available.add(connector);
                } else {
                    logger.debug("Connector not available: {}", connector.getSourceType());
                }
            } catch (Exception e) {
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
        return new HashSet<>(connectors.keySet());
    }

    /**
     * Get default academic sources (arXiv, PubMed, CrossRef, Semantic Scholar).
     *
     * @return list of default academic connectors
     */
    public List<SourceConnector> getDefaultAcademicConnectors() {
        List<SourceConnector> academic = new ArrayList<>();

        SourceType[] defaultTypes = {
                SourceType.ARXIV,
                SourceType.PUBMED,
                SourceType.CROSSREF,
                SourceType.SEMANTIC_SCHOLAR
        };

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
        connectors.clear();
        initializeConnectors();
    }
}
