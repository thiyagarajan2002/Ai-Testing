package org.ai.testing.ai.history;

import lombok.Data;

/**
 * Comparison between two consecutive AI execution snapshots.
 */
@Data
public class AiExecutionHistoryComparison {
    private AiExecutionHistoryEntry previous;
    private AiExecutionHistoryEntry current;
    private double previousPassRate;
    private double currentPassRate;
    private double passRateChange;
    private String trend;

    public static AiExecutionHistoryComparison compare(
            AiExecutionHistoryEntry previous,
            AiExecutionHistoryEntry current) {
        if (previous == null || current == null) {
            throw new IllegalArgumentException("both executions are required");
        }
        AiExecutionHistoryComparison comparison = new AiExecutionHistoryComparison();
        comparison.setPrevious(previous);
        comparison.setCurrent(current);
        comparison.setPreviousPassRate(previous.getPassRate());
        comparison.setCurrentPassRate(current.getPassRate());
        double change = current.getPassRate() - previous.getPassRate();
        comparison.setPassRateChange(change);
        comparison.setTrend(change > 0.0001 ? "IMPROVED" : change < -0.0001 ? "REGRESSED" : "UNCHANGED");
        return comparison;
    }
}
