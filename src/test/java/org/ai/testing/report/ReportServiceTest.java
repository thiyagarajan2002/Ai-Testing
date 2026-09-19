package org.ai.testing.report;

import org.ai.testing.TestFixtures;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.ReportGenerationException;
import org.ai.testing.report.generator.ReportGenerator;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Report service")
class ReportServiceTest {

    private TestRunResultDto run() {
        return TestFixtures.runResult("Service run",
                TestFixtures.suiteResult("Suite A",
                        TestFixtures.caseResult("TC-1", "passing", TestStatus.PASSED),
                        TestFixtures.caseResult("TC-2", "failing", TestStatus.FAILED)));
    }

    @Test
    @DisplayName("writes all five formats")
    void writesEveryFormat(@TempDir Path directory) {
        Map<String, Path> written = new ReportService(directory).generateAllReports(run());

        assertEquals(List.of("HTML", "JSON", "CSV", "MARKDOWN", "JUNIT-XML"),
                List.copyOf(written.keySet()));
        written.values().forEach(path ->
                assertTrue(Files.isRegularFile(path), path + " was not written"));
        assertTrue(Files.isRegularFile(directory.resolve("test-report.html")));
        assertTrue(Files.isRegularFile(directory.resolve("junit-report.xml")));
    }

    @Test
    @DisplayName("creates the output directory when it does not exist")
    void createsMissingDirectory(@TempDir Path directory) {
        Path nested = directory.resolve("build").resolve("reports");
        new ReportService(nested).generateAllReports(run());
        assertTrue(Files.isDirectory(nested));
    }

    @Test
    @DisplayName("shares one report id and timestamp across every format")
    void sharesOneEnvelope() {
        // Regression: the previous service created a fresh envelope per format,
        // so the HTML, JSON and CSV files could not be correlated.
        List<TestReportDto> seen = new ArrayList<>();
        List<ReportGenerator> capturing = List.of(
                capture("A", seen), capture("B", seen), capture("C", seen));

        new ReportService(capturing).generateAllReports(run());

        assertEquals(3, seen.size());
        assertEquals(1, seen.stream().map(TestReportDto::getReportId).distinct().count());
        assertEquals(1, seen.stream().map(TestReportDto::getGeneratedAt).distinct().count());
        assertEquals(List.of("A", "B", "C"),
                seen.stream().map(TestReportDto::getReportFormat).toList());
    }

    @Test
    @DisplayName("keeps going when one format fails, then reports the failure")
    void collectsGeneratorFailures(@TempDir Path directory) {
        List<TestReportDto> seen = new ArrayList<>();
        ReportGenerator broken = new ReportGenerator() {
            @Override
            public void generate(TestReportDto report) {
                throw new ReportGenerationException("disk full");
            }

            @Override
            public Path outputPath() {
                return directory.resolve("broken.txt");
            }

            @Override
            public String format() {
                return "BROKEN";
            }
        };

        ReportService service = new ReportService(List.of(broken, capture("OK", seen)));
        ReportGenerationException failure = assertThrows(ReportGenerationException.class,
                () -> service.generateAllReports(run()));

        assertTrue(failure.getMessage().contains("BROKEN"));
        assertEquals(1, seen.size(), "the healthy generator should still have run");
    }

    @Test
    @DisplayName("derives a severity from the failure rate")
    void derivesSeverity() {
        ReportService service = new ReportService();
        assertEquals("HIGH", service.createReport(run()).getSeverity());

        TestRunResultDto clean = TestFixtures.runResult("Clean",
                TestFixtures.suiteResult("S",
                        TestFixtures.caseResult("TC-1", "ok", TestStatus.PASSED)));
        assertEquals("NONE", service.createReport(clean).getSeverity());
    }

    @Test
    @DisplayName("rejects a null run and an unknown format")
    void rejectsBadInput() {
        ReportService service = new ReportService();
        assertThrows(IllegalArgumentException.class, () -> service.generateAllReports(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.generateReport(run(), "PDF"));
        assertThrows(IllegalArgumentException.class, () -> new ReportService(List.of()));
    }

    private ReportGenerator capture(String format, List<TestReportDto> sink) {
        return new ReportGenerator() {
            @Override
            public void generate(TestReportDto report) {
                TestReportDto snapshot = new TestReportDto();
                snapshot.setReportId(report.getReportId());
                snapshot.setGeneratedAt(report.getGeneratedAt());
                snapshot.setReportFormat(report.getReportFormat());
                sink.add(snapshot);
            }

            @Override
            public Path outputPath() {
                return Path.of("memory", format);
            }

            @Override
            public String format() {
                return format;
            }
        };
    }
}
