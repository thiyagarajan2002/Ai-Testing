package org.ai.testing.report.generator;

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

    public CsvReportGenerator() {
        this(Paths.get("reports", "test-report.csv"));
    }

    public CsvReportGenerator(Path outputPath) {
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null");
        }
        this.outputPath = outputPath;
    }

    @Override
    public void generate(TestReportDto report) {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }
        if (report.getTestRunResult() == null) {
            throw new IllegalArgumentException("Test run result cannot be null");
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, buildCsv(report), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV report: " + outputPath, e);
        }
    }

    private String buildCsv(TestReportDto report) {
        StringBuilder csv = new StringBuilder();
        csv.append("Report ID,Run ID,Environment,Execution Mode,Suite ID,Suite Name,")
                .append("Test Case ID,Test Case Name,Status,Executed,Message,")
                .append("Request URL,Request Headers,Query Params,Path Params,Request Content Type,Request Body,")
                .append("HTTP Status,Status Message,Response Time (ms),Response Headers,Response Body,")
                .append("Validation Type,Validation Field,Expected,Actual,Validation Status,Validation Message\n");

        var run = report.getTestRunResult();
        if (run.getSuiteResults() == null) {
            return csv.toString();
        }

        for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) {
            if (suite != null) {
                appendSuiteRows(csv, report, suite);
            }
        }
        return csv.toString();
    }

    private void appendSuiteRows(
            StringBuilder csv,
            TestReportDto report,
            TestSuiteExecutionResultDto suite) {

        if (suite.getTestResults() == null || suite.getTestResults().isEmpty()) {
            writeRow(csv, baseValues(report, suite, null,
                    suiteStatus(suite), suite.isExecuted(), suite.getMessage()));
            return;
        }

        for (TestCaseExecutor.TestCaseExecutionResult testCase : suite.getTestResults()) {
            if (testCase != null) {
                appendTestCaseRows(csv, report, suite, testCase);
            }
        }
    }

    private void appendTestCaseRows(
            StringBuilder csv,
            TestReportDto report,
            TestSuiteExecutionResultDto suite,
            TestCaseExecutor.TestCaseExecutionResult testCase) {

        BaseRequestDto request = testCase.getRequest();
        String requestUrl = request == null ? "" : nullToEmpty(request.getUrl());
        String requestHeaders = request == null ? "" : formatMap(request.getHeaders());
        String queryParams = request == null ? "" : formatMap(request.getQueryParams());
        String pathParams = request == null ? "" : formatMap(request.getPathParams());
        String requestContentType = "";
        String requestBody = "";

        if (request != null && request.getBody() != null) {
            requestContentType = nullToEmpty(request.getBody().getContentType());
            requestBody = nullToEmpty(request.getBody().getRawBody());
        }

        String httpStatus = "";
        String statusMessage = "";
        String responseTime = "";
        String responseHeaders = "";
        String responseBody = "";

        if (testCase.getResponse() != null) {
            httpStatus = String.valueOf(testCase.getResponse().getStatusCode());
            statusMessage = nullToEmpty(testCase.getResponse().getStatusMessage());
            responseTime = String.valueOf(testCase.getResponse().getResponseTimeMs());
            responseHeaders = formatMap(testCase.getResponse().getHeaders());
            responseBody = nullToEmpty(testCase.getResponse().getBody());
        }

        String[] requestResponse = {
                requestUrl, requestHeaders, queryParams, pathParams,
                requestContentType, requestBody, httpStatus, statusMessage,
                responseTime, responseHeaders, responseBody
        };

        if (testCase.getValidationSummary() == null
                || testCase.getValidationSummary().getResults() == null
                || testCase.getValidationSummary().getResults().isEmpty()) {
            writeRow(csv, combine(
                    baseValues(report, suite, testCase, testCaseStatus(testCase),
                            testCase.isExecuted(), testCase.getMessage()),
                    requestResponse,
                    new String[]{"", "", "", "", "", ""}
            ));
            return;
        }

        for (ValidationResultDto validation : testCase.getValidationSummary().getResults()) {
            if (validation == null) {
                continue;
            }
            writeRow(csv, combine(
                    baseValues(report, suite, testCase, testCaseStatus(testCase),
                            testCase.isExecuted(), testCase.getMessage()),
                    requestResponse,
                    new String[]{
                            validation.getValidationType(), validation.getField(),
                            validation.getExpected(), validation.getActual(),
                            validation.isPassed() ? "PASSED" : "FAILED",
                            validation.getMessage()
                    }
            ));
        }
    }

    private String[] baseValues(
            TestReportDto report,
            TestSuiteExecutionResultDto suite,
            TestCaseExecutor.TestCaseExecutionResult testCase,
            String status,
            boolean executed,
            String message) {

        return new String[]{
                report.getReportId(),
                report.getTestRunResult().getRunId(),
                report.getTestRunResult().getEnvironment(),
                report.getTestRunResult().getExecutionMode(),
                suite.getSuiteId(),
                suite.getSuiteName(),
                testCase == null ? "" : testCase.getTestCaseId(),
                testCase == null ? "" : testCase.getTestCaseName(),
                status,
                String.valueOf(executed),
                message
        };
    }

    private String[] combine(String[] first, String[] second, String[] third) {
        String[] result = new String[first.length + second.length + third.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        System.arraycopy(third, 0, result, first.length + second.length, third.length);
        return result;
    }

    private void writeRow(StringBuilder csv, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvValue(values[i]));
        }
        csv.append('\n');
    }

    private String formatMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.entrySet().stream()
                .map(e -> String.valueOf(e.getKey()) + "=" + String.valueOf(e.getValue()))
                .collect(Collectors.joining("; "));
    }

    private String testCaseStatus(TestCaseExecutor.TestCaseExecutionResult testCase) {
        if (!testCase.isExecuted()) return "SKIPPED";
        return testCase.isPassed() ? "PASSED" : "FAILED";
    }

    private String suiteStatus(TestSuiteExecutionResultDto suite) {
        if (!suite.isExecuted()) return "SKIPPED";
        return suite.isPassed() ? "PASSED" : "FAILED";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String csvValue(String value) {
        String escaped = nullToEmpty(value).replace("\"", "\"\"");
        return "\"" + escaped.replace("\r\n", "\n").replace("\r", "\n") + "\"";
    }
}
