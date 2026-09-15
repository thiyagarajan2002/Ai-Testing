package org.ai.testing.ai.history;

import java.util.List;

/**
 * Calculates pass-rate and failure trends from persisted AI execution history.
 */
public class AiExecutionHistoryTrendService {
    private final AiExecutionHistoryService historyService;

    public AiExecutionHistoryTrendService(AiExecutionHistoryService historyService) {
        if (historyService == null) {
            throw new IllegalArgumentException("history service is required");
        }
        this.historyService = historyService;
    }

    public AiExecutionHistoryTrend calculate(String sourceSuiteId) {
        List<AiExecutionHistoryEntry> history = historyService.getBySourceSuite(sourceSuiteId);
        if (history.isEmpty()) {
            throw new IllegalStateException("No AI execution history exists for source suite: " + sourceSuiteId);
        }
        return AiExecutionHistoryTrend.from(sourceSuiteId, history);
    }

    public AiExecutionHistoryTrend calculate(List<AiExecutionHistoryEntry> history) {
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("execution history is required");
        }
        String sourceSuiteId = history.get(0).getSourceSuiteId();
        return AiExecutionHistoryTrend.from(sourceSuiteId, history);
    }
}
