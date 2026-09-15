package org.ai.testing.regression;

import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testrun.executor.TestRunExecutor;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase1RegressionTest {

    @Test
    void shouldExecuteCoreApiRegressionFlow() {
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
    }
}
