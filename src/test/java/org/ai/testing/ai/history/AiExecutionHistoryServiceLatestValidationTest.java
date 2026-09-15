package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryServiceLatestValidationTest {

    @Test
    void shouldRejectNullSourceSuiteIdForGetLatest() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getLatest(null));

            assertEquals("source suite ID is required", exception.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldRejectBlankSourceSuiteIdForGetLatest() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getLatest("   "));

            assertEquals("source suite ID is required", exception.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
