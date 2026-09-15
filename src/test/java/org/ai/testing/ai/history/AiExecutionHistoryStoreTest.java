package org.ai.testing.ai.history;

import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
            LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);
            AiExecutionHistoryEntry first = entry("FIRST", 1, 2, baseTime);
            AiExecutionHistoryEntry second = entry("SECOND", 2, 2, baseTime.plusMinutes(1));
            store.save(first);
            store.save(second);

            AiExecutionHistoryComparison comparison = new AiExecutionHistoryService(store)
                    .compareLatest("SUITE-01");

            assertEquals("FIRST", comparison.getPrevious().getExecutionId());
            assertEquals("SECOND", comparison.getCurrent().getExecutionId());
            assertEquals(50.0, comparison.getPreviousPassRate(), 0.0001);
            assertEquals(100.0, comparison.getCurrentPassRate(), 0.0001);
            assertEquals(50.0, comparison.getPassRateChange(), 0.0001);
            assertEquals("IMPROVED", comparison.getTrend());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldDetectRegressedLatestExecution() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);
            store.save(entry("FIRST", 2, 2, baseTime));
            store.save(entry("SECOND", 1, 2, baseTime.plusMinutes(1)));

            AiExecutionHistoryComparison comparison = new AiExecutionHistoryService(store)
                    .compareLatest("SUITE-01");

            assertEquals(100.0, comparison.getPreviousPassRate(), 0.0001);
            assertEquals(50.0, comparison.getCurrentPassRate(), 0.0001);
            assertEquals(-50.0, comparison.getPassRateChange(), 0.0001);
            assertEquals("REGRESSED", comparison.getTrend());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldDetectUnchangedLatestExecution() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);
            store.save(entry("FIRST", 2, 2, baseTime));
            store.save(entry("SECOND", 2, 2, baseTime.plusMinutes(1)));

            AiExecutionHistoryComparison comparison = new AiExecutionHistoryService(store)
                    .compareLatest("SUITE-01");

            assertEquals(100.0, comparison.getPreviousPassRate(), 0.0001);
            assertEquals(100.0, comparison.getCurrentPassRate(), 0.0001);
            assertEquals(0.0, comparison.getPassRateChange(), 0.0001);
            assertEquals("UNCHANGED", comparison.getTrend());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldKeepHistoryIsolatedBySourceSuite() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);

            AiExecutionHistoryEntry suiteOneFirst = entry("SUITE-01-FIRST", 2, 2, baseTime);
            suiteOneFirst.setSourceSuiteId("SUITE-01");

            AiExecutionHistoryEntry suiteOneSecond = entry("SUITE-01-SECOND", 1, 2, baseTime.plusMinutes(1));
            suiteOneSecond.setSourceSuiteId("SUITE-01");

            AiExecutionHistoryEntry suiteTwoOnly = entry("SUITE-02-ONLY", 2, 2, baseTime.plusMinutes(2));
            suiteTwoOnly.setSourceSuiteId("SUITE-02");

            store.save(suiteOneFirst);
            store.save(suiteOneSecond);
            store.save(suiteTwoOnly);

            List<AiExecutionHistoryEntry> suiteOneHistory = store.findBySourceSuite("SUITE-01");
            List<AiExecutionHistoryEntry> suiteTwoHistory = store.findBySourceSuite("SUITE-02");

            assertEquals(2, suiteOneHistory.size());
            assertEquals(1, suiteTwoHistory.size());
            assertEquals("SUITE-01-FIRST", suiteOneHistory.get(0).getExecutionId());
            assertEquals("SUITE-01-SECOND", suiteOneHistory.get(1).getExecutionId());
            assertEquals("SUITE-02-ONLY", suiteTwoHistory.get(0).getExecutionId());
            assertTrue(suiteOneHistory.stream()
                    .allMatch(item -> "SUITE-01".equals(item.getSourceSuiteId())));
            assertTrue(suiteTwoHistory.stream()
                    .allMatch(item -> "SUITE-02".equals(item.getSourceSuiteId())));
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldFindLatestExecutionForRequestedSourceSuite() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);

            AiExecutionHistoryEntry first = entry("FIRST", 1, 2, baseTime);
            first.setSourceSuiteId("SUITE-01");

            AiExecutionHistoryEntry latest = entry("LATEST", 2, 2, baseTime.plusMinutes(5));
            latest.setSourceSuiteId("SUITE-01");

            AiExecutionHistoryEntry otherSuite = entry("OTHER-SUITE", 2, 2, baseTime.plusMinutes(10));
            otherSuite.setSourceSuiteId("SUITE-02");

            store.save(first);
            store.save(latest);
            store.save(otherSuite);

            AiExecutionHistoryEntry result = store.findLatest("SUITE-01");

            assertNotNull(result);
            assertEquals("LATEST", result.getExecutionId());
            assertEquals("SUITE-01", result.getSourceSuiteId());
            assertEquals(100.0, result.getPassRate(), 0.0001);
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldReturnNullWhenLatestExecutionDoesNotExist() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            store.save(entry("SUITE-01-ONLY", 2, 2, LocalDateTime.of(2026, 1, 1, 10, 0)));

            AiExecutionHistoryEntry result = store.findLatest("SUITE-NOT-FOUND");

            assertNull(result);
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectComparisonWithOnlyOneExecution() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            store.save(entry("ONLY", 1, 2, LocalDateTime.of(2026, 1, 1, 10, 0)));
            assertThrows(IllegalStateException.class,
                    () -> new AiExecutionHistoryService(store).compareLatest("SUITE-01"));
        } finally {
            Files.deleteIfExists(database);
        }
    }

    private static AiExecutionHistoryEntry entry(
            String id, int passed, int total, LocalDateTime executedAt) {
        AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
        entry.setExecutionId(id);
        entry.setExecutedAt(executedAt);
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
