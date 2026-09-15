package org.ai.testing.ai.model;

import lombok.Data;

/**
 * Comparison between two AI-generated execution history records.
 */
@Data
public class AiExecutionHistoryComparison {
    private String baselineExecutionId;
    private String latestExecutionId;
    private int baselineTotalTestCases;
    private int latestTotalTestCases;
    private int baselinePassedTestCases;
    private int latestPassedTestCases;
    private int baselineFailedTestCases;
    private int latestFailedTestCases;
    private int baselineSkippedTestCases;
    private int latestSkippedTestCases;
    private double baselinePassRate;
    private double latestPassRate;
    private double passRateChange;
    private boolean improved;
    private boolean regressed;

    public static AiExecutionHistoryComparison compare(
            AiExecutionHistoryEntry baseline,
            AiExecutionHistoryEntry latest) {
        if (baseline == null || latest == null) {
            throw new IllegalArgumentException("Both baseline and latest executions are required");
        }
        AiExecutionHistoryComparison comparison = new AiExecutionHistoryComparison();
        comparison.setBaselineExecutionId(baseline.getExecutionId());
        comparison.setLatestExecutionId(latest.getExecutionId());
        comparison.setBaselineTotalTestCases(baseline.getTotalTestCases());
        comparison.setLatestTotalTestCases(latest.getTotalTestCases());
        comparison.setBaselinePassedTestCases(baseline.getPassedTestCases());
        comparison.setLatestPassedTestCases(latest.getPassedTestCases());
        comparison.setBaselineFailedTestCases(baseline.getFailedTestCases());
        comparison.setLatestFailedTestCases(latest.getFailedTestCases());
        comparison.setBaselineSkippedTestCases(baseline.getSkippedTestCases());
        comparison.setLatestSkippedTestCases(latest.getSkippedTestCases());

        double baselineRate = passRate(baseline);
        double latestRate = passRate(latest);
        double change = latestRate - baselineRate;
        comparison.setBaselinePassRate(baselineRate);
        comparison.setLatestPassRate(latestRate);
        comparison.setPassRateChange(change);
        comparison.setImproved(change > 0.0);
        comparison.setRegressed(change < 0.0);
        return comparison;
    }

    private static double passRate(AiExecutionHistoryEntry entry) {
        if (entry.getTotalTestCases() == 0) {
            return 0.0;
        }
        return (entry.getPassedTestCases() * 100.0) / entry.getTotalTestCases();
    }
}
