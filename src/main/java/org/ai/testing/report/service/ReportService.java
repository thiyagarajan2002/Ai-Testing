package org.ai.testing.report.service;



import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.CsvReportGenerator;
import org.ai.testing.report.generator.HtmlReportGenerator;
import org.ai.testing.report.generator.JsonReportGenerator;
import org.ai.testing.report.generator.ReportGenerator;
import org.ai.testing.testrun.dto.TestRunResultDto;

import java.time.LocalDateTime;
import java.util.UUID;


public class ReportService {

    private final ReportGenerator htmlReportGenerator;
    private final ReportGenerator jsonReportGenerator;
    private final ReportGenerator csvReportGenerator;

    public ReportService() {
        this.htmlReportGenerator =
                new HtmlReportGenerator();

        this.jsonReportGenerator =
                new JsonReportGenerator();

        this.csvReportGenerator =
                new CsvReportGenerator();
    }

    public ReportService(
            ReportGenerator htmlReportGenerator,
            ReportGenerator jsonReportGenerator,
            ReportGenerator csvReportGenerator) {

        if (htmlReportGenerator == null) {
            throw new IllegalArgumentException(
                    "HTML report generator cannot be null"
            );
        }

        if (jsonReportGenerator == null) {
            throw new IllegalArgumentException(
                    "JSON report generator cannot be null"
            );
        }

        if (csvReportGenerator == null) {
            throw new IllegalArgumentException(
                    "CSV report generator cannot be null"
            );
        }

        this.htmlReportGenerator =
                htmlReportGenerator;

        this.jsonReportGenerator =
                jsonReportGenerator;

        this.csvReportGenerator =
                csvReportGenerator;
    }

    public TestReportDto generateHtmlReport(
            TestRunResultDto testRunResult) {

        TestReportDto report =
                createReport(
                        testRunResult,
                        "HTML"
                );

        htmlReportGenerator.generate(report);

        return report;
    }

    public TestReportDto generateJsonReport(
            TestRunResultDto testRunResult) {

        TestReportDto report =
                createReport(
                        testRunResult,
                        "JSON"
                );

        jsonReportGenerator.generate(report);

        return report;
    }

    public TestReportDto generateCsvReport(
            TestRunResultDto testRunResult) {

        TestReportDto report =
                createReport(
                        testRunResult,
                        "CSV"
                );

        csvReportGenerator.generate(report);

        return report;
    }

    public TestReportDto generateAllReports(
            TestRunResultDto testRunResult) {

        validateTestRunResult(testRunResult);

        TestReportDto htmlReport =
                createReport(
                        testRunResult,
                        "HTML"
                );

        TestReportDto jsonReport =
                createReport(
                        testRunResult,
                        "JSON"
                );

        TestReportDto csvReport =
                createReport(
                        testRunResult,
                        "CSV"
                );

        RuntimeException generationFailure = null;

        try {
            htmlReportGenerator.generate(htmlReport);
        } catch (RuntimeException e) {
            generationFailure = createOrAddFailure(
                    generationFailure,
                    "HTML report generation failed",
                    e
            );
        }

        try {
            jsonReportGenerator.generate(jsonReport);
        } catch (RuntimeException e) {
            generationFailure = createOrAddFailure(
                    generationFailure,
                    "JSON report generation failed",
                    e
            );
        }

        try {
            csvReportGenerator.generate(csvReport);
        } catch (RuntimeException e) {
            generationFailure = createOrAddFailure(
                    generationFailure,
                    "CSV report generation failed",
                    e
            );
        }

        if (generationFailure != null) {
            throw generationFailure;
        }

        return htmlReport;
    }

    private RuntimeException createOrAddFailure(
            RuntimeException existingFailure,
            String message,
            RuntimeException cause) {

        if (existingFailure == null) {
            return new RuntimeException(message, cause);
        }

        existingFailure.addSuppressed(
                new RuntimeException(message, cause)
        );

        return existingFailure;
    }

    private TestReportDto createReport(
            TestRunResultDto testRunResult,
            String format) {

        validateTestRunResult(testRunResult);

        TestReportDto report =
                new TestReportDto();

        report.setReportId(
                generateReportId(format)
        );

        report.setReportName(
                buildReportName(testRunResult)
        );

        report.setReportFormat(format);

        report.setGeneratedAt(
                LocalDateTime.now()
        );

        report.setTestRunResult(
                testRunResult
        );

        return report;
    }

    private void validateTestRunResult(
            TestRunResultDto testRunResult) {

        if (testRunResult == null) {
            throw new IllegalArgumentException(
                    "Test run result cannot be null"
            );
        }
    }

    private String generateReportId(
            String format) {

        return "REPORT-"
                + format
                + "-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private String buildReportName(
            TestRunResultDto testRunResult) {

        String runName =
                testRunResult.getRunName();

        if (runName == null
                || runName.isBlank()) {

            runName = "API Test Run";
        }

        return runName + " - Report";
    }
}