package org.ai.testing.report.generator;


import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CsvReportGenerator implements ReportGenerator {

    private final Path outputPath;

    public CsvReportGenerator() {
        this(Paths.get("reports", "test-report.csv"));
    }

    public CsvReportGenerator(Path outputPath) {

        if (outputPath == null) {
            throw new IllegalArgumentException(
                    "Output path cannot be null"
            );
        }

        this.outputPath = outputPath;
    }

    @Override
    public void generate(TestReportDto report) {

        if (report == null) {
            throw new IllegalArgumentException(
                    "Report cannot be null"
            );
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

            String csv = buildCsv(report);

            Files.writeString(
                    outputPath,
                    csv,
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to generate CSV report: "
                            + outputPath,
                    e
            );
        }
    }

    private String buildCsv(TestReportDto report) {

        StringBuilder csv = new StringBuilder();

        csv.append(
                "Report ID,"
                        + "Run ID,"
                        + "Environment,"
                        + "Execution Mode,"
                        + "Suite ID,"
                        + "Suite Name,"
                        + "Test Case ID,"
                        + "Test Case Name,"
                        + "Status,"
                        + "Executed,"
                        + "Message,"
                        + "HTTP Status,"
                        + "Response Time (ms),"
                        + "Validation Type,"
                        + "Validation Field,"
                        + "Expected,"
                        + "Actual,"
                        + "Validation Status,"
                        + "Validation Message"
                        + "\n"
        );

        var run =
                report.getTestRunResult();

        if (run.getSuiteResults() == null
                || run.getSuiteResults().isEmpty()) {

            return csv.toString();
        }

        for (TestSuiteExecutionResultDto suite
                : run.getSuiteResults()) {

            appendSuiteRows(
                    csv,
                    report,
                    suite
            );
        }

        return csv.toString();
    }

    private void appendSuiteRows(
            StringBuilder csv,
            TestReportDto report,
            TestSuiteExecutionResultDto suite) {

        if (suite.getTestResults() == null
                || suite.getTestResults().isEmpty()) {

            appendRow(
                    csv,
                    report.getReportId(),
                    report.getTestRunResult().getRunId(),
                    report.getTestRunResult().getEnvironment(),
                    report.getTestRunResult().getExecutionMode(),
                    suite.getSuiteId(),
                    suite.getSuiteName(),
                    "",
                    "",
                    suiteStatus(suite),
                    suite.isExecuted(),
                    suite.getMessage(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );

            return;
        }

        for (TestCaseExecutor.TestCaseExecutionResult testCase
                : suite.getTestResults()) {

            appendTestCaseRows(
                    csv,
                    report,
                    suite,
                    testCase
            );
        }
    }

    private void appendTestCaseRows(
            StringBuilder csv,
            TestReportDto report,
            TestSuiteExecutionResultDto suite,
            TestCaseExecutor.TestCaseExecutionResult testCase) {

        String reportId =
                report.getReportId();

        String runId =
                report.getTestRunResult().getRunId();

        String environment =
                report.getTestRunResult().getEnvironment();

        String executionMode =
                report.getTestRunResult().getExecutionMode();

        String httpStatus = "";

        String responseTime = "";

        if (testCase.getResponse() != null) {

            httpStatus =
                    String.valueOf(
                            testCase.getResponse()
                                    .getStatusCode()
                    );

            responseTime =
                    String.valueOf(
                            testCase.getResponse()
                                    .getResponseTimeMs()
                    );
        }

        if (testCase.getValidationSummary() == null
                || testCase.getValidationSummary()
                .getResults()
                .isEmpty()) {

            appendRow(
                    csv,
                    reportId,
                    runId,
                    environment,
                    executionMode,
                    suite.getSuiteId(),
                    suite.getSuiteName(),
                    testCase.getTestCaseId(),
                    testCase.getTestCaseName(),
                    testCaseStatus(testCase),
                    testCase.isExecuted(),
                    testCase.getMessage(),
                    httpStatus,
                    responseTime,
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );

            return;
        }

        for (ValidationResultDto validation
                : testCase.getValidationSummary()
                .getResults()) {

            appendRow(
                    csv,
                    reportId,
                    runId,
                    environment,
                    executionMode,
                    suite.getSuiteId(),
                    suite.getSuiteName(),
                    testCase.getTestCaseId(),
                    testCase.getTestCaseName(),
                    testCaseStatus(testCase),
                    testCase.isExecuted(),
                    testCase.getMessage(),
                    httpStatus,
                    responseTime,
                    validation.getValidationType(),
                    validation.getField(),
                    validation.getExpected(),
                    validation.getActual(),
                    validation.isPassed()
                            ? "PASSED"
                            : "FAILED",
                    validation.getMessage()
            );
        }
    }

    private void appendRow(
            StringBuilder csv,
            String reportId,
            String runId,
            String environment,
            String executionMode,
            String suiteId,
            String suiteName,
            String testCaseId,
            String testCaseName,
            String status,
            boolean executed,
            String message,
            String httpStatus,
            String responseTime,
            String validationType,
            String validationField,
            String expected,
            String actual,
            String validationStatus,
            String validationMessage) {

        csv.append(csvValue(reportId)).append(",");
        csv.append(csvValue(runId)).append(",");
        csv.append(csvValue(environment)).append(",");
        csv.append(csvValue(executionMode)).append(",");
        csv.append(csvValue(suiteId)).append(",");
        csv.append(csvValue(suiteName)).append(",");
        csv.append(csvValue(testCaseId)).append(",");
        csv.append(csvValue(testCaseName)).append(",");
        csv.append(csvValue(status)).append(",");
        csv.append(executed).append(",");
        csv.append(csvValue(message)).append(",");
        csv.append(csvValue(httpStatus)).append(",");
        csv.append(csvValue(responseTime)).append(",");
        csv.append(csvValue(validationType)).append(",");
        csv.append(csvValue(validationField)).append(",");
        csv.append(csvValue(expected)).append(",");
        csv.append(csvValue(actual)).append(",");
        csv.append(csvValue(validationStatus)).append(",");
        csv.append(csvValue(validationMessage)).append("\n");
    }

    private String testCaseStatus(
            TestCaseExecutor.TestCaseExecutionResult testCase) {

        if (!testCase.isExecuted()) {
            return "SKIPPED";
        }

        return testCase.isPassed()
                ? "PASSED"
                : "FAILED";
    }

    private String suiteStatus(
            TestSuiteExecutionResultDto suite) {

        if (!suite.isExecuted()) {
            return "SKIPPED";
        }

        return suite.isPassed()
                ? "PASSED"
                : "FAILED";
    }

    private String csvValue(String value) {

        if (value == null) {
            return "";
        }

        String escaped =
                value.replace("\"", "\"\"");

        return "\"" + escaped + "\"";
    }
}