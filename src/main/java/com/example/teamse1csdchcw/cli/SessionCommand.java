package com.example.teamse1csdchcw.cli;

// -- session services: audit logging & research sessions --
import com.example.teamse1csdchcw.service.session.JournalService;  // -- activity audit log --
import com.example.teamse1csdchcw.service.session.SessionService;  // -- research session mgmt --
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

// -- session cmd: manage research sessions & view activity --
// -- sessions group searches for a research project --
// -- journal = audit log of all user actions --
// -- subcommands: list, journal --
@Command(
        name = "session",
        description = "Session and journal management",
        subcommands = {
                SessionCommand.ListCommand.class,
                SessionCommand.JournalCommand.class
        }
)
public class SessionCommand implements Callable<Integer> {

    // -- no subcommand = show usage --
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'session list' or 'session journal'");
        return 0;
    }

    // -- list subcommand: shows recent research sessions --
    // -- usage: libsearch session list [-n 20] --
    @Command(name = "list", description = "List recent sessions")
    static class ListCommand implements Callable<Integer> {

        // -- -n flag: how many sessions to show --
        @Option(names = {"-n", "--number"}, description = "Number of sessions to show (default: 10)")
        private int number = 10;

        @Override
        public Integer call() throws Exception {
            try {
                SessionService service = new SessionService();
                // -- fetch recent sessions from sqlite --
                List<SessionService.SessionInfo> sessions = service.getRecentSessions(number);

                // -- handle empty case --
                if (sessions.isEmpty()) {
                    System.out.println("No sessions found.");
                    return 0;
                }

                // -- print session list --
                System.out.println();
                System.out.println("Recent Sessions (" + sessions.size() + "):");
                System.out.println("─".repeat(100));

                // -- sessioninfo has toString() for display --
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

    // -- journal subcommand: view activity audit log --
    // -- logs searches, bookmarks, downloads, etc --
    // -- usage: libsearch session journal [-n 50] [session-id] --
    @Command(name = "journal", description = "View activity journal")
    static class JournalCommand implements Callable<Integer> {

        // -- -n flag: how many entries to show --
        @Option(names = {"-n", "--number"}, description = "Number of entries to show (default: 20)")
        private int number = 20;

        // -- optional: filter by session id --
        // -- arity="0..1" means 0 or 1 values (optional arg) --
        @Parameters(index = "0", arity = "0..1", description = "Session ID (optional)")
        private String sessionId;

        @Override
        public Integer call() throws Exception {
            try {
                JournalService service = new JournalService();
                List<JournalService.JournalEntry> entries;

                // -- filter by session or get all recent --
                if (sessionId != null) {
                    // -- get entries for specific session --
                    entries = service.getSessionEntries(sessionId);
                    System.out.println();
                    System.out.println("Journal for session: " + sessionId);
                } else {
                    // -- get recent entries across all sessions --
                    entries = service.getRecentEntries(number);
                    System.out.println();
                    System.out.println("Recent Activity (last " + number + " entries):");
                }

                System.out.println("─".repeat(100));

                // -- print entries or empty msg --
                if (entries.isEmpty()) {
                    System.out.println("No journal entries found.");
                } else {
                    // -- journalentry has toString() for display --
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
