package org.ai.testing.report.generator;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.validation.AssertionType;
import org.ai.testing.validation.dto.ValidationResultDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CsvReportGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateCsvReportWithRequestAndResponseDetails() throws Exception {
        Path outputPath = tempDir.resolve("test-report.csv");
        CsvReportGenerator generator = new CsvReportGenerator(outputPath);

        generator.generate(createReport());

        assertTrue(Files.exists(outputPath));
        String csv = Files.readString(outputPath);

        assertTrue(csv.contains("Request URL"));
        assertTrue(csv.contains("Request Headers"));
        assertTrue(csv.contains("Query Params"));
        assertTrue(csv.contains("Path Params"));
        assertTrue(csv.contains("Request Content Type"));
        assertTrue(csv.contains("Request Body"));
        assertTrue(csv.contains("Response Headers"));
        assertTrue(csv.contains("Response Body"));
        assertTrue(csv.contains("REPORT-CSV-001"));
        assertTrue(csv.contains("RUN-001"));
        assertTrue(csv.contains("Sample API Run"));
        assertTrue(csv.contains("https://example.test/users/1"));
        assertTrue(csv.contains("Authorization=Bearer test-token"));
        assertTrue(csv.contains("application/json"));
        assertTrue(csv.contains("\"{\"\"name\"\":\"\"test\"\"}\""));
        assertTrue(csv.contains("content-type=application/json"));
        assertTrue(csv.contains("{\"\"id\"\":1}"));
        assertTrue(csv.contains("200"));
        assertTrue(csv.contains("45"));
        assertTrue(csv.contains("STATUS_CODE"));
        assertTrue(csv.contains("PASSED"));
    }

    @Test
    void shouldCreateParentDirectories() throws Exception {
        Path outputPath = tempDir.resolve("reports/csv/test-report.csv");
        new CsvReportGenerator(outputPath).generate(createReport());
        assertTrue(Files.exists(outputPath));
    }

    @Test
    void shouldGenerateHeaderWhenSuiteResultsAreEmpty() throws Exception {
        Path outputPath = tempDir.resolve("empty-report.csv");
        TestReportDto report = createReport();
        report.getTestRunResult().getSuiteResults().clear();

        new CsvReportGenerator(outputPath).generate(report);

        String csv = Files.readString(outputPath);
        assertTrue(csv.startsWith("Report ID,Run ID,Environment,Execution Mode"));
        assertEquals(1, csv.lines().count());
    }

    @Test
    void shouldGenerateSuiteRowWhenTestResultsAreEmpty() throws Exception {
        Path outputPath = tempDir.resolve("suite-report.csv");
        TestReportDto report = createReport();
        report.getTestRunResult().getSuiteResults().get(0).getTestResults().clear();

        new CsvReportGenerator(outputPath).generate(report);

        String csv = Files.readString(outputPath);
        assertTrue(csv.contains("SUITE-001"));
        assertTrue(csv.contains("Sample Suite"));
        assertTrue(csv.contains("PASSED"));
    }

    @Test
    void shouldGenerateRowWithoutValidationResults() throws Exception {
        Path outputPath = tempDir.resolve("no-validation-report.csv");
        TestReportDto report = createReport();
        TestCaseExecutor.TestCaseExecutionResult testCase =
                report.getTestRunResult().getSuiteResults().get(0).getTestResults().get(0);
        testCase.setValidationSummary(null);

        new CsvReportGenerator(outputPath).generate(report);

        String csv = Files.readString(outputPath);
        assertTrue(csv.contains("TC-001"));
        assertTrue(csv.contains("Get User"));
        assertTrue(csv.contains("PASSED"));
        assertTrue(csv.contains("200"));
        assertTrue(csv.contains("45"));
        assertTrue(csv.contains("Authorization=Bearer test-token"));
    }

    @Test
    void shouldEscapeCsvValues() throws Exception {
        Path outputPath = tempDir.resolve("escaped-report.csv");
        TestReportDto report = createReport();
        report.setReportId("REPORT,CSV,\"001\"");
        report.getTestRunResult().setRunName("Run, \"Sample\"");
        report.getTestRunResult().getSuiteResults().get(0).setSuiteName("Suite, \"QA\"");

        new CsvReportGenerator(outputPath).generate(report);

        String csv = Files.readString(outputPath);
        assertTrue(csv.contains("\"REPORT,CSV,\"\"001\"\"\""));
        assertTrue(csv.contains("\"Run, \"\"Sample\"\"\""));
        assertTrue(csv.contains("\"Suite, \"\"QA\"\"\""));
    }

    @Test
    void shouldRejectNullReport() {
        CsvReportGenerator generator = new CsvReportGenerator(tempDir.resolve("test-report.csv"));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> generator.generate(null));
        assertEquals("Report cannot be null", exception.getMessage());
    }

    @Test
    void shouldRejectReportWithoutTestRunResult() {
        CsvReportGenerator generator = new CsvReportGenerator(tempDir.resolve("test-report.csv"));
        TestReportDto report = new TestReportDto();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> generator.generate(report));
        assertEquals("Test run result cannot be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullOutputPath() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new CsvReportGenerator(null));
        assertEquals("Output path cannot be null", exception.getMessage());
    }

    private TestReportDto createReport() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://example.test/users/1");
        request.getHeaders().put("Authorization", "Bearer test-token");
        request.getQueryParams().put("active", "true");
        request.getPathParams().put("id", "1");

        RequestBodyDto requestBody = new RequestBodyDto();
        requestBody.setContentType("application/json");
        requestBody.setRawBody("{\"name\":\"test\"}");
        request.setBody(requestBody);

        ResponseDto response = new ResponseDto();
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.setBody("{\"id\":1}");
        response.setResponseTimeMs(45);
        response.getHeaders().put("content-type", "application/json");

        ValidationResultDto validation = new ValidationResultDto();
        validation.setPassed(true);
        validation.setValidationType(AssertionType.STATUS_CODE.name());
        validation.setField("statusCode");
        validation.setExpected("200");
        validation.setActual("200");
        validation.setMessage("Status code validation passed");

        ValidationSummaryDto validationSummary = new ValidationSummaryDto();
        validationSummary.setPassed(true);
        validationSummary.setTotal(1);
        validationSummary.setPassedCount(1);
        validationSummary.setFailedCount(0);
        validationSummary.getResults().add(validation);

        TestCaseExecutor.TestCaseExecutionResult testCase =
                new TestCaseExecutor.TestCaseExecutionResult();
        testCase.setTestCaseId("TC-001");
        testCase.setTestCaseName("Get User");
        testCase.setExecuted(true);
        testCase.setPassed(true);
        testCase.setMessage("Test case passed");
        testCase.setRequest(request);
        testCase.setResponse(response);
        testCase.setValidationSummary(validationSummary);

        TestSuiteExecutionResultDto suite = new TestSuiteExecutionResultDto();
        suite.setSuiteId("SUITE-001");
        suite.setSuiteName("Sample Suite");
        suite.setExecuted(true);
        suite.setPassed(true);
        suite.setMessage("Suite passed");
        suite.setExecutionTimeMs(50);
        suite.setTotalTestCases(1);
        suite.setPassedTestCases(1);
        suite.getTestResults().add(testCase);

        TestRunResultDto run = new TestRunResultDto();
        run.setRunId("RUN-001");
        run.setRunName("Sample API Run");
        run.setEnvironment("QA");
        run.setExecutionMode("SEQUENTIAL");
        run.setExecuted(true);
        run.setPassed(true);
        run.setStartTime(LocalDateTime.now());
        run.setEndTime(LocalDateTime.now());
        run.setExecutionTimeMs(50);
        run.setTotalSuites(1);
        run.setPassedSuites(1);
        run.setTotalTestCases(1);
        run.setPassedTestCases(1);
        run.getSuiteResults().add(suite);

        TestReportDto report = new TestReportDto();
        report.setReportId("REPORT-CSV-001");
        report.setReportName("Sample API Run - Report");
        report.setReportFormat("CSV");
        report.setGeneratedAt(LocalDateTime.now());
        report.setTestRunResult(run);
        return report;
    }
}
