package org.ai.testing.report.generator;

import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JsonReportGeneratorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldGenerateJsonReport() throws Exception {

        Path outputFile =
                tempDirectory.resolve("test-report.json");

        JsonReportGenerator generator =
                new JsonReportGenerator(outputFile);

        TestReportDto report =
                createTestReport();

        generator.generate(report);

        assertTrue(
                Files.exists(outputFile),
                "JSON report should be created"
        );

        String json =
                Files.readString(outputFile);

        assertFalse(json.isBlank());

        assertTrue(
                json.contains("\"reportId\"")
        );

        assertTrue(
                json.contains("REPORT-001")
        );

        assertTrue(
                json.contains("\"reportFormat\"")
        );

        assertTrue(
                json.contains("JSON")
        );

        assertTrue(
                json.contains("\"runId\"")
        );

        assertTrue(
                json.contains("RUN-001")
        );

        assertTrue(
                json.contains("\"environment\"")
        );

        assertTrue(
                json.contains("QA")
        );
    }

    @Test
    void shouldCreateParentDirectories() throws Exception {

        Path outputFile =
                tempDirectory
                        .resolve("reports")
                        .resolve("json")
                        .resolve("test-report.json");

        JsonReportGenerator generator =
                new JsonReportGenerator(outputFile);

        generator.generate(createTestReport());

        assertTrue(
                Files.exists(outputFile)
        );
    }

    @Test
    void shouldRejectNullReport() {

        Path outputFile =
                tempDirectory.resolve("test-report.json");

        JsonReportGenerator generator =
                new JsonReportGenerator(outputFile);

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(null)
        );
    }

    @Test
    void shouldRejectReportWithoutTestRunResult() {

        Path outputFile =
                tempDirectory.resolve("test-report.json");

        JsonReportGenerator generator =
                new JsonReportGenerator(outputFile);

        TestReportDto report =
                new TestReportDto();

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(report)
        );
    }

    private TestReportDto createTestReport() {

        TestReportDto report =
                new TestReportDto();

        report.setReportId("REPORT-001");
        report.setReportName("API Test Report");
        report.setReportFormat("JSON");
        report.setGeneratedAt(LocalDateTime.now());

        TestRunResultDto run =
                new TestRunResultDto();

        run.setRunId("RUN-001");
        run.setRunName("User API Tests");
        run.setEnvironment("QA");
        run.setExecutionMode("AUTOMATED");
        run.setExecuted(true);
        run.setPassed(true);
        run.setMessage("Test run passed");
        run.setExecutionTimeMs(1250);

        run.setTotalSuites(1);
        run.setPassedSuites(1);
        run.setFailedSuites(0);
        run.setSkippedSuites(0);

        run.setTotalTestCases(1);
        run.setPassedTestCases(1);
        run.setFailedTestCases(0);
        run.setSkippedTestCases(0);

        report.setTestRunResult(run);

        return report;
    }
}