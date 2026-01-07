package com.example.teamse1csdchcw.cli;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "repl",
        description = "Start interactive REPL mode",
        hidden = true
)
public class ReplCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("LibSearch Interactive REPL");
        System.out.println("Version 1.0.0");
        System.out.println();
        System.out.println("Type 'help' for available commands, 'exit' or 'quit' to exit");
        System.out.println();

        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(new DefaultParser())
                    .build();

            CommandLine rootCmd = new CommandLine(new LibSearchCLI());

            while (true) {
                String line;

                try {
                    line = reader.readLine("libsearch> ");
                } catch (org.jline.reader.UserInterruptException e) {
                    continue;
                } catch (org.jline.reader.EndOfFileException e) {
                    break;
                }

                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                line = line.trim();

                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    System.out.println("Goodbye!");
                    break;
                }

                if (line.equalsIgnoreCase("clear") || line.equalsIgnoreCase("cls")) {
                    terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);
                    terminal.flush();
                    continue;
                }

                String[] args = parseCommandLine(line);

                try {
                    int exitCode = rootCmd.execute(args);
                    if (exitCode != 0) {
                        System.out.println("Command failed with exit code: " + exitCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }

                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("REPL error: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    private String[] parseCommandLine(String line) {
        java.util.List<String> args = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;

        for (char c : line.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args.toArray(new String[0]);
    }
}
