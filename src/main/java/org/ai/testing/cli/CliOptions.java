package org.ai.testing.cli;

import org.ai.testing.testrun.dto.RunOptions;
import org.ai.testing.util.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed command line for the runner.
 *
 * <p>The previous build had no entry point beyond a hard-coded demo, so the
 * Postman and Bruno parsers could not actually be used. Every capability is now
 * reachable from the command line.</p>
 */
public final class CliOptions {

    private Path collection;
    private Path environment;
    private Path reportDirectory = Paths.get("reports");
    private final Map<String, String> variables = new LinkedHashMap<>();
    private final RunOptions runOptions = new RunOptions();
    private boolean demo;
    private boolean help;
    private boolean quiet;

    private CliOptions() {
    }

    /** Thrown for a malformed command line; the message is user-facing. */
    public static class UsageException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        public UsageException(String message) {
            super(message);
        }
    }

    public static CliOptions parse(String[] args) {
        CliOptions options = new CliOptions();

        if (args == null || args.length == 0) {
            options.demo = true;
            return options;
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> options.help = true;
                case "--demo" -> options.demo = true;
                case "-q", "--quiet" -> options.quiet = true;
                case "--fail-fast" -> options.runOptions.setFailFast(true);
                case "--no-redact" -> options.runOptions.setRedactSecrets(false);
                case "--no-follow-redirects" -> options.runOptions.setFollowRedirects(false);
                case "--parallel" -> options.runOptions.setExecutionMode("PARALLEL");

                case "-c", "--collection" ->
                        options.collection = Paths.get(next(args, ++i, arg));
                case "-e", "--env", "--environment" ->
                        options.environment = Paths.get(next(args, ++i, arg));
                case "-o", "--out", "--output" ->
                        options.reportDirectory = Paths.get(next(args, ++i, arg));

                case "--threads" ->
                        options.runOptions.setThreads(intArg(next(args, ++i, arg), arg));
                case "--retries" ->
                        options.runOptions.setRetries(intArg(next(args, ++i, arg), arg));
                case "--retry-delay" ->
                        options.runOptions.setRetryDelayMs(longArg(next(args, ++i, arg), arg));
                case "--timeout" ->
                        options.runOptions.setRequestTimeoutMs(longArg(next(args, ++i, arg), arg));
                case "--connect-timeout" ->
                        options.runOptions.setConnectTimeoutMs(longArg(next(args, ++i, arg), arg));
                case "--max-body" ->
                        options.runOptions.setMaxBodyChars(intArg(next(args, ++i, arg), arg));

                case "--tag" -> options.runOptions.getIncludeTags()
                        .addAll(splitList(next(args, ++i, arg)));
                case "--exclude-tag" -> options.runOptions.getExcludeTags()
                        .addAll(splitList(next(args, ++i, arg)));

                case "--var" -> {
                    String pair = next(args, ++i, arg);
                    int equals = pair.indexOf('=');
                    if (equals <= 0) {
                        throw new UsageException(
                                "--var expects name=value but received: " + pair);
                    }
                    options.variables.put(pair.substring(0, equals).trim(),
                            pair.substring(equals + 1));
                }

                default -> {
                    if (arg.startsWith("-")) {
                        throw new UsageException("Unknown option: " + arg);
                    }
                    // A bare path is treated as the collection.
                    if (options.collection == null) {
                        options.collection = Paths.get(arg);
                    } else {
                        throw new UsageException("Unexpected argument: " + arg);
                    }
                }
            }
        }

        if (!options.help && !options.demo && options.collection == null) {
            throw new UsageException(
                    "No collection given. Pass --collection <path>, or --demo to run the "
                            + "built-in example.");
        }
        return options;
    }

    private static String next(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new UsageException(option + " expects a value");
        }
        return args[index];
    }

    private static int intArg(String value, String option) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new UsageException(option + " expects a whole number but received: " + value);
        }
    }

    private static long longArg(String value, String option) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new UsageException(option + " expects a whole number but received: " + value);
        }
    }

    private static List<String> splitList(String value) {
        List<String> items = new ArrayList<>();
        if (Strings.isBlank(value)) {
            return items;
        }
        for (String part : value.split(",")) {
            if (Strings.hasText(part)) {
                items.add(part.trim());
            }
        }
        return items;
    }

    public static String usage() {
        return """
                AI API Testing Agent — run API collections and produce reports.

                USAGE
                  api-testing [options]
                  api-testing <collection> [options]

                SOURCE
                  -c, --collection <path>   Postman .json file, Bruno .bru file, or a
                                            Bruno collection directory
                  -e, --env <path>          Postman or Bruno environment file
                      --demo                Run the built-in example plan (no arguments
                                            also does this)

                OUTPUT
                  -o, --out <dir>           Report directory (default: reports)
                  -q, --quiet               Print only the final summary

                EXECUTION
                      --parallel            Run suites concurrently
                      --threads <n>         Worker threads for --parallel (default: 4)
                      --timeout <ms>        Per-request timeout (default: 60000)
                      --connect-timeout <ms>  Connect timeout (default: 15000)
                      --retries <n>         Retries after a transport error or a
                                            retryable status (default: 0)
                      --retry-delay <ms>    Pause between retries (default: 500)
                      --fail-fast           Stop at the first failing test case
                      --no-follow-redirects Do not follow 3xx responses
                      --max-body <chars>    Response body cap kept in reports

                FILTERING
                      --tag <a,b>           Only run cases carrying one of these tags
                      --exclude-tag <a,b>   Never run cases carrying these tags

                VARIABLES
                      --var name=value      Set a runtime variable; repeatable and
                                            highest precedence

                SECURITY
                      --no-redact           Write credentials to reports unmasked

                EXIT CODES
                  0  every executed test case passed
                  1  at least one test case failed or errored
                  2  the command line or the collection could not be understood

                EXAMPLES
                  api-testing --demo
                  api-testing -c samples/postman-collection.json -e samples/postman-env.json
                  api-testing -c samples/bruno --parallel --threads 8 --retries 2
                  api-testing -c api.json --tag smoke --fail-fast --out build/reports
                """;
    }

    public Path getCollection() {
        return collection;
    }

    public Path getEnvironment() {
        return environment;
    }

    public Path getReportDirectory() {
        return reportDirectory;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public RunOptions getRunOptions() {
        return runOptions;
    }

    public boolean isDemo() {
        return demo;
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isQuiet() {
        return quiet;
    }
}
