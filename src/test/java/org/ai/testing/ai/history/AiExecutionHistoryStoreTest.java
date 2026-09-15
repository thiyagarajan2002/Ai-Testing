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
    void shouldRejectNullDatabaseUrl() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AiExecutionHistoryStore(null));

        assertEquals("database URL is required", exception.getMessage());
    }

    @Test
    void shouldRejectBlankDatabaseUrl() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AiExecutionHistoryStore("   "));

        assertEquals("database URL is required", exception.getMessage());
    }

    @Test
    void shouldRejectNullHistoryEntry() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> store.save(null));

            assertEquals("valid history entry is required", exception.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectHistoryEntryWithoutExecutionId() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> store.save(entry));

            assertEquals("valid history entry is required", exception.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectBlankExecutionId() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
            entry.setExecutionId("   ");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> store.save(entry));

            assertEquals("valid history entry is required", exception.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldPersistFailedTestCaseIds() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryEntry entry = entry(
                    "FAILED-IDS",
                    1,
                    3,
                    LocalDateTime.of(2026, 1, 1, 10, 0));
            entry.setFailedTestCaseIds(List.of("TC-002", "TC-003"));

            store.save(entry);

            List<AiExecutionHistoryEntry> history = store.findBySourceSuite("SUITE-01");

            assertEquals(1, history.size());
            assertEquals(List.of("TC-002", "TC-003"), history.get(0).getFailedTestCaseIds());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldFindAllExecutionsInAscendingExecutionTimeOrder() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);

            store.save(entry("THIRD", 3, 3, baseTime.plusMinutes(20)));
            store.save(entry("FIRST", 1, 2, baseTime));
            store.save(entry("SECOND", 2, 2, baseTime.plusMinutes(10)));

            List<AiExecutionHistoryEntry> history = store.findAll();

            assertEquals(3, history.size());
            assertEquals("FIRST", history.get(0).getExecutionId());
            assertEquals("SECOND", history.get(1).getExecutionId());
            assertEquals("THIRD", history.get(2).getExecutionId());
            assertEquals(baseTime, history.get(0).getExecutedAt());
            assertEquals(baseTime.plusMinutes(10), history.get(1).getExecutedAt());
            assertEquals(baseTime.plusMinutes(20), history.get(2).getExecutedAt());
        } finally {
            Files.deleteIfExists(database);
        }
    }

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
