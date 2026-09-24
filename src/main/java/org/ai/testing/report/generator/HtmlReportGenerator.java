package org.ai.testing.report.generator;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.json.Json;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.util.HttpStatus;
import org.ai.testing.util.Strings;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A single self-contained HTML dashboard.
 *
 * <p>Rewritten from the previous flat page. It now carries a KPI strip, inline
 * SVG charts, live search, status and method filters, collapsible suites,
 * tabbed request/response panes with pretty-printed JSON, a copyable cURL
 * reproduction per case, a slowest-tests table, and a light/dark theme. There
 * are no external stylesheets, fonts or scripts, so the file can be emailed or
 * published as a CI artefact and still render exactly the same.</p>
 */
public class HtmlReportGenerator extends AbstractFileReportGenerator {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int BODY_PREVIEW_CHARS = 40_000;

    public HtmlReportGenerator() {
        this(Paths.get("reports", "test-report.html"));
    }

    public HtmlReportGenerator(Path outputPath) {
        super(outputPath, "HTML");
    }

    @Override
    protected String render(TestReportDto report) {
        TestRunResultDto run = report.getTestRunResult();
        StringBuilder html = new StringBuilder(64_000);

        html.append("<!DOCTYPE html>\n<html lang=\"en\" data-theme=\"light\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("<title>").append(esc(reportName(report))).append("</title>\n");
        html.append("<style>\n").append(STYLES).append("\n</style>\n</head>\n<body>\n");

        appendHeader(html, report, run);
        html.append("<main class=\"wrap\">\n");
        appendKpis(html, run);
        appendCharts(html, run);
        appendNarrative(html, report);
        appendWarnings(html, run);
        appendToolbar(html, run);
        appendSuites(html, run);
        appendSlowest(html, run);
        appendVariables(html, run);
        html.append("</main>\n");
        appendFooter(html, report);

        html.append("<script>\n").append(SCRIPT).append("\n</script>\n");
        html.append("</body>\n</html>\n");
        return html.toString();
    }

    // ==================================================================
    // Header and KPIs
    // ==================================================================

    private void appendHeader(StringBuilder html, TestReportDto report, TestRunResultDto run) {
        html.append("<header class=\"topbar\">\n<div class=\"wrap topbar-inner\">\n");
        html.append("<div class=\"brand\">\n");
        html.append("<div class=\"brand-mark\">API</div>\n<div>\n");
        html.append("<h1>").append(esc(reportName(report))).append("</h1>\n");
        html.append("<p class=\"brand-sub\">")
                .append(esc(Strings.defaultIfBlank(run.getMessage(), "")))
                .append("</p>\n</div>\n</div>\n");

        html.append("<div class=\"topbar-actions\">\n");
        html.append(statusPill(run.getStatus(), true));
        html.append("<button class=\"btn ghost\" id=\"themeToggle\" type=\"button\" "
                + "title=\"Toggle light and dark theme\">Dark</button>\n");
        html.append("<button class=\"btn ghost\" onclick=\"window.print()\" type=\"button\">"
                + "Print</button>\n");
        html.append("</div>\n</div>\n");

        html.append("<div class=\"wrap meta\">\n");
        meta(html, "Run ID", run.getRunId());
        meta(html, "Environment", run.getEnvironment());
        meta(html, "Execution mode", run.getExecutionMode());
        meta(html, "Started", run.getStartTime() == null
                ? "" : TIMESTAMP.format(run.getStartTime()));
        meta(html, "Finished", run.getEndTime() == null
                ? "" : TIMESTAMP.format(run.getEndTime()));
        meta(html, "Report ID", report.getReportId());
        html.append("</div>\n</header>\n");
    }

    private void meta(StringBuilder html, String label, String value) {
        html.append("<div class=\"meta-item\"><span class=\"meta-label\">")
                .append(esc(label)).append("</span><span class=\"meta-value\">")
                .append(esc(Strings.defaultIfBlank(value, "—")))
                .append("</span></div>\n");
    }

    private void appendKpis(StringBuilder html, TestRunResultDto run) {
        TestRunResultDto.Metrics metrics = run.getMetrics();

        html.append("<section class=\"kpis\">\n");

        // Pass-rate donut.
        html.append("<div class=\"card kpi kpi-donut\">\n");
        html.append(donut(metrics.getPassRate(), run));
        html.append("<div class=\"kpi-donut-text\"><span class=\"kpi-label\">Pass rate</span>");
        html.append("<span class=\"kpi-sub\">").append(run.getPassedTestCases())
                .append(" of ").append(executed(run)).append(" executed</span></div>\n");
        html.append("</div>\n");

        kpi(html, "Test cases", String.valueOf(run.getTotalTestCases()),
                run.getTotalSuites() + " suite(s)", "neutral");
        kpi(html, "Passed", String.valueOf(run.getPassedTestCases()), "assertions met", "passed");
        kpi(html, "Failed", String.valueOf(run.getFailedTestCases()),
                "assertion mismatch", run.getFailedTestCases() > 0 ? "failed" : "muted");
        kpi(html, "Errored", String.valueOf(run.getErroredTestCases()),
                "request could not run", run.getErroredTestCases() > 0 ? "error" : "muted");
        kpi(html, "Skipped", String.valueOf(run.getSkippedTestCases()),
                "disabled or filtered", run.getSkippedTestCases() > 0 ? "skipped" : "muted");
        kpi(html, "Duration", Strings.humanDuration(run.getExecutionTimeMs()),
                String.format(Locale.ROOT, "%.2f req/s", metrics.getRequestsPerSecond()),
                "neutral");
        kpi(html, "Latency p95", metrics.getP95ResponseTimeMs() + " ms",
                "median " + metrics.getMedianResponseTimeMs() + " ms · max "
                        + metrics.getMaxResponseTimeMs() + " ms", "neutral");
        kpi(html, "Transferred", Strings.humanBytes(metrics.getTotalBytes()),
                "response bodies", "neutral");

        html.append("</section>\n");
    }

    private void kpi(StringBuilder html, String label, String value,
                     String sub, String tone) {
        html.append("<div class=\"card kpi\">");
        html.append("<span class=\"kpi-label\">").append(esc(label)).append("</span>");
        html.append("<span class=\"kpi-value tone-").append(tone).append("\">")
                .append(esc(value)).append("</span>");
        html.append("<span class=\"kpi-sub\">").append(esc(sub)).append("</span>");
        html.append("</div>\n");
    }

    private int executed(TestRunResultDto run) {
        return run.getPassedTestCases() + run.getFailedTestCases() + run.getErroredTestCases();
    }

    /** Inline SVG ring; no chart library needed. */
    private String donut(double passRate, TestRunResultDto run) {
        double radius = 52;
        double circumference = 2 * Math.PI * radius;
        double filled = circumference * Math.max(0, Math.min(100, passRate)) / 100.0;
        String tone = run.getStatus() == TestStatus.PASSED ? "var(--pass)"
                : run.getStatus() == TestStatus.SKIPPED ? "var(--skip)" : "var(--fail)";

        return "<svg class=\"donut\" viewBox=\"0 0 128 128\" role=\"img\" "
                + "aria-label=\"Pass rate " + String.format(Locale.ROOT, "%.1f", passRate)
                + " percent\">"
                + "<circle cx=\"64\" cy=\"64\" r=\"" + radius
                + "\" fill=\"none\" stroke=\"var(--track)\" stroke-width=\"14\"/>"
                + "<circle cx=\"64\" cy=\"64\" r=\"" + radius
                + "\" fill=\"none\" stroke=\"" + tone + "\" stroke-width=\"14\""
                + " stroke-linecap=\"round\""
                + " stroke-dasharray=\"" + fmt(filled) + " " + fmt(circumference - filled) + "\""
                + " transform=\"rotate(-90 64 64)\"/>"
                + "<text x=\"64\" y=\"70\" text-anchor=\"middle\" class=\"donut-value\">"
                + String.format(Locale.ROOT, "%.0f%%", passRate) + "</text>"
                + "</svg>";
    }

    // ==================================================================
    // Charts
    // ==================================================================

    private void appendCharts(StringBuilder html, TestRunResultDto run) {
        Map<String, Integer> families = run.getMetrics().getStatusFamilies();
        Map<String, Integer> methods = run.getMetrics().getMethodCounts();
        if (families.isEmpty() && methods.isEmpty()) {
            return;
        }

        html.append("<section class=\"grid-2\">\n");

        if (!families.isEmpty()) {
            html.append("<div class=\"card\"><h2>Response status distribution</h2>\n");
            int max = families.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            html.append("<div class=\"bars\">\n");
            families.forEach((family, count) -> html.append(bar(
                    family, count, max, familyTone(family))));
            html.append("</div></div>\n");
        }

        if (!methods.isEmpty()) {
            html.append("<div class=\"card\"><h2>Requests by method</h2>\n");
            int max = methods.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            html.append("<div class=\"bars\">\n");
            methods.forEach((method, count) ->
                    html.append(bar(method, count, max, "accent")));
            html.append("</div></div>\n");
        }

        html.append("</section>\n");
    }

    private String bar(String label, int count, int max, String tone) {
        double percent = max == 0 ? 0 : (count * 100.0) / max;
        return "<div class=\"bar-row\">"
                + "<span class=\"bar-label\">" + esc(label) + "</span>"
                + "<span class=\"bar-track\"><span class=\"bar-fill tone-" + tone
                + "\" style=\"width:" + fmt(percent) + "%\"></span></span>"
                + "<span class=\"bar-count\">" + count + "</span></div>\n";
    }

    private String familyTone(String family) {
        return switch (family) {
            case "2xx" -> "passed";
            case "3xx" -> "accent";
            case "4xx" -> "skipped";
            case "5xx", "error" -> "failed";
            default -> "neutral";
        };
    }

    // ==================================================================
    // Narrative, warnings, toolbar
    // ==================================================================

    private void appendNarrative(StringBuilder html, TestReportDto report) {
        boolean hasContent = Strings.hasText(report.getSummary())
                || !report.getFindings().isEmpty()
                || !report.getRecommendations().isEmpty();
        if (!hasContent) {
            return;
        }

        html.append("<section class=\"card narrative\">\n<h2>Analysis</h2>\n");
        if (Strings.hasText(report.getSeverity())) {
            html.append("<p class=\"pill pill-neutral\">Severity: ")
                    .append(esc(report.getSeverity())).append("</p>\n");
        }
        if (Strings.hasText(report.getSummary())) {
            html.append("<p>").append(esc(report.getSummary())).append("</p>\n");
        }
        appendList(html, "Findings", report.getFindings());
        appendList(html, "Recommendations", report.getRecommendations());
        html.append("</section>\n");
    }

    private void appendList(StringBuilder html, String title, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        html.append("<h3>").append(esc(title)).append("</h3>\n<ul>\n");
        items.forEach(item -> html.append("<li>").append(esc(item)).append("</li>\n"));
        html.append("</ul>\n");
    }

    private void appendWarnings(StringBuilder html, TestRunResultDto run) {
        if (run.getWarnings().isEmpty()) {
            return;
        }
        html.append("<section class=\"card warnings\">\n<h2>Warnings (")
                .append(run.getWarnings().size()).append(")</h2>\n<ul>\n");
        run.getWarnings().forEach(warning ->
                html.append("<li>").append(esc(warning)).append("</li>\n"));
        html.append("</ul>\n</section>\n");
    }

    private void appendToolbar(StringBuilder html, TestRunResultDto run) {
        html.append("<section class=\"toolbar card\">\n");
        html.append("<input type=\"search\" id=\"searchBox\" class=\"search\" "
                + "placeholder=\"Filter by test name, URL, status code or message…\" "
                + "aria-label=\"Filter test cases\">\n");
        html.append("<div class=\"chips\" id=\"statusChips\">\n");
        chip(html, "all", "All", run.getTotalTestCases(), true);
        chip(html, "passed", "Passed", run.getPassedTestCases(), false);
        chip(html, "failed", "Failed", run.getFailedTestCases(), false);
        chip(html, "error", "Errored", run.getErroredTestCases(), false);
        chip(html, "skipped", "Skipped", run.getSkippedTestCases(), false);
        html.append("</div>\n");
        html.append("<div class=\"toolbar-buttons\">");
        html.append("<button class=\"btn ghost\" type=\"button\" id=\"expandAll\">"
                + "Expand all</button>");
        html.append("<button class=\"btn ghost\" type=\"button\" id=\"collapseAll\">"
                + "Collapse all</button>");
        html.append("</div>\n");
        html.append("<p class=\"filter-note\" id=\"filterNote\"></p>\n");
        html.append("</section>\n");
    }

    private void chip(StringBuilder html, String value, String label,
                      int count, boolean active) {
        html.append("<button type=\"button\" class=\"chip chip-").append(value)
                .append(active ? " active" : "")
                .append("\" data-status=\"").append(value).append("\">")
                .append(esc(label)).append(" <span class=\"chip-count\">")
                .append(count).append("</span></button>\n");
    }

    // ==================================================================
    // Suites and test cases
    // ==================================================================

    private void appendSuites(StringBuilder html, TestRunResultDto run) {
        html.append("<section id=\"suites\">\n");
        if (run.getSuiteResults().isEmpty()) {
            html.append("<div class=\"card empty\">This run contained no test suites.</div>\n");
        }
        int index = 0;
        for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) {
            appendSuite(html, suite, index++);
        }
        html.append("<div class=\"card empty hidden\" id=\"noMatches\">"
                + "No test cases match the current filter.</div>\n");
        html.append("</section>\n");
    }

    private void appendSuite(StringBuilder html, TestSuiteExecutionResultDto suite, int index) {
        boolean openByDefault = suite.getStatus().isFailure() || index == 0;

        html.append("<details class=\"suite card\" data-suite")
                .append(openByDefault ? " open" : "").append(">\n");
        html.append("<summary class=\"suite-head\">\n");
        html.append("<span class=\"chevron\" aria-hidden=\"true\"></span>\n");
        html.append("<span class=\"suite-title\">").append(esc(suite.getSuiteName()))
                .append("</span>\n");
        html.append("<span class=\"suite-counts\">");
        countBadge(html, "passed", suite.getPassedTestCases());
        countBadge(html, "failed", suite.getFailedTestCases());
        countBadge(html, "error", suite.getErroredTestCases());
        countBadge(html, "skipped", suite.getSkippedTestCases());
        html.append("</span>\n");
        html.append("<span class=\"suite-time\">")
                .append(Strings.humanDuration(suite.getExecutionTimeMs())).append("</span>\n");
        html.append(statusPill(suite.getStatus(), false));
        html.append("</summary>\n");

        html.append("<div class=\"suite-body\">\n");
        if (Strings.hasText(suite.getDescription())) {
            html.append("<p class=\"muted\">").append(esc(suite.getDescription()))
                    .append("</p>\n");
        }
        if (Strings.hasText(suite.getMessage())) {
            html.append("<p class=\"suite-message\">").append(esc(suite.getMessage()))
                    .append("</p>\n");
        }
        if (suite.getTestResults().isEmpty()) {
            html.append("<p class=\"muted\">No test cases were executed in this suite.</p>\n");
        }
        for (TestCaseResultDto testCase : suite.getTestResults()) {
            appendTestCase(html, testCase);
        }
        html.append("</div>\n</details>\n");
    }

    private void countBadge(StringBuilder html, String tone, int count) {
        if (count <= 0) {
            return;
        }
        html.append("<span class=\"count-badge tone-").append(tone).append("\">")
                .append(count).append("</span>");
    }

    private void appendTestCase(StringBuilder html, TestCaseResultDto result) {
        String status = result.getStatus() == TestStatus.ERROR
                ? "error" : result.getStatus().token();
        String url = result.getRequest() == null
                ? "" : Strings.nullToEmpty(result.getRequest().getUrl());

        String haystack = (Strings.nullToEmpty(result.getTestCaseName()) + " "
                + Strings.nullToEmpty(result.getTestCaseId()) + " "
                + Strings.nullToEmpty(result.getMethod()) + " " + url + " "
                + result.statusCode() + " "
                + Strings.nullToEmpty(result.getMessage()) + " "
                + String.join(" ", result.getTags())).toLowerCase(Locale.ROOT);

        html.append("<details class=\"case\" data-case data-status=\"").append(status)
                .append("\" data-search=\"").append(esc(haystack)).append("\"")
                .append(result.getStatus().isFailure() ? " open" : "").append(">\n");

        // ---- summary line
        html.append("<summary class=\"case-head\">\n");
        html.append("<span class=\"chevron\" aria-hidden=\"true\"></span>\n");
        html.append("<span class=\"dot dot-").append(status).append("\"></span>\n");
        html.append("<span class=\"method method-")
                .append(Strings.lower(Strings.defaultIfBlank(result.getMethod(), "get")))
                .append("\">").append(esc(result.getMethod())).append("</span>\n");
        html.append("<span class=\"case-name\">").append(esc(result.getTestCaseName()))
                .append("</span>\n");
        html.append("<span class=\"case-url\" title=\"").append(esc(url)).append("\">")
                .append(esc(url)).append("</span>\n");
        html.append("<span class=\"case-stats\">");
        if (result.getResponse() != null) {
            html.append("<span class=\"code code-").append(familyTone(
                            HttpStatus.family(result.statusCode())))
                    .append("\">").append(result.statusCode()).append("</span>");
            html.append("<span class=\"latency\">")
                    .append(result.responseTimeMs()).append(" ms</span>");
        }
        if (!result.getValidationSummary().getResults().isEmpty()) {
            html.append("<span class=\"assert-count\">")
                    .append(result.getValidationSummary().getPassedCount()).append('/')
                    .append(result.getValidationSummary().getTotal()).append("</span>");
        }
        html.append("</span>\n");
        html.append("</summary>\n");

        // ---- body
        html.append("<div class=\"case-body\">\n");

        if (Strings.hasText(result.getDescription())) {
            html.append("<p class=\"muted\">").append(esc(result.getDescription()))
                    .append("</p>\n");
        }

        html.append("<p class=\"case-message tone-").append(status).append("\">")
                .append(esc(result.getMessage())).append("</p>\n");

        if (Strings.hasText(result.getErrorDetail())) {
            html.append("<pre class=\"error-detail\">")
                    .append(esc(result.getErrorDetail())).append("</pre>\n");
        }

        appendTabs(html, result);
        html.append("</div>\n</details>\n");
    }

    private void appendTabs(StringBuilder html, TestCaseResultDto result) {
        html.append("<div class=\"tabs\" data-tabs>\n");
        html.append("<div class=\"tab-strip\" role=\"tablist\">");
        tabButton(html, "Assertions", true);
        tabButton(html, "Request", false);
        tabButton(html, "Response", false);
        tabButton(html, "cURL", false);
        html.append("</div>\n");

        // Assertions
        html.append("<div class=\"tab-panel active\">\n");
        appendAssertions(html, result);
        appendCaptures(html, result);
        html.append("</div>\n");

        // Request
        html.append("<div class=\"tab-panel\">\n");
        appendRequest(html, result);
        html.append("</div>\n");

        // Response
        html.append("<div class=\"tab-panel\">\n");
        appendResponse(html, result);
        html.append("</div>\n");

        // cURL
        html.append("<div class=\"tab-panel\">\n");
        html.append(codeBlock(Strings.defaultIfBlank(result.getCurlCommand(),
                "No request was sent."), "curl"));
        html.append("</div>\n");

        html.append("</div>\n");
    }

    private void tabButton(StringBuilder html, String label, boolean active) {
        html.append("<button type=\"button\" class=\"tab")
                .append(active ? " active" : "").append("\" role=\"tab\">")
                .append(esc(label)).append("</button>");
    }

    private void appendAssertions(StringBuilder html, TestCaseResultDto result) {
        List<ValidationResultDto> results = result.getValidationSummary().getResults();
        if (results.isEmpty()) {
            html.append("<p class=\"muted\">No assertions were configured "
                    + "for this test case.</p>\n");
            return;
        }

        html.append("<table class=\"data\">\n<thead><tr>"
                + "<th>Status</th><th>Type</th><th>Field</th><th>Operator</th>"
                + "<th>Expected</th><th>Actual</th><th>Detail</th></tr></thead>\n<tbody>\n");

        for (ValidationResultDto validation : results) {
            String tone = validation.isPassed() ? "passed" : "failed";
            html.append("<tr class=\"row-").append(tone).append("\">");
            html.append("<td><span class=\"pill pill-").append(tone).append("\">")
                    .append(validation.isPassed() ? "PASS" : "FAIL").append("</span></td>");
            html.append("<td>").append(esc(validation.getValidationType())).append("</td>");
            html.append("<td><code>").append(esc(validation.getField())).append("</code></td>");
            html.append("<td>").append(esc(Strings.lower(
                    Strings.nullToEmpty(validation.getOperator()).replace('_', ' ')))).append("</td>");
            html.append("<td class=\"wrap-cell\"><code>")
                    .append(esc(Strings.truncate(validation.getExpected(), 400)))
                    .append("</code></td>");
            html.append("<td class=\"wrap-cell\"><code>")
                    .append(esc(Strings.truncate(validation.getActual(), 400)))
                    .append("</code></td>");
            html.append("<td class=\"wrap-cell\">")
                    .append(esc(validation.getMessage())).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n");
    }

    private void appendCaptures(StringBuilder html, TestCaseResultDto result) {
        if (result.getCapturedVariables().isEmpty() && result.getFailedExtracts().isEmpty()) {
            return;
        }
        html.append("<h4>Captured variables</h4>\n");
        if (!result.getCapturedVariables().isEmpty()) {
            html.append(keyValueTable(result.getCapturedVariables(), "Variable", "Value"));
        }
        if (!result.getFailedExtracts().isEmpty()) {
            html.append("<ul class=\"warn-list\">\n");
            result.getFailedExtracts().forEach(extract ->
                    html.append("<li>Matched nothing: <code>").append(esc(extract))
                            .append("</code></li>\n"));
            html.append("</ul>\n");
        }
    }

    private void appendRequest(StringBuilder html, TestCaseResultDto result) {
        BaseRequestDto request = result.getRequest();
        if (request == null) {
            html.append("<p class=\"muted\">The request was never built.</p>\n");
            return;
        }

        html.append("<h4>Endpoint</h4>\n");
        html.append(codeBlock(Strings.nullToEmpty(result.getMethod()) + " "
                + Strings.nullToEmpty(request.getUrl()), "url"));

        if (Strings.hasText(result.getAuthApplied())) {
            html.append("<p class=\"muted\">Authentication applied: <code>")
                    .append(esc(result.getAuthApplied())).append("</code></p>\n");
        }

        if (!request.getQueryParams().isEmpty()) {
            html.append("<h4>Query parameters</h4>\n")
                    .append(keyValueTable(request.getQueryParams(), "Name", "Value"));
        }
        if (!request.getPathParams().isEmpty()) {
            html.append("<h4>Path parameters</h4>\n")
                    .append(keyValueTable(request.getPathParams(), "Name", "Value"));
        }

        html.append("<h4>Headers</h4>\n")
                .append(keyValueTable(request.getHeaders(), "Header", "Value"));

        if (request.getBody() != null && Strings.hasText(request.getBody().getRawBody())) {
            html.append("<h4>Body <span class=\"muted-inline\">")
                    .append(esc(Strings.defaultIfBlank(
                            request.getBody().getContentType(), "unknown type")))
                    .append("</span></h4>\n");
            html.append(codeBlock(prettyBody(request.getBody().getRawBody(),
                    request.getBody().getContentType()), "body"));
        }
    }

    private void appendResponse(StringBuilder html, TestCaseResultDto result) {
        ResponseDto response = result.getResponse();
        if (response == null) {
            html.append("<p class=\"muted\">No response was received.</p>\n");
            return;
        }

        html.append("<div class=\"stat-strip\">");
        stat(html, "Status", response.getStatusCode() + " " + response.getStatusMessage());
        stat(html, "Time", response.getResponseTimeMs() + " ms");
        stat(html, "Size", Strings.humanBytes(response.getBodySizeBytes()));
        stat(html, "Attempts", String.valueOf(response.getAttempts()));
        html.append("</div>\n");

        if (Strings.hasText(response.getFinalUrl())) {
            html.append("<p class=\"muted\">Resolved URL: <code>")
                    .append(esc(response.getFinalUrl())).append("</code></p>\n");
        }

        html.append("<h4>Headers</h4>\n")
                .append(keyValueTable(response.getHeaders(), "Header", "Value"));

        html.append("<h4>Body</h4>\n");
        String body = Strings.nullToEmpty(response.getBody());
        if (body.isEmpty()) {
            html.append("<p class=\"muted\">Empty response body.</p>\n");
        } else {
            html.append(codeBlock(prettyBody(body, response.header("Content-Type")), "body"));
        }
    }

    private void stat(StringBuilder html, String label, String value) {
        html.append("<span class=\"stat\"><span class=\"stat-label\">").append(esc(label))
                .append("</span><span class=\"stat-value\">").append(esc(value))
                .append("</span></span>");
    }

    // ==================================================================
    // Slowest tests and variables
    // ==================================================================

    private void appendSlowest(StringBuilder html, TestRunResultDto run) {
        List<TestCaseResultDto> slowest = run.slowestTests(10);
        if (slowest.isEmpty()) {
            return;
        }
        long max = slowest.get(0).responseTimeMs();

        html.append("<section class=\"card\">\n<h2>Slowest test cases</h2>\n");
        html.append("<div class=\"bars\">\n");
        for (TestCaseResultDto result : slowest) {
            double percent = max == 0 ? 0 : (result.responseTimeMs() * 100.0) / max;
            html.append("<div class=\"bar-row\">")
                    .append("<span class=\"bar-label\" title=\"")
                    .append(esc(result.getTestCaseName())).append("\">")
                    .append(esc(Strings.truncate(result.getTestCaseName(), 42)))
                    .append("</span>")
                    .append("<span class=\"bar-track\"><span class=\"bar-fill tone-")
                    .append(result.getStatus() == TestStatus.PASSED ? "passed" : "failed")
                    .append("\" style=\"width:").append(fmt(percent)).append("%\"></span></span>")
                    .append("<span class=\"bar-count\">").append(result.responseTimeMs())
                    .append(" ms</span></div>\n");
        }
        html.append("</div>\n</section>\n");
    }

    private void appendVariables(StringBuilder html, TestRunResultDto run) {
        if (run.getVariables().isEmpty()) {
            return;
        }
        html.append("<details class=\"card\">\n<summary class=\"plain-summary\">")
                .append("Variables in effect (").append(run.getVariables().size())
                .append(")</summary>\n");
        html.append(keyValueTable(run.getVariables(), "Name", "Value"));
        html.append("</details>\n");
    }

    private void appendFooter(StringBuilder html, TestReportDto report) {
        html.append("<footer class=\"wrap footer\">Generated ");
        html.append(esc(report.getGeneratedAt() == null
                ? TIMESTAMP.format(LocalDateTime.now())
                : TIMESTAMP.format(report.getGeneratedAt())));
        html.append(" by ").append(esc(Strings.defaultIfBlank(
                report.getGeneratedBy(), "AI API Testing Agent")));
        if (Strings.hasText(report.getToolVersion())) {
            html.append(' ').append(esc(report.getToolVersion()));
        }
        html.append("</footer>\n");
    }

    // ==================================================================
    // Shared fragments
    // ==================================================================

    private String statusPill(TestStatus status, boolean large) {
        String tone = status == TestStatus.ERROR ? "error" : status.token();
        return "<span class=\"pill pill-" + tone + (large ? " pill-lg" : "") + "\">"
                + status.name() + "</span>\n";
    }

    private String keyValueTable(Map<String, String> values, String keyLabel, String valueLabel) {
        if (values == null || values.isEmpty()) {
            return "<p class=\"muted\">None.</p>\n";
        }
        StringBuilder table = new StringBuilder();
        table.append("<table class=\"data kv\">\n<thead><tr><th>").append(esc(keyLabel))
                .append("</th><th>").append(esc(valueLabel)).append("</th></tr></thead>\n<tbody>\n");
        values.forEach((key, value) -> table.append("<tr><td><code>").append(esc(key))
                .append("</code></td><td class=\"wrap-cell\"><code>")
                .append(esc(Strings.truncate(value, 2000)))
                .append("</code></td></tr>\n"));
        table.append("</tbody>\n</table>\n");
        return table.toString();
    }

    private String codeBlock(String content, String kind) {
        return "<div class=\"code-block\" data-kind=\"" + kind + "\">"
                + "<button type=\"button\" class=\"copy-btn\">Copy</button>"
                + "<pre><code>" + esc(Strings.truncate(content, BODY_PREVIEW_CHARS))
                + "</code></pre></div>\n";
    }

    /** Indents JSON payloads so a body is readable without an external viewer. */
    private String prettyBody(String body, String contentType) {
        if (body == null) {
            return "";
        }
        boolean looksJson = (contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains("json"))
                || Json.isJson(body);
        return looksJson ? Json.prettyPrint(body) : body;
    }

    private String reportName(TestReportDto report) {
        return Strings.defaultIfBlank(report.getReportName(), "API Test Report");
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** Escapes text for safe insertion into markup or an attribute. */
    static String esc(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    // ==================================================================
    // Assets
    // ==================================================================

    private static final String STYLES = """
            :root{
              --bg:#f1f3f7; --panel:#ffffff; --ink:#111827; --muted:#6b7280;
              --line:#e2e6ee; --track:#e8ecf4; --accent:#3b5bdb;
              --pass:#12855f; --fail:#d33b4d; --skip:#c2721a; --err:#9333ea;
              --pass-bg:#e4f5ed; --fail-bg:#fdeaec; --skip-bg:#fdf1e3; --err-bg:#f4e9fd;
              --code-bg:#0f172a; --code-ink:#e2e8f0; --shadow:0 1px 2px rgba(16,24,40,.06),0 4px 16px rgba(16,24,40,.06);
            }
            html[data-theme="dark"]{
              --bg:#0d1117; --panel:#161b22; --ink:#e6edf3; --muted:#9198a1;
              --line:#2b323c; --track:#252c36; --accent:#6b8afd;
              --pass:#3fb984; --fail:#ff6b7e; --skip:#e0a253; --err:#c084fc;
              --pass-bg:#10291f; --fail-bg:#2d1418; --skip-bg:#2d2113; --err-bg:#241733;
              --code-bg:#010409; --code-ink:#c9d1d9; --shadow:0 1px 2px rgba(0,0,0,.4);
            }
            *{box-sizing:border-box}
            body{margin:0;background:var(--bg);color:var(--ink);
              font:14px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;
              -webkit-font-smoothing:antialiased}
            .wrap{width:min(1440px,94vw);margin:0 auto}
            h1{font-size:20px;margin:0;letter-spacing:-.01em}
            h2{font-size:15px;margin:0 0 14px;letter-spacing:.02em;text-transform:uppercase;color:var(--muted)}
            h3{font-size:14px;margin:18px 0 6px}
            h4{font-size:12px;margin:18px 0 6px;text-transform:uppercase;letter-spacing:.04em;color:var(--muted)}
            code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px}
            .muted{color:var(--muted)}
            .muted-inline{color:var(--muted);font-weight:400;text-transform:none;letter-spacing:0}
            .hidden{display:none !important}

            /* ---------- top bar ---------- */
            .topbar{background:var(--panel);border-bottom:1px solid var(--line);padding:18px 0 0}
            .topbar-inner{display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap}
            .brand{display:flex;align-items:center;gap:12px}
            .brand-mark{width:38px;height:38px;border-radius:10px;background:var(--accent);color:#fff;
              display:grid;place-items:center;font-weight:700;font-size:12px;letter-spacing:.04em}
            .brand-sub{margin:2px 0 0;color:var(--muted);font-size:13px}
            .topbar-actions{display:flex;align-items:center;gap:8px}
            .meta{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));
              gap:1px;background:var(--line);border-top:1px solid var(--line);margin-top:16px}
            .meta-item{background:var(--panel);padding:10px 12px;display:flex;flex-direction:column;gap:2px}
            .meta-label{font-size:11px;text-transform:uppercase;letter-spacing:.05em;color:var(--muted)}
            .meta-value{font-size:13px;font-weight:600;word-break:break-word}

            /* ---------- cards ---------- */
            .card{background:var(--panel);border:1px solid var(--line);border-radius:12px;
              padding:18px;margin:18px 0;box-shadow:var(--shadow)}
            .grid-2{display:grid;grid-template-columns:repeat(auto-fit,minmax(340px,1fr));gap:18px}
            .grid-2 .card{margin:0}
            .empty{text-align:center;color:var(--muted)}

            /* ---------- kpis ---------- */
            .kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:14px;margin:18px 0}
            .kpi{margin:0;padding:14px 16px;display:flex;flex-direction:column;gap:3px}
            .kpi-label{font-size:11px;text-transform:uppercase;letter-spacing:.05em;color:var(--muted)}
            .kpi-value{font-size:26px;font-weight:700;line-height:1.15;letter-spacing:-.02em}
            .kpi-sub{font-size:11px;color:var(--muted)}
            .kpi-donut{flex-direction:row;align-items:center;gap:14px;grid-row:span 1}
            .kpi-donut-text{display:flex;flex-direction:column;gap:2px}
            .donut{width:86px;height:86px;flex:0 0 auto}
            .donut-value{font-size:26px;font-weight:700;fill:var(--ink)}
            .tone-passed{color:var(--pass)} .tone-failed{color:var(--fail)}
            .tone-error{color:var(--err)} .tone-skipped{color:var(--skip)}
            .tone-neutral{color:var(--ink)} .tone-muted{color:var(--muted)}
            .tone-accent{color:var(--accent)}

            /* ---------- bars ---------- */
            .bars{display:flex;flex-direction:column;gap:8px}
            .bar-row{display:grid;grid-template-columns:150px 1fr 70px;align-items:center;gap:10px}
            .bar-label{font-size:12px;color:var(--muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
            .bar-track{height:9px;background:var(--track);border-radius:99px;overflow:hidden}
            .bar-fill{display:block;height:100%;border-radius:99px;background:var(--accent)}
            .bar-fill.tone-passed{background:var(--pass)} .bar-fill.tone-failed{background:var(--fail)}
            .bar-fill.tone-skipped{background:var(--skip)} .bar-fill.tone-accent{background:var(--accent)}
            .bar-count{font-size:12px;text-align:right;color:var(--muted);font-variant-numeric:tabular-nums}

            /* ---------- toolbar ---------- */
            .toolbar{display:flex;flex-wrap:wrap;align-items:center;gap:10px;position:sticky;top:0;z-index:20}
            .search{flex:1 1 280px;min-width:220px;padding:9px 12px;border:1px solid var(--line);
              border-radius:8px;background:var(--bg);color:var(--ink);font-size:13px}
            .search:focus{outline:2px solid var(--accent);outline-offset:1px}
            .chips{display:flex;flex-wrap:wrap;gap:6px}
            .chip{border:1px solid var(--line);background:var(--bg);color:var(--muted);
              padding:6px 11px;border-radius:99px;font-size:12px;cursor:pointer;font-weight:600}
            .chip:hover{border-color:var(--accent)}
            .chip.active{background:var(--accent);border-color:var(--accent);color:#fff}
            .chip-count{opacity:.75;font-weight:500}
            .toolbar-buttons{display:flex;gap:6px;margin-left:auto}
            .filter-note{flex-basis:100%;margin:0;font-size:12px;color:var(--muted)}
            .btn{border:1px solid var(--line);background:var(--bg);color:var(--ink);
              padding:6px 12px;border-radius:8px;font-size:12px;cursor:pointer;font-weight:600}
            .btn:hover{border-color:var(--accent);color:var(--accent)}

            /* ---------- pills ---------- */
            .pill{display:inline-block;padding:3px 10px;border-radius:99px;font-size:11px;
              font-weight:700;letter-spacing:.04em}
            .pill-lg{padding:6px 14px;font-size:12px}
            .pill-passed{background:var(--pass-bg);color:var(--pass)}
            .pill-failed{background:var(--fail-bg);color:var(--fail)}
            .pill-error{background:var(--err-bg);color:var(--err)}
            .pill-skipped{background:var(--skip-bg);color:var(--skip)}
            .pill-neutral{background:var(--track);color:var(--muted)}

            /* ---------- suites ---------- */
            .suite{padding:0;overflow:hidden}
            .suite-head,.case-head{display:flex;align-items:center;gap:10px;cursor:pointer;
              list-style:none;padding:14px 18px}
            .suite-head::-webkit-details-marker,.case-head::-webkit-details-marker{display:none}
            .suite-head{border-bottom:1px solid transparent}
            .suite[open] .suite-head{border-bottom-color:var(--line)}
            .suite-head:hover,.case-head:hover{background:var(--bg)}
            .chevron{width:0;height:0;border-left:5px solid var(--muted);
              border-top:4px solid transparent;border-bottom:4px solid transparent;
              transition:transform .15s ease;flex:0 0 auto}
            details[open]>summary>.chevron{transform:rotate(90deg)}
            .suite-title{font-weight:700;font-size:15px}
            .suite-counts{display:flex;gap:4px;margin-left:8px}
            .count-badge{min-width:22px;text-align:center;padding:1px 6px;border-radius:6px;
              font-size:11px;font-weight:700;background:var(--track)}
            .count-badge.tone-passed{background:var(--pass-bg);color:var(--pass)}
            .count-badge.tone-failed{background:var(--fail-bg);color:var(--fail)}
            .count-badge.tone-error{background:var(--err-bg);color:var(--err)}
            .count-badge.tone-skipped{background:var(--skip-bg);color:var(--skip)}
            .suite-time{margin-left:auto;font-size:12px;color:var(--muted);font-variant-numeric:tabular-nums}
            .suite-body{padding:6px 18px 18px}
            .suite-message{font-size:13px;color:var(--muted);margin:2px 0 12px}

            /* ---------- cases ---------- */
            .case{border:1px solid var(--line);border-radius:10px;margin:8px 0;overflow:hidden;background:var(--panel)}
            .case-head{padding:10px 14px}
            .dot{width:8px;height:8px;border-radius:50%;flex:0 0 auto}
            .dot-passed{background:var(--pass)} .dot-failed{background:var(--fail)}
            .dot-error{background:var(--err)} .dot-skipped{background:var(--skip)}
            .method{font-size:10px;font-weight:800;letter-spacing:.06em;padding:3px 7px;
              border-radius:5px;background:var(--track);color:var(--muted);flex:0 0 auto;min-width:54px;text-align:center}
            .method-get{background:#e3ecfd;color:#2b50c8} .method-post{background:#e2f5ec;color:#12855f}
            .method-put{background:#fdf1e3;color:#a35c11} .method-patch{background:#f4e9fd;color:#7e2fc4}
            .method-delete{background:#fdeaec;color:#c02b3d}
            html[data-theme="dark"] .method-get{background:#16243f;color:#7fa4ff}
            html[data-theme="dark"] .method-post{background:#0f2a20;color:#4cc590}
            html[data-theme="dark"] .method-put{background:#2c2213;color:#e0a253}
            html[data-theme="dark"] .method-patch{background:#241733;color:#c084fc}
            html[data-theme="dark"] .method-delete{background:#2d1418;color:#ff7d8e}
            .case-name{font-weight:600;flex:0 1 auto;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:34%}
            .case-url{color:var(--muted);font-size:12px;font-family:ui-monospace,Menlo,Consolas,monospace;
              flex:1 1 auto;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;direction:rtl;text-align:left}
            .case-stats{display:flex;align-items:center;gap:8px;margin-left:auto;flex:0 0 auto}
            .code{font-weight:700;font-size:12px;padding:2px 7px;border-radius:5px;background:var(--track)}
            .code.tone-passed{background:var(--pass-bg);color:var(--pass)}
            .code.tone-failed{background:var(--fail-bg);color:var(--fail)}
            .code.tone-skipped{background:var(--skip-bg);color:var(--skip)}
            .code.tone-accent{background:var(--track);color:var(--accent)}
            .latency,.assert-count{font-size:12px;color:var(--muted);font-variant-numeric:tabular-nums}
            .case-body{padding:4px 16px 16px;border-top:1px solid var(--line);background:var(--bg)}
            .case-message{font-size:13px;font-weight:600;margin:12px 0}
            .error-detail{background:var(--fail-bg);color:var(--fail);padding:10px 12px;
              border-radius:8px;font-size:12px;white-space:pre-wrap;word-break:break-word;margin:0 0 12px}

            /* ---------- tabs ---------- */
            .tab-strip{display:flex;gap:2px;border-bottom:1px solid var(--line);margin-bottom:12px;flex-wrap:wrap}
            .tab{border:none;background:none;color:var(--muted);padding:8px 13px;font-size:12px;
              font-weight:700;cursor:pointer;border-bottom:2px solid transparent;letter-spacing:.02em}
            .tab:hover{color:var(--ink)}
            .tab.active{color:var(--accent);border-bottom-color:var(--accent)}
            .tab-panel{display:none}
            .tab-panel.active{display:block}

            /* ---------- tables ---------- */
            table.data{width:100%;border-collapse:collapse;margin:6px 0;font-size:12.5px;
              background:var(--panel);border:1px solid var(--line);border-radius:8px;overflow:hidden}
            table.data th{background:var(--track);text-align:left;padding:8px 10px;
              font-size:11px;text-transform:uppercase;letter-spacing:.04em;color:var(--muted);font-weight:700}
            table.data td{padding:8px 10px;border-top:1px solid var(--line);vertical-align:top}
            table.kv td:first-child{width:28%}
            .wrap-cell{word-break:break-word;max-width:420px}
            .row-failed{background:var(--fail-bg)}
            .warn-list{margin:6px 0;padding-left:18px;color:var(--skip);font-size:12.5px}
            .warnings{border-left:3px solid var(--skip)}
            .warnings ul{margin:0;padding-left:18px;font-size:13px}

            /* ---------- code ---------- */
            .code-block{position:relative;margin:6px 0}
            .code-block pre{background:var(--code-bg);color:var(--code-ink);padding:13px 15px;
              border-radius:8px;overflow:auto;max-height:440px;margin:0;
              font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px;line-height:1.5;
              white-space:pre-wrap;word-break:break-word}
            .copy-btn{position:absolute;top:8px;right:8px;background:rgba(255,255,255,.12);
              color:#e2e8f0;border:none;border-radius:6px;padding:4px 9px;font-size:11px;
              cursor:pointer;font-weight:600;opacity:0;transition:opacity .15s}
            .code-block:hover .copy-btn{opacity:1}
            .copy-btn:hover{background:rgba(255,255,255,.24)}

            .stat-strip{display:flex;flex-wrap:wrap;gap:20px;padding:12px 14px;background:var(--panel);
              border:1px solid var(--line);border-radius:8px;margin:6px 0 4px}
            .stat{display:flex;flex-direction:column;gap:1px}
            .stat-label{font-size:10px;text-transform:uppercase;letter-spacing:.05em;color:var(--muted)}
            .stat-value{font-size:14px;font-weight:700;font-variant-numeric:tabular-nums}

            .plain-summary{cursor:pointer;font-weight:700;font-size:13px;list-style:none}
            .plain-summary::-webkit-details-marker{display:none}
            .footer{padding:26px 0 40px;text-align:center;color:var(--muted);font-size:12px}

            @media (max-width:760px){
              .case-url{display:none}
              .bar-row{grid-template-columns:110px 1fr 60px}
              .toolbar{position:static}
            }
            @media print{
              .toolbar,.topbar-actions,.copy-btn{display:none !important}
              body{background:#fff}
              .card,.case{box-shadow:none;break-inside:avoid}
              details{open:true}
              .tab-panel{display:block !important}
              .tab-strip{display:none}
            }
            """;

    private static final String SCRIPT = """
            (function () {
              var root = document.documentElement;

              /* ---------- theme ---------- */
              var toggle = document.getElementById('themeToggle');
              function readStored() {
                try { return window.localStorage.getItem('apiReportTheme'); }
                catch (e) { return null; }
              }
              function store(value) {
                try { window.localStorage.setItem('apiReportTheme', value); }
                catch (e) { /* file:// may block storage; theme simply resets */ }
              }
              function applyTheme(theme) {
                root.setAttribute('data-theme', theme);
                if (toggle) { toggle.textContent = theme === 'dark' ? 'Light' : 'Dark'; }
              }
              var stored = readStored();
              if (stored) {
                applyTheme(stored);
              } else if (window.matchMedia
                  && window.matchMedia('(prefers-color-scheme: dark)').matches) {
                applyTheme('dark');
              }
              if (toggle) {
                toggle.addEventListener('click', function () {
                  var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                  applyTheme(next);
                  store(next);
                });
              }

              /* ---------- tabs ---------- */
              document.querySelectorAll('[data-tabs]').forEach(function (group) {
                var tabs = group.querySelectorAll('.tab');
                var panels = group.querySelectorAll('.tab-panel');
                tabs.forEach(function (tab, index) {
                  tab.addEventListener('click', function () {
                    tabs.forEach(function (t) { t.classList.remove('active'); });
                    panels.forEach(function (p) { p.classList.remove('active'); });
                    tab.classList.add('active');
                    if (panels[index]) { panels[index].classList.add('active'); }
                  });
                });
              });

              /* ---------- copy buttons ---------- */
              document.querySelectorAll('.copy-btn').forEach(function (button) {
                button.addEventListener('click', function (event) {
                  event.preventDefault();
                  event.stopPropagation();
                  var block = button.parentElement.querySelector('code');
                  var text = block ? block.textContent : '';
                  var done = function () {
                    button.textContent = 'Copied';
                    window.setTimeout(function () { button.textContent = 'Copy'; }, 1400);
                  };
                  if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(text).then(done, function () {
                      button.textContent = 'Failed';
                    });
                  } else {
                    var area = document.createElement('textarea');
                    area.value = text;
                    document.body.appendChild(area);
                    area.select();
                    try { document.execCommand('copy'); done(); }
                    catch (e) { button.textContent = 'Failed'; }
                    document.body.removeChild(area);
                  }
                });
              });

              /* ---------- filtering ---------- */
              var cases = Array.prototype.slice.call(document.querySelectorAll('[data-case]'));
              var suites = Array.prototype.slice.call(document.querySelectorAll('[data-suite]'));
              var search = document.getElementById('searchBox');
              var chips = Array.prototype.slice.call(document.querySelectorAll('.chip'));
              var note = document.getElementById('filterNote');
              var noMatches = document.getElementById('noMatches');
              var activeStatus = 'all';

              function applyFilter() {
                var term = search ? search.value.trim().toLowerCase() : '';
                var visible = 0;

                cases.forEach(function (node) {
                  var statusOk = activeStatus === 'all'
                      || node.getAttribute('data-status') === activeStatus;
                  var textOk = term === ''
                      || (node.getAttribute('data-search') || '').indexOf(term) !== -1;
                  var show = statusOk && textOk;
                  node.classList.toggle('hidden', !show);
                  if (show) {
                    visible++;
                    if (term !== '' || activeStatus !== 'all') { node.open = true; }
                  }
                });

                suites.forEach(function (suite) {
                  var shown = suite.querySelectorAll('[data-case]:not(.hidden)').length;
                  var hasCases = suite.querySelectorAll('[data-case]').length > 0;
                  suite.classList.toggle('hidden', hasCases && shown === 0);
                  if (shown > 0 && (term !== '' || activeStatus !== 'all')) {
                    suite.open = true;
                  }
                });

                if (noMatches) { noMatches.classList.toggle('hidden', visible > 0); }
                if (note) {
                  note.textContent = (term === '' && activeStatus === 'all')
                      ? ''
                      : 'Showing ' + visible + ' of ' + cases.length + ' test cases.';
                }
              }

              if (search) { search.addEventListener('input', applyFilter); }
              chips.forEach(function (chip) {
                chip.addEventListener('click', function () {
                  chips.forEach(function (c) { c.classList.remove('active'); });
                  chip.classList.add('active');
                  activeStatus = chip.getAttribute('data-status');
                  applyFilter();
                });
              });

              function setAll(open) {
                document.querySelectorAll('details').forEach(function (node) {
                  node.open = open;
                });
              }
              var expand = document.getElementById('expandAll');
              var collapse = document.getElementById('collapseAll');
              if (expand) { expand.addEventListener('click', function () { setAll(true); }); }
              if (collapse) {
                collapse.addEventListener('click', function () {
                  setAll(false);
                  suites.forEach(function (s) { s.open = false; });
                });
              }

              /* Keyboard shortcut: "/" focuses the filter box. */
              document.addEventListener('keydown', function (event) {
                if (event.key === '/' && document.activeElement !== search && search) {
                  event.preventDefault();
                  search.focus();
                }
              });
            })();
            """;
}
