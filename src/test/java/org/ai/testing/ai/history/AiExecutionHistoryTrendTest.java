package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryTrendTest {

    @Test
    void shouldCalculateImprovedTrend() {
        AiExecutionHistoryTrend trend = AiExecutionHistoryTrend.from("SUITE-01", List.of(
                entry("E1", 60.0, 4),
                entry("E2", 80.0, 2),
                entry("E3", 90.0, 1)
        ));

        assertEquals("SUITE-01", trend.getSourceSuiteId());
        assertEquals(3, trend.getExecutionCount());
        assertEquals(60.0, trend.getFirstPassRate());
        assertEquals(90.0, trend.getLatestPassRate());
        assertEquals(30.0, trend.getPassRateChange());
        assertEquals(4, trend.getFirstFailedTestCases());
        assertEquals(1, trend.getLatestFailedTestCases());
        assertEquals(-3, trend.getFailedTestCaseChange());
        assertEquals("IMPROVED", trend.getTrend());
    }

    @Test
    void shouldCalculateRegressedTrend() {
        AiExecutionHistoryTrend trend = AiExecutionHistoryTrend.from("SUITE-01", List.of(
                entry("E1", 90.0, 1),
                entry("E2", 70.0, 3)
        ));

        assertEquals(-20.0, trend.getPassRateChange());
        assertEquals(2, trend.getFailedTestCaseChange());
        assertEquals("REGRESSED", trend.getTrend());
    }

    @Test
    void shouldCalculateUnchangedTrendAtBoundary() {
        AiExecutionHistoryTrend trend = AiExecutionHistoryTrend.from("SUITE-01", List.of(
                entry("E1", 50.0, 2),
                entry("E2", 50.0001, 2)
        ));

        assertEquals(0.0001, trend.getPassRateChange(), 0.0000001);
        assertEquals("UNCHANGED", trend.getTrend());
    }

    @Test
    void shouldSupportSingleExecution() {
        AiExecutionHistoryTrend trend = AiExecutionHistoryTrend.from("SUITE-01", List.of(
                entry("E1", 75.0, 1)
        ));

        assertEquals(1, trend.getExecutionCount());
        assertEquals(0.0, trend.getPassRateChange());
        assertEquals(0, trend.getFailedTestCaseChange());
        assertEquals("UNCHANGED", trend.getTrend());
    }

    @Test
    void shouldRejectEmptyHistory() {
        assertThrows(IllegalArgumentException.class,
                () -> AiExecutionHistoryTrend.from("SUITE-01", List.of()));
    }

    private AiExecutionHistoryEntry entry(String id, double passRate, int failedTests) {
        AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
        entry.setExecutionId(id);
        entry.setSourceSuiteId("SUITE-01");
        entry.setPassRate(passRate);
        entry.setFailedTestCases(failedTests);
        return entry;
    }
}
