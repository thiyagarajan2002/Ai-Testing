package org.ai.testing.ai.history;

import lombok.Data;

/**
 * Comparison between two consecutive AI execution snapshots.
 */
@Data
public class AiExecutionHistoryComparison {
    private static final double TREND_THRESHOLD = 0.0001;
    private static final double FLOATING_POINT_EPSILON = 0.000000001;

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
        comparison.setTrend(resolveTrend(change));
        return comparison;
    }

    private static String resolveTrend(double change) {
        double effectiveThreshold = TREND_THRESHOLD + FLOATING_POINT_EPSILON;
        if (change > effectiveThreshold) {
            return "IMPROVED";
        }
        if (change < -effectiveThreshold) {
            return "REGRESSED";
        }
        return "UNCHANGED";
    }
}
