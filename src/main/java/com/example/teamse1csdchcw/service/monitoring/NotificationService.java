package com.example.teamse1csdchcw.service.monitoring;

import com.example.teamse1csdchcw.domain.monitoring.Alert;
import com.example.teamse1csdchcw.domain.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void notify(Alert alert, List<SearchResult> results) {
        switch (alert.getNotificationType()) {
            case CONSOLE:
                notifyConsole(alert, results);
                break;
            case LOG:
                notifyLog(alert, results);
                break;
            case EMAIL:
                notifyEmail(alert, results);
                break;
        }
    }

    private void notifyConsole(Alert alert, List<SearchResult> results) {
        System.out.println();
        System.out.println("═".repeat(80));
        System.out.println("ALERT: " + alert.getName());
        System.out.println("═".repeat(80));
        System.out.println("Matched " + results.size() + " new papers:");
        System.out.println();

        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            SearchResult result = results.get(i);
            System.out.println((i + 1) + ". " + result.getTitle());
            System.out.println("   Source: " + result.getSource().getDisplayName());
            if (result.getUrl() != null) {
                System.out.println("   URL: " + result.getUrl());
            }
            System.out.println();
        }

        if (results.size() > 5) {
            System.out.println("... and " + (results.size() - 5) + " more");
            System.out.println();
        }

        System.out.println("═".repeat(80));
        System.out.println();
    }

    private void notifyLog(Alert alert, List<SearchResult> results) {
        logger.info("Alert '{}' triggered with {} matches", alert.getName(), results.size());

        for (SearchResult result : results) {
            logger.info("  - {} ({})", result.getTitle(), result.getSource().getDisplayName());
        }
    }

    private void notifyEmail(Alert alert, List<SearchResult> results) {
        String target = alert.getNotificationTarget();

        if (target == null || target.isEmpty()) {
            logger.warn("Email notification configured but no target email specified");
            notifyLog(alert, results);
            return;
        }

        logger.info("Would send email to {} about {} matches (email not implemented)", target, results.size());
        notifyLog(alert, results);
    }
}
