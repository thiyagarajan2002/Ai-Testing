package org.ai.testing.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Report-safe metadata for an isolated AI-generated suite execution.
 */
@Data
public class AiExecutionReportMetadata {
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

    public static AiExecutionReportMetadata from(AiGeneratedSuiteExecutionResult result) {
        if (result == null) {
            return null;
        }

        AiExecutionReportMetadata metadata = new AiExecutionReportMetadata();
        metadata.setSourceSuiteId(result.getSourceSuiteId());
        metadata.setSourceTestCaseId(result.getSourceTestCaseId());
        metadata.setExecuted(result.isExecuted());
        metadata.setPassed(result.isPassed());
        metadata.setMessage(result.getMessage());
        metadata.setTotalTestCases(result.getTotalTestCases());
        metadata.setPassedTestCases(result.getPassedTestCases());
        metadata.setFailedTestCases(result.getFailedTestCases());
        metadata.setSkippedTestCases(result.getSkippedTestCases());

        if (result.getTestCaseResults() != null) {
            result.getTestCaseResults().stream()
                    .filter(item -> item != null && item.isExecuted() && !item.isPassed())
                    .forEach(item -> metadata.getFailedTestCaseIds().add(item.getTestCaseId()));
        }
        return metadata;
    }
}
