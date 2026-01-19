package com.example.teamse1csdchcw;

// -- cli cmd handler from picocli lib --
import com.example.teamse1csdchcw.cli.LibSearchCLI;
// -- sqlite db setup util --
import com.example.teamse1csdchcw.repository.sqlite.DatabaseInitializer;
// -- picocli's main cmd line processor --
import picocli.CommandLine;

// -- java nio for file/dir ops --
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main entry point for LibSearch application.
 *
 * This launcher handles both CLI and GUI modes:
 * - With arguments: runs CLI/REPL mode (no JavaFX required)
 * - Without arguments: launches JavaFX GUI
 *
 * Using this class as the main entry point allows the fat JAR to work
 * because JavaFX classes are only loaded when GUI mode is explicitly requested
 * via reflection.
 */
// -- launcher class: decides cli vs gui mode at runtime --
// -- uses reflection for gui to avoid loading javafx when not needed --
public class Launcher {

    // -- entry point: args determine mode (cli w/ args, gui w/o args) --
    public static void main(String[] args) {
        // Ensure app directories exist
        // -- creates ~/.libsearch/logs, data, index dirs if missing --
        ensureAppDirectories();

        // Initialize database
        // -- runs schema.sql to create tables if db doesn't exist --
        DatabaseInitializer.initialize();

        // -- check if user passed any cmd line args --
        if (args.length > 0) {
            // CLI mode - no JavaFX needed
            // -- picocli parses args & routes to correct subcommand --
            // -- returns exit code (0=success, non-zero=error) --
            int exitCode = new CommandLine(new LibSearchCLI()).execute(args);
            System.exit(exitCode);
        } else {
            // GUI mode - launch JavaFX via reflection to avoid loading classes too early
            // -- no args = user wants graphical ui --
            launchGui(args);
        }
    }

    /**
     * Launch the JavaFX GUI using reflection.
     * This avoids loading JavaFX classes until they're actually needed.
     */
    // -- reflection-based gui launcher --
    // -- why reflection? fat jar can work w/o javafx on classpath --
    // -- only loads javafx classes when actually needed --
    private static void launchGui(String[] args) {
        try {
            // -- load javafx.application.Application class dynamically --
            Class<?> appClass = Class.forName("javafx.application.Application");
            // -- load our main app class --
            Class<?> helloAppClass = Class.forName("com.example.teamse1csdchcw.HelloApplication");
            // -- get the static launch(class, args) method --
            java.lang.reflect.Method launchMethod = appClass.getMethod("launch", Class.class, String[].class);
            // -- invoke launch(HelloApplication.class, args) --
            launchMethod.invoke(null, helloAppClass, args);
        } catch (ClassNotFoundException e) {
            // -- javafx not found on classpath/module path --
            System.err.println("Error: JavaFX runtime not found.");
            System.err.println("To run the GUI, ensure JavaFX is available on the module path.");
            System.err.println("For CLI mode, run with arguments (e.g., --help, repl, search).");
            System.exit(1);
        } catch (Exception e) {
            // -- any other reflection/launch error --
            System.err.println("Error launching GUI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // -- creates app data dirs in user's home folder --
    // -- ~/.libsearch/ is base dir for all app data --
    private static void ensureAppDirectories() {
        try {
            // -- get user home dir + .libsearch subdir --
            Path base = Path.of(System.getProperty("user.home"), ".libsearch");
            // -- logs/ for logback file appender --
            Files.createDirectories(base.resolve("logs"));
            // -- data/ for sqlite db file --
            Files.createDirectories(base.resolve("data"));
            // -- index/ for lucene full-text search index --
            Files.createDirectories(base.resolve("index"));
        } catch (Exception ignored) {
            // Intentionally silent: Logback may not be initialized yet
            // -- can't log errors here b/c logger may not exist yet --
        }
    }
}
