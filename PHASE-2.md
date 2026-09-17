# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Phase 2.19 implemented

Phase 2.19 upgrades the embedded AI historical execution dashboard from a static table into an interactive report component. The dashboard remains part of the generated HTML report and does not require an external web server or JavaScript library.

### Interactive historical filtering

The HTML dashboard now provides client-side controls for:

1. Source suite filtering.
2. Passed or failed status filtering.
3. Execution ID or suite text search.
4. Configurable visible row count.
5. Previous and next pagination controls.

Filtering and pagination operate on the historical rows already included in the report. No database connection is exposed to the browser.

### Historical trend visualization

Added a lightweight inline SVG pass-rate chart.

The chart:

1. Uses the historical execution records already present in the report.
2. Displays pass rate as the y-axis metric.
3. Shows an execution point for each historical record.
4. Updates when suite, status, or search filters change.
5. Uses native SVG and browser JavaScript only, so there is no charting dependency or external network request.

Each chart point includes the execution ID and pass rate in a native browser tooltip.

### Dashboard metrics

The historical dashboard continues to show:

- source suite
- execution count
- overall trend
- latest pass rate
- pass-rate change
- failed-test change
- first pass rate
- latest pass rate
- first failed-test count
- latest failed-test count
- execution history table

The existing HTML escaping rules remain in place for dynamic text. Values inserted into JavaScript data are separately escaped.

### Regression test

`HistoryAwareReportIntegrationTest` was extended to verify that generated HTML contains:

1. Suite filter control.
2. Status filter control.
3. Search control.
4. Page-size control.
5. Filtering function.
6. Pagination function.
7. Inline SVG chart.
8. Chart rendering function.
9. Pass-rate trend heading.

The test continues to verify that HTML, JSON, and CSV retain the historical execution records and `IMPROVED` trend.

## Phase 2.18 validation history

Phase 2.18 completed full Phase 1, Phase 2, and Petstore regression validation. The workflow verifies HTML, JSON, and CSV report generation and uploads Surefire and API report artifacts.

## Previous phases

Phase 2.17 connected approved AI execution to persistent history and embedded historical report metadata. Phase 2.16 introduced SQLite-backed persistent AI execution history. Phase 2.15 introduced historical execution tracking and comparison concepts. Phase 2.14 added AI execution reporting across HTML, JSON, and CSV. Phase 2.13 added controlled execution of approved AI-generated tests. Phase 2.12 added AI generation decision reporting. Phase 2.11 added generation review and approval. Phase 2.10 added controlled negative-suite expansion. Earlier phases added AI generation, response analysis, assertion suggestions, negative test data, failure analysis, provider abstraction, and per-test AI insights.

## Usage

History-enabled execution continues to use the existing report flow:

```java
try (AiExecutionHistoryStore store =
         new AiExecutionHistoryStore("jdbc:sqlite:data/ai-execution-history.db")) {
    AiExecutionHistoryService historyService = new AiExecutionHistoryService(store);
    AiGeneratedSuiteExecutor executor =
            new AiGeneratedSuiteExecutor(new TestCaseExecutor(), historyService);
    AiGeneratedSuiteExecutionResult result = executor.execute(orchestrationResult);

    List<AiExecutionHistoryEntry> history =
            historyService.getBySourceSuite(orchestrationResult.getSourceSuiteId());
    AiExecutionHistoryTrend trend =
            new AiExecutionHistoryTrendService(historyService)
                    .calculate(orchestrationResult.getSourceSuiteId());

    reportService.generateAllReports(
            testRunResult,
            orchestrationResult,
            result,
            trend,
            history);
}
```

Open the generated HTML report in a browser. The `AI Historical Execution Dashboard` provides filtering, pagination, search, and pass-rate visualization without requiring another service.

## Safety and determinism rules

1. The default AI provider remains `heuristic-ai-v1`.
2. Generated negative suites are not enabled automatically.
3. Existing manually authored test cases are not modified by generation or review.
4. Generated test IDs retain source test case information for traceability.
5. AI-generated tests are not attached until review passes and explicit approval is recorded.
6. Duplicate test IDs are rejected before attachment.
7. Report and history metadata are snapshots and do not mutate source execution objects.
8. AI-generated execution is isolated from normal regression totals.
9. Negative generated execution requires the explicit negative-suite execution policy.
10. External AI providers are optional and must be explicitly configured.
11. Regression tests use deterministic local HTTP endpoints where external mutable behavior is unnecessary.
12. Historical AI execution data is isolated from normal regression history.
13. SQLite persistence is selected explicitly through the `AiExecutionHistoryStore` JDBC URL.
14. History integrity tests verify that failed and skipped executions retain their correct classification.
15. Automatic history recording is enabled only when an `AiExecutionHistoryService` is explicitly injected.
16. Historical report data is copied into report metadata and does not expose database connections.
17. HTML historical values are escaped before rendering.
18. The interactive dashboard uses only data already embedded in the report.
19. No external JavaScript or chart service is required by the dashboard.
20. Historical filters affect report presentation only and do not modify persisted history.

## Validation status

Phase 2.19 implementation and regression-test updates are committed. GitHub Actions validation must complete on the new commit before declaring Phase 2.19 green.

## Known limitation

The dashboard is still embedded inside the generated HTML report. Filtering and charting are client-side and operate on the records included in that report. A future standalone dashboard could add server-side history queries, date-range filtering, larger-history pagination, and richer visualizations.

## Next planned phase

Run the complete Maven regression suite and GitHub Actions workflow for Phase 2.19. If the regression remains green, the next phase can focus on richer historical analytics such as date-range filtering, suite-level comparison views, and export-oriented history summaries.
