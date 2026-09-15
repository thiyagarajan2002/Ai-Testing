package org.ai.testing.ai;

import org.ai.testing.ai.model.AiExecutionHistoryComparison;
import org.ai.testing.ai.model.AiExecutionHistoryEntry;
import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiExecutionHistoryServiceTest {

    @Test
    void shouldRecordHistoryWithoutSharingResultList() {
        AiExecutionHistoryService service = new AiExecutionHistoryService();
        AiGeneratedSuiteExecutionResult result = execution("SUITE-01", true, 2, 2, 0);

        AiExecutionHistoryEntry entry = service.record(result);

        assertNotNull(entry.getExecutionId());
        assertEquals("SUITE-01", entry.getSourceSuiteId());
        assertEquals(2, entry.getTotalTestCases());
        assertEquals(2, entry.getPassedTestCases());
        assertEquals(0, entry.getFailedTestCases());
        assertEquals(1, service.getHistory().size());
    }

    @Test
    void shouldFilterHistoryBySourceSuite() {
        AiExecutionHistoryService service = new AiExecutionHistoryService();
        service.record(execution("SUITE-01", true, 1, 1, 0));
        service.record(execution("SUITE-02", false, 1, 0, 1));

        assertEquals(1, service.getHistory("SUITE-01").size());
        assertEquals("SUITE-01", service.getHistory("SUITE-01").get(0).getSourceSuiteId());
    }

    @Test
    void shouldCompareLatestExecutions() {
        AiExecutionHistoryService service = new AiExecutionHistoryService();
        service.record(execution("SUITE-01", false, 4, 2, 2));
        service.record(execution("SUITE-01", true, 4, 3, 1));

        AiExecutionHistoryComparison comparison = service.compareLatest("SUITE-01");

        assertEquals(50.0, comparison.getBaselinePassRate(), 0.0001);
        assertEquals(75.0, comparison.getLatestPassRate(), 0.0001);
        assertEquals(25.0, comparison.getPassRateChange(), 0.0001);
        assertTrue(comparison.isImproved());
        assertFalse(comparison.isRegressed());
    }

    @Test
    void shouldRejectComparisonWhenHistoryIsInsufficient() {
        AiExecutionHistoryService service = new AiExecutionHistoryService();
        service.record(execution("SUITE-01", true, 1, 1, 0));

        assertThrows(IllegalStateException.class, () -> service.compareLatest("SUITE-01"));
    }

    private AiGeneratedSuiteExecutionResult execution(
            String suiteId, boolean passed, int total, int passedCount, int failedCount) {
        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId(suiteId);
        result.setSourceTestCaseId("TC-01");
        result.setExecuted(true);
        result.setMessage("completed");
        for (int i = 0; i < total; i++) {
            TestCaseExecutor.TestCaseExecutionResult test = new TestCaseExecutor.TestCaseExecutionResult();
            test.setTestCaseId("AI-TC-" + i);
            test.setExecuted(true);
            test.setPassed(i < passedCount);
            result.addResult(test);
        }
        result.setPassed(passed);
        return result;
    }
}
