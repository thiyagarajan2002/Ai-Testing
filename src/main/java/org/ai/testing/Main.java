package org.ai.testing;

import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testrun.executor.TestRunExecutor;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    private static final Path REPORT_DIRECTORY = Paths.get("reports");
    private static final Path HTML_REPORT = REPORT_DIRECTORY.resolve("test-report.html");
    private static final Path JSON_REPORT = REPORT_DIRECTORY.resolve("test-report.json");
    private static final Path CSV_REPORT = REPORT_DIRECTORY.resolve("test-report.csv");

    public static void main(String[] args) {

        TestCaseDto testCase = new TestCaseDto();

        testCase.setTestCaseId("TC-001");
        testCase.setTestCaseName("Get User");
        testCase.setDescription("Validate GET user API");
        testCase.setMethod("GET");
        testCase.setEnabled(true);

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://jsonplaceholder.typicode.com/users/1");
        request.getHeaders().put("Accept", "application/json");

        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);

        AssertionDto statusAssertion = new AssertionDto();
        statusAssertion.setType(AssertionType.STATUS_CODE);
        statusAssertion.setField("statusCode");
        statusAssertion.setOperator(AssertionOperator.EQUALS);
        statusAssertion.setExpectedValue("200");

        testCase.setAssertions(List.of(statusAssertion));

        TestSuiteDto testSuite = new TestSuiteDto();

        testSuite.setSuiteId("SUITE-001");
        testSuite.setSuiteName("User API Tests");
        testSuite.setDescription("User API test suite");
        testSuite.setEnabled(true);
        testSuite.setTestCases(List.of(testCase));

        TestRunDto testRun = new TestRunDto();

        testRun.setRunId("RUN-001");
        testRun.setRunName("User API Test Run");
        testRun.setEnvironment("QA");
        testRun.setExecutionMode("SEQUENTIAL");
        testRun.setTestSuites(List.of(testSuite));

        TestRunExecutor executor = new TestRunExecutor();

        TestRunResultDto result = executor.execute(testRun);

        System.out.println();
        System.out.println("========================================");
        System.out.println("        API TEST RUN RESULT");
        System.out.println("========================================");

        System.out.println("Run ID          : " + result.getRunId());
        System.out.println("Run Name        : " + result.getRunName());
        System.out.println("Environment     : " + result.getEnvironment());
        System.out.println("Execution Mode  : " + result.getExecutionMode());

        System.out.println("----------------------------------------");

        System.out.println("Total Suites    : " + result.getTotalSuites());
        System.out.println("Passed Suites   : " + result.getPassedSuites());
        System.out.println("Failed Suites   : " + result.getFailedSuites());
        System.out.println("Skipped Suites  : " + result.getSkippedSuites());

        System.out.println("----------------------------------------");

        System.out.println("Total TestCases : " + result.getTotalTestCases());
        System.out.println("Passed Tests    : " + result.getPassedTestCases());
        System.out.println("Failed Tests    : " + result.getFailedTestCases());
        System.out.println("Skipped Tests   : " + result.getSkippedTestCases());

        System.out.println("----------------------------------------");

        System.out.println("Execution Time  : "
                + result.getExecutionTimeMs() + " ms");

        System.out.println("Status          : "
                + (result.isPassed() ? "PASSED" : "FAILED"));

        System.out.println("Message         : "
                + result.getMessage());

        verifyReports();

        System.out.println("========================================");

        System.out.println();
        System.out.println("Reports generated successfully:");
        printReportPath("HTML", HTML_REPORT);
        printReportPath("JSON", JSON_REPORT);
        printReportPath("CSV", CSV_REPORT);
    }

    private static void verifyReports() {
        Path[] reports = {HTML_REPORT, JSON_REPORT, CSV_REPORT};

        for (Path report : reports) {
            if (!Files.isRegularFile(report)) {
                throw new IllegalStateException(
                        "Expected report was not generated: " + report.toAbsolutePath()
                );
            }

            try {
                if (Files.size(report) == 0) {
                    throw new IllegalStateException(
                            "Generated report is empty: " + report.toAbsolutePath()
                    );
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException(
                        "Unable to verify report: " + report.toAbsolutePath(), e
                );
            }
        }
    }

    private static void printReportPath(String format, Path report) {
        try {
            System.out.println(String.format(
                    "  %-5s: %s (%d bytes)",
                    format,
                    report.toAbsolutePath(),
                    Files.size(report)
            ));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Unable to read report size: " + report.toAbsolutePath(), e
            );
        }
    }
}
