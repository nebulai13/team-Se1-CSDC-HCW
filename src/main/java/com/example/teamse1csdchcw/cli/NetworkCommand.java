package com.example.teamse1csdchcw.cli;

import com.example.teamse1csdchcw.service.network.NetworkDiagnostics;
import com.example.teamse1csdchcw.service.network.ProxyConfiguration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.Proxy;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "network",
        description = "Network configuration and diagnostics",
        subcommands = {
                NetworkCommand.DiagCommand.class,
                NetworkCommand.ProxyCommand.class,
                NetworkCommand.TestCommand.class
        }
)
public class NetworkCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'network diag', 'network proxy', or 'network test'");
        return 0;
    }

    @Command(name = "diag", description = "Run network diagnostics")
    static class DiagCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("Running network diagnostics...");
            System.out.println();

            NetworkDiagnostics.NetworkInfo info = NetworkDiagnostics.getNetworkInfo();
            System.out.print(info);

            return 0;
        }
    }

    @Command(name = "proxy", description = "Configure HTTP proxy")
    static class ProxyCommand implements Callable<Integer> {

        @Command(name = "show", description = "Show current proxy configuration")
        static class ShowCommand implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                ProxyConfiguration config = new ProxyConfiguration();

                System.out.println();
                System.out.println("Proxy Configuration:");
                System.out.println("─".repeat(50));
                System.out.println("Enabled:  " + (config.isEnabled() ? "Yes" : "No"));

                if (config.isEnabled()) {
                    System.out.println("Type:     " + config.getType());
                    System.out.println("Host:     " + config.getHost());
                    System.out.println("Port:     " + config.getPort());
                    System.out.println("Auth:     " + (config.isAuthenticationRequired() ? "Yes" : "No"));

                    if (config.isAuthenticationRequired()) {
                        System.out.println("Username: " + config.getUsername());
                    }
                }

                System.out.println("─".repeat(50));

                return 0;
            }
        }

        @Command(name = "set", description = "Set proxy configuration")
        static class SetCommand implements Callable<Integer> {

            @Parameters(index = "0", description = "Proxy host")
            private String host;

            @Parameters(index = "1", description = "Proxy port")
            private int port;

            @Option(names = {"-t", "--type"}, description = "Proxy type: HTTP or SOCKS (default: HTTP)")
            private String type = "HTTP";

            @Option(names = {"-u", "--username"}, description = "Proxy username")
            private String username;

            @Option(names = {"-p", "--password"}, description = "Proxy password")
            private String password;

            @Override
            public Integer call() throws Exception {
                try {
                    ProxyConfiguration config = new ProxyConfiguration();
                    config.setEnabled(true);
                    config.setType(Proxy.Type.valueOf(type.toUpperCase()));
                    config.setHost(host);
                    config.setPort(port);

                    if (username != null) {
                        config.setUsername(username);
                        config.setPassword(password);
                    }

                    config.save();
                    config.applyToSystemProperties();

                    System.out.println("Proxy configuration saved and applied.");
                    System.out.println("Restart the application for changes to take full effect.");

                    return 0;

                } catch (Exception e) {
                    System.err.println("Failed to set proxy configuration: " + e.getMessage());
                    return 1;
                }
            }
        }

        @Command(name = "disable", description = "Disable proxy")
        static class DisableCommand implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                try {
                    ProxyConfiguration config = new ProxyConfiguration();
                    config.setEnabled(false);
                    config.save();
                    config.clearSystemProperties();

                    System.out.println("Proxy disabled.");

                    return 0;

                } catch (Exception e) {
                    System.err.println("Failed to disable proxy: " + e.getMessage());
                    return 1;
                }
            }
        }

        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'network proxy show', 'network proxy set', or 'network proxy disable'");
            return 0;
        }
    }

    @Command(name = "test", description = "Test connection to a specific host")
    static class TestCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Host or URL to test")
        private String target;

        @Option(names = {"-h", "--http"}, description = "Test HTTP connection")
        private boolean http;

        @Override
        public Integer call() throws Exception {
            System.out.println("Testing connection to: " + target);
            System.out.println();

            NetworkDiagnostics.DiagnosticResult result;

            if (http) {
                String url = target.startsWith("http") ? target : "https://" + target;
                result = NetworkDiagnostics.testHttpConnection(url);
            } else {
                result = NetworkDiagnostics.testHost(target);
            }

            System.out.println(result);
            System.out.println();

            return result.reachable ? 0 : 1;
        }
    }
}
