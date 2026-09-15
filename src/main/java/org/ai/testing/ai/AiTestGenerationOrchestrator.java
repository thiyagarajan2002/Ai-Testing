package org.ai.testing.ai;

import org.ai.testing.ai.model.AiGeneratedNegativeTestSuite;
import org.ai.testing.ai.model.AiGeneratedTestSuite;
import org.ai.testing.ai.model.AiNegativeTestDataResult;
import org.ai.testing.ai.model.AiTestGenerationOrchestrationResult;
import org.ai.testing.ai.model.AiTestGenerationRequest;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates AI test generation, review, approval, and controlled attachment.
 * Generation and review never mutate the target suite. Attachment requires
 * explicit approval and performs duplicate-ID protection.
 */
public class AiTestGenerationOrchestrator {

    private final AiTestCaseGenerator testCaseGenerator;
    private final AiNegativeTestDataGenerator negativeDataGenerator;
    private final AiNegativeTestSuiteBuilder negativeSuiteBuilder;

    public AiTestGenerationOrchestrator() {
        this(new AiTestCaseGenerator(), new AiNegativeTestDataGenerator(),
                new AiNegativeTestSuiteBuilder());
    }

    public AiTestGenerationOrchestrator(
            AiTestCaseGenerator testCaseGenerator,
            AiNegativeTestDataGenerator negativeDataGenerator,
            AiNegativeTestSuiteBuilder negativeSuiteBuilder) {
        this.testCaseGenerator = testCaseGenerator;
        this.negativeDataGenerator = negativeDataGenerator;
        this.negativeSuiteBuilder = negativeSuiteBuilder;
    }

    public AiTestGenerationOrchestrationResult generate(
            TestSuiteDto targetSuite,
            AiTestGenerationRequest request,
            boolean includeNegativeTests) {
        validateTargetSuite(targetSuite);
        if (request == null) {
            throw new IllegalArgumentException("AI generation request is required");
        }

        AiGeneratedTestSuite positiveSuite = testCaseGenerator.generate(request);
        TestCaseDto sourceTestCase = findPositiveSource(positiveSuite);

        AiGeneratedNegativeTestSuite negativeSuite = null;
        if (includeNegativeTests) {
            AiNegativeTestDataResult negativeData = negativeDataGenerator.generate(request);
            negativeSuite = negativeSuiteBuilder.build(sourceTestCase, negativeData, false);
        }

        AiTestGenerationOrchestrationResult result = new AiTestGenerationOrchestrationResult();
        result.setSourceSuiteId(targetSuite.getSuiteId());
        result.setSourceTestCaseId(sourceTestCase.getTestCaseId());
        result.setPositiveSuite(positiveSuite);
        result.setNegativeSuite(negativeSuite);
        return result;
    }

    public AiTestGenerationOrchestrationResult review(
            TestSuiteDto targetSuite,
            AiTestGenerationOrchestrationResult result) {
        validateTargetSuite(targetSuite);
        validateResult(result);

        List<String> findings = new ArrayList<>();
        List<TestCaseDto> generated = result.getAllGeneratedTestCases();

        if (generated.isEmpty()) {
            findings.add("No AI-generated test cases were produced.");
        }

        Set<String> ids = new HashSet<>();
        Set<String> existingIds = new HashSet<>();
        if (targetSuite.getTestCases() != null) {
            for (TestCaseDto testCase : targetSuite.getTestCases()) {
                if (testCase != null && testCase.getTestCaseId() != null) {
                    existingIds.add(testCase.getTestCaseId());
                }
            }
        }

        for (TestCaseDto testCase : generated) {
            if (testCase == null) {
                findings.add("Generated test case must not be null.");
                continue;
            }
            if (testCase.getTestCaseId() == null || testCase.getTestCaseId().isBlank()) {
                findings.add("Generated test case has no ID.");
            } else if (!ids.add(testCase.getTestCaseId())) {
                findings.add("Generated test case ID is duplicated: " + testCase.getTestCaseId());
            }
            if (existingIds.contains(testCase.getTestCaseId())) {
                findings.add("Generated test case ID already exists in target suite: "
                        + testCase.getTestCaseId());
            }
            if (testCase.getRequest() == null
                    || testCase.getRequest().getUrl() == null
                    || testCase.getRequest().getUrl().isBlank()) {
                findings.add("Generated test case has no request URL: " + testCase.getTestCaseId());
            }
        }

        result.setReviewFindings(findings);
        result.setReviewPassed(findings.isEmpty());
        result.setReviewStatus(findings.isEmpty() ? "PASSED" : "FAILED");
        return result;
    }

    public AiTestGenerationOrchestrationResult approve(
            AiTestGenerationOrchestrationResult result) {
        validateResult(result);
        if (!result.isReviewPassed()) {
            throw new IllegalStateException("AI generation result must pass review before approval");
        }
        result.setApproved(true);
        return result;
    }

    public AiTestGenerationOrchestrationResult attachApproved(
            TestSuiteDto targetSuite,
            AiTestGenerationOrchestrationResult result) {
        validateTargetSuite(targetSuite);
        validateResult(result);
        if (!result.isApproved()) {
            throw new IllegalStateException("AI generation result requires explicit approval");
        }
        if (!result.isReviewPassed()) {
            throw new IllegalStateException("AI generation result must pass review before attachment");
        }
        if (result.isAttached()) {
            return result;
        }

        List<TestCaseDto> targetCases = targetSuite.getTestCases();
        if (targetCases == null) {
            targetCases = new ArrayList<>();
            targetSuite.setTestCases(targetCases);
        }

        Set<String> existingIds = new HashSet<>();
        for (TestCaseDto testCase : targetCases) {
            if (testCase != null && testCase.getTestCaseId() != null) {
                existingIds.add(testCase.getTestCaseId());
            }
        }

        for (TestCaseDto testCase : result.getAllGeneratedTestCases()) {
            if (testCase == null || testCase.getTestCaseId() == null) {
                throw new IllegalStateException("Generated test case is invalid");
            }
            if (!existingIds.add(testCase.getTestCaseId())) {
                throw new IllegalStateException(
                        "Generated test case ID already exists in target suite: "
                                + testCase.getTestCaseId());
            }
            targetCases.add(testCase);
        }

        if (result.getNegativeSuite() != null) {
            result.getNegativeSuite().setEnabled(true);
        }
        result.setAttached(true);
        return result;
    }

    private TestCaseDto findPositiveSource(AiGeneratedTestSuite suite) {
        if (suite == null || suite.getTestCases() == null || suite.getTestCases().isEmpty()) {
            throw new IllegalStateException("AI positive test generation produced no test case");
        }
        return suite.getTestCases().get(0);
    }

    private void validateTargetSuite(TestSuiteDto targetSuite) {
        if (targetSuite == null) {
            throw new IllegalArgumentException("target test suite is required");
        }
        if (targetSuite.getSuiteId() == null || targetSuite.getSuiteId().isBlank()) {
            throw new IllegalArgumentException("target suite ID is required");
        }
    }

    private void validateResult(AiTestGenerationOrchestrationResult result) {
        if (result == null) {
            throw new IllegalArgumentException("AI generation result is required");
        }
    }
}
