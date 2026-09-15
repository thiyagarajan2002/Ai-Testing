package org.ai.testing.regression;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase1RegressionTest {

    private static final String PETSTORE_BASE_URL =
            "https://petstore3.swagger.io/api/v3";

    private static final Path REPORT_DIRECTORY = Paths.get("reports");

    @Test
    void shouldExecuteAllHttpMethodsAndGenerateAllReports() {
        TestRunDto run = buildRegressionRun();

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
    }

    private static TestRunDto buildRegressionRun() {
        List<TestCaseDto> testCases = new ArrayList<>();
        testCases.add(buildGetTestCase());
        testCases.add(buildPostTestCase());
        testCases.add(buildPutTestCase());
        testCases.add(buildPatchTestCase());
        testCases.add(buildDeleteTestCase());

        TestSuiteDto suite = new TestSuiteDto();
        suite.setSuiteId("REG-P1-SUITE");
        suite.setSuiteName("Phase 1 HTTP Method Regression");
        suite.setDescription(
                "Regression coverage for GET, POST, PUT, PATCH and DELETE executors");
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

    private static TestCaseDto buildGetTestCase() {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-GET",
                "Phase 1 GET Executor Regression",
                "GET",
                PETSTORE_BASE_URL + "/openapi.json");
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto buildPostTestCase() {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-POST",
                "Phase 1 POST Executor Regression",
                "POST",
                PETSTORE_BASE_URL + "/pet");
        testCase.getRequest().setBody(jsonBody(
                "{\"id\":0,\"name\":\"phase1-regression-pet\",\"photoUrls\":[]}"));
        testCase.getRequest().getHeaders().put("Accept", "application/json");
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto buildPutTestCase() {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-PUT",
                "Phase 1 PUT Executor Regression",
                "PUT",
                PETSTORE_BASE_URL + "/pet");
        testCase.getRequest().setBody(jsonBody(
                "{\"id\":0,\"name\":\"phase1-regression-pet-put\",\"photoUrls\":[]}"));
        testCase.getRequest().getHeaders().put("Accept", "application/json");
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto buildPatchTestCase() {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-PATCH",
                "Phase 1 PATCH Executor Regression",
                "PATCH",
                PETSTORE_BASE_URL + "/pet/1");
        testCase.getRequest().setBody(jsonBody(
                "{\"name\":\"phase1-regression-patch\"}"));
        testCase.getRequest().getHeaders().put("Accept", "application/json");
        testCase.setAssertions(List.of(statusAssertion(200), bodyNotEmptyAssertion()));
        return testCase;
    }

    private static TestCaseDto buildDeleteTestCase() {
        TestCaseDto testCase = baseTestCase(
                "REG-P1-DELETE",
                "Phase 1 DELETE Executor Regression",
                "DELETE",
                PETSTORE_BASE_URL + "/pet/1");
        testCase.getRequest().getHeaders().put("Accept", "application/json");
        testCase.setAssertions(List.of(statusAssertion(200)));
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
