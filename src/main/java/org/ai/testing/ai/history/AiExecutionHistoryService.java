package org.ai.testing.ai.history;

import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.ai.model.AiTestGenerationOrchestrationResult;

import java.util.List;

/**
 * Application service for persistent AI execution history.
 */
public class AiExecutionHistoryService {
    private final AiExecutionHistoryStore store;

    public AiExecutionHistoryService(AiExecutionHistoryStore store) {
        if (store == null) {
            throw new IllegalArgumentException("history store is required");
        }
        this.store = store;
    }

    public AiExecutionHistoryEntry record(AiGeneratedSuiteExecutionResult executionResult) {
        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(executionResult);
        store.save(entry);
        return entry;
    }

    public AiExecutionHistoryEntry recordApprovedExecution(
            AiTestGenerationOrchestrationResult generationResult,
            AiGeneratedSuiteExecutionResult executionResult) {
        if (generationResult == null) {
            throw new IllegalArgumentException("generation result is required");
        }
        if (!generationResult.isApproved()) {
            throw new IllegalStateException("AI generation must be approved before history recording");
        }
        if (!generationResult.isAttached()) {
            throw new IllegalStateException("AI generation must be attached before history recording");
        }
        return record(executionResult);
    }

    public List<AiExecutionHistoryEntry> getAll() {
        return store.findAll();
    }

    public List<AiExecutionHistoryEntry> getBySourceSuite(String sourceSuiteId) {
        if (sourceSuiteId == null || sourceSuiteId.isBlank()) {
            throw new IllegalArgumentException("source suite ID is required");
        }
        return store.findBySourceSuite(sourceSuiteId);
    }

    public AiExecutionHistoryEntry getLatest(String sourceSuiteId) {
        if (sourceSuiteId == null || sourceSuiteId.isBlank()) {
            throw new IllegalArgumentException("source suite ID is required");
        }
        return store.findLatest(sourceSuiteId);
    }

    public AiExecutionHistoryComparison compareLatest(String sourceSuiteId) {
        List<AiExecutionHistoryEntry> history = getBySourceSuite(sourceSuiteId);
        if (history.size() < 2) {
            throw new IllegalStateException("At least two executions are required for comparison");
        }
        return AiExecutionHistoryComparison.compare(
                history.get(history.size() - 2), history.get(history.size() - 1));
    }
}
