package org.ai.testing.ai.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable-style snapshot data for one AI-generated suite execution.
 * The stored values are copied from the execution result so later execution changes
 * do not modify historical records.
 */
@Data
public class AiExecutionHistoryEntry {
    private String executionId;
    private LocalDateTime executedAt;
    private String sourceSuiteId;
    private String sourceTestCaseId;
    private boolean executed;
    private boolean passed;
    private String message;
    private int totalTestCases;
    private int passedTestCases;
    private int failedTestCases;
    private int skippedTestCases;
    private List<String> failedTestCaseIds = new ArrayList<>();

    public static AiExecutionHistoryEntry from(
            String executionId,
            LocalDateTime executedAt,
            AiGeneratedSuiteExecutionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Execution result cannot be null");
        }
        AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
        entry.setExecutionId(executionId);
        entry.setExecutedAt(executedAt);
        entry.setSourceSuiteId(result.getSourceSuiteId());
        entry.setSourceTestCaseId(result.getSourceTestCaseId());
        entry.setExecuted(result.isExecuted());
        entry.setPassed(result.isPassed());
        entry.setMessage(result.getMessage());
        entry.setTotalTestCases(result.getTotalTestCases());
        entry.setPassedTestCases(result.getPassedTestCases());
        entry.setFailedTestCases(result.getFailedTestCases());
        entry.setSkippedTestCases(result.getSkippedTestCases());
        if (result.getTestCaseResults() != null) {
            result.getTestCaseResults().stream()
                    .filter(item -> item != null && item.isExecuted() && !item.isPassed())
                    .forEach(item -> entry.getFailedTestCaseIds().add(item.getTestCaseId()));
        }
        return entry;
    }
}
