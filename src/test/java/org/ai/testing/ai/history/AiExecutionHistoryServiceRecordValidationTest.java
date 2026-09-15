package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryServiceRecordValidationTest {

    @Test
    void shouldRejectNullExecutionResultForRecord() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            AiExecutionHistoryService service = new AiExecutionHistoryService(store);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.record(null));

            assertEquals("execution result is required", exception.getMessage());
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
