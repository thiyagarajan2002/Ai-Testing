package org.ai.testing.ai.history;

import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiExecutionHistoryEntryDataIntegrityTest {

    @Test
    void shouldUseZeroPassRateWhenThereAreNoTestCases() {
        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId("SUITE-EMPTY");
        result.setSourceTestCaseId("TC-EMPTY");

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(0, entry.getTotalTestCases());
        assertEquals(0, entry.getPassedTestCases());
        assertEquals(0, entry.getFailedTestCases());
        assertEquals(0, entry.getSkippedTestCases());
        assertEquals(0.0, entry.getPassRate());
        assertTrue(entry.getFailedTestCaseIds().isEmpty());
    }

    @Test
    void shouldCalculatePassRateFromPassedAndTotalTests() {
        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId("SUITE-DATA");
        result.setSourceTestCaseId("TC-DATA");

        result.addResult(executedResult("TC-1", true));
        result.addResult(executedResult("TC-2", true));
        result.addResult(executedResult("TC-3", false));
        result.addResult(executedResult("TC-4", true));

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(4, entry.getTotalTestCases());
        assertEquals(3, entry.getPassedTestCases());
        assertEquals(1, entry.getFailedTestCases());
        assertEquals(0, entry.getSkippedTestCases());
        assertEquals(75.0, entry.getPassRate());
    }

    @Test
    void shouldPreserveFailedAndSkippedCounts() {
        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId("SUITE-MIXED");
        result.setSourceTestCaseId("TC-MIXED");

        result.addResult(executedResult("TC-PASS", true));
        result.addResult(executedResult("TC-FAIL", false));
        result.addResult(skippedResult("TC-SKIP"));
        result.addResult(skippedResult("TC-SKIP-2"));

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(4, entry.getTotalTestCases());
        assertEquals(1, entry.getPassedTestCases());
        assertEquals(1, entry.getFailedTestCases());
        assertEquals(2, entry.getSkippedTestCases());
        assertEquals(25.0, entry.getPassRate());
    }

    @Test
    void shouldCaptureOnlyExecutedFailedTestCaseIds() {
        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId("SUITE-IDS");
        result.setSourceTestCaseId("TC-IDS");

        result.addResult(executedResult("TC-PASS", true));
        result.addResult(executedResult("TC-FAIL-1", false));
        result.addResult(skippedResult("TC-SKIP"));
        result.addResult(executedResult("TC-FAIL-2", false));

        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(result);

        assertEquals(List.of("TC-FAIL-1", "TC-FAIL-2"), entry.getFailedTestCaseIds());
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
