package org.ai.testing.ai;

import org.ai.testing.ai.model.AiExecutionHistoryComparison;
import org.ai.testing.ai.model.AiExecutionHistoryEntry;
import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;

import java.util.List;

/**
 * Coordinates recording and comparison of isolated AI-generated executions.
 */
public class AiExecutionHistoryService {

    private final AiExecutionHistoryStore store;

    public AiExecutionHistoryService() {
        this(new AiExecutionHistoryStore());
    }

    public AiExecutionHistoryService(AiExecutionHistoryStore store) {
        if (store == null) {
            throw new IllegalArgumentException("History store cannot be null");
        }
        this.store = store;
    }

    public AiExecutionHistoryEntry record(AiGeneratedSuiteExecutionResult result) {
        return store.record(result);
    }

    public List<AiExecutionHistoryEntry> getHistory() {
        return store.findAll();
    }

    public List<AiExecutionHistoryEntry> getHistory(String sourceSuiteId) {
        return store.findBySourceSuite(sourceSuiteId);
    }

    public AiExecutionHistoryComparison compareLatest(String sourceSuiteId) {
        List<AiExecutionHistoryEntry> history = getHistory(sourceSuiteId);
        if (history.size() < 2) {
            throw new IllegalStateException("At least two AI executions are required for comparison");
        }
        return AiExecutionHistoryComparison.compare(
                history.get(history.size() - 2),
                history.get(history.size() - 1));
    }

    public void clearHistory() {
        store.clear();
    }
}
