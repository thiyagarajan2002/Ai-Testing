package org.ai.testing.report.generator;

import org.ai.testing.ai.model.AiExecutionReportMetadata;
import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.ai.model.AiGenerationReportMetadata;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

public class CsvReportGenerator implements ReportGenerator {
    private final Path outputPath;
    public CsvReportGenerator() { this(Paths.get("reports", "test-report.csv")); }
    public CsvReportGenerator(Path outputPath) {
        if (outputPath == null) throw new IllegalArgumentException("Output path cannot be null");
        this.outputPath = outputPath;
    }
    @Override
    public void generate(TestReportDto report) {
        if (report == null) throw new IllegalArgumentException("Report cannot be null");
        if (report.getTestRunResult() == null) throw new IllegalArgumentException("Test run result cannot be null");
        try {
            Path parent = outputPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(outputPath, buildCsv(report), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new RuntimeException("Failed to generate CSV report: " + outputPath, e); }
    }
    private String buildCsv(TestReportDto report) {
        StringBuilder csv = new StringBuilder();
        csv.append("Report ID,Run ID,Environment,Execution Mode,Run Name,AI Severity,AI Summary,AI Findings,AI Recommendations,")
                .append("AI Generation Strategy,AI Source Suite ID,AI Source Test Case ID,AI Positive Test Count,AI Negative Test Count,")
                .append("AI Review Status,AI Review Passed,AI Approved,AI Attached,AI Review Findings,")
                .append("AI Execution Status,AI Execution Passed,AI Execution Message,AI Execution Source Suite ID,AI Execution Source Test Case ID,")
                .append("AI Execution Total Tests,AI Execution Passed Tests,AI Execution Failed Tests,AI Execution Skipped Tests,AI Execution Failed Test IDs,")
                .append("Suite ID,Suite Name,Test Case ID,Test Case Name,Status,Executed,Message,")
                .append("AI Failure Detected,AI Severity Per Test,AI Category,AI Summary Per Test,AI Root Cause,AI Evidence,AI Recommendations Per Test,")
                .append("Request URL,Request Headers,Query Params,Path Params,Request Content Type,Request Body,")
                .append("HTTP Status,Status Message,Response Time (ms),Response Headers,Response Body,")
                .append("Validation Type,Validation Field,Expected,Actual,Validation Status,Validation Message\n");
        var run = report.getTestRunResult();
        if (run.getSuiteResults() == null) return csv.toString();
        for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) if (suite != null) appendSuiteRows(csv, report, suite);
        return csv.toString();
    }
    private void appendSuiteRows(StringBuilder csv, TestReportDto report, TestSuiteExecutionResultDto suite) {
        if (suite.getTestResults() == null || suite.getTestResults().isEmpty()) {
            writeRow(csv, baseValues(report, suite, null, suiteStatus(suite), suite.isExecuted(), suite.getMessage(), null));
            return;
        }
        for (TestCaseExecutor.TestCaseExecutionResult testCase : suite.getTestResults()) if (testCase != null) appendTestCaseRows(csv, report, suite, testCase);
    }
    private void appendTestCaseRows(StringBuilder csv, TestReportDto report, TestSuiteExecutionResultDto suite, TestCaseExecutor.TestCaseExecutionResult testCase) {
        BaseRequestDto request = testCase.getRequest();
        String requestUrl = request == null ? "" : nullToEmpty(request.getUrl());
        String requestHeaders = request == null ? "" : formatMap(request.getHeaders());
        String queryParams = request == null ? "" : formatMap(request.getQueryParams());
        String pathParams = request == null ? "" : formatMap(request.getPathParams());
        String requestContentType = "", requestBody = "";
        if (request != null && request.getBody() != null) {
            requestContentType = nullToEmpty(request.getBody().getContentType());
            requestBody = nullToEmpty(request.getBody().getRawBody());
        }
        String httpStatus = "", statusMessage = "", responseTime = "", responseHeaders = "", responseBody = "";
        if (testCase.getResponse() != null) {
            httpStatus = Integer.toString(testCase.getResponse().getStatusCode());
            statusMessage = nullToEmpty(testCase.getResponse().getStatusMessage());
            responseTime = Long.toString(testCase.getResponse().getResponseTimeMs());
            responseHeaders = formatMap(testCase.getResponse().getHeaders());
            responseBody = nullToEmpty(testCase.getResponse().getBody());
        }
        String[] requestResponse = {requestUrl, requestHeaders, queryParams, pathParams, requestContentType, requestBody,
                httpStatus, statusMessage, responseTime, responseHeaders, responseBody};
        if (testCase.getValidationSummary() == null || testCase.getValidationSummary().getResults() == null || testCase.getValidationSummary().getResults().isEmpty()) {
            writeRow(csv, combine(baseValues(report, suite, testCase, testCaseStatus(testCase), testCase.isExecuted(), testCase.getMessage(), testCase.getAiFailureAnalysis()), requestResponse, new String[]{"", "", "", "", "", ""}));
            return;
        }
        for (ValidationResultDto validation : testCase.getValidationSummary().getResults()) {
            if (validation == null) continue;
            writeRow(csv, combine(baseValues(report, suite, testCase, testCaseStatus(testCase), testCase.isExecuted(), testCase.getMessage(), testCase.getAiFailureAnalysis()), requestResponse,
                    new String[]{validation.getValidationType(), validation.getField(), validation.getExpected(), validation.getActual(), validation.isPassed() ? "PASSED" : "FAILED", validation.getMessage()}));
        }
    }
    private String[] baseValues(TestReportDto report, TestSuiteExecutionResultDto suite, TestCaseExecutor.TestCaseExecutionResult testCase,
                                String status, boolean executed, String message, AiFailureAnalysis aiFailureAnalysis) {
        String[] ai = aiValues(aiFailureAnalysis), generation = generationValues(report.getAiGenerationMetadata()), execution = executionValues(report.getAiExecutionMetadata());
        return new String[]{report.getReportId(), report.getTestRunResult().getRunId(), report.getTestRunResult().getEnvironment(), report.getTestRunResult().getExecutionMode(), report.getTestRunResult().getRunName(), report.getAiSeverity(), report.getAiSummary(), formatList(report.getAiFindings()), formatList(report.getAiRecommendations()),
                generation[0], generation[1], generation[2], generation[3], generation[4], generation[5], generation[6], generation[7], generation[8], generation[9],
                execution[0], execution[1], execution[2], execution[3], execution[4], execution[5], execution[6], execution[7], execution[8], execution[9],
                suite.getSuiteId(), suite.getSuiteName(), testCase == null ? "" : testCase.getTestCaseId(), testCase == null ? "" : testCase.getTestCaseName(), status, String.valueOf(executed), message,
                ai[0], ai[1], ai[2], ai[3], ai[4], ai[5], ai[6]};
    }
    private String[] generationValues(AiGenerationReportMetadata metadata) {
        if (metadata == null) return new String[]{"", "", "", "", "", "", "", "", "", ""};
        return new String[]{nullToEmpty(metadata.getStrategy()), nullToEmpty(metadata.getSourceSuiteId()), nullToEmpty(metadata.getSourceTestCaseId()), String.valueOf(metadata.getPositiveTestCaseCount()), String.valueOf(metadata.getNegativeTestCaseCount()), nullToEmpty(metadata.getReviewStatus()), String.valueOf(metadata.isReviewPassed()), String.valueOf(metadata.isApproved()), String.valueOf(metadata.isAttached()), formatList(metadata.getReviewFindings())};
    }
    private String[] executionValues(AiExecutionReportMetadata metadata) {
        if (metadata == null) return new String[]{"", "", "", "", "", "", "", "", "", ""};
        return new String[]{metadata.isExecuted() ? "EXECUTED" : "NOT_EXECUTED", metadata.isPassed() ? "PASSED" : "FAILED", nullToEmpty(metadata.getMessage()), nullToEmpty(metadata.getSourceSuiteId()), nullToEmpty(metadata.getSourceTestCaseId()), String.valueOf(metadata.getTotalTestCases()), String.valueOf(metadata.getPassedTestCases()), String.valueOf(metadata.getFailedTestCases()), String.valueOf(metadata.getSkippedTestCases()), formatList(metadata.getFailedTestCaseIds())};
    }
    private String[] aiValues(AiFailureAnalysis analysis) {
        if (analysis == null) return new String[]{"", "", "", "", "", "", ""};
        return new String[]{analysis.isFailureDetected() ? "true" : "false", nullToEmpty(analysis.getSeverity()), nullToEmpty(analysis.getCategory()), nullToEmpty(analysis.getSummary()), nullToEmpty(analysis.getLikelyRootCause()), formatList(analysis.getEvidence()), formatList(analysis.getRecommendations())};
    }
    private String[] combine(String[] first, String[] second, String[] third) {
        String[] result = new String[first.length + second.length + third.length];
        System.arraycopy(first, 0, result, 0, first.length); System.arraycopy(second, 0, result, first.length, second.length); System.arraycopy(third, 0, result, first.length + second.length, third.length); return result;
    }
    private void writeRow(StringBuilder csv, String[] values) {
        for (int i = 0; i < values.length; i++) { if (i > 0) csv.append(','); csv.append(csvValue(values[i])); } csv.append('\n');
    }
    private String formatMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.entrySet().stream().map(e -> String.valueOf(e.getKey()) + "=" + String.valueOf(e.getValue())).collect(Collectors.joining("; "));
    }
    private String formatList(java.util.List<String> values) { if (values == null || values.isEmpty()) return ""; return String.join("; ", values); }
    private String testCaseStatus(TestCaseExecutor.TestCaseExecutionResult testCase) { if (!testCase.isExecuted()) return "SKIPPED"; return testCase.isPassed() ? "PASSED" : "FAILED"; }
    private String suiteStatus(TestSuiteExecutionResultDto suite) { if (!suite.isExecuted()) return "SKIPPED"; return suite.isPassed() ? "PASSED" : "FAILED"; }
    private String nullToEmpty(String value) { return value == null ? "" : value; }
    private String csvValue(String value) { String escaped = nullToEmpty(value).replace("\"", "\"\""); return "\"" + escaped.replace("\r\n", "\n").replace("\r", "\n") + "\""; }
}
