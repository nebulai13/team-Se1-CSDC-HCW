package com.example.teamse1csdchcw.cli;

// -- domain model for saved papers --
import com.example.teamse1csdchcw.domain.user.Bookmark;
// -- data access layer for bookmarks --
import com.example.teamse1csdchcw.repository.BookmarkRepository;
// -- picocli annotations --
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

// -- bookmark cmd: manage saved papers --
// -- has nested subcommands: list, delete, find --
@Command(
        name = "bookmark",
        description = "Manage bookmarks",
        // -- nested subcommands (inner classes below) --
        subcommands = {
                BookmarkCommand.ListCommand.class,
                BookmarkCommand.DeleteCommand.class,
                BookmarkCommand.FindCommand.class
        }
)
public class BookmarkCommand implements Callable<Integer> {

    // -- called when user runs "bookmark" w/o subcommand --
    @Override
    public Integer call() throws Exception {
        // -- show available subcommands --
        System.out.println("Use 'bookmark list', 'bookmark delete', or 'bookmark find'");
        return 0;
    }

    // -- list subcommand: shows all saved bookmarks --
    // -- usage: libsearch bookmark list [-v] --
    @Command(name = "list", description = "List all bookmarks")
    static class ListCommand implements Callable<Integer> {

        // -- -v flag shows extra details (url, notes, tags) --
        @Option(names = {"-v", "--verbose"},
                description = "Show full details")
        private boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                // -- repo handles sqlite queries --
                BookmarkRepository repo = new BookmarkRepository();
                // -- fetch all bookmarks from db --
                List<Bookmark> bookmarks = repo.findAll();

                // -- handle empty case --
                if (bookmarks.isEmpty()) {
                    System.out.println("No bookmarks found.");
                    return 0;
                }

                // -- print header --
                System.out.println();
                System.out.println("Bookmarks (" + bookmarks.size() + "):");
                System.out.println("─".repeat(100));

                // -- iterate & print each bookmark --
                for (int i = 0; i < bookmarks.size(); i++) {
                    Bookmark bm = bookmarks.get(i);
                    // -- always show title --
                    System.out.println((i + 1) + ". " + bm.getTitle());

                    // -- verbose mode shows all fields --
                    if (verbose) {
                        if (bm.getUrl() != null) {
                            System.out.println("   URL: " + bm.getUrl());
                        }
                        if (bm.getNotes() != null && !bm.getNotes().isEmpty()) {
                            System.out.println("   Notes: " + bm.getNotes());
                        }
                        // -- tags stored as list, join w/ comma --
                        if (bm.getTags() != null && !bm.getTags().isEmpty()) {
                            System.out.println("   Tags: " + String.join(", ", bm.getTags()));
                        }
                        if (bm.getCreatedAt() != null) {
                            System.out.println("   Created: " + bm.getCreatedAt());
                        }
                        System.out.println();
                    }
                }

                // -- hint to use verbose --
                if (!verbose) {
                    System.out.println("─".repeat(100));
                    System.out.println("Use --verbose to see full details");
                }

                return 0;

            } catch (Exception e) {
                System.err.println("Failed to list bookmarks: " + e.getMessage());
                return 1;  // -- error exit code --
            }
        }
    }

    // -- delete subcommand: remove bookmark by id --
    // -- usage: libsearch bookmark delete <id> --
    @Command(name = "delete", description = "Delete a bookmark by ID")
    static class DeleteCommand implements Callable<Integer> {

        // -- positional arg: bookmark uuid --
        @Parameters(index = "0", description = "Bookmark ID to delete")
        private String bookmarkId;

        @Override
        public Integer call() throws Exception {
            try {
                BookmarkRepository repo = new BookmarkRepository();
                // -- delete from sqlite by id --
                repo.delete(bookmarkId);

                System.out.println("Bookmark deleted successfully.");
                return 0;

            } catch (Exception e) {
                System.err.println("Failed to delete bookmark: " + e.getMessage());
                return 1;
            }
        }
    }

    // -- find subcommand: search bookmarks by tag --
    // -- usage: libsearch bookmark find "ml" --
    @Command(name = "find", description = "Find bookmarks by tag")
    static class FindCommand implements Callable<Integer> {

        // -- positional arg: tag to search for --
        @Parameters(index = "0", description = "Tag to search for")
        private String tag;

        @Override
        public Integer call() throws Exception {
            try {
                BookmarkRepository repo = new BookmarkRepository();
                // -- query db for bookmarks w/ matching tag --
                List<Bookmark> bookmarks = repo.findByTag(tag);

                // -- handle no matches --
                if (bookmarks.isEmpty()) {
                    System.out.println("No bookmarks found with tag: " + tag);
                    return 0;
                }

                // -- print results --
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
