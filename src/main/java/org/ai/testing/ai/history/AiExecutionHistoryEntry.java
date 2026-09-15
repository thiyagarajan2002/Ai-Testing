package org.ai.testing.ai.history;

import lombok.Data;
import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent snapshot of one isolated AI-generated suite execution.
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
    private double passRate;
    private List<String> failedTestCaseIds = new ArrayList<>();

    public static AiExecutionHistoryEntry from(AiGeneratedSuiteExecutionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("execution result is required");
        }
        AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
        entry.setExecutionId("AI-EXEC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        entry.setExecutedAt(LocalDateTime.now());
        entry.setSourceSuiteId(result.getSourceSuiteId());
        entry.setSourceTestCaseId(result.getSourceTestCaseId());
        entry.setExecuted(result.isExecuted());
        entry.setPassed(result.isPassed());
        entry.setMessage(result.getMessage());
        entry.setTotalTestCases(result.getTotalTestCases());
        entry.setPassedTestCases(result.getPassedTestCases());
        entry.setFailedTestCases(result.getFailedTestCases());
        entry.setSkippedTestCases(result.getSkippedTestCases());
        entry.setPassRate(entry.getTotalTestCases() == 0 ? 0.0
                : (entry.getPassedTestCases() * 100.0) / entry.getTotalTestCases());
        if (result.getTestCaseResults() != null) {
            result.getTestCaseResults().stream()
                    .filter(item -> item != null && item.isExecuted() && !item.isPassed())
                    .forEach(item -> entry.getFailedTestCaseIds().add(item.getTestCaseId()));
        }
        return entry;
    }
}
