package org.ai.testing.testsuite.executor;


import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;

public class TestSuiteExecutor {

    private final TestCaseExecutor testCaseExecutor;

    public TestSuiteExecutor() {
        this.testCaseExecutor =
                new TestCaseExecutor();
    }

    public TestSuiteExecutionResultDto execute(
            TestSuiteDto testSuite) {

        if (testSuite == null) {
            throw new IllegalArgumentException(
                    "Test suite cannot be null"
            );
        }

        TestSuiteExecutionResultDto suiteResult =
                new TestSuiteExecutionResultDto();

        suiteResult.setSuiteId(
                testSuite.getSuiteId()
        );

        suiteResult.setSuiteName(
                testSuite.getSuiteName()
        );

        if (!testSuite.isEnabled()) {

            suiteResult.setExecuted(false);
            suiteResult.setPassed(false);
            suiteResult.setSkippedTestCases(
                    testSuite.getTestCases() == null
                            ? 0
                            : testSuite.getTestCases().size()
            );

            suiteResult.setMessage(
                    "Test suite is disabled"
            );

            return suiteResult;
        }

        long startTime =
                System.currentTimeMillis();

        suiteResult.setExecuted(true);

        if (testSuite.getTestCases() == null
                || testSuite.getTestCases().isEmpty()) {

            suiteResult.setPassed(false);
            suiteResult.setMessage(
                    "Test suite contains no test cases"
            );
            suiteResult.setExecutionTimeMs(
                    System.currentTimeMillis() - startTime
            );

            return suiteResult;
        }

        for (TestCaseDto testCase :
                testSuite.getTestCases()) {

            if (testCase == null) {
                continue;
            }

            TestCaseExecutor.TestCaseExecutionResult
                    testResult =
                    testCaseExecutor.execute(testCase);

            suiteResult.getTestResults()
                    .add(testResult);

            if (!testResult.isExecuted()) {

                suiteResult.setSkippedTestCases(
                        suiteResult.getSkippedTestCases() + 1
                );

            } else if (testResult.isPassed()) {

                suiteResult.setPassedTestCases(
                        suiteResult.getPassedTestCases() + 1
                );

            } else {

                suiteResult.setFailedTestCases(
                        suiteResult.getFailedTestCases() + 1
                );
            }
        }

        suiteResult.setTotalTestCases(
                suiteResult.getPassedTestCases()
                        + suiteResult.getFailedTestCases()
                        + suiteResult.getSkippedTestCases()
        );

        suiteResult.setPassed(
                suiteResult.getFailedTestCases() == 0
                        && suiteResult.getSkippedTestCases() == 0
                        && suiteResult.getTotalTestCases() > 0
        );

        suiteResult.setExecutionTimeMs(
                System.currentTimeMillis() - startTime
        );

        suiteResult.setMessage(
                buildSummaryMessage(suiteResult)
        );

        return suiteResult;
    }

    private String buildSummaryMessage(
            TestSuiteExecutionResultDto result) {

        if (result.isPassed()) {
            return "Test suite passed. "
                    + result.getPassedTestCases()
                    + " test case(s) passed.";
        }

        return "Test suite failed. "
                + result.getPassedTestCases()
                + " passed, "
                + result.getFailedTestCases()
                + " failed, "
                + result.getSkippedTestCases()
                + " skipped.";
    }
}