package org.ai.testing.ai.history;

import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;

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
