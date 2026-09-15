package org.ai.testing.regression;

import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase1RegressionTest {

    private static final Path REPORT_DIRECTORY = Paths.get("reports");

    @Test
    void shouldExecuteCoreApiRegressionFlowAndGenerateAllReports() {
        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("REG-P1-001");
        testCase.setTestCaseName("Phase 1 Core GET Regression");
        testCase.setMethod("GET");
        testCase.setEnabled(true);

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://petstore3.swagger.io/api/v3/openapi.json");
        request.getHeaders().put("Accept", "application/json");
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);

        AssertionDto status = new AssertionDto();
        status.setType(AssertionType.STATUS_CODE);
        status.setField("statusCode");
        status.setOperator(AssertionOperator.EQUALS);
        status.setExpectedValue("200");
        testCase.setAssertions(List.of(status));

        TestSuiteDto suite = new TestSuiteDto();
        suite.setSuiteId("REG-P1-SUITE");
        suite.setSuiteName("Phase 1 Core Regression");
        suite.setEnabled(true);
        suite.setTestCases(List.of(testCase));

        TestRunDto run = new TestRunDto();
        run.setRunId("REG-P1-RUN");
        run.setRunName("Phase 1 Core Regression");
        run.setEnvironment("REGRESSION");
        run.setExecutionMode("SEQUENTIAL");
        run.setTestSuites(List.of(suite));

        TestRunResultDto result = new TestRunExecutor().execute(run);

        assertTrue(result.isPassed(), result.getMessage());
        assertEquals(1, result.getTotalSuites());
        assertEquals(1, result.getPassedSuites());
        assertEquals(1, result.getTotalTestCases());
        assertEquals(1, result.getPassedTestCases());
        assertEquals(0, result.getFailedTestCases());

        TestReportDto report = new ReportService().generateAllReports(result);
        assertNotNull(report);
        assertEquals("HTML", report.getReportFormat());
        assertNotNull(report.getAiSummary());
        assertNotNull(report.getAiSeverity());

        assertGeneratedReport("test-report.html");
        assertGeneratedReport("test-report.json");
        assertGeneratedReport("test-report.csv");
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
