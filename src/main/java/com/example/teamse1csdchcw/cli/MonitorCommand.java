package com.example.teamse1csdchcw.cli;

import com.example.teamse1csdchcw.domain.monitoring.Alert;
import com.example.teamse1csdchcw.repository.AlertRepository;
import com.example.teamse1csdchcw.service.monitoring.MonitoringService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "monitor",
        description = "Keyword monitoring and alerts",
        subcommands = {
                MonitorCommand.ListCommand.class,
                MonitorCommand.AddCommand.class,
                MonitorCommand.DeleteCommand.class,
                MonitorCommand.CheckCommand.class
        }
)
public class MonitorCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'monitor list', 'monitor add', 'monitor delete', or 'monitor check'");
        return 0;
    }

    @Command(name = "list", description = "List all alerts")
    static class ListCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                AlertRepository repo = new AlertRepository();
                List<Alert> alerts = repo.findAll();

                if (alerts.isEmpty()) {
                    System.out.println("No alerts configured.");
                    return 0;
                }

                System.out.println();
                System.out.println("Configured Alerts (" + alerts.size() + "):");
                System.out.println("─".repeat(100));

                for (int i = 0; i < alerts.size(); i++) {
                    Alert alert = alerts.get(i);
                    String status = alert.isEnabled() ? "ENABLED" : "DISABLED";

                    System.out.printf("%d. %s [%s]%n", i + 1, alert.getName(), status);
                    System.out.println("   Keywords: " + String.join(", ", alert.getKeywords()));

                    if (alert.getAuthorFilter() != null) {
                        System.out.println("   Author: " + alert.getAuthorFilter());
                    }

                    if (alert.getYearFrom() != null || alert.getYearTo() != null) {
                        String yearRange = "";
                        if (alert.getYearFrom() != null) yearRange += alert.getYearFrom();
                        yearRange += " - ";
                        if (alert.getYearTo() != null) yearRange += alert.getYearTo();
                        System.out.println("   Year Range: " + yearRange);
                    }

                    System.out.println("   Notification: " + alert.getNotificationType());

                    if (alert.getLastChecked() != null) {
                        System.out.println("   Last Checked: " + alert.getLastChecked());
                        System.out.println("   Total Matches: " + alert.getMatchCount());
                    }

                    System.out.println("   ID: " + alert.getId());
                    System.out.println();
                }

                System.out.println("─".repeat(100));

                return 0;

            } catch (Exception e) {
                System.err.println("Failed to list alerts: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "add", description = "Add a new alert")
    static class AddCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Alert name")
        private String name;

        @Parameters(index = "1..*", description = "Keywords to monitor (space-separated)")
        private List<String> keywords;

        @Option(names = {"-a", "--author"}, description = "Filter by author")
        private String author;

        @Option(names = {"-y", "--year-from"}, description = "Filter from year")
        private Integer yearFrom;

        @Option(names = {"-Y", "--year-to"}, description = "Filter to year")
        private Integer yearTo;

        @Option(names = {"-n", "--notification"}, description = "Notification type: CONSOLE, LOG, EMAIL")
        private String notificationType = "CONSOLE";

        @Option(names = {"-t", "--target"}, description = "Notification target (email address)")
        private String target;

        @Override
        public Integer call() throws Exception {
            try {
                Alert alert = new Alert();
                alert.setName(name);
                alert.setKeywords(keywords);
                alert.setAuthorFilter(author);
                alert.setYearFrom(yearFrom);
                alert.setYearTo(yearTo);
                alert.setNotificationType(Alert.NotificationType.valueOf(notificationType.toUpperCase()));
                alert.setNotificationTarget(target);

                AlertRepository repo = new AlertRepository();
                repo.save(alert);

                System.out.println("Alert created successfully.");
                System.out.println("ID: " + alert.getId());

                return 0;

            } catch (Exception e) {
                System.err.println("Failed to create alert: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "delete", description = "Delete an alert")
    static class DeleteCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Alert ID to delete")
        private String alertId;

        @Override
        public Integer call() throws Exception {
            try {
                AlertRepository repo = new AlertRepository();
                repo.delete(alertId);

                System.out.println("Alert deleted successfully.");
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to delete alert: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "check", description = "Check all alerts now")
    static class CheckCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                System.out.println("Checking all enabled alerts...");
                System.out.println();

                MonitoringService service = new MonitoringService();
                service.checkAllAlerts();

                System.out.println("Alert check completed.");

                service.shutdown();
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to check alerts: " + e.getMessage());
                return 1;
            }
        }
    }
}
