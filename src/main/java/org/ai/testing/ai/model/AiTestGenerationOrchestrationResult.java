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

    public AiGenerationReportMetadata toReportMetadata() {
        AiGenerationReportMetadata metadata = new AiGenerationReportMetadata();
        metadata.setSourceSuiteId(sourceSuiteId);
        metadata.setSourceTestCaseId(sourceTestCaseId);
        metadata.setPositiveTestCaseCount(
                positiveSuite == null ? 0 : positiveSuite.getTestCaseCount());
        metadata.setNegativeTestCaseCount(
                negativeSuite == null ? 0 : negativeSuite.getTestCaseCount());
        metadata.setStrategy(resolveStrategy());
        metadata.setReviewStatus(reviewStatus);
        metadata.setReviewPassed(reviewPassed);
        metadata.setApproved(approved);
        metadata.setAttached(attached);
        metadata.setReviewFindings(reviewFindings == null
                ? new ArrayList<>()
                : new ArrayList<>(reviewFindings));
        return metadata;
    }

    private String resolveStrategy() {
        if (positiveSuite != null && positiveSuite.getStrategy() != null) {
            return positiveSuite.getStrategy();
        }
        if (negativeSuite != null && negativeSuite.getStrategy() != null) {
            return negativeSuite.getStrategy();
        }
        return "unknown";
    }
}
