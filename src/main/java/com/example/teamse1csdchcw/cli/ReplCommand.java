package com.example.teamse1csdchcw.cli;

// -- jline: gnu readline-like lib for interactive terminal input --
// -- provides line editing, history, tab completion --
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

// -- repl cmd: read-eval-print loop for interactive use --
// -- like python repl or shell: enter commands interactively --
// -- hidden=true: doesn't show in help (accessed via -i flag) --
// -- usage: libsearch repl OR libsearch -i --
@Command(
        name = "repl",
        description = "Start interactive REPL mode",
        hidden = true  // -- hidden from help since -i flag is preferred --
)
public class ReplCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        // -- print banner --
        System.out.println("LibSearch Interactive REPL");
        System.out.println("Version 1.0.0");
        System.out.println();
        System.out.println("Type 'help' for available commands, 'exit' or 'quit' to exit");
        System.out.println();

        // -- try-with-resources: auto-close terminal on exit --
        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)  // -- use system terminal (stdin/stdout) --
                .build()) {

            // -- linereader provides readline-like input --
            // -- supports arrow keys, history, etc --
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(new DefaultParser())  // -- handles quotes, escapes --
                    .build();

            // -- create picocli cmd for parsing user input --
            CommandLine rootCmd = new CommandLine(new LibSearchCLI());

            // -- main repl loop: read input, execute, repeat --
            while (true) {
                String line;

                try {
                    // -- prompt user w/ "libsearch> " prefix --
                    line = reader.readLine("libsearch> ");
                } catch (org.jline.reader.UserInterruptException e) {
                    // -- ctrl+c: continue to next iteration --
                    continue;
                } catch (org.jline.reader.EndOfFileException e) {
                    // -- ctrl+d: exit repl --
                    break;
                }

                // -- skip empty input --
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                line = line.trim();

                // -- exit commands --
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    System.out.println("Goodbye!");
                    break;
                }

                // -- clear screen command --
                if (line.equalsIgnoreCase("clear") || line.equalsIgnoreCase("cls")) {
                    // -- send terminal escape code to clear screen --
                    terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);
                    terminal.flush();
                    continue;
                }

                // -- parse input into args array (handles quotes) --
                String[] args = parseCommandLine(line);

                // -- execute command via picocli --
                try {
                    int exitCode = rootCmd.execute(args);
                    // -- show error if command failed --
                    if (exitCode != 0) {
                        System.out.println("Command failed with exit code: " + exitCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }

                // -- blank line between commands --
                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("REPL error: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    // -- parses cmd line string into args array --
    // -- handles quoted strings and escape chars --
    // -- e.g., 'search "machine learning"' → ["search", "machine learning"] --
    private String[] parseCommandLine(String line) {
        java.util.List<String> args = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;  // -- inside quoted string --
        boolean escaped = false;   // -- prev char was backslash --

        // -- process char by char --
        for (char c : line.toCharArray()) {
            if (escaped) {
                // -- escaped char: add literally --
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                // -- backslash: next char is escaped --
                escaped = true;
            } else if (c == '"') {
                // -- quote: toggle quoted mode --
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                // -- space outside quotes: end of arg --
                if (current.length() > 0) {
                    args.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                // -- regular char: add to current arg --
                current.append(c);
            }
        }

        // -- add final arg if any --
        if (current.length() > 0) {
            args.add(current.toString());
        }

        // -- convert list to array --
        return args.toArray(new String[0]);
    }
}
