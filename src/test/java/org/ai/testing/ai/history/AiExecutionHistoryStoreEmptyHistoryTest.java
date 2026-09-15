package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiExecutionHistoryStoreEmptyHistoryTest {

    @Test
    void shouldReturnEmptyListWhenNoHistoryExists() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            List<AiExecutionHistoryEntry> history = store.findAll();

            assertNotNull(history);
            assertTrue(history.isEmpty());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldReturnEmptyListWhenSourceSuiteHasNoHistory() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            List<AiExecutionHistoryEntry> history = store.findBySourceSuite("SUITE-NOT-FOUND");

            assertNotNull(history);
            assertTrue(history.isEmpty());
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
