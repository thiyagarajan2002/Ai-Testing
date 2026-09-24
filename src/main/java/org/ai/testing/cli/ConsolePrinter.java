package org.ai.testing.cli;

import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.util.Strings;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Writes a readable run summary to the terminal.
 *
 * <p>Colour is emitted only when the environment looks like an interactive
 * terminal and {@code NO_COLOR} is unset, so piping the output into a log file
 * does not fill it with escape sequences.</p>
 */
public class ConsolePrinter {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";

    private final PrintStream out;
    private final boolean colour;
    private final boolean quiet;

    public ConsolePrinter(PrintStream out, boolean quiet) {
        this.out = out;
        this.quiet = quiet;
        this.colour = System.console() != null && System.getenv("NO_COLOR") == null;
    }

    public void banner(String title, String subtitle) {
        if (quiet) {
            return;
        }
        out.println();
        out.println(colour(BOLD + CYAN) + "  " + title + reset());
        if (Strings.hasText(subtitle)) {
            out.println(colour(DIM) + "  " + subtitle + reset());
        }
        out.println(rule());
    }

    public void suiteResults(TestRunResultDto run) {
        if (quiet) {
            return;
        }
        for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) {
            out.println();
            out.printf("  %s %s %s%n",
                    icon(suite.getStatus()),
                    colour(BOLD) + Strings.nullToEmpty(suite.getSuiteName()) + reset(),
                    colour(DIM) + "(" + Strings.humanDuration(suite.getExecutionTimeMs())
                            + ")" + reset());

            for (TestCaseResultDto testCase : suite.getTestResults()) {
                out.printf("    %s %-6s %-44s %s%n",
                        icon(testCase.getStatus()),
                        Strings.nullToEmpty(testCase.getMethod()),
                        Strings.truncate(Strings.nullToEmpty(testCase.getTestCaseName()), 44),
                        detail(testCase));
            }
        }
    }

    private String detail(TestCaseResultDto testCase) {
        if (testCase.getStatus() == TestStatus.SKIPPED) {
            return colour(DIM) + Strings.nullToEmpty(testCase.getMessage()) + reset();
        }
        StringBuilder detail = new StringBuilder();
        if (testCase.getResponse() != null) {
            detail.append(testCase.statusCode()).append(' ')
                    .append(testCase.getResponse().getStatusMessage())
                    .append(colour(DIM)).append(" · ")
                    .append(testCase.responseTimeMs()).append(" ms").append(reset());
        }
        if (testCase.getStatus().isFailure()) {
            detail.append(colour(RED)).append("  ")
                    .append(Strings.truncate(Strings.nullToEmpty(testCase.getMessage()), 90))
                    .append(reset());
        }
        return detail.toString();
    }

    public void failures(TestRunResultDto run) {
        if (run.failures().isEmpty()) {
            return;
        }
        out.println();
        out.println(colour(BOLD + RED) + "  Failures" + reset());
        out.println(rule());

        for (TestCaseResultDto failure : run.failures()) {
            out.println();
            out.println("  " + colour(BOLD) + Strings.nullToEmpty(failure.getTestCaseName())
                    + reset());
            out.println("    " + failure.getMethod() + " "
                    + (failure.getRequest() == null ? "" : failure.getRequest().getUrl()));
            out.println("    " + colour(RED) + Strings.nullToEmpty(failure.getMessage()) + reset());

            for (ValidationResultDto validation : failure.getValidationSummary().getResults()) {
                if (validation.isPassed()) {
                    continue;
                }
                out.printf("      - %s %s: expected %s, actual %s%n",
                        Strings.nullToEmpty(validation.getValidationType()),
                        Strings.nullToEmpty(validation.getField()),
                        quoted(validation.getExpected()),
                        quoted(validation.getActual()));
            }
            if (Strings.hasText(failure.getErrorDetail())) {
                out.println("      " + colour(DIM)
                        + Strings.truncate(failure.getErrorDetail(), 300) + reset());
            }
        }
    }

    public void warnings(TestRunResultDto run) {
        if (run.getWarnings().isEmpty() || quiet) {
            return;
        }
        out.println();
        out.println(colour(BOLD + YELLOW) + "  Warnings" + reset());
        run.getWarnings().forEach(warning -> out.println("    - " + warning));
    }

    public void summary(TestRunResultDto run) {
        TestRunResultDto.Metrics metrics = run.getMetrics();
        out.println();
        out.println(rule());
        out.printf("  %s  %s%n", statusBadge(run.getStatus()),
                Strings.nullToEmpty(run.getMessage()));
        out.println(rule());
        out.printf("  Suites      %d total · %d passed · %d failed · %d skipped%n",
                run.getTotalSuites(), run.getPassedSuites(),
                run.getFailedSuites() + run.getErroredSuites(), run.getSkippedSuites());
        out.printf("  Test cases  %d total · %d passed · %d failed · %d errored · %d skipped%n",
                run.getTotalTestCases(), run.getPassedTestCases(),
                run.getFailedTestCases(), run.getErroredTestCases(), run.getSkippedTestCases());
        out.printf("  Pass rate   %.1f%%%n", metrics.getPassRate());
        out.printf("  Latency     avg %d ms · median %d ms · p95 %d ms · max %d ms%n",
                metrics.getAverageResponseTimeMs(), metrics.getMedianResponseTimeMs(),
                metrics.getP95ResponseTimeMs(), metrics.getMaxResponseTimeMs());
        out.printf("  Duration    %s (%.2f req/s, %s transferred)%n",
                Strings.humanDuration(run.getExecutionTimeMs()),
                metrics.getRequestsPerSecond(),
                Strings.humanBytes(metrics.getTotalBytes()));
    }

    public void reports(Map<String, Path> reports) {
        if (reports == null || reports.isEmpty()) {
            return;
        }
        out.println();
        out.println(colour(BOLD) + "  Reports" + reset());
        reports.forEach((format, path) ->
                out.printf("    %-10s %s%n", format, path.toAbsolutePath()));
    }

    public void error(String message) {
        out.println();
        out.println(colour(BOLD + RED) + "  Error: " + reset() + message);
    }

    public void info(String message) {
        if (!quiet) {
            out.println("  " + message);
        }
    }

    public void println(String message) {
        out.println(message);
    }

    // ------------------------------------------------------------------

    private String quoted(String value) {
        return "'" + Strings.truncate(Strings.nullToEmpty(value)
                .replace("\n", "\\n"), 120) + "'";
    }

    private String icon(TestStatus status) {
        return switch (status) {
            case PASSED -> colour(GREEN) + "PASS" + reset();
            case FAILED -> colour(RED) + "FAIL" + reset();
            case ERROR -> colour(MAGENTA) + "ERR " + reset();
            case SKIPPED -> colour(DIM) + "SKIP" + reset();
        };
    }

    private String statusBadge(TestStatus status) {
        String tint = switch (status) {
            case PASSED -> GREEN;
            case FAILED -> RED;
            case ERROR -> MAGENTA;
            case SKIPPED -> YELLOW;
        };
        return colour(BOLD + tint) + status.name().toUpperCase(Locale.ROOT) + reset();
    }

    private String rule() {
        return colour(DIM) + "  " + "-".repeat(72) + reset();
    }

    private String colour(String code) {
        return colour ? code : "";
    }

    private String reset() {
        return colour ? RESET : "";
    }
}
