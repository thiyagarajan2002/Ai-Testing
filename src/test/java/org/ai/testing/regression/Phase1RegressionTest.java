package org.ai.testing.regression;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testrun.executor.TestRunExecutor;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase1RegressionTest {

    private static final Path REPORT_DIRECTORY = Paths.get("reports");

    @Test
    void shouldExecuteAllHttpMethodsAndGenerateAllReports() throws IOException {
        HttpServer server = createLocalServer();
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            TestRunDto run = buildRegressionRun(baseUrl);

            TestRunResultDto result = new TestRunExecutor().execute(run);

            assertTrue(result.isPassed(), result.getMessage());
            assertEquals(1, result.getTotalSuites());
            assertEquals(1, result.getPassedSuites());
            assertEquals(5, result.getTotalTestCases());
            assertEquals(5, result.getPassedTestCases());
            assertEquals(0, result.getFailedTestCases());
            assertEquals(0, result.getSkippedTestCases());

            TestReportDto report = new ReportService().generateAllReports(result);
            assertNotNull(report);
            assertEquals("HTML", report.getReportFormat());
            assertNotNull(report.getAiSummary());
            assertNotNull(report.getAiSeverity());

            assertGeneratedReport("test-report.html");
            assertGeneratedReport("test-report.json");
            assertGeneratedReport("test-report.csv");
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer createLocalServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/get", exchange -> respond(exchange, "GET"));
        server.createContext("/api/post", exchange -> respond(exchange, "POST"));
        server.createContext("/api/put", exchange -> respond(exchange, "PUT"));
        server.createContext("/api/patch", exchange -> respond(exchange, "PATCH"));
        server.createContext("/api/delete", exchange -> respond(exchange, "DELETE"));
        return server;
    }

    private static void respond(HttpExchange exchange, String expectedMethod) throws IOException {
        try (exchange) {
            String response = "{\"method\":\"" + expectedMethod + "\",\"status\":\"ok\"}";
            byte[] body = response.getBytes(StandardCharsets.UTF_8);

            if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("X-Test-Server", "phase1-regression");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private static TestRunDto buildRegressionRun(String baseUrl) {
        List<TestCaseDto> testCases = new ArrayList<>();
        testCases.add(buildGetTestCase(baseUrl));
        testCases.add(buildPostTestCase(baseUrl));
        testCases.add(buildPutTestCase(baseUrl));
        testCases.add(buildPatchTestCase(baseUrl));
        testCases.add(buildDeleteTestCase(baseUrl));

        TestSuiteDto suite = new TestSuiteDto();
        suite.setSuiteId("REG-P1-SUITE");
        suite.setSuiteName("Phase 1 HTTP Method Regression");
        suite.setDescription(
                "Regression coverage for GET, POST, PUT, PATCH and DELETE executors using a deterministic local HTTP server");
        suite.setEnabled(true);
        suite.setTestCases(testCases);

        TestRunDto run = new TestRunDto();
        run.setRunId("REG-P1-RUN");
        run.setRunName("Phase 1 HTTP Method Regression");
        run.setEnvironment("REGRESSION");
        run.setExecutionMode("SEQUENTIAL");
        run.setTestSuites(List.of(suite));
        return run;
    }

    private static TestCaseDto buildGetTestCase(String baseUrl) {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-GET",
                "Phase 1 GET Executor Regression",
                "GET",
                baseUrl + "/api/get");
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto buildPostTestCase(String baseUrl) {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-POST",
                "Phase 1 POST Executor Regression",
                "POST",
                baseUrl + "/api/post");
        testCase.getRequest().setBody(jsonBody(
                "{\"id\":1,\"name\":\"phase1-regression-post\"}"));
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto buildPutTestCase(String baseUrl) {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-PUT",
                "Phase 1 PUT Executor Regression",
                "PUT",
                baseUrl + "/api/put");
        testCase.getRequest().setBody(jsonBody(
                "{\"id\":1,\"name\":\"phase1-regression-put\"}"));
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto buildPatchTestCase(String baseUrl) {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-PATCH",
                "Phase 1 PATCH Executor Regression",
                "PATCH",
                baseUrl + "/api/patch");
        testCase.getRequest().setBody(jsonBody(
                "{\"name\":\"phase1-regression-patch\"}"));
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto buildDeleteTestCase(String baseUrl) {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-DELETE",
                "Phase 1 DELETE Executor Regression",
                "DELETE",
                baseUrl + "/api/delete");
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto baseTestCase(
            String id,
            String name,
            String method,
            String url) {
        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId(id);
        testCase.setTestCaseName(name);
        testCase.setMethod(method);
        testCase.setEnabled(true);

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(url);
        request.getHeaders().put("Content-Type", "application/json");
        request.getHeaders().put("Accept", "application/json");
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);
        return testCase;
    }

    private static AssertionDto statusAssertion(int expectedStatus) {
        AssertionDto assertion = new AssertionDto();
        assertion.setType(AssertionType.STATUS_CODE);
        assertion.setField("statusCode");
        assertion.setOperator(AssertionOperator.EQUALS);
        assertion.setExpectedValue(String.valueOf(expectedStatus));
        return assertion;
    }

    private static AssertionDto bodyNotEmptyAssertion() {
        AssertionDto assertion = new AssertionDto();
        assertion.setType(AssertionType.RESPONSE_BODY);
        assertion.setField("responseBody");
        assertion.setOperator(AssertionOperator.NOT_EMPTY);
        assertion.setExpectedValue("");
        return assertion;
    }

    private static RequestBodyDto jsonBody(String rawBody) {
        RequestBodyDto body = new RequestBodyDto();
        body.setContentType("application/json");
        body.setRawBody(rawBody);
        return body;
    }

    private static void assertGeneratedReport(String fileName) {
        Path report = REPORT_DIRECTORY.resolve(fileName);
        assertTrue(Files.isRegularFile(report),
                "Expected report was not generated: " + report.toAbsolutePath());
        try {
            assertTrue(Files.size(report) > 0,
                    "Generated report is empty: " + report.toAbsolutePath());
        } catch (java.io.IOException e) {
            fail("Unable to inspect report: " + report.toAbsolutePath(), e);
        }
    }
}
