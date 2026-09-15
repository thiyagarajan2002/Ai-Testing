package org.ai.testing.report.service;

import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiReportInsightBuilderTest {

    @Test
    void shouldReportHighSeverityForFailures() {
        TestRunResultDto run = new TestRunResultDto();
        run.setFailedTestCases(2);
        run.setFailedSuites(1);

        TestReportDto report = report(run);
        new AiReportInsightBuilder().populate(report);

        assertEquals("HIGH", report.getAiSeverity());
        assertTrue(report.getAiSummary().contains("failed API tests"));
        assertFalse(report.getAiRecommendations().isEmpty());
    }

    @Test
    void shouldReportMediumSeverityForSkippedTests() {
        TestRunResultDto run = new TestRunResultDto();
        run.setSkippedTestCases(1);

        TestReportDto report = report(run);
        new AiReportInsightBuilder().populate(report);

        assertEquals("MEDIUM", report.getAiSeverity());
        assertTrue(report.getAiSummary().contains("skipped API tests"));
    }

    @Test
    void shouldReportInfoForCleanRun() {
        TestRunResultDto run = new TestRunResultDto();

        TestReportDto report = report(run);
        new AiReportInsightBuilder().populate(report);

        assertEquals("INFO", report.getAiSeverity());
        assertTrue(report.getAiSummary().contains("no failed or skipped tests"));
    }

    @Test
    void shouldRejectMissingReport() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiReportInsightBuilder().populate(null));
    }

    private TestReportDto report(TestRunResultDto run) {
        TestReportDto report = new TestReportDto();
        report.setTestRunResult(run);
        return report;
    }
}
