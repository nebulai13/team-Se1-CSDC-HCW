package com.example.teamse1csdchcw.cli;

import com.example.teamse1csdchcw.service.session.JournalService;
import com.example.teamse1csdchcw.service.session.SessionService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "session",
        description = "Session and journal management",
        subcommands = {
                SessionCommand.ListCommand.class,
                SessionCommand.JournalCommand.class
        }
)
public class SessionCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'session list' or 'session journal'");
        return 0;
    }

    @Command(name = "list", description = "List recent sessions")
    static class ListCommand implements Callable<Integer> {

        @Option(names = {"-n", "--number"}, description = "Number of sessions to show (default: 10)")
        private int number = 10;

        @Override
        public Integer call() throws Exception {
            try {
                SessionService service = new SessionService();
                List<SessionService.SessionInfo> sessions = service.getRecentSessions(number);

                if (sessions.isEmpty()) {
                    System.out.println("No sessions found.");
                    return 0;
                }

                System.out.println();
                System.out.println("Recent Sessions (" + sessions.size() + "):");
                System.out.println("─".repeat(100));

                for (int i = 0; i < sessions.size(); i++) {
                    System.out.println((i + 1) + ". " + sessions.get(i));
                }

                System.out.println("─".repeat(100));

                return 0;

            } catch (Exception e) {
                System.err.println("Failed to list sessions: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "journal", description = "View activity journal")
    static class JournalCommand implements Callable<Integer> {

        @Option(names = {"-n", "--number"}, description = "Number of entries to show (default: 20)")
        private int number = 20;

        @Parameters(index = "0", arity = "0..1", description = "Session ID (optional)")
        private String sessionId;

        @Override
        public Integer call() throws Exception {
            try {
                JournalService service = new JournalService();
                List<JournalService.JournalEntry> entries;

                if (sessionId != null) {
                    entries = service.getSessionEntries(sessionId);
                    System.out.println();
                    System.out.println("Journal for session: " + sessionId);
                } else {
                    entries = service.getRecentEntries(number);
                    System.out.println();
                    System.out.println("Recent Activity (last " + number + " entries):");
                }

                System.out.println("─".repeat(100));

                if (entries.isEmpty()) {
                    System.out.println("No journal entries found.");
                } else {
                    for (JournalService.JournalEntry entry : entries) {
                        System.out.println(entry);
                    }
                }

                System.out.println("─".repeat(100));

                return 0;

            } catch (Exception e) {
                System.err.println("Failed to view journal: " + e.getMessage());
                return 1;
            }
        }
    }
}
