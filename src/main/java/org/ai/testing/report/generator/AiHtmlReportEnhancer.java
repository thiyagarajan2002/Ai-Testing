package org.ai.testing.report.generator;

import org.ai.testing.ai.history.AiExecutionHistoryEntry;
import org.ai.testing.ai.history.AiExecutionHistoryTrend;
import org.ai.testing.ai.model.AiExecutionReportMetadata;
import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.ai.model.AiGenerationReportMetadata;
import org.ai.testing.ai.model.AiHistoryReportMetadata;
import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Adds AI generation, isolated execution, historical trends, and per-test failure insights to an HTML report.
 */
public class AiHtmlReportEnhancer {

    public void enhance(Path htmlPath, TestReportDto report) {
        if (htmlPath == null) throw new IllegalArgumentException("HTML path cannot be null");
        if (report == null || report.getTestRunResult() == null) {
            throw new IllegalArgumentException("Report and test run result are required");
        }
        try {
            String html = Files.readString(htmlPath, StandardCharsets.UTF_8);
            String section = buildSection(report);
            String marker = "<div class=\"footer\">";
            int index = html.lastIndexOf(marker);
            if (index < 0) index = html.lastIndexOf("</body>");
            if (index < 0) html = html + section;
            else html = html.substring(0, index) + section + html.substring(index);
            Files.writeString(htmlPath, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to enhance HTML report: " + htmlPath, e);
        }
    }

    private String buildSection(TestReportDto report) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"card\" style=\"margin-bottom:20px;\">");
        html.append("<h2>AI Test Insights</h2>");
        html.append("<p><strong>Run AI Severity:</strong> ").append(escape(report.getAiSeverity())).append("</p>");
        html.append("<p><strong>Run AI Summary:</strong> ").append(escape(report.getAiSummary())).append("</p>");
        appendGenerationMetadata(html, report.getAiGenerationMetadata());
        appendExecutionMetadata(html, report.getAiExecutionMetadata());
        appendHistoryMetadata(html, report.getAiHistoryMetadata());

        var run = report.getTestRunResult();
        if (run.getSuiteResults() == null || run.getSuiteResults().isEmpty()) {
            html.append("<p>No per-test AI insights available.</p>");
        } else {
            for (TestSuiteExecutionResultDto suite : run.getSuiteResults()) {
                if (suite == null || suite.getTestResults() == null) continue;
                for (TestCaseExecutor.TestCaseExecutionResult test : suite.getTestResults()) {
                    if (test != null && test.getAiFailureAnalysis() != null) appendInsight(html, test);
                }
            }
        }
        html.append("</div>");
        return html.toString();
    }

    private void appendGenerationMetadata(StringBuilder html, AiGenerationReportMetadata metadata) {
        if (metadata == null) return;
        html.append("<div style=\"border-top:1px solid #dee2e6;padding:15px 0;\">");
        html.append("<h3>AI Generation Decision</h3>");
        html.append("<p><strong>Strategy:</strong> ").append(escape(metadata.getStrategy())).append("</p>");
        html.append("<p><strong>Source Suite:</strong> ").append(escape(metadata.getSourceSuiteId())).append("</p>");
        html.append("<p><strong>Source Test Case:</strong> ").append(escape(metadata.getSourceTestCaseId())).append("</p>");
        html.append("<p><strong>Positive Tests:</strong> ").append(metadata.getPositiveTestCaseCount())
                .append(" | <strong>Negative Tests:</strong> ").append(metadata.getNegativeTestCaseCount()).append("</p>");
        html.append("<p><strong>Review:</strong> ").append(escape(metadata.getReviewStatus()))
                .append(" | <strong>Approved:</strong> ").append(metadata.isApproved() ? "YES" : "NO")
                .append(" | <strong>Attached:</strong> ").append(metadata.isAttached() ? "YES" : "NO").append("</p>");
        appendList(html, "Review Findings", metadata.getReviewFindings());
        html.append("</div>");
    }

    private void appendExecutionMetadata(StringBuilder html, AiExecutionReportMetadata metadata) {
        if (metadata == null) return;
        html.append("<div style=\"border-top:2px solid #495057;padding:15px 0;\">");
        html.append("<h3>AI Generated Execution Dashboard</h3>");
        html.append("<p><strong>Execution:</strong> ").append(metadata.isExecuted() ? "EXECUTED" : "NOT EXECUTED")
                .append(" | <strong>Status:</strong> ").append(metadata.isPassed() ? "PASSED" : "FAILED").append("</p>");
        html.append("<p><strong>Source Suite:</strong> ").append(escape(metadata.getSourceSuiteId()))
                .append(" | <strong>Source Test Case:</strong> ").append(escape(metadata.getSourceTestCaseId())).append("</p>");
        html.append("<p><strong>Total:</strong> ").append(metadata.getTotalTestCases())
                .append(" | <strong>Passed:</strong> ").append(metadata.getPassedTestCases())
                .append(" | <strong>Failed:</strong> ").append(metadata.getFailedTestCases())
                .append(" | <strong>Skipped:</strong> ").append(metadata.getSkippedTestCases()).append("</p>");
        html.append("<p><strong>Message:</strong> ").append(escape(metadata.getMessage())).append("</p>");
        appendList(html, "Failed AI Test Cases", metadata.getFailedTestCaseIds());
        html.append("</div>");
    }

    private void appendHistoryMetadata(StringBuilder html, AiHistoryReportMetadata metadata) {
        if (metadata == null) return;
        html.append("<div style=\"border-top:2px solid #343a40;padding:15px 0;\">");
        html.append("<h3>AI Historical Execution Dashboard</h3>");

        AiExecutionHistoryTrend trend = metadata.getTrend();
        if (trend != null) {
            html.append("<p><strong>Source Suite:</strong> ").append(escape(trend.getSourceSuiteId())).append("</p>");
            html.append("<p><strong>Executions:</strong> ").append(trend.getExecutionCount())
                    .append(" | <strong>Trend:</strong> ").append(escape(trend.getTrend())).append("</p>");
            html.append("<p><strong>First Pass Rate:</strong> ").append(formatRate(trend.getFirstPassRate()))
                    .append("% | <strong>Latest Pass Rate:</strong> ").append(formatRate(trend.getLatestPassRate()))
                    .append("% | <strong>Change:</strong> ").append(formatSigned(trend.getPassRateChange())).append("%</p>");
            html.append("<p><strong>First Failed Tests:</strong> ").append(trend.getFirstFailedTestCases())
                    .append(" | <strong>Latest Failed Tests:</strong> ").append(trend.getLatestFailedTestCases())
                    .append(" | <strong>Failure Change:</strong> ").append(formatSignedInt(trend.getFailedTestCaseChange())).append("</p>");
        }

        List<AiExecutionHistoryEntry> history = metadata.getHistory();
        if (history != null && !history.isEmpty()) {
            html.append("<h4>Execution History</h4>");
            html.append("<table style=\"width:100%;border-collapse:collapse;\">");
            html.append("<thead><tr>")
                    .append("<th style=\"text-align:left;border-bottom:1px solid #dee2e6;padding:8px;\">Execution ID</th>")
                    .append("<th style=\"text-align:left;border-bottom:1px solid #dee2e6;padding:8px;\">Executed At</th>")
                    .append("<th style=\"text-align:left;border-bottom:1px solid #dee2e6;padding:8px;\">Pass Rate</th>")
                    .append("<th style=\"text-align:left;border-bottom:1px solid #dee2e6;padding:8px;\">Failed</th>")
                    .append("<th style=\"text-align:left;border-bottom:1px solid #dee2e6;padding:8px;\">Status</th>")
                    .append("</tr></thead><tbody>");
            for (AiExecutionHistoryEntry entry : history) {
                if (entry == null) continue;
                html.append("<tr>")
                        .append("<td style=\"padding:8px;border-bottom:1px solid #f1f3f5;\">").append(escape(entry.getExecutionId())).append("</td>")
                        .append("<td style=\"padding:8px;border-bottom:1px solid #f1f3f5;\">").append(escape(String.valueOf(entry.getExecutedAt()))).append("</td>")
                        .append("<td style=\"padding:8px;border-bottom:1px solid #f1f3f5;\">").append(formatRate(entry.getPassRate())).append("%</td>")
                        .append("<td style=\"padding:8px;border-bottom:1px solid #f1f3f5;\">").append(entry.getFailedTestCases()).append("</td>")
                        .append("<td style=\"padding:8px;border-bottom:1px solid #f1f3f5;\">").append(entry.isPassed() ? "PASSED" : "FAILED").append("</td>")
                        .append("</tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</div>");
    }

    private String formatRate(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String formatSigned(double value) {
        return String.format(java.util.Locale.ROOT, "%+.2f", value);
    }

    private String formatSignedInt(int value) {
        return String.format(java.util.Locale.ROOT, "%+d", value);
    }

    private void appendInsight(StringBuilder html, TestCaseExecutor.TestCaseExecutionResult test) {
        AiFailureAnalysis insight = test.getAiFailureAnalysis();
        html.append("<div style=\"border-top:1px solid #dee2e6;padding:15px 0;\">");
        html.append("<h3>").append(escape(test.getTestCaseId())).append(" - ").append(escape(test.getTestCaseName())).append("</h3>");
        html.append("<p><strong>Detected:</strong> ").append(insight.isFailureDetected() ? "YES" : "NO")
                .append(" | <strong>Severity:</strong> ").append(escape(insight.getSeverity()))
                .append(" | <strong>Category:</strong> ").append(escape(insight.getCategory())).append("</p>");
        html.append("<p><strong>Summary:</strong> ").append(escape(insight.getSummary())).append("</p>");
        html.append("<p><strong>Likely Root Cause:</strong> ").append(escape(insight.getLikelyRootCause())).append("</p>");
        appendList(html, "Evidence", insight.getEvidence());
        appendList(html, "Recommendations", insight.getRecommendations());
        html.append("</div>");
    }

    private void appendList(StringBuilder html, String title, List<String> values) {
        if (values == null || values.isEmpty()) return;
        html.append("<p><strong>").append(title).append(":</strong></p><ul>");
        for (String value : values) html.append("<li>").append(escape(value)).append("</li>");
        html.append("</ul>");
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
