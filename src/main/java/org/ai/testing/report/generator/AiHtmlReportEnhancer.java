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
import java.util.Locale;

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

        html.append("<div id=\"ai-history-dashboard\" style=\"border-top:2px solid #343a40;padding:15px 0;\">");
        html.append("<h3>AI Historical Execution Dashboard</h3>");
        AiExecutionHistoryTrend trend = metadata.getTrend();

        if (trend != null) {
            html.append("<div style=\"display:flex;flex-wrap:wrap;gap:12px;margin:12px 0;\">");
            metricCard(html, "Source Suite", trend.getSourceSuiteId());
            metricCard(html, "Executions", String.valueOf(trend.getExecutionCount()));
            metricCard(html, "Trend", trend.getTrend());
            metricCard(html, "Latest Pass Rate", formatRate(trend.getLatestPassRate()) + "%");
            metricCard(html, "Pass Rate Change", formatSigned(trend.getPassRateChange()) + "%");
            metricCard(html, "Failure Change", formatSignedInt(trend.getFailedTestCaseChange()));
            html.append("</div>");
        }

        List<AiExecutionHistoryEntry> history = metadata.getHistory();
        if (history == null || history.isEmpty()) {
            html.append("<p id=\"ai-history-empty\">No historical execution records available.</p>");
            html.append("</div>");
            return;
        }

        html.append("<div style=\"display:flex;flex-wrap:wrap;gap:10px;margin:15px 0;\">");
        html.append("<label>Suite <select id=\"ai-history-suite-filter\" onchange=\"filterAiHistory()\"><option value=\"ALL\">All suites</option>");
        java.util.LinkedHashSet<String> suites = new java.util.LinkedHashSet<>();
        for (AiExecutionHistoryEntry entry : history) {
            if (entry != null && entry.getSourceSuiteId() != null) suites.add(entry.getSourceSuiteId());
        }
        for (String suite : suites) {
            html.append("<option value=\"").append(escapeAttribute(suite)).append("\">").append(escape(suite)).append("</option>");
        }
        html.append("</select></label>");
        html.append("<label>Status <select id=\"ai-history-status-filter\" onchange=\"filterAiHistory()\">")
                .append("<option value=\"ALL\">All statuses</option><option value=\"PASSED\">Passed</option><option value=\"FAILED\">Failed</option></select></label>");
        html.append("<label>Search <input id=\"ai-history-search\" type=\"search\" placeholder=\"Execution ID or suite\" oninput=\"filterAiHistory()\"></label>");
        html.append("<label>Rows <select id=\"ai-history-page-size\" onchange=\"filterAiHistory()\"><option>5</option><option>10</option><option>25</option></select></label>");
        html.append("</div>");

        html.append("<p><strong>First Pass Rate:</strong> ").append(formatRate(trend == null ? 0 : trend.getFirstPassRate()))
                .append("% | <strong>Latest Pass Rate:</strong> ").append(formatRate(trend == null ? 0 : trend.getLatestPassRate()))
                .append("% | <strong>First Failed Tests:</strong> ").append(trend == null ? 0 : trend.getFirstFailedTestCases())
                .append(" | <strong>Latest Failed Tests:</strong> ").append(trend == null ? 0 : trend.getLatestFailedTestCases()).append("</p>");

        html.append("<div style=\"overflow-x:auto;\"><table id=\"ai-history-table\" style=\"width:100%;border-collapse:collapse;\">");
        html.append("<thead><tr>")
                .append(th("Execution ID")).append(th("Suite")).append(th("Executed At"))
                .append(th("Pass Rate")).append(th("Failed")).append(th("Status"))
                .append("</tr></thead><tbody>");
        for (AiExecutionHistoryEntry entry : history) {
            if (entry == null) continue;
            String status = entry.isPassed() ? "PASSED" : "FAILED";
            String suite = entry.getSourceSuiteId() == null ? "" : entry.getSourceSuiteId();
            html.append("<tr class=\"ai-history-row\" data-suite=\"").append(escapeAttribute(suite))
                    .append("\" data-status=\"").append(status).append("\" data-search=\"")
                    .append(escapeAttribute((entry.getExecutionId() == null ? "" : entry.getExecutionId()) + " " + suite))
                    .append("\">");
            html.append(td(entry.getExecutionId())).append(td(suite)).append(td(String.valueOf(entry.getExecutedAt())))
                    .append(td(formatRate(entry.getPassRate()) + "%"))
                    .append(td(String.valueOf(entry.getFailedTestCases()))).append(td(status));
            html.append("</tr>");
        }
        html.append("</tbody></table></div>");
        html.append("<div style=\"display:flex;justify-content:space-between;align-items:center;margin-top:10px;\">");
        html.append("<span id=\"ai-history-count\"></span>");
        html.append("<div><button type=\"button\" onclick=\"changeAiHistoryPage(-1)\">Previous</button> <span id=\"ai-history-page\"></span> <button type=\"button\" onclick=\"changeAiHistoryPage(1)\">Next</button></div>");
        html.append("</div>");

        html.append("<h4>Pass Rate Trend</h4>");
        html.append("<div style=\"overflow-x:auto;\"><svg id=\"ai-history-chart\" viewBox=\"0 0 760 260\" role=\"img\" aria-label=\"AI execution pass rate trend\" style=\"width:100%;min-width:600px;height:260px;border:1px solid #dee2e6;\"></svg></div>");
        html.append(historyDataScript(history));
        html.append("</div>");
    }

    private void metricCard(StringBuilder html, String label, String value) {
        html.append("<div style=\"border:1px solid #dee2e6;border-radius:6px;padding:10px 14px;min-width:130px;\">")
                .append("<div style=\"font-size:12px;\">").append(escape(label)).append("</div>")
                .append("<strong>").append(escape(value)).append("</strong></div>");
    }

    private String th(String value) {
        return "<th style=\"text-align:left;border-bottom:1px solid #dee2e6;padding:8px;\">" + escape(value) + "</th>";
    }

    private String td(String value) {
        return "<td style=\"padding:8px;border-bottom:1px solid #f1f3f5;\">" + escape(value) + "</td>";
    }

    private String historyDataScript(List<AiExecutionHistoryEntry> history) {
        StringBuilder data = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            AiExecutionHistoryEntry entry = history.get(i);
            if (entry == null) continue;
            if (data.length() > 1) data.append(',');
            data.append("{\"id\":\"").append(jsEscape(entry.getExecutionId())).append("\",\"suite\":\"")
                    .append(jsEscape(entry.getSourceSuiteId())).append("\",\"rate\":").append(entry.getPassRate()).append("}");
        }
        data.append("]");
        return "<script>" +
                "const aiHistoryData=" + data + ";" +
                "let aiHistoryPage=1;" +
                "function aiHistoryRows(){return Array.from(document.querySelectorAll('#ai-history-table .ai-history-row'));}" +
                "function filterAiHistory(){" +
                "const suite=document.getElementById('ai-history-suite-filter').value;" +
                "const status=document.getElementById('ai-history-status-filter').value;" +
                "const search=document.getElementById('ai-history-search').value.toLowerCase();" +
                "const size=parseInt(document.getElementById('ai-history-page-size').value,10);" +
                "const rows=aiHistoryRows().filter(r=>(suite==='ALL'||r.dataset.suite===suite)&&(status==='ALL'||r.dataset.status===status)&&r.dataset.search.toLowerCase().includes(search));" +
                "const pages=Math.max(1,Math.ceil(rows.length/size));aiHistoryPage=Math.min(aiHistoryPage,pages);" +
                "rows.forEach((r,i)=>r.style.display=(i>=(aiHistoryPage-1)*size&&i<aiHistoryPage*size)?'table-row':'none');" +
                "aiHistoryRows().filter(r=>!rows.includes(r)).forEach(r=>r.style.display='none');" +
                "document.getElementById('ai-history-count').textContent=rows.length+' matching execution(s)';" +
                "document.getElementById('ai-history-page').textContent='Page '+aiHistoryPage+' / '+pages;" +
                "drawAiHistoryChart(rows);" +
                "}" +
                "function changeAiHistoryPage(delta){aiHistoryPage=Math.max(1,aiHistoryPage+delta);filterAiHistory();}" +
                "function drawAiHistoryChart(rows){" +
                "const svg=document.getElementById('ai-history-chart');if(!svg)return;" +
                "const selected=rows.map(r=>{const id=r.dataset.search.split(' ')[0];return aiHistoryData.find(x=>x.id===id)||aiHistoryData.find(x=>r.dataset.suite===x.suite);}).filter(Boolean);" +
                "const points=selected.length?selected:aiHistoryData;const w=760,h=260,p=35;svg.innerHTML='';" +
                "if(!points.length){svg.innerHTML='<text x=35 y=130>No chart data</text>';return;}" +
                "const max=Math.max(100,...points.map(x=>x.rate));const min=Math.min(0,...points.map(x=>x.rate));" +
                "const sx=i=>p+i*Math.max(1,(w-2*p)/Math.max(1,points.length-1));" +
                "const sy=v=>h-p-(v-min)*((h-2*p)/Math.max(1,max-min));" +
                "let path='';points.forEach((x,i)=>{path+=(i?'L':'M')+sx(i)+' '+sy(x.rate)+' ';});" +
                "svg.innerHTML='<line x1=\"35\" y1=\"225\" x2=\"725\" y2=\"225\" stroke=\"currentColor\"/><line x1=\"35\" y1=\"35\" x2=\"35\" y2=\"225\" stroke=\"currentColor\"/><path d=\"'+path+'\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"3\"/>' + points.map((x,i)=>'<circle cx=\"'+sx(i)+'\" cy=\"'+sy(x.rate)+'\" r=\"4\" fill=\"currentColor\"><title>'+x.id+': '+x.rate+'%</title></circle>').join('') + '<text x=\"40\" y=\"25\">Pass rate %</text>';" +
                "}" +
                "document.addEventListener('DOMContentLoaded',filterAiHistory);" +
                "</script>";
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
        html.append("<p><strong>").append(escape(title)).append(":</strong></p><ul>");
        for (String value : values) html.append("<li>").append(escape(value)).append("</li>");
        html.append("</ul>");
    }

    private String formatRate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatSigned(double value) {
        return String.format(Locale.ROOT, "%+.2f", value);
    }

    private String formatSignedInt(int value) {
        return String.format(Locale.ROOT, "%+d", value);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String escapeAttribute(String value) {
        return escape(value);
    }

    private String jsEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("</", "<\\/");
    }
}
