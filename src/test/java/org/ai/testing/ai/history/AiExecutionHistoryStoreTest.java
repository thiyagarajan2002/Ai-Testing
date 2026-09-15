package org.ai.testing.ai.history;

import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiExecutionHistoryStoreTest {
    @Test
    void shouldPersistAndReloadExecutionHistory() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
            result.setSourceSuiteId("SUITE-01");
            result.setSourceTestCaseId("TC-01");
            result.setExecuted(true);
            result.setPassed(true);
            result.setMessage("completed");
            result.setTotalTestCases(2);
            result.setPassedTestCases(2);
            AiExecutionHistoryEntry saved = new AiExecutionHistoryService(store).record(result);

            List<AiExecutionHistoryEntry> history = store.findBySourceSuite("SUITE-01");
            assertEquals(1, history.size());
            assertEquals(saved.getExecutionId(), history.get(0).getExecutionId());
            assertEquals(100.0, history.get(0).getPassRate(), 0.0001);
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldCompareLatestTwoExecutions() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryEntry first = entry("FIRST", 1, 2);
            AiExecutionHistoryEntry second = entry("SECOND", 2, 2);
            store.save(first);
            store.save(second);

            AiExecutionHistoryComparison comparison = new AiExecutionHistoryService(store)
                    .compareLatest("SUITE-01");

            assertEquals(50.0, comparison.getPreviousPassRate(), 0.0001);
            assertEquals(100.0, comparison.getCurrentPassRate(), 0.0001);
            assertEquals(50.0, comparison.getPassRateChange(), 0.0001);
            assertEquals("IMPROVED", comparison.getTrend());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectComparisonWithOnlyOneExecution() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            store.save(entry("ONLY", 1, 2));
            assertThrows(IllegalStateException.class,
                    () -> new AiExecutionHistoryService(store).compareLatest("SUITE-01"));
        } finally {
            Files.deleteIfExists(database);
        }
    }

    private static AiExecutionHistoryEntry entry(String id, int passed, int total) {
        AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
        entry.setExecutionId(id);
        entry.setExecutedAt(java.time.LocalDateTime.now().plusNanos(id.hashCode()));
        entry.setSourceSuiteId("SUITE-01");
        entry.setSourceTestCaseId("TC-01");
        entry.setExecuted(true);
        entry.setPassed(passed == total);
        entry.setTotalTestCases(total);
        entry.setPassedTestCases(passed);
        entry.setFailedTestCases(total - passed);
        entry.setSkippedTestCases(0);
        entry.setPassRate(passed * 100.0 / total);
        entry.setFailedTestCaseIds(List.of());
        return entry;
    }
}
