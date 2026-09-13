package org.ai.testing.report.service;


import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.ReportGenerator;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ReportServiceTest {

    private ReportService reportService;

    private CapturingReportGenerator htmlGenerator;
    private CapturingReportGenerator jsonGenerator;
    private CapturingReportGenerator csvGenerator;

    @BeforeEach
    void setUp() {
        htmlGenerator = new CapturingReportGenerator();
        jsonGenerator = new CapturingReportGenerator();
        csvGenerator = new CapturingReportGenerator();

        reportService = new ReportService(
                htmlGenerator,
                jsonGenerator,
                csvGenerator
        );
    }

    @Test
    public void shouldGenerateHtmlReport() {
        TestRunResultDto result = createTestRunResult();

        TestReportDto report = reportService.generateHtmlReport(result);

        assertNotNull(report);

        assertNotNull(report.getReportId());
        assertTrue(report.getReportId().startsWith("REPORT-HTML-"));

        assertEquals("Sample API Run - Report", report.getReportName());
        assertEquals("HTML", report.getReportFormat());
        assertNotNull(report.getGeneratedAt());
        assertSame(result, report.getTestRunResult());

        assertNotNull(htmlGenerator.capturedReport);
        assertSame(report, htmlGenerator.capturedReport);

        assertNull(jsonGenerator.capturedReport);
        assertNull(csvGenerator.capturedReport);
    }

    @Test
    public void shouldGenerateJsonReport() {
        TestRunResultDto result = createTestRunResult();

        TestReportDto report = reportService.generateJsonReport(result);

        assertNotNull(report);

        assertNotNull(report.getReportId());
        assertTrue(report.getReportId().startsWith("REPORT-JSON-"));

        assertEquals("Sample API Run - Report", report.getReportName());
        assertEquals("JSON", report.getReportFormat());
        assertNotNull(report.getGeneratedAt());
        assertSame(result, report.getTestRunResult());

        assertNotNull(jsonGenerator.capturedReport);
        assertSame(report, jsonGenerator.capturedReport);

        assertNull(htmlGenerator.capturedReport);
        assertNull(csvGenerator.capturedReport);
    }

    @Test
    public void shouldGenerateCsvReport() {
        TestRunResultDto result = createTestRunResult();

        TestReportDto report = reportService.generateCsvReport(result);

        assertNotNull(report);

        assertNotNull(report.getReportId());
        assertTrue(report.getReportId().startsWith("REPORT-CSV-"));

        assertEquals("Sample API Run - Report", report.getReportName());
        assertEquals("CSV", report.getReportFormat());
        assertNotNull(report.getGeneratedAt());
        assertSame(result, report.getTestRunResult());

        assertNotNull(csvGenerator.capturedReport);
        assertSame(report, csvGenerator.capturedReport);

        assertNull(htmlGenerator.capturedReport);
        assertNull(jsonGenerator.capturedReport);
    }

    @Test
    public void shouldGenerateAllReports() {
        TestRunResultDto result = createTestRunResult();

        TestReportDto returnedReport =
                reportService.generateAllReports(result);

        assertNotNull(returnedReport);

        assertEquals("HTML", returnedReport.getReportFormat());
        assertEquals("Sample API Run - Report", returnedReport.getReportName());
        assertSame(result, returnedReport.getTestRunResult());

        assertNotNull(htmlGenerator.capturedReport);
        assertNotNull(jsonGenerator.capturedReport);
        assertNotNull(csvGenerator.capturedReport);

        assertEquals("HTML",
                htmlGenerator.capturedReport.getReportFormat());

        assertEquals("JSON",
                jsonGenerator.capturedReport.getReportFormat());

        assertEquals("CSV",
                csvGenerator.capturedReport.getReportFormat());

        assertSame(result,
                htmlGenerator.capturedReport.getTestRunResult());

        assertSame(result,
                jsonGenerator.capturedReport.getTestRunResult());

        assertSame(result,
                csvGenerator.capturedReport.getTestRunResult());
    }

    @Test
    public void shouldGenerateDifferentReportIdsForEachFormat() {
        TestRunResultDto result = createTestRunResult();

        reportService.generateAllReports(result);

        String htmlReportId =
                htmlGenerator.capturedReport.getReportId();

        String jsonReportId =
                jsonGenerator.capturedReport.getReportId();

        String csvReportId =
                csvGenerator.capturedReport.getReportId();

        assertNotNull(htmlReportId);
        assertNotNull(jsonReportId);
        assertNotNull(csvReportId);

        assertNotEquals(htmlReportId, jsonReportId);
        assertNotEquals(htmlReportId, csvReportId);
        assertNotEquals(jsonReportId, csvReportId);

        assertTrue(htmlReportId.startsWith("REPORT-HTML-"));
        assertTrue(jsonReportId.startsWith("REPORT-JSON-"));
        assertTrue(csvReportId.startsWith("REPORT-CSV-"));
    }

    @Test
    public void shouldUseDefaultRunNameWhenRunNameIsBlank() {
        TestRunResultDto result = new TestRunResultDto();
        result.setRunName("");

        TestReportDto report =
                reportService.generateHtmlReport(result);

        assertEquals(
                "API Test Run - Report",
                report.getReportName()
        );
    }

    @Test
    public void shouldUseDefaultRunNameWhenRunNameIsNull() {
        TestRunResultDto result = new TestRunResultDto();
        result.setRunName(null);

        TestReportDto report =
                reportService.generateHtmlReport(result);

        assertEquals(
                "API Test Run - Report",
                report.getReportName()
        );
    }

    @Test
    public void shouldRejectNullTestRunResultForHtmlReport() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> reportService.generateHtmlReport(null)
                );

        assertEquals(
                "Test run result cannot be null",
                exception.getMessage()
        );
    }

    @Test
    public void shouldRejectNullTestRunResultForJsonReport() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> reportService.generateJsonReport(null)
                );

        assertEquals(
                "Test run result cannot be null",
                exception.getMessage()
        );
    }

    @Test
    public void shouldRejectNullTestRunResultForCsvReport() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> reportService.generateCsvReport(null)
                );

        assertEquals(
                "Test run result cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullTestRunResultForAllReports() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> reportService.generateAllReports(null)
                );

        assertEquals(
                "Test run result cannot be null",
                exception.getMessage()
        );

        assertNull(htmlGenerator.capturedReport);
        assertNull(jsonGenerator.capturedReport);
        assertNull(csvGenerator.capturedReport);
    }

    @Test
    public void shouldRejectNullHtmlGenerator() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ReportService(
                                null,
                                jsonGenerator,
                                csvGenerator
                        )
                );

        assertEquals(
                "HTML report generator cannot be null",
                exception.getMessage()
        );
    }

    @Test
    public void shouldRejectNullJsonGenerator() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ReportService(
                                htmlGenerator,
                                null,
                                csvGenerator
                        )
                );

        assertEquals(
                "JSON report generator cannot be null",
                exception.getMessage()
        );
    }

    @Test
    public void shouldRejectNullCsvGenerator() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ReportService(
                                htmlGenerator,
                                jsonGenerator,
                                null
                        )
                );

        assertEquals(
                "CSV report generator cannot be null",
                exception.getMessage()
        );
    }

    @Test
    public void shouldGenerateAllReportsOnlyOncePerGenerator() {
        TestRunResultDto result = createTestRunResult();

        reportService.generateAllReports(result);

        assertEquals(1, htmlGenerator.generateCount);
        assertEquals(1, jsonGenerator.generateCount);
        assertEquals(1, csvGenerator.generateCount);
    }

    private TestRunResultDto createTestRunResult() {
        TestRunResultDto result = new TestRunResultDto();

        result.setRunId("RUN-001");
        result.setRunName("Sample API Run");
        result.setEnvironment("QA");
        result.setExecutionMode("SEQUENTIAL");
        result.setExecuted(true);
        result.setPassed(true);

        return result;
    }

    private static class CapturingReportGenerator
            implements ReportGenerator {

        private TestReportDto capturedReport;
        private int generateCount;

        @Override
        public void generate(TestReportDto report) {
            this.capturedReport = report;
            this.generateCount++;
        }
    }
}