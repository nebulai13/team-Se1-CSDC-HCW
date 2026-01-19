package com.example.teamse1csdchcw.cli;

// -- network utilities --
import com.example.teamse1csdchcw.service.network.NetworkDiagnostics;  // -- connectivity checks --
import com.example.teamse1csdchcw.service.network.ProxyConfiguration;  // -- proxy settings mgmt --
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.Proxy;  // -- java.net proxy type enum --
import java.util.List;
import java.util.concurrent.Callable;

// -- network cmd: diagnostics & proxy config --
// -- useful for troubleshooting connectivity issues --
// -- subcommands: diag, proxy, test --
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

    // -- no subcommand = show usage --
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'network diag', 'network proxy', or 'network test'");
        return 0;
    }

    // -- diag subcommand: shows network info --
    // -- ip addresses, dns, reachability of common hosts --
    // -- usage: libsearch network diag --
    @Command(name = "diag", description = "Run network diagnostics")
    static class DiagCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("Running network diagnostics...");
            System.out.println();

            // -- gathers network interfaces, ip addrs, tests connections --
            NetworkDiagnostics.NetworkInfo info = NetworkDiagnostics.getNetworkInfo();
            // -- networkinfo has toString() that formats output --
            System.out.print(info);

            return 0;
        }
    }

    // -- proxy subcommand: configure http/socks proxy --
    // -- for users behind corporate firewalls --
    // -- has own nested subcommands: show, set, disable --
    @Command(name = "proxy", description = "Configure HTTP proxy")
    static class ProxyCommand implements Callable<Integer> {

        // -- show subcommand: display current proxy settings --
        // -- usage: libsearch network proxy show --
        @Command(name = "show", description = "Show current proxy configuration")
        static class ShowCommand implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                // -- loads proxy config from sqlite --
                ProxyConfiguration config = new ProxyConfiguration();

                System.out.println();
                System.out.println("Proxy Configuration:");
                System.out.println("─".repeat(50));
                System.out.println("Enabled:  " + (config.isEnabled() ? "Yes" : "No"));

                // -- only show details if proxy is enabled --
                if (config.isEnabled()) {
                    System.out.println("Type:     " + config.getType());  // -- HTTP or SOCKS --
                    System.out.println("Host:     " + config.getHost());
                    System.out.println("Port:     " + config.getPort());
                    System.out.println("Auth:     " + (config.isAuthenticationRequired() ? "Yes" : "No"));

                    // -- show username but not password --
                    if (config.isAuthenticationRequired()) {
                        System.out.println("Username: " + config.getUsername());
                    }
                }

                System.out.println("─".repeat(50));

                return 0;
            }
        }

        // -- set subcommand: configure proxy --
        // -- usage: libsearch network proxy set proxy.corp.com 8080 -t HTTP --
        @Command(name = "set", description = "Set proxy configuration")
        static class SetCommand implements Callable<Integer> {

            // -- positional args: host and port --
            @Parameters(index = "0", description = "Proxy host")
            private String host;

            @Parameters(index = "1", description = "Proxy port")
            private int port;

            // -- proxy type: HTTP (most common) or SOCKS --
            @Option(names = {"-t", "--type"}, description = "Proxy type: HTTP or SOCKS (default: HTTP)")
            private String type = "HTTP";

            // -- optional auth credentials --
            @Option(names = {"-u", "--username"}, description = "Proxy username")
            private String username;

            @Option(names = {"-p", "--password"}, description = "Proxy password")
            private String password;

            @Override
            public Integer call() throws Exception {
                try {
                    ProxyConfiguration config = new ProxyConfiguration();
                    config.setEnabled(true);
                    // -- Proxy.Type.HTTP or Proxy.Type.SOCKS --
                    config.setType(Proxy.Type.valueOf(type.toUpperCase()));
                    config.setHost(host);
                    config.setPort(port);

                    // -- set auth if username provided --
                    if (username != null) {
                        config.setUsername(username);
                        config.setPassword(password);
                    }

                    // -- persist to sqlite --
                    config.save();
                    // -- set java system properties for http.proxyHost etc --
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

        // -- disable subcommand: turn off proxy --
        // -- usage: libsearch network proxy disable --
        @Command(name = "disable", description = "Disable proxy")
        static class DisableCommand implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                try {
                    ProxyConfiguration config = new ProxyConfiguration();
                    config.setEnabled(false);
                    config.save();
                    // -- clear http.proxyHost etc system props --
                    config.clearSystemProperties();

                    System.out.println("Proxy disabled.");

                    return 0;

                } catch (Exception e) {
                    System.err.println("Failed to disable proxy: " + e.getMessage());
                    return 1;
                }
            }
        }

        // -- no subcommand = show usage --
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'network proxy show', 'network proxy set', or 'network proxy disable'");
            return 0;
        }
    }

    // -- test subcommand: test connectivity to specific host --
    // -- useful for debugging api access issues --
    // -- usage: libsearch network test arxiv.org --http --
    @Command(name = "test", description = "Test connection to a specific host")
    static class TestCommand implements Callable<Integer> {

        // -- host or url to test --
        @Parameters(index = "0", description = "Host or URL to test")
        private String target;

        // -- -h flag: test http instead of just tcp --
        @Option(names = {"-h", "--http"}, description = "Test HTTP connection")
        private boolean http;

        @Override
        public Integer call() throws Exception {
            System.out.println("Testing connection to: " + target);
            System.out.println();

            NetworkDiagnostics.DiagnosticResult result;

            if (http) {
                // -- http test: sends actual http request --
                String url = target.startsWith("http") ? target : "https://" + target;
                result = NetworkDiagnostics.testHttpConnection(url);
            } else {
                // -- tcp test: just checks if host is reachable --
                result = NetworkDiagnostics.testHost(target);
            }

            // -- print result (reachable, latency, etc) --
            System.out.println(result);
            System.out.println();

            // -- exit code reflects reachability --
            return result.reachable ? 0 : 1;
        }
    }
}
