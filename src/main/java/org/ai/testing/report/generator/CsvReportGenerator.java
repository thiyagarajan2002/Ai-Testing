package org.ai.testing.report.generator;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.util.Strings;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One row per assertion, so the file pivots cleanly in a spreadsheet.
 *
 * <p>A UTF-8 byte-order mark is emitted because Excel otherwise renders
 * non-ASCII response bodies as mojibake, and every field is quoted with doubled
 * inner quotes so multi-line JSON bodies cannot break the row structure.</p>
 */
public class CsvReportGenerator extends AbstractFileReportGenerator {

    private static final String BOM = "\uFEFF";

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] COLUMNS = {
            "Report ID", "Generated At", "Run ID", "Run Name", "Environment", "Execution Mode",
            "Suite ID", "Suite Name", "Suite Status",
            "Test Case ID", "Test Case Name", "Method", "Tags", "Test Status", "Executed",
            "Message", "Error Type",
            "Request URL", "Request Headers", "Query Params", "Path Params",
            "Request Content Type", "Request Body",
            "HTTP Status", "Status Message", "Response Time (ms)", "Response Size (bytes)",
            "Attempts", "Response Headers", "Response Body",
            "Assertion Type", "Assertion Field", "Assertion Operator",
            "Expected", "Actual", "Assertion Status", "Assertion Message"
    };

    public CsvReportGenerator() {
        this(Paths.get("reports", "test-report.csv"));
    }

    public CsvReportGenerator(Path outputPath) {
        super(outputPath, "CSV");
    }

    @Override
    protected String render(TestReportDto report) {
        StringBuilder csv = new StringBuilder(BOM);
        writeRow(csv, COLUMNS);

        TestRunResultDto run = report.getTestRunResult();
        for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) {
            if (suite == null) {
                continue;
            }
            if (suite.getTestResults().isEmpty()) {
                writeRow(csv, row(report, run, suite, null, null));
                continue;
            }
            for (TestCaseResultDto testCase : suite.getTestResults()) {
                if (testCase == null) {
                    continue;
                }
                List<ValidationResultDto> validations =
                        testCase.getValidationSummary().getResults();
                if (validations.isEmpty()) {
                    writeRow(csv, row(report, run, suite, testCase, null));
                    continue;
                }
                for (ValidationResultDto validation : validations) {
                    writeRow(csv, row(report, run, suite, testCase, validation));
                }
            }
        }
        return csv.toString();
    }

    private String[] row(TestReportDto report, TestRunResultDto run,
                         TestSuiteExecutionResultDto suite,
                         TestCaseResultDto testCase,
                         ValidationResultDto validation) {

        BaseRequestDto request = testCase == null ? null : testCase.getRequest();
        ResponseDto response = testCase == null ? null : testCase.getResponse();

        List<String> values = new ArrayList<>();
        values.add(report.getReportId());
        values.add(report.getGeneratedAt() == null ? "" : TIMESTAMP.format(report.getGeneratedAt()));
        values.add(run.getRunId());
        values.add(run.getRunName());
        values.add(run.getEnvironment());
        values.add(run.getExecutionMode());

        values.add(suite.getSuiteId());
        values.add(suite.getSuiteName());
        values.add(suite.getStatus().name());

        values.add(testCase == null ? "" : testCase.getTestCaseId());
        values.add(testCase == null ? "" : testCase.getTestCaseName());
        values.add(testCase == null ? "" : testCase.getMethod());
        values.add(testCase == null ? "" : String.join(" ", testCase.getTags()));
        values.add(testCase == null ? suite.getStatus().name() : testCase.getStatus().name());
        values.add(testCase == null
                ? String.valueOf(suite.isExecuted())
                : String.valueOf(testCase.isExecuted()));
        values.add(testCase == null ? suite.getMessage() : testCase.getMessage());
        values.add(testCase == null ? "" : testCase.getErrorType());

        values.add(request == null ? "" : request.getUrl());
        values.add(request == null ? "" : formatMap(request.getHeaders()));
        values.add(request == null ? "" : formatMap(request.getQueryParams()));
        values.add(request == null ? "" : formatMap(request.getPathParams()));
        values.add(request == null || request.getBody() == null
                ? "" : request.getBody().getContentType());
        values.add(request == null || request.getBody() == null
                ? "" : request.getBody().getRawBody());

        values.add(response == null ? "" : String.valueOf(response.getStatusCode()));
        values.add(response == null ? "" : response.getStatusMessage());
        values.add(response == null ? "" : String.valueOf(response.getResponseTimeMs()));
        values.add(response == null ? "" : String.valueOf(response.getBodySizeBytes()));
        values.add(response == null ? "" : String.valueOf(response.getAttempts()));
        values.add(response == null ? "" : formatMap(response.getHeaders()));
        values.add(response == null ? "" : response.getBody());

        values.add(validation == null ? "" : validation.getValidationType());
        values.add(validation == null ? "" : validation.getField());
        values.add(validation == null ? "" : validation.getOperator());
        values.add(validation == null ? "" : validation.getExpected());
        values.add(validation == null ? "" : validation.getActual());
        values.add(validation == null ? "" : (validation.isPassed() ? "PASSED" : "FAILED"));
        values.add(validation == null ? "" : validation.getMessage());

        return values.toArray(new String[0]);
    }

    private void writeRow(StringBuilder csv, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvValue(values[i]));
        }
        csv.append("\r\n");
    }

    private String formatMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + Strings.nullToEmpty(entry.getValue()))
                .collect(Collectors.joining("; "));
    }

    private String csvValue(String value) {
        String normalised = Strings.nullToEmpty(value)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\"", "\"\"");
        return "\"" + normalised + "\"";
    }
}
