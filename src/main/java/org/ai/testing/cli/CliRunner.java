package org.ai.testing.cli;

import org.ai.testing.parser.CollectionLoader;
import org.ai.testing.parser.CollectionParseException;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.testrun.dto.RunOptions;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testrun.executor.TestRunExecutor;
import org.ai.testing.util.Strings;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Turns a parsed command line into a run, a report set and an exit code. */
public class CliRunner {

    public static final int EXIT_PASSED = 0;
    public static final int EXIT_FAILED = 1;
    public static final int EXIT_USAGE = 2;

    private final PrintStream out;

    public CliRunner(PrintStream out) {
        this.out = out;
    }

    public int run(String[] args) {

        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (CliOptions.UsageException e) {
            out.println(e.getMessage());
            out.println();
            out.println(CliOptions.usage());
            return EXIT_USAGE;
        }

        if (options.isHelp()) {
            out.println(CliOptions.usage());
            return EXIT_PASSED;
        }

        ConsolePrinter printer = new ConsolePrinter(out, options.isQuiet());

        TestRunDto testRun;
        try {
            testRun = options.isDemo()
                    ? DemoPlan.build()
                    : new CollectionLoader().load(options.getCollection(),
                            options.getEnvironment());
        } catch (CollectionParseException | IllegalArgumentException e) {
            printer.error(e.getMessage());
            return EXIT_USAGE;
        }

        applyOptions(testRun, options);

        printer.banner(
                Strings.defaultIfBlank(testRun.getRunName(), "API Test Run"),
                describeSource(options) + " · " + testRun.getTestSuites().size()
                        + " suite(s) · " + testRun.totalTestCases() + " test case(s) · "
                        + testRun.getOptions().getExecutionMode().toLowerCase());

        ReportService reportService = new ReportService(options.getReportDirectory());
        TestRunResultDto result = new TestRunExecutor(reportService).execute(testRun);

        printer.suiteResults(result);
        printer.failures(result);
        printer.warnings(result);
        printer.summary(result);
        printer.reports(expectedReportPaths(reportService, options.getReportDirectory()));

        return result.getFailedTestCases() + result.getErroredTestCases() > 0
                ? EXIT_FAILED
                : EXIT_PASSED;
    }

    private void applyOptions(TestRunDto testRun, CliOptions options) {
        RunOptions source = options.getRunOptions();
        testRun.setOptions(source);
        // Command-line variables sit in the environment layer so a value
        // captured at run time can still override them.
        testRun.getEnvironmentVariables().putAll(options.getVariables());
    }

    private String describeSource(CliOptions options) {
        if (options.isDemo()) {
            return "built-in demo plan";
        }
        String source = String.valueOf(options.getCollection());
        if (options.getEnvironment() != null) {
            source += " + " + options.getEnvironment();
        }
        return source;
    }

    private Map<String, Path> expectedReportPaths(ReportService service, Path directory) {
        Map<String, Path> paths = new LinkedHashMap<>();
        paths.put("HTML", directory.resolve("test-report.html"));
        paths.put("JSON", directory.resolve("test-report.json"));
        paths.put("CSV", directory.resolve("test-report.csv"));
        paths.put("MARKDOWN", directory.resolve("test-report.md"));
        paths.put("JUNIT-XML", directory.resolve("junit-report.xml"));
        return paths;
    }
}
