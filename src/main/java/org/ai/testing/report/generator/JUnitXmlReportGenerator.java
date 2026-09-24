package org.ai.testing.report.generator;

import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.util.Strings;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Surefire-compatible JUnit XML.
 *
 * <p>New in this version. Jenkins, GitLab CI, GitHub Actions and Azure Pipelines
 * all ingest this format natively, so an API run shows up in the build's test
 * tab next to the unit tests instead of being buried in an HTML artefact.</p>
 */
public class JUnitXmlReportGenerator extends AbstractFileReportGenerator {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public JUnitXmlReportGenerator() {
        this(Paths.get("reports", "junit-report.xml"));
    }

    public JUnitXmlReportGenerator(Path outputPath) {
        super(outputPath, "JUNIT-XML");
    }

    @Override
    protected String render(TestReportDto report) {
        TestRunResultDto run = report.getTestRunResult();
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuites name=\"").append(escape(run.getRunName()))
                .append("\" tests=\"").append(run.getTotalTestCases())
                .append("\" failures=\"").append(run.getFailedTestCases())
                .append("\" errors=\"").append(run.getErroredTestCases())
                .append("\" skipped=\"").append(run.getSkippedTestCases())
                .append("\" time=\"").append(seconds(run.getExecutionTimeMs()))
                .append("\">\n");

        for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) {
            appendSuite(xml, suite, run);
        }

        xml.append("</testsuites>\n");
        return xml.toString();
    }

    private void appendSuite(StringBuilder xml, TestSuiteExecutionResultDto suite,
                             TestRunResultDto run) {

        xml.append("  <testsuite name=\"").append(escape(suite.getSuiteName()))
                .append("\" tests=\"").append(suite.getTotalTestCases())
                .append("\" failures=\"").append(suite.getFailedTestCases())
                .append("\" errors=\"").append(suite.getErroredTestCases())
                .append("\" skipped=\"").append(suite.getSkippedTestCases())
                .append("\" time=\"").append(seconds(suite.getExecutionTimeMs()))
                .append('"');
        if (suite.getStartedAt() != null) {
            xml.append(" timestamp=\"").append(ISO.format(suite.getStartedAt())).append('"');
        }
        xml.append(">\n");

        xml.append("    <properties>\n")
                .append("      <property name=\"environment\" value=\"")
                .append(escape(run.getEnvironment())).append("\"/>\n")
                .append("      <property name=\"executionMode\" value=\"")
                .append(escape(run.getExecutionMode())).append("\"/>\n")
                .append("    </properties>\n");

        for (TestCaseResultDto testCase : suite.getTestResults()) {
            appendCase(xml, suite, testCase);
        }

        xml.append("  </testsuite>\n");
    }

    private void appendCase(StringBuilder xml, TestSuiteExecutionResultDto suite,
                            TestCaseResultDto testCase) {

        xml.append("    <testcase name=\"")
                .append(escape(Strings.defaultIfBlank(
                        testCase.getTestCaseName(), testCase.getTestCaseId())))
                .append("\" classname=\"").append(escape(suite.getSuiteName()))
                .append("\" time=\"").append(seconds(testCase.getExecutionTimeMs()))
                .append("\">\n");

        switch (testCase.getStatus()) {
            case SKIPPED -> xml.append("      <skipped message=\"")
                    .append(escape(testCase.getMessage())).append("\"/>\n");

            case ERROR -> xml.append("      <error message=\"")
                    .append(escape(testCase.getMessage()))
                    .append("\" type=\"")
                    .append(escape(Strings.defaultIfBlank(testCase.getErrorType(), "Error")))
                    .append("\">")
                    .append(escape(Strings.nullToEmpty(testCase.getErrorDetail())))
                    .append("</error>\n");

            case FAILED -> {
                xml.append("      <failure message=\"")
                        .append(escape(testCase.getMessage()))
                        .append("\" type=\"AssertionFailure\">");
                for (ValidationResultDto validation
                        : testCase.getValidationSummary().getResults()) {
                    if (!validation.isPassed()) {
                        xml.append(escape(validation.getValidationType()
                                + " " + Strings.nullToEmpty(validation.getField())
                                + ": " + Strings.nullToEmpty(validation.getMessage())))
                                .append('\n');
                    }
                }
                xml.append("</failure>\n");
            }

            default -> {
            }
        }

        if (testCase.getResponse() != null) {
            xml.append("      <system-out>")
                    .append(escape(testCase.getMethod() + " "
                            + Strings.nullToEmpty(testCase.getRequest() == null
                            ? "" : testCase.getRequest().getUrl())
                            + " -> " + testCase.getResponse().getStatusCode() + " "
                            + testCase.getResponse().getStatusMessage()
                            + " in " + testCase.getResponse().getResponseTimeMs() + " ms"))
                    .append("</system-out>\n");
        }

        xml.append("    </testcase>\n");
    }

    private String seconds(long milliseconds) {
        return String.format(Locale.ROOT, "%.3f", milliseconds / 1000.0);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> {
                    // XML 1.0 forbids most control characters outright.
                    if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) {
                        escaped.append(c);
                    } else {
                        escaped.append(' ');
                    }
                }
            }
        }
        return escaped.toString();
    }
}
