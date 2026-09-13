package org.ai.testing.report.generator;



import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HtmlReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path outputPath;

    public HtmlReportGenerator() {
        this(Paths.get("reports", "test-report.html"));
    }

    public HtmlReportGenerator(Path outputPath) {
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
            throw new IllegalArgumentException(
                    "Test run result cannot be null"
            );
        }

        try {
            Path parent = outputPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            String html = buildHtml(report);

            Files.writeString(
                    outputPath,
                    html,
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to generate HTML report: " + outputPath,
                    e
            );
        }
    }

    private String buildHtml(TestReportDto report) {

        TestRunResultDto run = report.getTestRunResult();

        StringBuilder html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>
                """);

        html.append(escapeHtml(
                report.getReportName() != null
                        ? report.getReportName()
                        : "API Test Report"
        ));

        html.append("""
                    </title>

                    <style>

                        * {
                            box-sizing: border-box;
                        }

                        body {
                            margin: 0;
                            padding: 0;
                            font-family: Arial, Helvetica, sans-serif;
                            background: #f4f6f8;
                            color: #202124;
                        }

                        .container {
                            width: 95%;
                            max-width: 1400px;
                            margin: 30px auto;
                        }

                        .header {
                            background: #ffffff;
                            padding: 25px;
                            border-radius: 10px;
                            margin-bottom: 20px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                        }

                        .header h1 {
                            margin: 0 0 10px;
                        }

                        .metadata {
                            display: grid;
                            grid-template-columns:
                                repeat(auto-fit, minmax(200px, 1fr));
                            gap: 12px;
                            margin-top: 20px;
                        }

                        .metadata-item {
                            background: #f8f9fa;
                            padding: 12px;
                            border-radius: 6px;
                        }

                        .metadata-label {
                            font-size: 12px;
                            color: #6c757d;
                            margin-bottom: 5px;
                        }

                        .metadata-value {
                            font-weight: bold;
                        }

                        .summary-grid {
                            display: grid;
                            grid-template-columns:
                                repeat(auto-fit, minmax(180px, 1fr));
                            gap: 15px;
                            margin-bottom: 20px;
                        }

                        .card {
                            background: #ffffff;
                            padding: 20px;
                            border-radius: 10px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                        }

                        .card-title {
                            font-size: 13px;
                            color: #6c757d;
                            margin-bottom: 8px;
                        }

                        .card-value {
                            font-size: 28px;
                            font-weight: bold;
                        }

                        .passed {
                            color: #198754;
                        }

                        .failed {
                            color: #dc3545;
                        }

                        .skipped {
                            color: #fd7e14;
                        }

                        .neutral {
                            color: #495057;
                        }

                        .suite {
                            background: #ffffff;
                            margin-bottom: 20px;
                            border-radius: 10px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                            overflow: hidden;
                        }

                        .suite-header {
                            padding: 18px 20px;
                            border-bottom: 1px solid #dee2e6;
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            gap: 15px;
                        }

                        .suite-title {
                            margin: 0;
                        }

                        .status {
                            padding: 5px 12px;
                            border-radius: 20px;
                            font-size: 12px;
                            font-weight: bold;
                        }

                        .status-passed {
                            background: #d1e7dd;
                            color: #0f5132;
                        }

                        .status-failed {
                            background: #f8d7da;
                            color: #842029;
                        }

                        .status-skipped {
                            background: #ffe5d0;
                            color: #984c0c;
                        }

                        .suite-body {
                            padding: 20px;
                        }

                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 10px;
                        }

                        th,
                        td {
                            padding: 10px;
                            border-bottom: 1px solid #dee2e6;
                            text-align: left;
                            vertical-align: top;
                        }

                        th {
                            background: #f8f9fa;
                            font-size: 13px;
                        }

                        td {
                            font-size: 13px;
                        }

                        .test-case {
                            margin-bottom: 25px;
                        }

                        .test-case:last-child {
                            margin-bottom: 0;
                        }

                        .test-case-header {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            gap: 10px;
                            margin-bottom: 10px;
                        }

                        .test-case-title {
                            font-size: 16px;
                            font-weight: bold;
                        }

                        .validation-title {
                            margin-top: 18px;
                            margin-bottom: 8px;
                        }

                        pre {
                            background: #212529;
                            color: #f8f9fa;
                            padding: 15px;
                            border-radius: 6px;
                            overflow-x: auto;
                            white-space: pre-wrap;
                            word-break: break-word;
                        }

                        .footer {
                            text-align: center;
                            color: #6c757d;
                            font-size: 12px;
                            padding: 20px;
                        }

                    </style>
                </head>

                <body>

                <div class="container">
                """);

        appendHeader(html, report, run);
        appendSummary(html, run);
        appendSuites(html, run);

        html.append("""
                    <div class="footer">
                        Generated by AI API Testing Agent
                    </div>
                </div>

                </body>
                </html>
                """);

        return html.toString();
    }

    private void appendHeader(
            StringBuilder html,
            TestReportDto report,
            TestRunResultDto run) {

        String generatedAt =
                report.getGeneratedAt() != null
                        ? formatDate(report.getGeneratedAt())
                        : formatDate(LocalDateTime.now());

        html.append("""
                <div class="header">

                    <h1>
                """);

        html.append(escapeHtml(
                report.getReportName() != null
                        ? report.getReportName()
                        : "API Test Report"
        ));

        html.append("""
                    </h1>

                    <div class="metadata">

                        <div class="metadata-item">
                            <div class="metadata-label">Report ID</div>
                            <div class="metadata-value">
                """);

        html.append(escapeHtml(
                nullToEmpty(report.getReportId())
        ));

        html.append("""
                            </div>
                        </div>

                        <div class="metadata-item">
                            <div class="metadata-label">Run ID</div>
                            <div class="metadata-value">
                """);

        html.append(escapeHtml(
                nullToEmpty(run.getRunId())
        ));

        html.append("""
                            </div>
                        </div>

                        <div class="metadata-item">
                            <div class="metadata-label">Environment</div>
                            <div class="metadata-value">
                """);

        html.append(escapeHtml(
                nullToEmpty(run.getEnvironment())
        ));

        html.append("""
                            </div>
                        </div>

                        <div class="metadata-item">
                            <div class="metadata-label">Execution Mode</div>
                            <div class="metadata-value">
                """);

        html.append(escapeHtml(
                nullToEmpty(run.getExecutionMode())
        ));

        html.append("""
                            </div>
                        </div>

                        <div class="metadata-item">
                            <div class="metadata-label">Generated At</div>
                            <div class="metadata-value">
                """);

        html.append(escapeHtml(generatedAt));

        html.append("""
                            </div>
                        </div>

                    </div>
                </div>
                """);
    }

    private void appendSummary(
            StringBuilder html,
            TestRunResultDto run) {

        html.append("""
                <div class="summary-grid">

                    <div class="card">
                        <div class="card-title">Run Status</div>
                        <div class="card-value %s">
                """.formatted(
                run.isPassed() ? "passed" : "failed"
        ));

        html.append(run.isPassed() ? "PASSED" : "FAILED");

        html.append("""
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-title">Suites</div>
                        <div class="card-value neutral">
                """);

        html.append(run.getTotalSuites());

        html.append("""
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-title">Passed Suites</div>
                        <div class="card-value passed">
                """);

        html.append(run.getPassedSuites());

        html.append("""
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-title">Failed Suites</div>
                        <div class="card-value failed">
                """);

        html.append(run.getFailedSuites());

        html.append("""
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-title">Skipped Suites</div>
                        <div class="card-value skipped">
                """);

        html.append(run.getSkippedSuites());

        html.append("""
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-title">Total Test Cases</div>
                        <div class="card-value neutral">
                """);

        html.append(run.getTotalTestCases());

        html.append("""
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-title">Passed Test Cases</div>
                        <div class="card-value passed">
                """);

        html.append(run.getPassedTestCases());

        html.append("""
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-title">Failed Test Cases</div>
                        <div class="card-value failed">
                """);

        html.append(run.getFailedTestCases());

        html.append("""
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-title">Execution Time</div>
                        <div class="card-value neutral">
                """);

        html.append(run.getExecutionTimeMs());
        html.append(" ms");

        html.append("""
                        </div>
                    </div>

                </div>
                """);
    }

    private void appendSuites(
            StringBuilder html,
            TestRunResultDto run) {

        if (run.getSuiteResults() == null
                || run.getSuiteResults().isEmpty()) {

            html.append("""
                    <div class="card">
                        <h3>No suite results available</h3>
                    </div>
                    """);

            return;
        }

        for (TestSuiteExecutionResultDto suite
                : run.getSuiteResults()) {

            appendSuite(html, suite);
        }
    }

    private void appendSuite(
            StringBuilder html,
            TestSuiteExecutionResultDto suite) {

        String statusClass;
        String statusText;

        if (!suite.isExecuted()) {
            statusClass = "status-skipped";
            statusText = "SKIPPED";
        } else if (suite.isPassed()) {
            statusClass = "status-passed";
            statusText = "PASSED";
        } else {
            statusClass = "status-failed";
            statusText = "FAILED";
        }

        html.append("""
                <div class="suite">

                    <div class="suite-header">

                        <div>
                            <h2 class="suite-title">
                """);

        html.append(escapeHtml(
                nullToEmpty(suite.getSuiteName())
        ));

        html.append("""
                            </h2>

                            <small>
                """);

        html.append(escapeHtml(
                nullToEmpty(suite.getSuiteId())
        ));

        html.append("""
                            </small>
                        </div>

                        <span class="status %s">
                """.formatted(statusClass));

        html.append(statusText);

        html.append("""
                        </span>

                    </div>

                    <div class="suite-body">

                        <p>
                            <strong>Message:</strong>
                """);

        html.append(escapeHtml(
                nullToEmpty(suite.getMessage())
        ));

        html.append("""
                        </p>

                        <p>
                            <strong>Execution Time:</strong>
                """);

        html.append(suite.getExecutionTimeMs());
        html.append("""
                            ms
                        </p>
                """);

        appendTestCases(html, suite);

        html.append("""
                    </div>
                </div>
                """);
    }

    private void appendTestCases(
            StringBuilder html,
            TestSuiteExecutionResultDto suite) {

        if (suite.getTestResults() == null
                || suite.getTestResults().isEmpty()) {

            html.append("""
                    <p>No test case results available.</p>
                    """);

            return;
        }

        for (TestCaseExecutor.TestCaseExecutionResult testCase
                : suite.getTestResults()) {

            appendTestCase(html, testCase);
        }
    }

    private void appendTestCase(
            StringBuilder html,
            TestCaseExecutor.TestCaseExecutionResult testCase) {

        String statusClass;
        String statusText;

        if (!testCase.isExecuted()) {
            statusClass = "status-skipped";
            statusText = "SKIPPED";
        } else if (testCase.isPassed()) {
            statusClass = "status-passed";
            statusText = "PASSED";
        } else {
            statusClass = "status-failed";
            statusText = "FAILED";
        }

        html.append("""
                <div class="test-case">

                    <div class="test-case-header">

                        <div class="test-case-title">
                """);

        html.append(escapeHtml(
                nullToEmpty(testCase.getTestCaseName())
        ));

        html.append("""
                        </div>

                        <span class="status %s">
                """.formatted(statusClass));

        html.append(statusText);

        html.append("""
                        </span>

                    </div>

                    <p>
                        <strong>Test Case ID:</strong>
                """);

        html.append(escapeHtml(
                nullToEmpty(testCase.getTestCaseId())
        ));

        html.append("""
                    </p>

                    <p>
                        <strong>Message:</strong>
                """);

        html.append(escapeHtml(
                nullToEmpty(testCase.getMessage())
        ));

        html.append("""
                    </p>
                """);

        appendResponse(html, testCase);
        appendValidationResults(html, testCase);

        html.append("""
                </div>
                """);
    }

    private void appendResponse(
            StringBuilder html,
            TestCaseExecutor.TestCaseExecutionResult testCase) {

        if (testCase.getResponse() == null) {
            return;
        }

        html.append("""
                <h4>Response</h4>

                <table>

                    <tr>
                        <th>Status Code</th>
                        <td>
                """);

        html.append(testCase.getResponse().getStatusCode());

        html.append("""
                        </td>
                    </tr>

                    <tr>
                        <th>Status Message</th>
                        <td>
                """);

        html.append(escapeHtml(
                nullToEmpty(
                        testCase.getResponse().getStatusMessage()
                )
        ));

        html.append("""
                        </td>
                    </tr>

                    <tr>
                        <th>Response Time</th>
                        <td>
                """);

        html.append(testCase.getResponse().getResponseTimeMs());

        html.append("""
                            ms
                        </td>
                    </tr>

                </table>

                <h4>Response Body</h4>

                <pre>
                """);

        html.append(escapeHtml(
                nullToEmpty(testCase.getResponse().getBody())
        ));

        html.append("""
                </pre>
                """);
    }

    private void appendValidationResults(
            StringBuilder html,
            TestCaseExecutor.TestCaseExecutionResult testCase) {

        if (testCase.getValidationSummary() == null
                || testCase.getValidationSummary().getResults() == null
                || testCase.getValidationSummary().getResults().isEmpty()) {

            return;
        }

        html.append("""
                <h4 class="validation-title">
                    Validation Results
                </h4>

                <table>

                    <thead>
                        <tr>
                            <th>Status</th>
                            <th>Type</th>
                            <th>Field</th>
                            <th>Expected</th>
                            <th>Actual</th>
                            <th>Message</th>
                        </tr>
                    </thead>

                    <tbody>
                """);

        for (ValidationResultDto result
                : testCase.getValidationSummary().getResults()) {

            html.append("<tr>");

            html.append("<td>");

            if (result.isPassed()) {
                html.append("<span class=\"passed\">PASSED</span>");
            } else {
                html.append("<span class=\"failed\">FAILED</span>");
            }

            html.append("</td>");

            html.append("<td>");
            html.append(escapeHtml(
                    nullToEmpty(result.getValidationType())
            ));
            html.append("</td>");

            html.append("<td>");
            html.append(escapeHtml(
                    nullToEmpty(result.getField())
            ));
            html.append("</td>");

            html.append("<td>");
            html.append(escapeHtml(
                    nullToEmpty(result.getExpected())
            ));
            html.append("</td>");

            html.append("<td>");
            html.append(escapeHtml(
                    nullToEmpty(result.getActual())
            ));
            html.append("</td>");

            html.append("<td>");
            html.append(escapeHtml(
                    nullToEmpty(result.getMessage())
            ));
            html.append("</td>");

            html.append("</tr>");
        }

        html.append("""
                    </tbody>

                </table>
                """);
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
