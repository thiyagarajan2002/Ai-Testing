package org.ai.testing.report.service;

import org.ai.testing.report.dto.TestReportDto;
import org.ai.testing.testrun.dto.TestRunResultDto;

/**
 * Builds deterministic AI-style report insights from test-run statistics.
 * This class is provider-neutral and does not call an external LLM.
 */
public class AiReportInsightBuilder {

    public void populate(TestReportDto report) {
        if (report == null || report.getTestRunResult() == null) {
            throw new IllegalArgumentException("Report and test run result are required");
        }

        TestRunResultDto run = report.getTestRunResult();
        report.getAiFindings().clear();
        report.getAiRecommendations().clear();

        if (run.getFailedTestCases() > 0 || run.getFailedSuites() > 0) {
            report.setAiSeverity("HIGH");
            report.setAiSummary("AI analysis detected failed API tests that require investigation.");
            report.getAiFindings().add("The run contains " + run.getFailedTestCases()
                    + " failed test case(s) and " + run.getFailedSuites() + " failed suite(s).");
            report.getAiRecommendations().add("Review failed assertions, HTTP status codes, response bodies, and response headers.");
            report.getAiRecommendations().add("Use the AI failure analyzer on failed responses to identify likely root causes.");
            return;
        }

        if (run.getSkippedTestCases() > 0 || run.getSkippedSuites() > 0) {
            report.setAiSeverity("MEDIUM");
            report.setAiSummary("AI analysis found a successful run with skipped API tests.");
            report.getAiFindings().add("The run contains " + run.getSkippedTestCases()
                    + " skipped test case(s) and " + run.getSkippedSuites() + " skipped suite(s).");
            report.getAiRecommendations().add("Review skipped tests and confirm that they are intentionally disabled or unavailable.");
            return;
        }

        report.setAiSeverity("INFO");
        report.setAiSummary("AI analysis found no failed or skipped tests in the run.");
        report.getAiFindings().add("All executed test cases and suites passed.");
        report.getAiRecommendations().add("Continue monitoring response time, assertions, and API contract changes.");
    }
}
