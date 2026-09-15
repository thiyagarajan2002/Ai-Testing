package org.ai.testing.testrun;

import com.sun.net.httpserver.HttpServer;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.ReportGenerator;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testrun.executor.TestRunExecutor;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestRunExecutorTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/users/1", exchange -> {
            byte[] body = "{\"userId\":1,\"id\":1}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldExecuteTestRunAndGenerateAllReports() {
        CapturingReportGenerator html = new CapturingReportGenerator();
        CapturingReportGenerator json = new CapturingReportGenerator();
        CapturingReportGenerator csv = new CapturingReportGenerator();
        ReportService reportService = new ReportService(html, json, csv);

        TestRunDto testRun = createTestRun();
        TestRunResultDto result = new TestRunExecutor(
                new org.ai.testing.testsuite.executor.TestSuiteExecutor(),
                reportService
        ).execute(testRun);

        assertNotNull(result);
        assertEquals("RUN-001", result.getRunId());
        assertTrue(result.isExecuted());
        assertTrue(result.isPassed());
        assertEquals(1, result.getTotalSuites());
        assertEquals(1, result.getPassedSuites());
        assertEquals(0, result.getFailedSuites());
        assertEquals(0, result.getSkippedSuites());
        assertEquals(1, result.getTotalTestCases());
        assertEquals(1, result.getPassedTestCases());
        assertEquals(0, result.getFailedTestCases());
        assertEquals(0, result.getSkippedTestCases());

        var testResult = result.getSuiteResults().get(0).getTestResults().get(0);
        assertEquals(200, testResult.getResponse().getStatusCode());
        assertNotNull(testResult.getRequest());
        assertEquals("Bearer test-token", testResult.getRequest().getHeaders().get("Authorization"));
        assertEquals("application/json", testResult.getResponse().getHeaders().get("content-type"));

        assertNotNull(html.report);
        assertNotNull(json.report);
        assertNotNull(csv.report);
        assertEquals("HTML", html.report.getReportFormat());
        assertEquals("JSON", json.report.getReportFormat());
        assertEquals("CSV", csv.report.getReportFormat());
        assertEquals(1, html.count);
        assertEquals(1, json.count);
        assertEquals(1, csv.count);
    }

    private TestRunDto createTestRun() {
        int port = server.getAddress().getPort();

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("http://localhost:" + port + "/users/1");
        request.getHeaders().put("Authorization", "Bearer test-token");

        AssertionDto bodyAssertion = new AssertionDto();
        bodyAssertion.setType(AssertionType.RESPONSE_BODY);
        bodyAssertion.setField("body");
        bodyAssertion.setOperator(AssertionOperator.CONTAINS);
        bodyAssertion.setExpectedValue("userId");

        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("TC-001");
        testCase.setTestCaseName("Get User");
        testCase.setMethod("GET");
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);
        testCase.setAssertions(List.of(bodyAssertion));

        TestSuiteDto suite = new TestSuiteDto();
        suite.setSuiteId("SUITE-001");
        suite.setSuiteName("User API Tests");
        suite.setEnabled(true);
        suite.setTestCases(List.of(testCase));

        TestRunDto run = new TestRunDto();
        run.setRunId("RUN-001");
        run.setRunName("User API Regression Run");
        run.setEnvironment("QA");
        run.setExecutionMode("SEQUENTIAL");
        run.setTestSuites(List.of(suite));
        return run;
    }

    private static class CapturingReportGenerator implements ReportGenerator {
        private TestReportDto report;
        private int count;

        @Override
        public void generate(TestReportDto report) {
            this.report = report;
            this.count++;
        }
    }
}
