package org.ai.testing.ai;

import org.ai.testing.ai.model.AiTestGenerationOrchestrationResult;
import org.ai.testing.ai.model.AiTestGenerationRequest;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiTestGenerationOrchestratorTest {

    @Test
    void shouldGenerateAndReviewWithoutMutatingTargetSuite() {
        TestSuiteDto suite = targetSuite();
        AiTestGenerationRequest request = request();

        AiTestGenerationOrchestrationResult result = new AiTestGenerationOrchestrator()
                .generate(suite, request, true);

        assertEquals(0, suite.getTestCases().size());
        assertEquals("SUITE-001", result.getSourceSuiteId());
        assertNotNull(result.getPositiveSuite());
        assertNotNull(result.getNegativeSuite());
        assertFalse(result.isApproved());
        assertFalse(result.isAttached());

        new AiTestGenerationOrchestrator().review(suite, result);

        assertTrue(result.isReviewPassed());
        assertEquals("PASSED", result.getReviewStatus());
        assertTrue(result.getReviewFindings().isEmpty());
    }

    @Test
    void shouldRequireExplicitApprovalBeforeAttachment() {
        TestSuiteDto suite = targetSuite();
        AiTestGenerationOrchestrator orchestrator = new AiTestGenerationOrchestrator();
        AiTestGenerationOrchestrationResult result = orchestrator.generate(suite, request(), true);
        orchestrator.review(suite, result);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orchestrator.attachApproved(suite, result));

        assertEquals("AI generation result requires explicit approval", error.getMessage());
        assertEquals(0, suite.getTestCases().size());
    }

    @Test
    void shouldApproveAndAttachPositiveAndNegativeTests() {
        TestSuiteDto suite = targetSuite();
        AiTestGenerationOrchestrator orchestrator = new AiTestGenerationOrchestrator();

        AiTestGenerationOrchestrationResult result = orchestrator.generate(suite, request(), true);
        orchestrator.review(suite, result);
        orchestrator.approve(result);
        orchestrator.attachApproved(suite, result);

        assertTrue(result.isApproved());
        assertTrue(result.isAttached());
        assertEquals(result.getGeneratedTestCaseCount(), suite.getTestCases().size());
        assertTrue(result.getNegativeSuite().isEnabled());
        assertTrue(suite.getTestCases().stream()
                .allMatch(testCase -> testCase.getTestCaseId().contains("TC-001")));
    }

    @Test
    void shouldRejectApprovalWhenReviewFailed() {
        TestSuiteDto suite = targetSuite();
        TestCaseDto existing = new TestCaseDto();
        existing.setTestCaseId("TC-001-POSITIVE");
        suite.getTestCases().add(existing);

        AiTestGenerationOrchestrator orchestrator = new AiTestGenerationOrchestrator();
        AiTestGenerationOrchestrationResult result = orchestrator.generate(suite, request(), false);
        orchestrator.review(suite, result);

        assertFalse(result.isReviewPassed());
        assertTrue(result.getReviewFindings().stream()
                .anyMatch(finding -> finding.contains("already exists")));
        assertThrows(IllegalStateException.class, () -> orchestrator.approve(result));
    }

    private TestSuiteDto targetSuite() {
        TestSuiteDto suite = new TestSuiteDto();
        suite.setSuiteId("SUITE-001");
        suite.setSuiteName("User API Suite");
        return suite;
    }

    private AiTestGenerationRequest request() {
        AiTestGenerationRequest request = new AiTestGenerationRequest();
        request.setTestCaseIdPrefix("TC-001");
        request.setTestCaseNamePrefix("User API");
        request.setMethod("POST");
        request.setUrl("https://example.test/users");
        request.setExpectedStatusCode(201);
        request.setRequestBody("{\"name\":\"Test\",\"age\":25}");
        request.getHeaders().put("Content-Type", "application/json");
        return request;
    }
}
