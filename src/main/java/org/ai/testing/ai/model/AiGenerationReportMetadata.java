package org.ai.testing.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Report-safe summary of an AI test generation decision.
 */
@Data
public class AiGenerationReportMetadata {

    private String sourceSuiteId;
    private String sourceTestCaseId;
    private int positiveTestCaseCount;
    private int negativeTestCaseCount;
    private String strategy;
    private String reviewStatus = "NOT_REVIEWED";
    private boolean reviewPassed;
    private boolean approved;
    private boolean attached;
    private List<String> reviewFindings = new ArrayList<>();
}
