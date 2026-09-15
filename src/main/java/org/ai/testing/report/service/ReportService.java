package org.ai.testing.report.service;

import org.ai.testing.ai.history.AiExecutionHistoryEntry;
import org.ai.testing.ai.history.AiExecutionHistoryTrend;
import org.ai.testing.ai.model.AiExecutionReportMetadata;
import org.ai.testing.ai.model.AiGenerationReportMetadata;
import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.ai.model.AiHistoryReportMetadata;
import org.ai.testing.ai.model.AiTestGenerationOrchestrationResult;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.AiHtmlReportGenerator;
import org.ai.testing.report.generator.CsvReportGenerator;
import org.ai.testing.report.generator.JsonReportGenerator;
import org.ai.testing.report.generator.ReportGenerator;
import org.ai.testing.testrun.dto.TestRunResultDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReportService {

    private final ReportGenerator htmlReportGenerator;
    private final ReportGenerator jsonReportGenerator;
    private final ReportGenerator csvReportGenerator;
    private final AiReportInsightBuilder aiReportInsightBuilder;

    public ReportService() {
        this(new AiHtmlReportGenerator(), new JsonReportGenerator(), new CsvReportGenerator(),
                new AiReportInsightBuilder());
    }

    public ReportService(ReportGenerator htmlReportGenerator, ReportGenerator jsonReportGenerator,
                         ReportGenerator csvReportGenerator) {
        this(htmlReportGenerator, jsonReportGenerator, csvReportGenerator, new AiReportInsightBuilder());
    }

    public ReportService(ReportGenerator htmlReportGenerator, ReportGenerator jsonReportGenerator,
                         ReportGenerator csvReportGenerator, AiReportInsightBuilder aiReportInsightBuilder) {
        if (htmlReportGenerator == null) throw new IllegalArgumentException("HTML report generator cannot be null");
        if (jsonReportGenerator == null) throw new IllegalArgumentException("JSON report generator cannot be null");
        if (csvReportGenerator == null) throw new IllegalArgumentException("CSV report generator cannot be null");
        if (aiReportInsightBuilder == null) throw new IllegalArgumentException("AI report insight builder cannot be null");
        this.htmlReportGenerator = htmlReportGenerator;
        this.jsonReportGenerator = jsonReportGenerator;
        this.csvReportGenerator = csvReportGenerator;
        this.aiReportInsightBuilder = aiReportInsightBuilder;
    }

    public TestReportDto generateHtmlReport(TestRunResultDto testRunResult) {
        TestReportDto report = createReport(testRunResult, "HTML", null, null, null);
        htmlReportGenerator.generate(report);
        return report;
    }

    public TestReportDto generateJsonReport(TestRunResultDto testRunResult) {
        TestReportDto report = createReport(testRunResult, "JSON", null, null, null);
        jsonReportGenerator.generate(report);
        return report;
    }

    public TestReportDto generateCsvReport(TestRunResultDto testRunResult) {
        TestReportDto report = createReport(testRunResult, "CSV", null, null, null);
        csvReportGenerator.generate(report);
        return report;
    }

    public TestReportDto generateAllReports(TestRunResultDto testRunResult) {
        return generateAllReports(testRunResult, null, null, null, null);
    }

    public TestReportDto generateAllReports(TestRunResultDto testRunResult,
                                            AiTestGenerationOrchestrationResult generationResult) {
        return generateAllReports(testRunResult, generationResult, null, null, null);
    }

    public TestReportDto generateAllReports(TestRunResultDto testRunResult,
                                            AiTestGenerationOrchestrationResult generationResult,
                                            AiGeneratedSuiteExecutionResult executionResult) {
        return generateAllReports(testRunResult, generationResult, executionResult, null, null);
    }

    public TestReportDto generateAllReports(TestRunResultDto testRunResult,
                                            AiTestGenerationOrchestrationResult generationResult,
                                            AiGeneratedSuiteExecutionResult executionResult,
                                            AiExecutionHistoryTrend historyTrend,
                                            List<AiExecutionHistoryEntry> history) {
        validateTestRunResult(testRunResult);
        AiGenerationReportMetadata generationMetadata = generationResult == null
                ? null : generationResult.toReportMetadata();
        AiExecutionReportMetadata executionMetadata = AiExecutionReportMetadata.from(executionResult);
        AiHistoryReportMetadata historyMetadata = AiHistoryReportMetadata.from(historyTrend, history);

        TestReportDto htmlReport = createReport(testRunResult, "HTML", generationMetadata, executionMetadata, historyMetadata);
        TestReportDto jsonReport = createReport(testRunResult, "JSON", generationMetadata, executionMetadata, historyMetadata);
        TestReportDto csvReport = createReport(testRunResult, "CSV", generationMetadata, executionMetadata, historyMetadata);
        htmlReportGenerator.generate(htmlReport);
        jsonReportGenerator.generate(jsonReport);
        csvReportGenerator.generate(csvReport);
        return htmlReport;
    }

    private TestReportDto createReport(TestRunResultDto testRunResult, String format,
                                       AiGenerationReportMetadata generationMetadata,
                                       AiExecutionReportMetadata executionMetadata,
                                       AiHistoryReportMetadata historyMetadata) {
        validateTestRunResult(testRunResult);
        TestReportDto report = new TestReportDto();
        report.setReportId(generateReportId(format));
        report.setReportName(buildReportName(testRunResult));
        report.setReportFormat(format);
        report.setGeneratedAt(LocalDateTime.now());
        report.setTestRunResult(testRunResult);
        report.setAiGenerationMetadata(generationMetadata);
        report.setAiExecutionMetadata(executionMetadata);
        report.setAiHistoryMetadata(historyMetadata);
        aiReportInsightBuilder.populate(report);
        return report;
    }

    private void validateTestRunResult(TestRunResultDto testRunResult) {
        if (testRunResult == null) throw new IllegalArgumentException("Test run result cannot be null");
    }

    private String generateReportId(String format) {
        return "REPORT-" + format + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String buildReportName(TestRunResultDto testRunResult) {
        String runName = testRunResult.getRunName();
        if (runName == null || runName.isBlank()) runName = "API Test Run";
        return runName + " - Report";
    }
}
