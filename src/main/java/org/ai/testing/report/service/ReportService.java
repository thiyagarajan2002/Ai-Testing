package org.ai.testing.report.service;

import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.CsvReportGenerator;
import org.ai.testing.report.generator.HtmlReportGenerator;
import org.ai.testing.report.generator.JUnitXmlReportGenerator;
import org.ai.testing.report.generator.JsonReportGenerator;
import org.ai.testing.report.generator.MarkdownReportGenerator;
import org.ai.testing.report.generator.ReportGenerationException;
import org.ai.testing.report.generator.ReportGenerator;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.util.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the report envelope once and hands it to every configured generator.
 *
 * <p>The previous implementation built a separate envelope per format, each
 * with its own report ID and timestamp, so the HTML, JSON and CSV files from a
 * single run disagreed about when they were produced and could not be
 * correlated. One envelope is now shared, and the format field is set per
 * generator immediately before it writes.</p>
 */
public class ReportService {

    /** Default directory for generated files. */
    public static final Path DEFAULT_DIRECTORY = Paths.get("reports");

    public static final String VERSION = "2.0.0";

    private final List<ReportGenerator> generators;

    public ReportService() {
        this(DEFAULT_DIRECTORY);
    }

    /** Writes all five formats into {@code directory}. */
    public ReportService(Path directory) {
        Path base = directory == null ? DEFAULT_DIRECTORY : directory;
        this.generators = List.of(
                new HtmlReportGenerator(base.resolve("test-report.html")),
                new JsonReportGenerator(base.resolve("test-report.json")),
                new CsvReportGenerator(base.resolve("test-report.csv")),
                new MarkdownReportGenerator(base.resolve("test-report.md")),
                new JUnitXmlReportGenerator(base.resolve("junit-report.xml")));
    }

    /** Uses exactly the generators supplied; handy for tests and custom setups. */
    public ReportService(List<ReportGenerator> generators) {
        if (generators == null || generators.isEmpty()) {
            throw new IllegalArgumentException("At least one report generator is required");
        }
        for (ReportGenerator generator : generators) {
            if (generator == null) {
                throw new IllegalArgumentException("Report generator cannot be null");
            }
        }
        this.generators = List.copyOf(generators);
    }

    /**
     * Writes every configured report.
     *
     * <p>A failure in one format does not prevent the others from being
     * written; all failures are collected and reported together.</p>
     *
     * @return the paths written, keyed by format label
     */
    public Map<String, Path> generateAllReports(TestRunResultDto testRunResult) {

        TestReportDto report = createReport(testRunResult);
        Map<String, Path> written = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();

        for (ReportGenerator generator : generators) {
            try {
                report.setReportFormat(generator.format());
                generator.generate(report);
                written.put(generator.format(), generator.outputPath());
            } catch (RuntimeException e) {
                failures.add(generator.format() + ": " + e.getMessage());
            }
        }

        if (!failures.isEmpty()) {
            throw new ReportGenerationException(
                    "One or more reports could not be generated — "
                            + String.join(" | ", failures));
        }
        return written;
    }

    /** Runs a single named generator, for example {@code "HTML"}. */
    public Path generateReport(TestRunResultDto testRunResult, String format) {
        TestReportDto report = createReport(testRunResult);
        for (ReportGenerator generator : generators) {
            if (generator.format().equalsIgnoreCase(format)) {
                report.setReportFormat(generator.format());
                generator.generate(report);
                return generator.outputPath();
            }
        }
        throw new IllegalArgumentException("No report generator registered for format: " + format);
    }

    public Path generateHtmlReport(TestRunResultDto testRunResult) {
        return generateReport(testRunResult, "HTML");
    }

    public Path generateJsonReport(TestRunResultDto testRunResult) {
        return generateReport(testRunResult, "JSON");
    }

    public Path generateCsvReport(TestRunResultDto testRunResult) {
        return generateReport(testRunResult, "CSV");
    }

    /** The formats this service will produce, in write order. */
    public List<String> formats() {
        return generators.stream().map(ReportGenerator::format).toList();
    }

    /** Builds the shared envelope for one run. */
    public TestReportDto createReport(TestRunResultDto testRunResult) {
        if (testRunResult == null) {
            throw new IllegalArgumentException("Test run result cannot be null");
        }

        TestReportDto report = new TestReportDto();
        report.setReportId(generateReportId());
        report.setReportName(buildReportName(testRunResult));
        report.setGeneratedAt(LocalDateTime.now());
        report.setToolVersion("v" + VERSION);
        report.setTestRunResult(testRunResult);
        report.setSeverity(severity(testRunResult));
        report.setSummary(testRunResult.getMessage());
        return report;
    }

    private String generateReportId() {
        return "REPORT-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String buildReportName(TestRunResultDto testRunResult) {
        return Strings.defaultIfBlank(testRunResult.getRunName(), "API Test Run") + " — Report";
    }

    /** A coarse severity derived from how much of the run broke. */
    private String severity(TestRunResultDto run) {
        if (run.getErroredTestCases() > 0) {
            return "HIGH";
        }
        if (run.getFailedTestCases() == 0) {
            return "NONE";
        }
        double failureRate = run.getTotalTestCases() == 0
                ? 0 : (run.getFailedTestCases() * 100.0) / run.getTotalTestCases();
        if (failureRate >= 50) {
            return "HIGH";
        }
        return failureRate >= 20 ? "MEDIUM" : "LOW";
    }
}
