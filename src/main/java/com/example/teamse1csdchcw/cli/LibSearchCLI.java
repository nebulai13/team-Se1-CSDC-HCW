package com.example.teamse1csdchcw.cli;

// -- picocli: annotation-based cli framework --
import picocli.CommandLine;
import picocli.CommandLine.Command;  // -- marks class as cmd/subcmd --
import picocli.CommandLine.Option;   // -- defines optional flags --

import java.util.concurrent.Callable;  // -- allows returning exit code --

// -- @Command: picocli annotation defining the root cli cmd --
// -- name = what user types, mixinStandardHelpOptions adds --help & --version --
// -- subcommands = nested cmds (search, bookmark, index, etc) --
@Command(
        name = "libsearch",
        mixinStandardHelpOptions = true,  // -- auto-adds -h/--help, -V/--version --
        version = "LibSearch 1.0.0",
        description = "Academic federated search application with offline indexing",
        // -- all subcommands registered here; each has own class --
        subcommands = {
                SearchCommand.class,      // -- search academic papers --
                BookmarkCommand.class,    // -- manage saved papers --
                IndexCommand.class,       // -- lucene index ops --
                NetworkCommand.class,     // -- network diag/proxy cfg --
                MonitorCommand.class,     // -- keyword alert monitoring --
                SessionCommand.class,     // -- research session mgmt --
                ReplCommand.class         // -- interactive shell mode --
        }
)
// -- main cli class: implements Callable<Integer> for exit codes --
// -- picocli calls call() when user runs "libsearch" w/o subcommand --
public class LibSearchCLI implements Callable<Integer> {

    // -- @Option: flag that user can pass on cmd line --
    // -- -i or --interactive starts repl mode --
    @Option(names = {"-i", "--interactive"},
            description = "Start interactive REPL mode")
    private boolean interactive;

    // -- verbose flag for debug output (passed down to subcmds) --
    @Option(names = {"-v", "--verbose"},
            description = "Enable verbose output")
    private boolean verbose;

    // -- call() runs when no subcommand specified --
    // -- returns 0 = success, non-zero = error (unix convention) --
    @Override
    public Integer call() throws Exception {
        // -- if -i flag, launch interactive repl --
        if (interactive) {
            return new ReplCommand().call();
        }

        // -- no args: print usage/help text --
        CommandLine.usage(this, System.out);
        return 0;  // -- success exit code --
    }

    // -- getter for verbose flag (used by subcmds) --
    public boolean isVerbose() {
        return verbose;
    }
}
