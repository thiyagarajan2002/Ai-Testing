package org.ai.testing.ai.history;

import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.ai.model.AiTestGenerationOrchestrationResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryServiceApprovedExecutionTest {

    @Test
    void shouldRecordExecutionWhenGenerationIsApprovedAndAttached() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);
            AiTestGenerationOrchestrationResult generation = new AiTestGenerationOrchestrationResult();
            generation.setSourceSuiteId("SUITE-APPROVED");
            generation.setApproved(true);
            generation.setAttached(true);

            AiGeneratedSuiteExecutionResult execution = new AiGeneratedSuiteExecutionResult();
            execution.setSourceSuiteId("SUITE-APPROVED");
            execution.setSourceTestCaseId("TC-APPROVED");

            AiExecutionHistoryEntry entry = service.recordApprovedExecution(generation, execution);

            assertEquals("SUITE-APPROVED", entry.getSourceSuiteId());
            assertEquals("TC-APPROVED", entry.getSourceTestCaseId());
            assertEquals(1, service.getBySourceSuite("SUITE-APPROVED").size());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectRecordingBeforeApproval() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);
            AiTestGenerationOrchestrationResult generation = new AiTestGenerationOrchestrationResult();
            generation.setApproved(false);
            generation.setAttached(true);

            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> service.recordApprovedExecution(generation, new AiGeneratedSuiteExecutionResult()));

            assertEquals("AI generation must be approved before history recording", error.getMessage());
            assertEquals(0, service.getAll().size());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectRecordingBeforeAttachment() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);
            AiTestGenerationOrchestrationResult generation = new AiTestGenerationOrchestrationResult();
            generation.setApproved(true);
            generation.setAttached(false);

            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> service.recordApprovedExecution(generation, new AiGeneratedSuiteExecutionResult()));

            assertEquals("AI generation must be attached before history recording", error.getMessage());
            assertEquals(0, service.getAll().size());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectNullGenerationResult() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            IllegalArgumentException error = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.recordApprovedExecution(null, new AiGeneratedSuiteExecutionResult()));

            assertEquals("generation result is required", error.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
