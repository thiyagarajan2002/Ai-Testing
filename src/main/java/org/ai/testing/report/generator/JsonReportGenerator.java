package org.ai.testing.report.generator;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.json.Json;
import org.ai.testing.json.JsonValue;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Serialises the full report to JSON.
 *
 * <p>Written by hand against the project's own JSON writer rather than a
 * data-binding library. That removes the Jackson dependency altogether, and
 * with it the {@code LocalDateTime} serialisation failure that previously
 * required a separate JSR-310 module to be kept in version lockstep.</p>
 */
public class JsonReportGenerator extends AbstractFileReportGenerator {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public JsonReportGenerator() {
        this(Paths.get("reports", "test-report.json"));
    }

    public JsonReportGenerator(Path outputPath) {
        super(outputPath, "JSON");
    }

    @Override
    protected String render(TestReportDto report) {
        JsonValue root = JsonValue.object()
                .put("reportId", report.getReportId())
                .put("reportName", report.getReportName())
                .put("reportFormat", report.getReportFormat())
                .put("generatedAt", time(report.getGeneratedAt()))
                .put("generatedBy", report.getGeneratedBy())
                .put("toolVersion", report.getToolVersion());

        if (report.getSummary() != null) {
            root.put("summary", report.getSummary());
        }
        if (report.getSeverity() != null) {
            root.put("severity", report.getSeverity());
        }
        if (!report.getFindings().isEmpty()) {
            root.put("findings", strings(report.getFindings()));
        }
        if (!report.getRecommendations().isEmpty()) {
            root.put("recommendations", strings(report.getRecommendations()));
        }

        root.put("testRunResult", run(report.getTestRunResult()));
        return Json.write(root, true);
    }

    private JsonValue run(TestRunResultDto run) {
        JsonValue node = JsonValue.object()
                .put("runId", run.getRunId())
                .put("runName", run.getRunName())
                .put("environment", run.getEnvironment())
                .put("executionMode", run.getExecutionMode())
                .put("description", run.getDescription())
                .put("status", run.getStatus().name())
                .put("passed", JsonValue.of(run.isPassed()))
                .put("message", run.getMessage())
                .put("startTime", time(run.getStartTime()))
                .put("endTime", time(run.getEndTime()))
                .put("executionTimeMs", JsonValue.of(run.getExecutionTimeMs()));

        node.put("suiteCounts", JsonValue.object()
                .put("total", JsonValue.of(run.getTotalSuites()))
                .put("passed", JsonValue.of(run.getPassedSuites()))
                .put("failed", JsonValue.of(run.getFailedSuites()))
                .put("errored", JsonValue.of(run.getErroredSuites()))
                .put("skipped", JsonValue.of(run.getSkippedSuites())));

        node.put("testCaseCounts", JsonValue.object()
                .put("total", JsonValue.of(run.getTotalTestCases()))
                .put("passed", JsonValue.of(run.getPassedTestCases()))
                .put("failed", JsonValue.of(run.getFailedTestCases()))
                .put("errored", JsonValue.of(run.getErroredTestCases()))
                .put("skipped", JsonValue.of(run.getSkippedTestCases())));

        node.put("metrics", metrics(run.getMetrics()));
        node.put("variables", stringMap(run.getVariables()));
        node.put("warnings", strings(run.getWarnings()));

        JsonValue suites = JsonValue.array();
        for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) {
            suites.add(suite(suite));
        }
        node.put("suiteResults", suites);
        return node;
    }

    private JsonValue metrics(TestRunResultDto.Metrics metrics) {
        return JsonValue.object()
                .put("minResponseTimeMs", JsonValue.of(metrics.getMinResponseTimeMs()))
                .put("maxResponseTimeMs", JsonValue.of(metrics.getMaxResponseTimeMs()))
                .put("averageResponseTimeMs", JsonValue.of(metrics.getAverageResponseTimeMs()))
                .put("medianResponseTimeMs", JsonValue.of(metrics.getMedianResponseTimeMs()))
                .put("p90ResponseTimeMs", JsonValue.of(metrics.getP90ResponseTimeMs()))
                .put("p95ResponseTimeMs", JsonValue.of(metrics.getP95ResponseTimeMs()))
                .put("totalBytes", JsonValue.of(metrics.getTotalBytes()))
                .put("passRate", JsonValue.of(round(metrics.getPassRate())))
                .put("requestsPerSecond", JsonValue.of(round(metrics.getRequestsPerSecond())))
                .put("statusFamilies", intMap(metrics.getStatusFamilies()))
                .put("methodCounts", intMap(metrics.getMethodCounts()));
    }

    private JsonValue suite(TestSuiteExecutionResultDto suite) {
        JsonValue node = JsonValue.object()
                .put("suiteId", suite.getSuiteId())
                .put("suiteName", suite.getSuiteName())
                .put("description", suite.getDescription())
                .put("status", suite.getStatus().name())
                .put("passed", JsonValue.of(suite.isPassed()))
                .put("executed", JsonValue.of(suite.isExecuted()))
                .put("message", suite.getMessage())
                .put("startedAt", time(suite.getStartedAt()))
                .put("executionTimeMs", JsonValue.of(suite.getExecutionTimeMs()))
                .put("totalTestCases", JsonValue.of(suite.getTotalTestCases()))
                .put("passedTestCases", JsonValue.of(suite.getPassedTestCases()))
                .put("failedTestCases", JsonValue.of(suite.getFailedTestCases()))
                .put("erroredTestCases", JsonValue.of(suite.getErroredTestCases()))
                .put("skippedTestCases", JsonValue.of(suite.getSkippedTestCases()));

        JsonValue cases = JsonValue.array();
        for (TestCaseResultDto result : suite.getTestResults()) {
            cases.add(testCase(result));
        }
        node.put("testResults", cases);
        return node;
    }

    private JsonValue testCase(TestCaseResultDto result) {
        JsonValue node = JsonValue.object()
                .put("testCaseId", result.getTestCaseId())
                .put("testCaseName", result.getTestCaseName())
                .put("description", result.getDescription())
                .put("method", result.getMethod())
                .put("status", result.getStatus().name())
                .put("passed", JsonValue.of(result.isPassed()))
                .put("executed", JsonValue.of(result.isExecuted()))
                .put("message", result.getMessage())
                .put("errorType", result.getErrorType())
                .put("errorDetail", result.getErrorDetail())
                .put("authApplied", result.getAuthApplied())
                .put("startedAt", time(result.getStartedAt()))
                .put("executionTimeMs", JsonValue.of(result.getExecutionTimeMs()))
                .put("curlCommand", result.getCurlCommand())
                .put("tags", strings(List.copyOf(result.getTags())))
                .put("capturedVariables", stringMap(result.getCapturedVariables()))
                .put("failedExtracts", strings(List.copyOf(result.getFailedExtracts())));

        node.put("request", request(result.getRequest()));
        node.put("response", response(result.getResponse()));
        node.put("validation", validation(result));
        return node;
    }

    private JsonValue request(BaseRequestDto request) {
        if (request == null) {
            return JsonValue.NULL;
        }
        JsonValue node = JsonValue.object()
                .put("url", request.getUrl())
                .put("headers", stringMap(request.getHeaders()))
                .put("queryParams", stringMap(request.getQueryParams()))
                .put("pathParams", stringMap(request.getPathParams()));

        if (request.getBody() == null) {
            node.put("body", JsonValue.NULL);
        } else {
            node.put("body", JsonValue.object()
                    .put("mode", request.getBody().getMode().name())
                    .put("contentType", request.getBody().getContentType())
                    .put("rawBody", request.getBody().getRawBody()));
        }
        return node;
    }

    private JsonValue response(ResponseDto response) {
        if (response == null) {
            return JsonValue.NULL;
        }
        return JsonValue.object()
                .put("statusCode", JsonValue.of(response.getStatusCode()))
                .put("statusMessage", response.getStatusMessage())
                .put("responseTimeMs", JsonValue.of(response.getResponseTimeMs()))
                .put("bodySizeBytes", JsonValue.of(response.getBodySizeBytes()))
                .put("attempts", JsonValue.of(response.getAttempts()))
                .put("finalUrl", response.getFinalUrl())
                .put("headers", stringMap(response.getHeaders()))
                .put("body", response.getBody());
    }

    private JsonValue validation(TestCaseResultDto result) {
        JsonValue node = JsonValue.object()
                .put("passed", JsonValue.of(result.getValidationSummary().isPassed()))
                .put("total", JsonValue.of(result.getValidationSummary().getTotal()))
                .put("passedCount", JsonValue.of(result.getValidationSummary().getPassedCount()))
                .put("failedCount", JsonValue.of(result.getValidationSummary().getFailedCount()));

        JsonValue results = JsonValue.array();
        for (ValidationResultDto validation : result.getValidationSummary().getResults()) {
            results.add(JsonValue.object()
                    .put("passed", JsonValue.of(validation.isPassed()))
                    .put("validationType", validation.getValidationType())
                    .put("field", validation.getField())
                    .put("operator", validation.getOperator())
                    .put("expected", validation.getExpected())
                    .put("actual", validation.getActual())
                    .put("message", validation.getMessage())
                    .put("description", validation.getDescription()));
        }
        node.put("results", results);
        return node;
    }

    // ------------------------------------------------------------------

    private JsonValue strings(List<String> values) {
        JsonValue array = JsonValue.array();
        if (values != null) {
            values.forEach(value -> array.add(JsonValue.of(value)));
        }
        return array;
    }

    private JsonValue stringMap(Map<String, String> values) {
        JsonValue object = JsonValue.object();
        if (values != null) {
            values.forEach(object::put);
        }
        return object;
    }

    private JsonValue intMap(Map<String, Integer> values) {
        JsonValue object = JsonValue.object();
        if (values != null) {
            values.forEach((key, value) -> object.put(key, JsonValue.of(value)));
        }
        return object;
    }

    private String time(LocalDateTime value) {
        return value == null ? null : ISO.format(value);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
