package com.example.teamse1csdchcw.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
        name = "libsearch",
        mixinStandardHelpOptions = true,
        version = "LibSearch 1.0.0",
        description = "Academic federated search application with offline indexing",
        subcommands = {
                SearchCommand.class,
                BookmarkCommand.class,
                IndexCommand.class,
                NetworkCommand.class,
                MonitorCommand.class,
                SessionCommand.class,
                ReplCommand.class
        }
)
public class LibSearchCLI implements Callable<Integer> {

    @Option(names = {"-i", "--interactive"},
            description = "Start interactive REPL mode")
    private boolean interactive;

    @Option(names = {"-v", "--verbose"},
            description = "Enable verbose output")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        if (interactive) {
            return new ReplCommand().call();
        }

        CommandLine.usage(this, System.out);
        return 0;
    }

    public boolean isVerbose() {
        return verbose;
    }
}
