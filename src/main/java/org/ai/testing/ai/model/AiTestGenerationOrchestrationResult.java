package org.ai.testing.ai.model;

import lombok.Data;
import org.ai.testing.testcase.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the reviewable result of AI test generation before attachment to a suite.
 */
@Data
public class AiTestGenerationOrchestrationResult {

    private String sourceSuiteId;
    private String sourceTestCaseId;
    private AiGeneratedTestSuite positiveSuite;
    private AiGeneratedNegativeTestSuite negativeSuite;
    private boolean reviewPassed;
    private boolean approved;
    private boolean attached;
    private String reviewStatus = "NOT_REVIEWED";
    private List<String> reviewFindings = new ArrayList<>();

    public List<TestCaseDto> getAllGeneratedTestCases() {
        List<TestCaseDto> cases = new ArrayList<>();
        if (positiveSuite != null && positiveSuite.getTestCases() != null) {
            cases.addAll(positiveSuite.getTestCases());
        }
        if (negativeSuite != null && negativeSuite.getTestCases() != null) {
            cases.addAll(negativeSuite.getTestCases());
        }
        return cases;
    }

    public int getGeneratedTestCaseCount() {
        return getAllGeneratedTestCases().size();
    }
}
