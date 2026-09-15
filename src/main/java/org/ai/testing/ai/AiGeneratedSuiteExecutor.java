package org.ai.testing.ai;

import org.ai.testing.ai.model.AiGeneratedNegativeTestSuite;
import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.ai.model.AiTestGenerationOrchestrationResult;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;

/**
 * Executes only approved and attached AI-generated tests as an isolated suite.
 * Normal regression execution is not changed by this service.
 */
public class AiGeneratedSuiteExecutor {

    private final TestCaseExecutor testCaseExecutor;

    public AiGeneratedSuiteExecutor() {
        this(new TestCaseExecutor());
    }

    public AiGeneratedSuiteExecutor(TestCaseExecutor testCaseExecutor) {
        if (testCaseExecutor == null) {
            throw new IllegalArgumentException("test case executor is required");
        }
        this.testCaseExecutor = testCaseExecutor;
    }

    public AiGeneratedSuiteExecutionResult execute(
            AiTestGenerationOrchestrationResult orchestrationResult) {
        validate(orchestrationResult);

        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId(orchestrationResult.getSourceSuiteId());
        result.setSourceTestCaseId(orchestrationResult.getSourceTestCaseId());

        for (TestCaseDto testCase : orchestrationResult.getAllGeneratedTestCases()) {
            result.addResult(testCaseExecutor.execute(testCase));
        }

        result.setExecuted(true);
        result.setMessage(result.isPassed()
                ? "AI-generated suite execution passed"
                : "AI-generated suite execution completed with failures");
        return result;
    }

    public AiGeneratedSuiteExecutionResult executeNegativeSuite(
            AiTestGenerationOrchestrationResult orchestrationResult) {
        validate(orchestrationResult);

        AiGeneratedNegativeTestSuite negativeSuite = orchestrationResult.getNegativeSuite();
        if (!AiNegativeTestSuiteExecutionPolicy.isExecutable(negativeSuite)) {
            throw new IllegalStateException(
                    "AI negative suite must be explicitly enabled and contain test cases");
        }

        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId(orchestrationResult.getSourceSuiteId());
        result.setSourceTestCaseId(orchestrationResult.getSourceTestCaseId());

        for (TestCaseDto testCase : negativeSuite.getTestCases()) {
            result.addResult(testCaseExecutor.execute(testCase));
        }

        result.setExecuted(true);
        result.setMessage(result.isPassed()
                ? "AI negative suite execution passed"
                : "AI negative suite execution completed with failures");
        return result;
    }

    private void validate(AiTestGenerationOrchestrationResult result) {
        if (result == null) {
            throw new IllegalArgumentException("AI generation result is required");
        }
        if (!result.isApproved()) {
            throw new IllegalStateException("AI generation result requires explicit approval");
        }
        if (!result.isAttached()) {
            throw new IllegalStateException("AI generation result must be attached before execution");
        }
        if (!result.isReviewPassed()) {
            throw new IllegalStateException("AI generation result must pass review before execution");
        }
    }
}
