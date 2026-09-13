package org.ai.testing.report.generator;


import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.validation.dto.ValidationResultDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;
import org.ai.testing.dto.common.ResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


class HtmlReportGeneratorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldGenerateHtmlReport() throws Exception {

        Path outputFile =
                tempDirectory.resolve("test-report.html");

        HtmlReportGenerator generator =
                new HtmlReportGenerator(outputFile);

        TestReportDto report =
                createTestReport();

        generator.generate(report);

        assertTrue(
                Files.exists(outputFile),
                "HTML report should be created"
        );

        String html =
                Files.readString(outputFile);

        assertFalse(html.isBlank());

        assertTrue(
                html.contains("API Test Report")
        );

        assertTrue(
                html.contains("RUN-001")
        );

        assertTrue(
                html.contains("QA")
        );

        assertTrue(
                html.contains("User API Suite")
        );

        assertTrue(
                html.contains("Get User")
        );

        assertTrue(
                html.contains("PASSED")
        );

        assertTrue(
                html.contains("200")
        );

        assertTrue(
                html.contains("userId")
        );
    }

    @Test
    void shouldCreateParentDirectories() throws Exception {

        Path outputFile =
                tempDirectory
                        .resolve("reports")
                        .resolve("html")
                        .resolve("test-report.html");

        HtmlReportGenerator generator =
                new HtmlReportGenerator(outputFile);

        generator.generate(createTestReport());

        assertTrue(
                Files.exists(outputFile)
        );
    }

    @Test
    void shouldEscapeHtmlContent() throws Exception {

        Path outputFile =
                tempDirectory.resolve("escaped-report.html");

        HtmlReportGenerator generator =
                new HtmlReportGenerator(outputFile);

        TestReportDto report =
                createTestReport();

        report.getTestRunResult()
                .setRunName("<script>alert('test')</script>");

        generator.generate(report);

        String html =
                Files.readString(outputFile);

        assertFalse(
                html.contains("<script>alert('test')</script>")
        );

        assertTrue(
                html.contains("&lt;script&gt;")
        );
    }

    @Test
    void shouldRejectNullReport() {

        Path outputFile =
                tempDirectory.resolve("null-report.html");

        HtmlReportGenerator generator =
                new HtmlReportGenerator(outputFile);

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(null)
        );
    }

    @Test
    void shouldRejectReportWithoutTestRunResult() {

        Path outputFile =
                tempDirectory.resolve("invalid-report.html");

        HtmlReportGenerator generator =
                new HtmlReportGenerator(outputFile);

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
        report.setReportFormat("HTML");
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

        TestSuiteExecutionResultDto suite =
                new TestSuiteExecutionResultDto();

        suite.setSuiteId("SUITE-001");
        suite.setSuiteName("User API Suite");
        suite.setExecuted(true);
        suite.setPassed(true);
        suite.setMessage("Suite passed");
        suite.setExecutionTimeMs(900);
        suite.setTotalTestCases(1);
        suite.setPassedTestCases(1);
        suite.setFailedTestCases(0);
        suite.setSkippedTestCases(0);

        TestCaseExecutor.TestCaseExecutionResult testCase =
                new TestCaseExecutor.TestCaseExecutionResult();

        testCase.setTestCaseId("TC-001");
        testCase.setTestCaseName("Get User");
        testCase.setExecuted(true);
        testCase.setPassed(true);
        testCase.setMessage("Test case passed");

        ResponseDto response =
                new ResponseDto();

        response.setStatusCode(200);
        response.setStatusMessage("200 OK");
        response.setBody(
                "{\"userId\":1,\"id\":1}"
        );
        response.setResponseTimeMs(120);

        testCase.setResponse(response);

        ValidationResultDto validation =
                new ValidationResultDto();

        validation.setPassed(true);
        validation.setValidationType("STATUS_CODE");
        validation.setField("statusCode");
        validation.setExpected("200");
        validation.setActual("200");
        validation.setMessage(
                "Status code validation passed"
        );

        ValidationSummaryDto validationSummary =
                new ValidationSummaryDto();

        validationSummary.setPassed(true);
        validationSummary.setTotal(1);
        validationSummary.setPassedCount(1);
        validationSummary.setFailedCount(0);
        validationSummary.getResults().add(validation);

        testCase.setValidationSummary(validationSummary);

        suite.getTestResults().add(testCase);
        run.getSuiteResults().add(suite);

        report.setTestRunResult(run);

        return report;
    }
}