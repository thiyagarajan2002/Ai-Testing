package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryServiceTest {
    @Test
    void shouldRejectNullSourceSuiteId() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getBySourceSuite(null));

            assertEquals("source suite ID is required", exception.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectBlankSourceSuiteId() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getBySourceSuite("   "));

            assertEquals("source suite ID is required", exception.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectInvalidSourceSuiteIdForLatestLookup() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            assertThrows(IllegalArgumentException.class, () -> service.getLatest(null));
            assertThrows(IllegalArgumentException.class, () -> service.getLatest(""));
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectInvalidSourceSuiteIdForComparison() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            assertThrows(IllegalArgumentException.class, () -> service.compareLatest(null));
            assertThrows(IllegalArgumentException.class, () -> service.compareLatest("   "));
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
