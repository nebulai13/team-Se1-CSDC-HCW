package com.example.teamse1csdchcw.service.monitoring;

import com.example.teamse1csdchcw.domain.monitoring.Alert;
import com.example.teamse1csdchcw.domain.search.SearchQuery;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import com.example.teamse1csdchcw.domain.source.SourceType;
import com.example.teamse1csdchcw.repository.AlertRepository;
import com.example.teamse1csdchcw.service.search.FederatedSearchService;
import com.example.teamse1csdchcw.service.search.QueryParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    private final AlertRepository alertRepository;
    private final FederatedSearchService searchService;
    private final QueryParserService queryParser;
    private final NotificationService notificationService;

    public MonitoringService() {
        this.alertRepository = new AlertRepository();
        this.searchService = new FederatedSearchService();
        this.queryParser = new QueryParserService();
        this.notificationService = new NotificationService();
    }

    public void checkAllAlerts() {
        try {
            List<Alert> alerts = alertRepository.findEnabled();
            logger.info("Checking {} enabled alerts", alerts.size());

            for (Alert alert : alerts) {
                try {
                    checkAlert(alert);
                } catch (Exception e) {
                    logger.error("Failed to check alert: {}", alert.getId(), e);
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to retrieve alerts", e);
        }
    }

    public void checkAlert(Alert alert) throws Exception {
        logger.debug("Checking alert: {} ({})", alert.getName(), alert.getId());

        SearchQuery query = buildQuery(alert);

        Set<SourceType> sources = new HashSet<>();
        sources.add(SourceType.ARXIV);
        sources.add(SourceType.PUBMED);
        sources.add(SourceType.CROSSREF);

        List<SearchResult> results = searchService.search(query, sources);

        if (!results.isEmpty()) {
            logger.info("Alert '{}' matched {} results", alert.getName(), results.size());

            notificationService.notify(alert, results);

            alertRepository.updateLastChecked(alert.getId(), LocalDateTime.now(), results.size());
        } else {
            logger.debug("Alert '{}' had no matches", alert.getName());
            alertRepository.updateLastChecked(alert.getId(), LocalDateTime.now(), 0);
        }
    }

    private SearchQuery buildQuery(Alert alert) {
        String queryText = String.join(" OR ", alert.getKeywords());
        SearchQuery query = queryParser.parse(queryText);

        if (alert.getAuthorFilter() != null) {
            query.setAuthorFilter(alert.getAuthorFilter());
        }

        if (alert.getYearFrom() != null) {
            query.setYearFrom(alert.getYearFrom());
        }

        if (alert.getYearTo() != null) {
            query.setYearTo(alert.getYearTo());
        }

        return query;
    }

    public void shutdown() {
        if (searchService != null) {
            searchService.shutdown();
        }
    }
}
