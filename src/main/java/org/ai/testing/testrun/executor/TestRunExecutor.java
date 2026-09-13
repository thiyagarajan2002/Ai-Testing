package org.ai.testing.testrun.executor;

import org.ai.testing.report.service.ReportService;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.testsuite.executor.TestSuiteExecutor;

import java.time.LocalDateTime;


public class TestRunExecutor {

    private final TestSuiteExecutor testSuiteExecutor;
    private final ReportService reportService;

    public TestRunExecutor() {
        this.testSuiteExecutor = new TestSuiteExecutor();
        this.reportService = new ReportService();
    }

    public TestRunExecutor(
            TestSuiteExecutor testSuiteExecutor,
            ReportService reportService) {

        if (testSuiteExecutor == null) {
            throw new IllegalArgumentException(
                    "Test suite executor cannot be null"
            );
        }

        if (reportService == null) {
            throw new IllegalArgumentException(
                    "Report service cannot be null"
            );
        }

        this.testSuiteExecutor = testSuiteExecutor;
        this.reportService = reportService;
    }

    public TestRunResultDto execute(TestRunDto testRun) {

        if (testRun == null) {
            throw new IllegalArgumentException(
                    "Test run cannot be null"
            );
        }

        TestRunResultDto result = new TestRunResultDto();

        result.setRunId(testRun.getRunId());
        result.setRunName(testRun.getRunName());
        result.setEnvironment(testRun.getEnvironment());
        result.setExecutionMode(testRun.getExecutionMode());
        result.setExecuted(true);

        LocalDateTime startTime = LocalDateTime.now();
        long startNanos = System.nanoTime();

        result.setStartTime(startTime);

        if (testRun.getTestSuites() == null
                || testRun.getTestSuites().isEmpty()) {

            result.setEndTime(LocalDateTime.now());

            result.setExecutionTimeMs(
                    elapsedMilliseconds(startNanos)
            );

            result.setPassed(false);

            result.setMessage(
                    "Test run contains no test suites"
            );

            generateReport(result);

            return result;
        }

        for (TestSuiteDto testSuite
                : testRun.getTestSuites()) {

            if (testSuite == null) {
                continue;
            }

            TestSuiteExecutionResultDto suiteResult =
                    testSuiteExecutor.execute(testSuite);

            result.getSuiteResults().add(suiteResult);

            updateSuiteCounts(result, suiteResult);
            updateTestCaseCounts(result, suiteResult);
        }

        result.setTotalSuites(
                result.getPassedSuites()
                        + result.getFailedSuites()
                        + result.getSkippedSuites()
        );

        result.setEndTime(LocalDateTime.now());

        result.setExecutionTimeMs(
                elapsedMilliseconds(startNanos)
        );

        result.setPassed(
                result.getFailedSuites() == 0
                        && result.getSkippedSuites() == 0
                        && result.getTotalSuites() > 0
        );

        result.setMessage(
                buildSummaryMessage(result)
        );

        generateReport(result);

        return result;
    }

    private void updateSuiteCounts(
            TestRunResultDto result,
            TestSuiteExecutionResultDto suiteResult) {

        if (!suiteResult.isExecuted()) {
            result.setSkippedSuites(
                    result.getSkippedSuites() + 1
            );
        } else if (suiteResult.isPassed()) {
            result.setPassedSuites(
                    result.getPassedSuites() + 1
            );
        } else {
            result.setFailedSuites(
                    result.getFailedSuites() + 1
            );
        }
    }

    private void updateTestCaseCounts(
            TestRunResultDto result,
            TestSuiteExecutionResultDto suiteResult) {

        result.setTotalTestCases(
                result.getTotalTestCases()
                        + suiteResult.getTotalTestCases()
        );

        result.setPassedTestCases(
                result.getPassedTestCases()
                        + suiteResult.getPassedTestCases()
        );

        result.setFailedTestCases(
                result.getFailedTestCases()
                        + suiteResult.getFailedTestCases()
        );

        result.setSkippedTestCases(
                result.getSkippedTestCases()
                        + suiteResult.getSkippedTestCases()
        );
    }

    private void generateReport(
            TestRunResultDto result) {

        try {

            reportService.generateAllReports(result);

        } catch (Exception e) {

            result.setMessage(
                    result.getMessage()
                            + " | Report generation failed: "
                            + e.getMessage()
            );
        }
    }

    private long elapsedMilliseconds(long startNanos) {

        return (System.nanoTime() - startNanos)
                / 1_000_000;
    }

    private String buildSummaryMessage(
            TestRunResultDto result) {

        if (result.isPassed()) {

            return String.format(
                    "Test run passed. Suites: %d, Test cases: %d",
                    result.getTotalSuites(),
                    result.getTotalTestCases()
            );
        }

        return String.format(
                "Test run failed. "
                        + "Passed suites: %d, "
                        + "Failed suites: %d, "
                        + "Skipped suites: %d",
                result.getPassedSuites(),
                result.getFailedSuites(),
                result.getSkippedSuites()
        );
    }
}