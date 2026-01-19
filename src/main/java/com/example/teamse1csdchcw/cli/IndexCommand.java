package com.example.teamse1csdchcw.cli;

// -- lucene index mgmt service --
import com.example.teamse1csdchcw.service.index.IndexService;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

// -- index cmd: manage lucene full-text search index --
// -- lucene = high-perf search engine lib used for offline search --
// -- subcommands: stats, optimize, clear --
@Command(
        name = "index",
        description = "Manage the local search index",
        subcommands = {
                IndexCommand.StatsCommand.class,
                IndexCommand.OptimizeCommand.class,
                IndexCommand.ClearCommand.class
        }
)
public class IndexCommand implements Callable<Integer> {

    // -- no subcommand = show usage --
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'index stats', 'index optimize', or 'index clear'");
        return 0;
    }

    // -- stats subcommand: shows doc count, size, etc --
    // -- usage: libsearch index stats --
    @Command(name = "stats", description = "Show index statistics")
    static class StatsCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                // -- create index service (opens lucene index) --
                IndexService indexService = new IndexService();
                // -- get stats object w/ counts --
                IndexService.IndexStats stats = indexService.getStats();

                // -- print stats table --
                System.out.println();
                System.out.println("Index Statistics:");
                System.out.println("─".repeat(50));
                // -- active docs (not marked deleted) --
                System.out.println("Documents:        " + stats.documentCount);
                // -- docs pending deletion (will be removed on optimize) --
                System.out.println("Deleted docs:     " + stats.deletedDocCount);
                // -- active + deleted --
                System.out.println("Total docs:       " + stats.totalDocCount);
                // -- disk space used (formatted as KB/MB/GB) --
                System.out.println("Index size:       " + stats.getFormattedSize());
                System.out.println("─".repeat(50));

                // -- close lucene index reader/writer --
                indexService.close();
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to get index stats: " + e.getMessage());
                return 1;
            }
        }
    }

    // -- optimize subcommand: merge lucene segments --
    // -- lucene stores docs in segments; merging improves perf --
    // -- usage: libsearch index optimize --
    @Command(name = "optimize", description = "Optimize the index (merge segments)")
    static class OptimizeCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                System.out.println("Optimizing index...");

                IndexService indexService = new IndexService();
                // -- forceMerge(1) combines all segments into one --
                indexService.optimize();

                System.out.println("Index optimized successfully.");

                indexService.close();
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to optimize index: " + e.getMessage());
                return 1;
            }
        }
    }

    // -- clear subcommand: delete all indexed docs --
    // -- destructive: requires confirmation --
    // -- usage: libsearch index clear --
    @Command(name = "clear", description = "Clear all documents from the index")
    static class ClearCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                // -- prompt for confirmation (destructive op) --
                System.out.print("Are you sure you want to clear the index? (yes/no): ");
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                String response = scanner.nextLine().trim().toLowerCase();

                // -- only proceed if user types "yes" exactly --
                if (!response.equals("yes")) {
                    System.out.println("Operation cancelled.");
                    return 0;
                }

                System.out.println("Clearing index...");

                IndexService indexService = new IndexService();
                // -- deleteAll removes all docs from lucene index --
                indexService.deleteAll();

                System.out.println("Index cleared successfully.");

                indexService.close();
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to clear index: " + e.getMessage());
                return 1;
            }
        }
    }
}
