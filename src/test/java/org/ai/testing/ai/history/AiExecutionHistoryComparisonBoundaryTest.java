package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiExecutionHistoryComparisonBoundaryTest {

    @Test
    void shouldDetectTinyPositiveChangeAsImproved() {
        AiExecutionHistoryComparison comparison = compare(50.0000, 50.0002);

        assertEquals(0.0002, comparison.getPassRateChange(), 0.0000001);
        assertEquals("IMPROVED", comparison.getTrend());
    }

    @Test
    void shouldTreatExactlyPoint0001ChangeAsUnchanged() {
        AiExecutionHistoryComparison comparison = compare(50.0, 50.0001);

        assertEquals(0.0001, comparison.getPassRateChange(), 0.0000001);
        assertEquals("UNCHANGED", comparison.getTrend());
    }

    @Test
    void shouldDetectTinyNegativeChangeAsRegressed() {
        AiExecutionHistoryComparison comparison = compare(50.0002, 50.0);

        assertEquals(-0.0002, comparison.getPassRateChange(), 0.0000001);
        assertEquals("REGRESSED", comparison.getTrend());
    }

    @Test
    void shouldDetectLargePositiveChangeAsImproved() {
        AiExecutionHistoryComparison comparison = compare(0.0, 100.0);

        assertEquals(100.0, comparison.getPassRateChange());
        assertEquals("IMPROVED", comparison.getTrend());
    }

    @Test
    void shouldDetectLargeNegativeChangeAsRegressed() {
        AiExecutionHistoryComparison comparison = compare(100.0, 0.0);

        assertEquals(-100.0, comparison.getPassRateChange());
        assertEquals("REGRESSED", comparison.getTrend());
    }

    @Test
    void shouldDetectNoChangeAsUnchanged() {
        AiExecutionHistoryComparison comparison = compare(75.0, 75.0);

        assertEquals(0.0, comparison.getPassRateChange());
        assertEquals("UNCHANGED", comparison.getTrend());
    }

    private AiExecutionHistoryComparison compare(double previousRate, double currentRate) {
        AiExecutionHistoryEntry previous = entry("PREVIOUS", previousRate);
        AiExecutionHistoryEntry current = entry("CURRENT", currentRate);
        return AiExecutionHistoryComparison.compare(previous, current);
    }

    private AiExecutionHistoryEntry entry(String id, double passRate) {
        AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
        entry.setExecutionId(id);
        entry.setPassRate(passRate);
        return entry;
    }
}
