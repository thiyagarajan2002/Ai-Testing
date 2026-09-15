package org.ai.testing.ai.history;

import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiExecutionHistoryEntryPassRateBoundaryTest {

    @Test
    void shouldCalculate100PercentForAllPassedTests() {
        AiGeneratedSuiteExecutionResult result = resultWith(
                executedResult("TC-1", true),
                executedResult("TC-2", true),
                executedResult("TC-3", true));

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(3, entry.getTotalTestCases());
        assertEquals(3, entry.getPassedTestCases());
        assertEquals(0, entry.getFailedTestCases());
        assertEquals(0, entry.getSkippedTestCases());
        assertEquals(100.0, entry.getPassRate());
        assertTrue(entry.getFailedTestCaseIds().isEmpty());
    }

    @Test
    void shouldCalculateZeroPercentForAllFailedTests() {
        AiGeneratedSuiteExecutionResult result = resultWith(
                executedResult("TC-FAIL-1", false),
                executedResult("TC-FAIL-2", false));

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(2, entry.getTotalTestCases());
        assertEquals(0, entry.getPassedTestCases());
        assertEquals(2, entry.getFailedTestCases());
        assertEquals(0, entry.getSkippedTestCases());
        assertEquals(0.0, entry.getPassRate());
        assertEquals(List.of("TC-FAIL-1", "TC-FAIL-2"), entry.getFailedTestCaseIds());
    }

    @Test
    void shouldCalculatePartialPassRate() {
        AiGeneratedSuiteExecutionResult result = resultWith(
                executedResult("TC-1", true),
                executedResult("TC-2", true),
                executedResult("TC-3", false),
                executedResult("TC-4", false),
                executedResult("TC-5", true));

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(5, entry.getTotalTestCases());
        assertEquals(3, entry.getPassedTestCases());
        assertEquals(2, entry.getFailedTestCases());
        assertEquals(60.0, entry.getPassRate());
    }

    @Test
    void shouldCalculateZeroPassRateWhenAllTestsAreSkipped() {
        AiGeneratedSuiteExecutionResult result = resultWith(
                skippedResult("TC-SKIP-1"),
                skippedResult("TC-SKIP-2"),
                skippedResult("TC-SKIP-3"));

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(3, entry.getTotalTestCases());
        assertEquals(0, entry.getPassedTestCases());
        assertEquals(0, entry.getFailedTestCases());
        assertEquals(3, entry.getSkippedTestCases());
        assertEquals(0.0, entry.getPassRate());
        assertTrue(entry.getFailedTestCaseIds().isEmpty());
    }

    @Test
    void shouldKeepFailedTestIdsEmptyWhenThereAreNoFailures() {
        AiGeneratedSuiteExecutionResult result = resultWith(
                executedResult("TC-PASS", true),
                skippedResult("TC-SKIP"));

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(List.of(), entry.getFailedTestCaseIds());
    }

    private AiGeneratedSuiteExecutionResult resultWith(TestCaseExecutor.TestCaseExecutionResult... results) {
        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId("SUITE-BOUNDARY");
        result.setSourceTestCaseId("TC-BOUNDARY");
        for (TestCaseExecutor.TestCaseExecutionResult item : results) {
            result.addResult(item);
        }
        return result;
    }

    private TestCaseExecutor.TestCaseExecutionResult executedResult(String id, boolean passed) {
        TestCaseExecutor.TestCaseExecutionResult result = new TestCaseExecutor.TestCaseExecutionResult();
        result.setTestCaseId(id);
        result.setTestCaseName(id);
        result.setExecuted(true);
        result.setPassed(passed);
        return result;
    }

    private TestCaseExecutor.TestCaseExecutionResult skippedResult(String id) {
        TestCaseExecutor.TestCaseExecutionResult result = new TestCaseExecutor.TestCaseExecutionResult();
        result.setTestCaseId(id);
        result.setTestCaseName(id);
        result.setExecuted(false);
        result.setPassed(false);
        return result;
    }
}
