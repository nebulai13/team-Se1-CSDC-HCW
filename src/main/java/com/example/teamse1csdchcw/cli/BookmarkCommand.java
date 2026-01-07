package com.example.teamse1csdchcw.cli;

import com.example.teamse1csdchcw.domain.user.Bookmark;
import com.example.teamse1csdchcw.repository.BookmarkRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "bookmark",
        description = "Manage bookmarks",
        subcommands = {
                BookmarkCommand.ListCommand.class,
                BookmarkCommand.DeleteCommand.class,
                BookmarkCommand.FindCommand.class
        }
)
public class BookmarkCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'bookmark list', 'bookmark delete', or 'bookmark find'");
        return 0;
    }

    @Command(name = "list", description = "List all bookmarks")
    static class ListCommand implements Callable<Integer> {

        @Option(names = {"-v", "--verbose"},
                description = "Show full details")
        private boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                BookmarkRepository repo = new BookmarkRepository();
                List<Bookmark> bookmarks = repo.findAll();

                if (bookmarks.isEmpty()) {
                    System.out.println("No bookmarks found.");
                    return 0;
                }

                System.out.println();
                System.out.println("Bookmarks (" + bookmarks.size() + "):");
                System.out.println("─".repeat(100));

                for (int i = 0; i < bookmarks.size(); i++) {
                    Bookmark bm = bookmarks.get(i);
                    System.out.println((i + 1) + ". " + bm.getTitle());

                    if (verbose) {
                        if (bm.getUrl() != null) {
                            System.out.println("   URL: " + bm.getUrl());
                        }
                        if (bm.getNotes() != null && !bm.getNotes().isEmpty()) {
                            System.out.println("   Notes: " + bm.getNotes());
                        }
                        if (bm.getTags() != null && !bm.getTags().isEmpty()) {
                            System.out.println("   Tags: " + String.join(", ", bm.getTags()));
                        }
                        if (bm.getCreatedAt() != null) {
                            System.out.println("   Created: " + bm.getCreatedAt());
                        }
                        System.out.println();
                    }
                }

                if (!verbose) {
                    System.out.println("─".repeat(100));
                    System.out.println("Use --verbose to see full details");
                }

                return 0;

            } catch (Exception e) {
                System.err.println("Failed to list bookmarks: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "delete", description = "Delete a bookmark by ID")
    static class DeleteCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Bookmark ID to delete")
        private String bookmarkId;

        @Override
        public Integer call() throws Exception {
            try {
                BookmarkRepository repo = new BookmarkRepository();
                repo.delete(bookmarkId);

                System.out.println("Bookmark deleted successfully.");
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to delete bookmark: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "find", description = "Find bookmarks by tag")
    static class FindCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Tag to search for")
        private String tag;

        @Override
        public Integer call() throws Exception {
            try {
                BookmarkRepository repo = new BookmarkRepository();
                List<Bookmark> bookmarks = repo.findByTag(tag);

                if (bookmarks.isEmpty()) {
                    System.out.println("No bookmarks found with tag: " + tag);
                    return 0;
                }

                System.out.println();
                System.out.println("Bookmarks with tag '" + tag + "' (" + bookmarks.size() + "):");
                System.out.println("─".repeat(100));

                for (int i = 0; i < bookmarks.size(); i++) {
                    Bookmark bm = bookmarks.get(i);
                    System.out.println((i + 1) + ". " + bm.getTitle());
                    if (bm.getUrl() != null) {
                        System.out.println("   URL: " + bm.getUrl());
                    }
                    System.out.println();
                }

                return 0;

            } catch (Exception e) {
                System.err.println("Failed to find bookmarks: " + e.getMessage());
                return 1;
            }
        }
    }
}
