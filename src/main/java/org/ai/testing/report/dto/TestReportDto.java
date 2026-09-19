package org.ai.testing.report.dto;

import org.ai.testing.testrun.dto.TestRunResultDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The envelope handed to every report generator. */
public class TestReportDto {

    private String reportId;
    private String reportName;
    private String reportFormat;
    private LocalDateTime generatedAt;
    private String generatedBy = "AI API Testing Agent";
    private String toolVersion;

    private TestRunResultDto testRunResult;

    /** Optional narrative fields an analysis step can populate. */
    private String summary;
    private String severity;
    private List<String> findings = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public void setToolVersion(String toolVersion) {
        this.toolVersion = toolVersion;
    }

    public TestRunResultDto getTestRunResult() {
        return testRunResult;
    }

    public void setTestRunResult(TestRunResultDto testRunResult) {
        this.testRunResult = testRunResult;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public List<String> getFindings() {
        return findings;
    }

    public void setFindings(List<String> findings) {
        this.findings = findings == null ? new ArrayList<>() : findings;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations == null ? new ArrayList<>() : recommendations;
    }
}
