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
    private final AiReportInsightBuilder aiReportInsightBuilder;

    public ReportService() {
        this(new HtmlReportGenerator(), new JsonReportGenerator(), new CsvReportGenerator(),
                new AiReportInsightBuilder());
    }

    public ReportService(
            ReportGenerator htmlReportGenerator,
            ReportGenerator jsonReportGenerator,
            ReportGenerator csvReportGenerator) {
        this(htmlReportGenerator, jsonReportGenerator, csvReportGenerator,
                new AiReportInsightBuilder());
    }

    public ReportService(
            ReportGenerator htmlReportGenerator,
            ReportGenerator jsonReportGenerator,
            ReportGenerator csvReportGenerator,
            AiReportInsightBuilder aiReportInsightBuilder) {
        if (htmlReportGenerator == null) {
            throw new IllegalArgumentException("HTML report generator cannot be null");
        }
        if (jsonReportGenerator == null) {
            throw new IllegalArgumentException("JSON report generator cannot be null");
        }
        if (csvReportGenerator == null) {
            throw new IllegalArgumentException("CSV report generator cannot be null");
        }
        if (aiReportInsightBuilder == null) {
            throw new IllegalArgumentException("AI report insight builder cannot be null");
        }

        this.htmlReportGenerator = htmlReportGenerator;
        this.jsonReportGenerator = jsonReportGenerator;
        this.csvReportGenerator = csvReportGenerator;
        this.aiReportInsightBuilder = aiReportInsightBuilder;
    }

    public TestReportDto generateHtmlReport(TestRunResultDto testRunResult) {
        TestReportDto report = createReport(testRunResult, "HTML");
        htmlReportGenerator.generate(report);
        return report;
    }

    public TestReportDto generateJsonReport(TestRunResultDto testRunResult) {
        TestReportDto report = createReport(testRunResult, "JSON");
        jsonReportGenerator.generate(report);
        return report;
    }

    public TestReportDto generateCsvReport(TestRunResultDto testRunResult) {
        TestReportDto report = createReport(testRunResult, "CSV");
        csvReportGenerator.generate(report);
        return report;
    }

    public TestReportDto generateAllReports(TestRunResultDto testRunResult) {
        validateTestRunResult(testRunResult);

        TestReportDto htmlReport = createReport(testRunResult, "HTML");
        TestReportDto jsonReport = createReport(testRunResult, "JSON");
        TestReportDto csvReport = createReport(testRunResult, "CSV");

        htmlReportGenerator.generate(htmlReport);
        jsonReportGenerator.generate(jsonReport);
        csvReportGenerator.generate(csvReport);

        return htmlReport;
    }

    private TestReportDto createReport(TestRunResultDto testRunResult, String format) {
        validateTestRunResult(testRunResult);

        TestReportDto report = new TestReportDto();
        report.setReportId(generateReportId(format));
        report.setReportName(buildReportName(testRunResult));
        report.setReportFormat(format);
        report.setGeneratedAt(LocalDateTime.now());
        report.setTestRunResult(testRunResult);

        // Keep the report name backward compatible. AI severity is stored
        // in the dedicated AI fields and rendered separately by reports.
        aiReportInsightBuilder.populate(report);

        return report;
    }

    private void validateTestRunResult(TestRunResultDto testRunResult) {
        if (testRunResult == null) {
            throw new IllegalArgumentException("Test run result cannot be null");
        }
    }

    private String generateReportId(String format) {
        return "REPORT-" + format + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String buildReportName(TestRunResultDto testRunResult) {
        String runName = testRunResult.getRunName();
        if (runName == null || runName.isBlank()) {
            runName = "API Test Run";
        }
        return runName + " - Report";
    }
}
