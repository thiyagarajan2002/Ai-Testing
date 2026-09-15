package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryServiceValidationTest {

    @Test
    void shouldRejectNullSourceSuiteIdForGetBySourceSuite() throws Exception {
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
    void shouldRejectBlankSourceSuiteIdForGetBySourceSuite() throws Exception {
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
}
