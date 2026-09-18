package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiHistoryAnalyticsServiceTest {

    private final AiHistoryAnalyticsService service = new AiHistoryAnalyticsService();

    @Test
    void shouldFilterBySuiteStatusAndDateRange() {
        AiExecutionHistoryEntry first = entry("E1", "S1", 50, 1, true, LocalDateTime.of(2026, 9, 10, 10, 0));
        AiExecutionHistoryEntry second = entry("E2", "S2", 100, 0, false, LocalDateTime.of(2026, 9, 11, 10, 0));

        List<AiExecutionHistoryEntry> result = service.filter(
                List.of(first, second), "S1", "FAILED",
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10));

        assertEquals(1, result.size());
        assertEquals("E1", result.get(0).getExecutionId());
    }

    @Test
    void shouldRejectInvalidDateRange() {
        assertThrows(IllegalArgumentException.class, () ->
                service.filter(List.of(), null, "ALL",
                        LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 10)));
    }

    @Test
    void shouldCompareSuites() {
        AiExecutionHistoryEntry a = entry("A", "S1", 50, 1, false, LocalDateTime.of(2026, 9, 10, 10, 0));
        AiExecutionHistoryEntry b = entry("B", "S1", 100, 0, true, LocalDateTime.of(2026, 9, 11, 10, 0));
        AiExecutionHistoryEntry c = entry("C", "S2", 75, 1, false, LocalDateTime.of(2026, 9, 11, 11, 0));

        Map<String, AiHistoryAnalyticsService.SuiteHistorySummary> result =
                service.compareSuites(List.of(a, b, c));

        assertEquals(2, result.size());
        assertEquals(2, result.get("S1").executionCount());
        assertEquals(75.0, result.get("S1").averagePassRate());
        assertEquals(100.0, result.get("S1").latestPassRate());
        assertEquals(1, result.get("S1").totalFailedTests());
    }

    @Test
    void shouldExportCsv() {
        AiExecutionHistoryEntry entry = entry(
                "E1", "S1", 87.5, 1, false,
                LocalDateTime.of(2026, 9, 10, 10, 0));
        entry.setSourceTestCaseId("TC-1");

        String csv = service.exportCsv(List.of(entry));

        assertTrue(csv.startsWith("Execution ID,Suite,Source Test Case,Executed At,Pass Rate,Failed,Skipped,Status"));
        assertTrue(csv.contains(""E1""));
        assertTrue(csv.contains(""S1""));
        assertTrue(csv.contains("87.50"));
        assertTrue(csv.contains("FAILED"));
    }

    private AiExecutionHistoryEntry entry(
            String id, String suite, double rate, int failed,
            boolean passed, LocalDateTime time) {
        AiExecutionHistoryEntry e = new AiExecutionHistoryEntry();
        e.setExecutionId(id);
        e.setSourceSuiteId(suite);
        e.setExecutedAt(time);
        e.setPassRate(rate);
        e.setFailedTestCases(failed);
        e.setPassed(passed);
        return e;
    }
}
