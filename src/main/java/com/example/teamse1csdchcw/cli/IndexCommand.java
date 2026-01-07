package com.example.teamse1csdchcw.cli;

import com.example.teamse1csdchcw.service.index.IndexService;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

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

    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'index stats', 'index optimize', or 'index clear'");
        return 0;
    }

    @Command(name = "stats", description = "Show index statistics")
    static class StatsCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                IndexService indexService = new IndexService();
                IndexService.IndexStats stats = indexService.getStats();

                System.out.println();
                System.out.println("Index Statistics:");
                System.out.println("─".repeat(50));
                System.out.println("Documents:        " + stats.documentCount);
                System.out.println("Deleted docs:     " + stats.deletedDocCount);
                System.out.println("Total docs:       " + stats.totalDocCount);
                System.out.println("Index size:       " + stats.getFormattedSize());
                System.out.println("─".repeat(50));

                indexService.close();
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to get index stats: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "optimize", description = "Optimize the index (merge segments)")
    static class OptimizeCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                System.out.println("Optimizing index...");

                IndexService indexService = new IndexService();
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

    @Command(name = "clear", description = "Clear all documents from the index")
    static class ClearCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            try {
                System.out.print("Are you sure you want to clear the index? (yes/no): ");
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                String response = scanner.nextLine().trim().toLowerCase();

                if (!response.equals("yes")) {
                    System.out.println("Operation cancelled.");
                    return 0;
                }

                System.out.println("Clearing index...");

                IndexService indexService = new IndexService();
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
