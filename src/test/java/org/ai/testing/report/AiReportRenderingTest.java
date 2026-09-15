package org.ai.testing.report;

import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.AiHtmlReportGenerator;
import org.ai.testing.report.generator.CsvReportGenerator;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReportRenderingTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRenderPerTestAiInsightInHtml() throws Exception {
        Path html = tempDir.resolve("ai-report.html");
        TestReportDto report = createReport();

        new AiHtmlReportGenerator(html).generate(report);

        String content = Files.readString(html);
        assertTrue(content.contains("AI Test Insights"));
        assertTrue(content.contains("SERVER_ERROR"));
        assertTrue(content.contains("Database service is unavailable"));
        assertTrue(content.contains("Restart or investigate the dependent service."));
    }

    @Test
    void shouldRenderPerTestAiInsightInCsv() throws Exception {
        Path csv = tempDir.resolve("ai-report.csv");
        TestReportDto report = createReport();

        new CsvReportGenerator(csv).generate(report);

        String content = Files.readString(csv);
        assertTrue(content.contains("AI Failure Detected"));
        assertTrue(content.contains("AI Severity Per Test"));
        assertTrue(content.contains("SERVER_ERROR"));
        assertTrue(content.contains("Database service is unavailable"));
    }

    private TestReportDto createReport() {
        AiFailureAnalysis analysis = new AiFailureAnalysis();
        analysis.setFailureDetected(true);
        analysis.setSeverity("CRITICAL");
        analysis.setCategory("SERVER_ERROR");
        analysis.setSummary("Database service is unavailable");
        analysis.setLikelyRootCause("The API dependency returned HTTP 500.");
        analysis.setEvidence(List.of("HTTP status was 500"));
        analysis.setRecommendations(List.of("Restart or investigate the dependent service."));

        TestCaseExecutor.TestCaseExecutionResult test = new TestCaseExecutor.TestCaseExecutionResult();
        test.setTestCaseId("TC-AI-REPORT-001");
        test.setTestCaseName("AI report rendering");
        test.setExecuted(true);
        test.setPassed(false);
        test.setMessage("Test case failed");
        test.setAiFailureAnalysis(analysis);

        TestSuiteExecutionResultDto suite = new TestSuiteExecutionResultDto();
        suite.setSuiteId("SUITE-AI-001");
        suite.setSuiteName("AI Report Suite");
        suite.setExecuted(true);
        suite.setPassed(false);
        suite.setTestResults(List.of(test));

        TestRunResultDto run = new TestRunResultDto();
        run.setRunId("RUN-AI-REPORT-001");
        run.setRunName("AI Report Rendering Run");
        run.setEnvironment("TEST");
        run.setExecutionMode("SEQUENTIAL");
        run.setSuiteResults(List.of(suite));
        run.setTotalSuites(1);
        run.setFailedSuites(1);
        run.setTotalTestCases(1);
        run.setFailedTestCases(1);
        run.setPassed(false);

        TestReportDto report = new TestReportDto();
        report.setReportId("REPORT-AI-001");
        report.setReportName("AI Report Rendering - Report");
        report.setReportFormat("HTML");
        report.setTestRunResult(run);
        report.setAiSeverity("HIGH");
        report.setAiSummary("Run contains a failed API test.");
        report.setAiFindings(List.of("One test failed."));
        report.setAiRecommendations(List.of("Review the failure insight."));
        return report;
    }
}
