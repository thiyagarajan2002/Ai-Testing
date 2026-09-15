package org.ai.testing.ai;

import org.ai.testing.ai.model.AiExecutionHistoryEntry;
import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * In-memory history for AI-generated suite executions.
 * This is intentionally isolated from normal regression results.
 */
public class AiExecutionHistoryStore {

    private final List<AiExecutionHistoryEntry> entries = new ArrayList<>();

    public synchronized AiExecutionHistoryEntry record(AiGeneratedSuiteExecutionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Execution result cannot be null");
        }
        AiExecutionHistoryEntry entry = AiExecutionHistoryEntry.from(
                "AI-EXEC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                LocalDateTime.now(),
                result);
        entries.add(entry);
        return entry;
    }

    public synchronized List<AiExecutionHistoryEntry> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized List<AiExecutionHistoryEntry> findBySourceSuite(String sourceSuiteId) {
        if (sourceSuiteId == null) {
            return List.of();
        }
        return Collections.unmodifiableList(entries.stream()
                .filter(entry -> sourceSuiteId.equals(entry.getSourceSuiteId()))
                .toList());
    }

    public synchronized void clear() {
        entries.clear();
    }
}
