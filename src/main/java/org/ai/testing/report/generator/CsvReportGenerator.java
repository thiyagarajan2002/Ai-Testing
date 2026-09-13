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
                .append("Request URL,Request Headers,Request Query Params,Request Path Params,Request Body,")
                .append("HTTP Status,Status Message,Response Time (ms),Response Headers,Response Body,")
                .append("Validation Type,Validation Field,Expected,Actual,Validation Status,Validation Message\n");

        var run = report.getTestRunResult();
        if (run.getSuiteResults() == null || run.getSuiteResults().isEmpty()) {
            return csv.toString();
        }

        for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) {
            if (suite != null) {
                appendSuiteRows(csv, report, suite);
            }
        }
        return csv.toString();
    }

    private void appendSuiteRows(StringBuilder csv, TestReportDto report,
                                 TestSuiteExecutionResultDto suite) {
        if (suite.getTestResults() == null || suite.getTestResults().isEmpty()) {
            appendRow(csv, report, suite, null,
                    suiteStatus(suite), String.valueOf(suite.isExecuted()), suite.getMessage(),
                    "", "", "", "", "",
                    "", "", "", "", "",
                    "", "", "", "", "", "");
            return;
        }

        for (TestCaseExecutor.TestCaseExecutionResult testCase : suite.getTestResults()) {
            if (testCase != null) {
                appendTestCaseRows(csv, report, suite, testCase);
            }
        }
    }

    private void appendTestCaseRows(StringBuilder csv, TestReportDto report,
                                    TestSuiteExecutionResultDto suite,
                                    TestCaseExecutor.TestCaseExecutionResult testCase) {
        BaseRequestDto request = testCase.getRequest();
        String requestUrl = request == null ? "" : request.getUrl();
        String requestHeaders = request == null ? "" : formatMap(request.getHeaders());
        String requestQueryParams = request == null ? "" : formatMap(request.getQueryParams());
        String requestPathParams = request == null ? "" : formatMap(request.getPathParams());
        String requestBody = request != null && request.getBody() != null
                ? request.getBody().getRawBody() : "";

        String httpStatus = "";
        String statusMessage = "";
        String responseTime = "";
        String responseHeaders = "";
        String responseBody = "";

        if (testCase.getResponse() != null) {
            httpStatus = String.valueOf(testCase.getResponse().getStatusCode());
            statusMessage = testCase.getResponse().getStatusMessage();
            responseTime = String.valueOf(testCase.getResponse().getResponseTimeMs());
            responseHeaders = formatMap(testCase.getResponse().getHeaders());
            responseBody = testCase.getResponse().getBody();
        }

        var summary = testCase.getValidationSummary();
        if (summary == null || summary.getResults() == null || summary.getResults().isEmpty()) {
            appendRow(csv, report, suite, testCase,
                    testCaseStatus(testCase), testCase.isExecuted(), testCase.getMessage(),
                    requestUrl, requestHeaders, requestQueryParams, requestPathParams, requestBody,
                    httpStatus, statusMessage, responseTime, responseHeaders, responseBody,
                    "", "", "", "", "", "");
            return;
        }

        for (ValidationResultDto validation : summary.getResults()) {
            if (validation == null) {
                continue;
            }
            appendRow(csv, report, suite, testCase,
                    testCaseStatus(testCase), testCase.isExecuted(), testCase.getMessage(),
                    requestUrl, requestHeaders, requestQueryParams, requestPathParams, requestBody,
                    httpStatus, statusMessage, responseTime, responseHeaders, responseBody,
                    validation.getValidationType(), validation.getField(), validation.getExpected(),
                    validation.getActual(), validation.isPassed() ? "PASSED" : "FAILED", validation.getMessage());
        }
    }

    private void appendRow(StringBuilder csv, TestReportDto report,
                           TestSuiteExecutionResultDto suite,
                           TestCaseExecutor.TestCaseExecutionResult testCase,
                           String... values) {
        var run = report.getTestRunResult();
        String[] prefix = {
                report.getReportId(), run.getRunId(), run.getEnvironment(), run.getExecutionMode(),
                suite.getSuiteId(), suite.getSuiteName(),
                testCase == null ? "" : testCase.getTestCaseId(),
                testCase == null ? "" : testCase.getTestCaseName()
        };

        for (String value : prefix) {
            appendCsvValue(csv, value);
        }
        for (String value : values) {
            appendCsvValue(csv, value);
        }
        csv.append('\n');
    }

    private void appendCsvValue(StringBuilder csv, String value) {
        if (csv.length() > 0 && csv.charAt(csv.length() - 1) != '\n') {
            csv.append(',');
        }
        csv.append(csvValue(value));
    }

    private String testCaseStatus(TestCaseExecutor.TestCaseExecutionResult testCase) {
        if (!testCase.isExecuted()) {
            return "SKIPPED";
        }
        return testCase.isPassed() ? "PASSED" : "FAILED";
    }

    private String suiteStatus(TestSuiteExecutionResultDto suite) {
        if (!suite.isExecuted()) {
            return "SKIPPED";
        }
        return suite.isPassed() ? "PASSED" : "FAILED";
    }

    private String formatMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .map(entry -> String.valueOf(entry.getKey()) + "=" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining("; "));
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"")
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        return "\"" + escaped + "\"";
    }
}
