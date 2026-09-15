package org.ai.testing.ai;

import org.ai.testing.ai.model.AiGeneratedNegativeTestSuite;
import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.ai.model.AiGeneratedTestSuite;
import org.ai.testing.ai.model.AiTestGenerationOrchestrationResult;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiGeneratedSuiteExecutorTest {

    @Test
    void shouldExecuteApprovedAttachedGeneratedSuiteSeparately() {
        TestCaseDto testCase = disabledTestCase("TC-AI-001");

        AiGeneratedTestSuite positiveSuite = new AiGeneratedTestSuite();
        positiveSuite.setTestCases(List.of(testCase));

        AiTestGenerationOrchestrationResult orchestration = new AiTestGenerationOrchestrationResult();
        orchestration.setSourceSuiteId("SUITE-001");
        orchestration.setSourceTestCaseId("TC-SOURCE");
        orchestration.setPositiveSuite(positiveSuite);
        orchestration.setReviewPassed(true);
        orchestration.setApproved(true);
        orchestration.setAttached(true);

        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutor().execute(orchestration);

        assertTrue(result.isExecuted());
        assertEquals("SUITE-001", result.getSourceSuiteId());
        assertEquals(1, result.getTotalTestCases());
        assertEquals(1, result.getSkippedTestCases());
        assertEquals(0, result.getPassedTestCases());
        assertFalse(result.isPassed());
    }

    @Test
    void shouldRejectExecutionWithoutApproval() {
        AiTestGenerationOrchestrationResult orchestration = validResult();
        orchestration.setApproved(false);

        assertThrows(IllegalStateException.class,
                () -> new AiGeneratedSuiteExecutor().execute(orchestration));
    }

    @Test
    void shouldRejectExecutionWithoutAttachment() {
        AiTestGenerationOrchestrationResult orchestration = validResult();
        orchestration.setAttached(false);

        assertThrows(IllegalStateException.class,
                () -> new AiGeneratedSuiteExecutor().execute(orchestration));
    }

    @Test
    void shouldRejectDisabledNegativeSuite() {
        AiTestGenerationOrchestrationResult orchestration = validResult();
        AiGeneratedNegativeTestSuite negativeSuite = new AiGeneratedNegativeTestSuite();
        negativeSuite.setTestCases(List.of(disabledTestCase("TC-AI-NEG-001")));
        negativeSuite.setEnabled(false);
        orchestration.setNegativeSuite(negativeSuite);

        assertThrows(IllegalStateException.class,
                () -> new AiGeneratedSuiteExecutor().executeNegativeSuite(orchestration));
    }

    private AiTestGenerationOrchestrationResult validResult() {
        AiTestGenerationOrchestrationResult result = new AiTestGenerationOrchestrationResult();
        result.setSourceSuiteId("SUITE-001");
        result.setSourceTestCaseId("TC-SOURCE");
        result.setReviewPassed(true);
        result.setApproved(true);
        result.setAttached(true);
        return result;
    }

    private TestCaseDto disabledTestCase(String id) {
        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId(id);
        testCase.setTestCaseName(id);
        testCase.setEnabled(false);
        return testCase;
    }
}
