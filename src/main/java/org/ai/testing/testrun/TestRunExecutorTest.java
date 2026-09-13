package org.ai.testing.testrun;


import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testrun.executor.TestRunExecutor;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;
import org.ai.testing.testrun.dto.TestRunDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class TestRunExecutorTest {

    @Test
    void executeTestRun() {

        // =============================================
        // 1. Create request
        // =============================================

        BaseRequestDto request =
                new BaseRequestDto();

        request.setUrl(
                "https://jsonplaceholder.typicode.com/posts/1"
        );

        // =============================================
        // 2. Create status code assertion
        // =============================================

        AssertionDto statusAssertion =
                new AssertionDto();

        statusAssertion.setType(
                AssertionType.STATUS_CODE
        );

        statusAssertion.setField(
                "statusCode"
        );

        statusAssertion.setOperator(
                AssertionOperator.EQUALS
        );

        statusAssertion.setExpectedValue(
                "200"
        );

        // =============================================
        // 3. Create response body assertion
        // =============================================

        AssertionDto bodyAssertion =
                new AssertionDto();

        bodyAssertion.setType(
                AssertionType.RESPONSE_BODY
        );

        bodyAssertion.setField(
                "body"
        );

        bodyAssertion.setOperator(
                AssertionOperator.CONTAINS
        );

        bodyAssertion.setExpectedValue(
                "userId"
        );

        // =============================================
        // 4. Create test case
        // =============================================

        TestCaseDto testCase =
                new TestCaseDto();

        testCase.setTestCaseId(
                "TC-001"
        );

        testCase.setTestCaseName(
                "Get Post By ID"
        );

        testCase.setDescription(
                "Validate GET post API"
        );

        testCase.setMethod(
                "GET"
        );

        testCase.setRequest(
                request
        );

        testCase.setExpectedStatusCode(
                200
        );

        testCase.setAssertions(
                List.of(
                        statusAssertion,
                        bodyAssertion
                )
        );

        testCase.setEnabled(true);

        // =============================================
        // 5. Create test suite
        // =============================================

        TestSuiteDto testSuite =
                new TestSuiteDto();

        testSuite.setSuiteId(
                "SUITE-001"
        );

        testSuite.setSuiteName(
                "Post API Tests"
        );

        testSuite.setDescription(
                "Post API test suite"
        );

        testSuite.setEnabled(true);

        testSuite.setTestCases(
                List.of(testCase)
        );

        // =============================================
        // 6. Create test run
        // =============================================

        TestRunDto testRun =
                new TestRunDto();

        testRun.setRunId(
                "RUN-001"
        );

        testRun.setRunName(
                "Post API Regression Run"
        );

        testRun.setEnvironment(
                "QA"
        );

        testRun.setExecutionMode(
                "SEQUENTIAL"
        );

        testRun.setTestSuites(
                List.of(testSuite)
        );

        // =============================================
        // 7. Execute test run
        // =============================================

        TestRunExecutor executor =
                new TestRunExecutor();

        TestRunResultDto result =
                executor.execute(testRun);

        // =============================================
        // 8. Print result
        // =============================================

        System.out.println();
        System.out.println(
                "=========================================="
        );
        System.out.println(
                "           TEST RUN RESULT"
        );
        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Run ID        : "
                        + result.getRunId()
        );

        System.out.println(
                "Run Name      : "
                        + result.getRunName()
        );

        System.out.println(
                "Environment   : "
                        + result.getEnvironment()
        );

        System.out.println(
                "Executed      : "
                        + result.isExecuted()
        );

        System.out.println(
                "Passed        : "
                        + result.isPassed()
        );

        System.out.println(
                "Total Suites  : "
                        + result.getTotalSuites()
        );

        System.out.println(
                "Passed Suites : "
                        + result.getPassedSuites()
        );

        System.out.println(
                "Failed Suites : "
                        + result.getFailedSuites()
        );

        System.out.println(
                "Skipped Suites: "
                        + result.getSkippedSuites()
        );

        System.out.println(
                "Total Cases   : "
                        + result.getTotalTestCases()
        );

        System.out.println(
                "Passed Cases  : "
                        + result.getPassedTestCases()
        );

        System.out.println(
                "Failed Cases  : "
                        + result.getFailedTestCases()
        );

        System.out.println(
                "Skipped Cases : "
                        + result.getSkippedTestCases()
        );

        System.out.println(
                "Execution Time: "
                        + result.getExecutionTimeMs()
                        + " ms"
        );

        System.out.println(
                "Message       : "
                        + result.getMessage()
        );

        System.out.println(
                "=========================================="
        );

        // =============================================
        // 9. Assertions
        // =============================================

        assertNotNull(result);

        assertEquals(
                "RUN-001",
                result.getRunId()
        );

        assertEquals(
                "Post API Regression Run",
                result.getRunName()
        );

        assertEquals(
                "QA",
                result.getEnvironment()
        );

        assertTrue(
                result.isExecuted()
        );

        assertEquals(
                1,
                result.getTotalSuites()
        );

        assertEquals(
                1,
                result.getPassedSuites()
        );

        assertEquals(
                0,
                result.getFailedSuites()
        );

        assertEquals(
                0,
                result.getSkippedSuites()
        );

        assertEquals(
                1,
                result.getTotalTestCases()
        );

        assertEquals(
                1,
                result.getPassedTestCases()
        );

        assertEquals(
                0,
                result.getFailedTestCases()
        );

        assertEquals(
                0,
                result.getSkippedTestCases()
        );

        assertTrue(
                result.isPassed()
        );

        assertNotNull(
                result.getStartTime()
        );

        assertNotNull(
                result.getEndTime()
        );

        assertTrue(
                result.getExecutionTimeMs() >= 0
        );
    }
}