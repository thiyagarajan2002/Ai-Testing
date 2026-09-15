package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryStoreLifecycleTest {

    @Test
    void shouldRejectOperationsAfterStoreIsClosed() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try {
            AiExecutionHistoryStore store = new AiExecutionHistoryStore("jdbc:sqlite:" + database);
            store.close();

            assertThrows(IllegalStateException.class, store::findAll);
            assertThrows(IllegalStateException.class, () -> store.findBySourceSuite("SUITE-01"));
            assertThrows(IllegalStateException.class, () -> store.findLatest("SUITE-01"));
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldPersistHistoryAcrossSeparateStoreInstances() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try {
            String databaseUrl = "jdbc:sqlite:" + database;
            AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
            entry.setExecutionId("PERSIST-001");
            entry.setExecutedAt(LocalDateTime.of(2026, 9, 15, 10, 0));
            entry.setSourceSuiteId("SUITE-PERSIST");
            entry.setSourceTestCaseId("TC-PERSIST");
            entry.setExecuted(true);
            entry.setPassed(false);
            entry.setTotalTestCases(4);
            entry.setPassedTestCases(3);
            entry.setFailedTestCases(1);
            entry.setSkippedTestCases(0);
            entry.setPassRate(75.0);
            entry.setFailedTestCaseIds(List.of("TC-FAIL"));

            try (AiExecutionHistoryStore firstStore = new AiExecutionHistoryStore(databaseUrl)) {
                firstStore.save(entry);
            }

            try (AiExecutionHistoryStore secondStore = new AiExecutionHistoryStore(databaseUrl)) {
                List<AiExecutionHistoryEntry> history = secondStore.findBySourceSuite("SUITE-PERSIST");

                assertEquals(1, history.size());
                AiExecutionHistoryEntry loaded = history.get(0);
                assertEquals("PERSIST-001", loaded.getExecutionId());
                assertEquals("SUITE-PERSIST", loaded.getSourceSuiteId());
                assertEquals("TC-PERSIST", loaded.getSourceTestCaseId());
                assertEquals(4, loaded.getTotalTestCases());
                assertEquals(3, loaded.getPassedTestCases());
                assertEquals(1, loaded.getFailedTestCases());
                assertEquals(0, loaded.getSkippedTestCases());
                assertEquals(75.0, loaded.getPassRate(), 0.0001);
                assertEquals(List.of("TC-FAIL"), loaded.getFailedTestCaseIds());
            }
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void shouldAllowClosingAnAlreadyClosedStoreWithoutChangingPersistence() throws Exception {
        Path database = Files.createTempFile("ai-history-", ".db");
        try {
            String databaseUrl = "jdbc:sqlite:" + database;
            AiExecutionHistoryStore store = new AiExecutionHistoryStore(databaseUrl);
            store.close();
            assertThrows(IllegalStateException.class, store::close);
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
