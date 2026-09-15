package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

class AiExecutionHistoryStoreEmptyLatestTest {

    @Test
    void shouldReturnNullWhenLatestExecutionIsRequestedFromEmptyHistory() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try (AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database)) {
            assertNull(store.findLatest("SUITE-NOT-FOUND"));
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
