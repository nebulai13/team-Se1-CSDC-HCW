package com.example.teamse1csdchcw.cli;

// -- alert domain model --
import com.example.teamse1csdchcw.domain.monitoring.Alert;
// -- data access for alerts --
import com.example.teamse1csdchcw.repository.AlertRepository;
// -- monitoring service runs searches & triggers notifications --
import com.example.teamse1csdchcw.service.monitoring.MonitoringService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

// -- monitor cmd: keyword monitoring & alerts --
// -- like google scholar alerts: notifies when new papers match keywords --
// -- subcommands: list, add, delete, check --
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

    // -- no subcommand = show usage --
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'monitor list', 'monitor add', 'monitor delete', or 'monitor check'");
        return 0;
    }

    // -- list subcommand: shows all configured alerts --
    // -- usage: libsearch monitor list --
    @Command(name = "list", description = "List all alerts")
    static class ListCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                AlertRepository repo = new AlertRepository();
                // -- get all alerts from sqlite --
                List<Alert> alerts = repo.findAll();

                // -- handle empty case --
                if (alerts.isEmpty()) {
                    System.out.println("No alerts configured.");
                    return 0;
                }

                System.out.println();
                System.out.println("Configured Alerts (" + alerts.size() + "):");
                System.out.println("─".repeat(100));

                // -- print each alert's config --
                for (int i = 0; i < alerts.size(); i++) {
                    Alert alert = alerts.get(i);
                    // -- show enabled/disabled status --
                    String status = alert.isEnabled() ? "ENABLED" : "DISABLED";

                    System.out.printf("%d. %s [%s]%n", i + 1, alert.getName(), status);
                    // -- keywords that trigger this alert --
                    System.out.println("   Keywords: " + String.join(", ", alert.getKeywords()));

                    // -- optional author filter --
                    if (alert.getAuthorFilter() != null) {
                        System.out.println("   Author: " + alert.getAuthorFilter());
                    }

                    // -- optional year range filter --
                    if (alert.getYearFrom() != null || alert.getYearTo() != null) {
                        String yearRange = "";
                        if (alert.getYearFrom() != null) yearRange += alert.getYearFrom();
                        yearRange += " - ";
                        if (alert.getYearTo() != null) yearRange += alert.getYearTo();
                        System.out.println("   Year Range: " + yearRange);
                    }

                    // -- how user is notified: console, log, or email --
                    System.out.println("   Notification: " + alert.getNotificationType());

                    // -- show stats if alert has been checked before --
                    if (alert.getLastChecked() != null) {
                        System.out.println("   Last Checked: " + alert.getLastChecked());
                        System.out.println("   Total Matches: " + alert.getMatchCount());
                    }

                    // -- show uuid for delete command --
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

    // -- add subcommand: create new keyword alert --
    // -- usage: libsearch monitor add "ml papers" machine learning -a "hinton" --
    @Command(name = "add", description = "Add a new alert")
    static class AddCommand implements Callable<Integer> {

        // -- first arg: human-readable name for alert --
        @Parameters(index = "0", description = "Alert name")
        private String name;

        // -- remaining args: keywords to search for --
        // -- index="1..*" = all args from position 1 onwards --
        @Parameters(index = "1..*", description = "Keywords to monitor (space-separated)")
        private List<String> keywords;

        // -- optional filters --
        @Option(names = {"-a", "--author"}, description = "Filter by author")
        private String author;

        @Option(names = {"-y", "--year-from"}, description = "Filter from year")
        private Integer yearFrom;

        @Option(names = {"-Y", "--year-to"}, description = "Filter to year")
        private Integer yearTo;

        // -- notification type: where to send alerts --
        @Option(names = {"-n", "--notification"}, description = "Notification type: CONSOLE, LOG, EMAIL")
        private String notificationType = "CONSOLE";

        // -- email address for EMAIL notification type --
        @Option(names = {"-t", "--target"}, description = "Notification target (email address)")
        private String target;

        @Override
        public Integer call() throws Exception {
            try {
                // -- create alert pojo w/ all config --
                Alert alert = new Alert();
                alert.setName(name);
                alert.setKeywords(keywords);
                alert.setAuthorFilter(author);
                alert.setYearFrom(yearFrom);
                alert.setYearTo(yearTo);
                // -- parse string to enum --
                alert.setNotificationType(Alert.NotificationType.valueOf(notificationType.toUpperCase()));
                alert.setNotificationTarget(target);

                // -- persist to sqlite --
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

    // -- delete subcommand: remove alert by id --
    // -- usage: libsearch monitor delete <uuid> --
    @Command(name = "delete", description = "Delete an alert")
    static class DeleteCommand implements Callable<Integer> {

        // -- uuid from "monitor list" command --
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

    // -- check subcommand: run all alerts immediately --
    // -- searches sources & triggers notifications for matches --
    // -- usage: libsearch monitor check --
    @Command(name = "check", description = "Check all alerts now")
    static class CheckCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                System.out.println("Checking all enabled alerts...");
                System.out.println();

                // -- monitoring service orchestrates searches --
                MonitoringService service = new MonitoringService();
                // -- runs each enabled alert, notifies on matches --
                service.checkAllAlerts();

                System.out.println("Alert check completed.");

                // -- cleanup resources --
                service.shutdown();
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to check alerts: " + e.getMessage());
                return 1;
            }
        }
    }
}
