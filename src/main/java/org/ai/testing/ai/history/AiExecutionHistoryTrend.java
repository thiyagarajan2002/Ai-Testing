package org.ai.testing.ai.history;

import lombok.Data;

/**
 * Aggregated trend calculated from historical AI execution snapshots.
 */
@Data
public class AiExecutionHistoryTrend {
    private static final double TREND_THRESHOLD = 0.0001;
    private static final double FLOATING_POINT_TOLERANCE = 0.000000001;

    private String sourceSuiteId;
    private int executionCount;
    private double firstPassRate;
    private double latestPassRate;
    private double passRateChange;
    private int firstFailedTestCases;
    private int latestFailedTestCases;
    private int failedTestCaseChange;
    private String trend;

    public static AiExecutionHistoryTrend from(String sourceSuiteId,
                                                java.util.List<AiExecutionHistoryEntry> history) {
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("at least one execution history entry is required");
        }

        AiExecutionHistoryEntry first = history.get(0);
        AiExecutionHistoryEntry latest = history.get(history.size() - 1);

        AiExecutionHistoryTrend trend = new AiExecutionHistoryTrend();
        trend.setSourceSuiteId(sourceSuiteId);
        trend.setExecutionCount(history.size());
        trend.setFirstPassRate(first.getPassRate());
        trend.setLatestPassRate(latest.getPassRate());
        trend.setPassRateChange(latest.getPassRate() - first.getPassRate());
        trend.setFirstFailedTestCases(first.getFailedTestCases());
        trend.setLatestFailedTestCases(latest.getFailedTestCases());
        trend.setFailedTestCaseChange(latest.getFailedTestCases() - first.getFailedTestCases());

        double change = trend.getPassRateChange();
        if (change - TREND_THRESHOLD > FLOATING_POINT_TOLERANCE) {
            trend.setTrend("IMPROVED");
        } else if (change + TREND_THRESHOLD < -FLOATING_POINT_TOLERANCE) {
            trend.setTrend("REGRESSED");
        } else {
            trend.setTrend("UNCHANGED");
        }
        return trend;
    }
}
