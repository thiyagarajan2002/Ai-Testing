package org.ai.testing.report;

import org.ai.testing.ai.history.AiExecutionHistoryEntry;
import org.ai.testing.ai.history.AiExecutionHistoryTrend;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.AiHtmlReportGenerator;
import org.ai.testing.report.generator.CsvReportGenerator;
import org.ai.testing.report.generator.JsonReportGenerator;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryAwareReportIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldIncludeHistoryInHtmlJsonAndCsvReports() throws Exception {
        TestRunResultDto run = new TestRunResultDto();
        run.setRunId("RUN-HISTORY-01");
        run.setRunName("History Report Test");
        run.setEnvironment("QA");
        run.setExecutionMode("SEQUENTIAL");

        AiExecutionHistoryEntry first = history("AI-EXEC-001", "S1", 50.0, 1, LocalDateTime.of(2026, 9, 15, 10, 0));
        AiExecutionHistoryEntry latest = history("AI-EXEC-002", "S1", 100.0, 0, LocalDateTime.of(2026, 9, 16, 10, 0));
        List<AiExecutionHistoryEntry> history = List.of(first, latest);
        AiExecutionHistoryTrend trend = AiExecutionHistoryTrend.from("S1", history);

        Path html = tempDir.resolve("history.html");
        Path json = tempDir.resolve("history.json");
        Path csv = tempDir.resolve("history.csv");

        ReportService service = new ReportService(
                new AiHtmlReportGenerator(html),
                new JsonReportGenerator(json),
                new CsvReportGenerator(csv));

        TestReportDto report = service.generateAllReports(run, null, null, trend, history);

        assertNotNull(report.getAiHistoryMetadata());
        assertEquals("S1", report.getAiHistoryMetadata().getTrend().getSourceSuiteId());
        assertEquals(2, report.getAiHistoryMetadata().getTrend().getExecutionCount());
        assertEquals(50.0, report.getAiHistoryMetadata().getTrend().getPassRateChange());
        assertEquals(2, report.getAiHistoryMetadata().getHistory().size());

        assertTrue(Files.exists(html));
        assertTrue(Files.exists(json));
        assertTrue(Files.exists(csv));

        String htmlContent = Files.readString(html);
        assertTrue(htmlContent.contains("AI Historical Execution Dashboard"));
        assertTrue(htmlContent.contains("AI-EXEC-001"));
        assertTrue(htmlContent.contains("AI-EXEC-002"));
        assertTrue(htmlContent.contains("IMPROVED"));
        assertTrue(htmlContent.contains("ai-history-suite-filter"));
        assertTrue(htmlContent.contains("ai-history-status-filter"));
        assertTrue(htmlContent.contains("ai-history-search"));
        assertTrue(htmlContent.contains("ai-history-page-size"));
        assertTrue(htmlContent.contains("filterAiHistory()"));
        assertTrue(htmlContent.contains("changeAiHistoryPage"));
        assertTrue(htmlContent.contains("ai-history-chart"));
        assertTrue(htmlContent.contains("drawAiHistoryChart"));
        assertTrue(htmlContent.contains("Pass Rate Trend"));

        String jsonContent = Files.readString(json);
        assertTrue(jsonContent.contains("aiHistoryMetadata"));
        assertTrue(jsonContent.contains("AI-EXEC-001"));
        assertTrue(jsonContent.contains("AI-EXEC-002"));

        String csvContent = Files.readString(csv);
        assertTrue(csvContent.contains("AI History Execution Count"));
        assertTrue(csvContent.contains("AI History Latest Pass Rate"));
        assertTrue(csvContent.contains("AI History Trend"));
        assertTrue(csvContent.contains("AI-EXEC-001"));
        assertTrue(csvContent.contains("AI-EXEC-002"));
        assertTrue(csvContent.contains("IMPROVED"));
    }

    private AiExecutionHistoryEntry history(String id, String suiteId, double passRate, int failed, LocalDateTime time) {
        AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
        entry.setExecutionId(id);
        entry.setExecutedAt(time);
        entry.setSourceSuiteId(suiteId);
        entry.setExecuted(true);
        entry.setPassed(failed == 0);
        entry.setTotalTestCases(2);
        entry.setPassedTestCases(passRate == 100.0 ? 2 : 1);
        entry.setFailedTestCases(failed);
        entry.setSkippedTestCases(0);
        entry.setPassRate(passRate);
        return entry;
    }
}
