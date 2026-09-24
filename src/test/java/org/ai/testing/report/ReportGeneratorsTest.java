package org.ai.testing.report;

import org.ai.testing.TestFixtures;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.report.generator.CsvReportGenerator;
import org.ai.testing.report.generator.HtmlReportGenerator;
import org.ai.testing.report.generator.JUnitXmlReportGenerator;
import org.ai.testing.report.generator.JsonReportGenerator;
import org.ai.testing.report.generator.MarkdownReportGenerator;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Report generators")
class ReportGeneratorsTest {

    /** A run carrying characters that must not break any output format. */
    private TestRunResultDto hostileRun() {
        TestCaseResultDto nasty = TestFixtures.caseResult(
                "TC-X", "<script>alert('xss')</script> & \"quotes\"", TestStatus.FAILED);
        nasty.setMessage("Line one\nLine two, with a comma & an <angle>");
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test/search?q=a&b=<c>");
        request.header("Authorization", "Bearer super-secret-token");
        nasty.setRequest(request);
        nasty.setCurlCommand("curl -X GET 'https://api.test'");

        return TestFixtures.runResult("Hostile & <run>",
                TestFixtures.suiteResult("Suite \"one\"",
                        TestFixtures.caseResult("TC-1", "fine", TestStatus.PASSED),
                        nasty,
                        TestFixtures.caseResult("TC-2", "off", TestStatus.SKIPPED)));
    }

    private TestReportDto report(TestRunResultDto run) {
        return new ReportService().createReport(run);
    }

    @Test
    @DisplayName("HTML escapes markup rather than embedding it")
    void htmlEscapesContent(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("r.html");
        new HtmlReportGenerator(file).generate(report(hostileRun()));
        String html = Files.readString(file);

        assertFalse(html.contains("<script>alert"), "script markup must be escaped");
        assertTrue(html.contains("&lt;script&gt;alert"));
        assertTrue(html.trim().endsWith("</html>"));
    }

    @Test
    @DisplayName("HTML is self-contained, with no external resource of any kind")
    void htmlIsSelfContained(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("r.html");
        new HtmlReportGenerator(file).generate(report(hostileRun()));
        String html = Files.readString(file);

        assertFalse(html.contains("<link "), "no external stylesheet");
        assertFalse(html.contains("src=\"http"), "no external script or image");
        assertFalse(html.contains("@import"), "no imported stylesheet");
    }

    @Test
    @DisplayName("HTML carries the interactive controls the report relies on")
    void htmlCarriesControls(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("r.html");
        new HtmlReportGenerator(file).generate(report(hostileRun()));
        String html = Files.readString(file);

        assertTrue(html.contains("id=\"searchBox\""));
        assertTrue(html.contains("id=\"statusChips\""));
        assertTrue(html.contains("id=\"themeToggle\""));
        assertTrue(html.contains("data-tabs"));
        assertTrue(html.contains("class=\"donut\""));
        assertEquals(3, countOccurrences(html, "<details class=\"case\" data-case"),
                "one collapsible block per test case");
    }

    @Test
    @DisplayName("CSV quotes every field and doubles inner quotes")
    void csvQuotesFields(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("r.csv");
        new CsvReportGenerator(file).generate(report(hostileRun()));
        String csv = Files.readString(file, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF\"Report ID\""), "expected a BOM and a quoted header");
        assertTrue(csv.contains("\"\"quotes\"\""), "inner quotes should be doubled");
        assertTrue(csv.contains("\r\n"), "rows should end with CRLF");

        // Every physical row must have an even number of quote characters.
        for (String line : csv.split("\r\n")) {
            if (!line.isEmpty()) {
                assertEquals(0, countOccurrences(line, "\"") % 2,
                        "unbalanced quotes in: " + line);
            }
        }
    }

    @Test
    @DisplayName("JUnit XML parses as well-formed XML")
    void junitXmlIsWellFormed(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("junit.xml");
        new JUnitXmlReportGenerator(file).generate(report(hostileRun()));
        byte[] xml = Files.readAllBytes(file);

        assertDoesNotThrow(() -> DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml)));

        String text = new String(xml, StandardCharsets.UTF_8);
        assertTrue(text.contains("tests=\"3\""));
        assertTrue(text.contains("<failure"));
        assertTrue(text.contains("<skipped"));
    }

    @Test
    @DisplayName("JSON output parses back into the same counts")
    void jsonRoundTrips(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("r.json");
        new JsonReportGenerator(file).generate(report(hostileRun()));

        var parsed = org.ai.testing.json.Json.read(file);
        assertEquals(3, parsed.path("testRunResult").path("testCaseCounts")
                .path("total").asInt(0));
        assertEquals(1, parsed.path("testRunResult").path("testCaseCounts")
                .path("failed").asInt(0));
        assertEquals("Hostile & <run>",
                parsed.path("testRunResult").path("runName").asText());
    }

    @Test
    @DisplayName("Markdown escapes pipes so tables stay intact")
    void markdownEscapesPipes(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("r.md");
        TestRunResultDto run = hostileRun();
        run.getSuiteResults().get(0).setSuiteName("A | B");
        new MarkdownReportGenerator(file).generate(report(run));

        String markdown = Files.readString(file);
        assertTrue(markdown.contains("A \\| B"));
        assertTrue(markdown.startsWith("# "));
        assertTrue(markdown.contains("## Failures"));
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
