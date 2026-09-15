package org.ai.testing.ai;

import org.ai.testing.ai.history.AiExecutionHistoryEntry;
import org.ai.testing.ai.history.AiExecutionHistoryService;
import org.ai.testing.ai.history.AiExecutionHistoryStore;
import org.ai.testing.ai.model.AiGeneratedNegativeTestSuite;
import org.ai.testing.ai.model.AiGeneratedTestSuite;
import org.ai.testing.ai.model.AiTestGenerationOrchestrationResult;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiGeneratedSuiteExecutorHistoryIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAutomaticallyRecordApprovedPositiveExecution() {
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore(databaseUrl())) {
            AiExecutionHistoryService historyService = new AiExecutionHistoryService(store);
            AiGeneratedSuiteExecutor executor = new AiGeneratedSuiteExecutor(
                    new org.ai.testing.testcase.executor.TestCaseExecutor(), historyService);

            AiTestGenerationOrchestrationResult orchestration = approvedAttachedResult();
            AiGeneratedTestSuite positiveSuite = new AiGeneratedTestSuite();
            positiveSuite.setTestCases(List.of(disabledTestCase("TC-AI-001")));
            orchestration.setPositiveSuite(positiveSuite);

            var execution = executor.execute(orchestration);

            List<AiExecutionHistoryEntry> history = historyService.getBySourceSuite("SUITE-001");
            assertEquals(1, history.size());
            assertNotNull(history.get(0).getExecutionId());
            assertEquals("SUITE-001", history.get(0).getSourceSuiteId());
            assertEquals("TC-SOURCE", history.get(0).getSourceTestCaseId());
            assertEquals(execution.isPassed(), history.get(0).isPassed());
            assertEquals(execution.getTotalTestCases(), history.get(0).getTotalTestCases());
        }
    }

    @Test
    void shouldAutomaticallyRecordApprovedNegativeExecution() {
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore(databaseUrl())) {
            AiExecutionHistoryService historyService = new AiExecutionHistoryService(store);
            AiGeneratedSuiteExecutor executor = new AiGeneratedSuiteExecutor(
                    new org.ai.testing.testcase.executor.TestCaseExecutor(), historyService);

            AiTestGenerationOrchestrationResult orchestration = approvedAttachedResult();
            AiGeneratedNegativeTestSuite negativeSuite = new AiGeneratedNegativeTestSuite();
            negativeSuite.setEnabled(true);
            negativeSuite.setTestCases(List.of(disabledTestCase("TC-AI-NEG-001")));
            orchestration.setNegativeSuite(negativeSuite);

            var execution = executor.executeNegativeSuite(orchestration);

            List<AiExecutionHistoryEntry> history = historyService.getBySourceSuite("SUITE-001");
            assertEquals(1, history.size());
            assertTrue(history.get(0).getExecutionId().startsWith("AI-EXEC-"));
            assertEquals(execution.getTotalTestCases(), history.get(0).getTotalTestCases());
            assertEquals(execution.getSkippedTestCases(), history.get(0).getSkippedTestCases());
        }
    }

    private AiTestGenerationOrchestrationResult approvedAttachedResult() {
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

    private String databaseUrl() {
        return "jdbc:sqlite:" + tempDir.resolve("ai-history.db").toAbsolutePath();
    }
}
