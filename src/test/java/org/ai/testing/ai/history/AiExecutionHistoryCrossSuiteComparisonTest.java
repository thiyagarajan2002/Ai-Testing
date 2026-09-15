package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiExecutionHistoryCrossSuiteComparisonTest {

    @Test
    void shouldCompareOnlyExecutionsFromRequestedSuite() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            LocalDateTime baseTime = LocalDateTime.of(2026, 2, 1, 10, 0);

            AiExecutionHistoryEntry suiteOneFirst = entry(
                    "S1-FIRST", "SUITE-01", 1, 2, baseTime);
            AiExecutionHistoryEntry suiteOneLatest = entry(
                    "S1-LATEST", "SUITE-01", 2, 2, baseTime.plusMinutes(5));
            AiExecutionHistoryEntry suiteTwoFirst = entry(
                    "S2-FIRST", "SUITE-02", 2, 2, baseTime.plusMinutes(1));
            AiExecutionHistoryEntry suiteTwoLatest = entry(
                    "S2-LATEST", "SUITE-02", 1, 2, baseTime.plusMinutes(10));

            store.save(suiteOneFirst);
            store.save(suiteTwoFirst);
            store.save(suiteOneLatest);
            store.save(suiteTwoLatest);

            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            AiExecutionHistoryComparison suiteOneComparison = service.compareLatest("SUITE-01");
            AiExecutionHistoryComparison suiteTwoComparison = service.compareLatest("SUITE-02");

            assertNotNull(suiteOneComparison);
            assertEquals("S1-FIRST", suiteOneComparison.getPrevious().getExecutionId());
            assertEquals("S1-LATEST", suiteOneComparison.getCurrent().getExecutionId());
            assertEquals(50.0, suiteOneComparison.getPreviousPassRate(), 0.0001);
            assertEquals(100.0, suiteOneComparison.getCurrentPassRate(), 0.0001);
            assertEquals(50.0, suiteOneComparison.getPassRateChange(), 0.0001);
            assertEquals("IMPROVED", suiteOneComparison.getTrend());

            assertNotNull(suiteTwoComparison);
            assertEquals("S2-FIRST", suiteTwoComparison.getPrevious().getExecutionId());
            assertEquals("S2-LATEST", suiteTwoComparison.getCurrent().getExecutionId());
            assertEquals(100.0, suiteTwoComparison.getPreviousPassRate(), 0.0001);
            assertEquals(50.0, suiteTwoComparison.getCurrentPassRate(), 0.0001);
            assertEquals(-50.0, suiteTwoComparison.getPassRateChange(), 0.0001);
            assertEquals("REGRESSED", suiteTwoComparison.getTrend());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldKeepLatestExecutionScopedToItsSuite() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            LocalDateTime baseTime = LocalDateTime.of(2026, 2, 2, 10, 0);

            AiExecutionHistoryEntry suiteOneLatest = entry(
                    "S1-LATEST", "SUITE-01", 1, 2, baseTime.plusMinutes(20));
            AiExecutionHistoryEntry suiteTwoLatest = entry(
                    "S2-LATEST", "SUITE-02", 2, 2, baseTime.plusMinutes(30));

            store.save(suiteOneLatest);
            store.save(suiteTwoLatest);

            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            assertEquals("S1-LATEST", service.getLatest("SUITE-01").getExecutionId());
            assertEquals("SUITE-01", service.getLatest("SUITE-01").getSourceSuiteId());
            assertEquals("S2-LATEST", service.getLatest("SUITE-02").getExecutionId());
            assertEquals("SUITE-02", service.getLatest("SUITE-02").getSourceSuiteId());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    private static AiExecutionHistoryEntry entry(
            String executionId,
            String sourceSuiteId,
            int passed,
            int total,
            LocalDateTime executedAt) {
        AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
        entry.setExecutionId(executionId);
        entry.setExecutedAt(executedAt);
        entry.setSourceSuiteId(sourceSuiteId);
        entry.setSourceTestCaseId(sourceSuiteId + "-TC");
        entry.setExecuted(true);
        entry.setPassed(passed == total);
        entry.setTotalTestCases(total);
        entry.setPassedTestCases(passed);
        entry.setFailedTestCases(total - passed);
        entry.setSkippedTestCases(0);
        entry.setPassRate(passed * 100.0 / total);
        return entry;
    }
}
