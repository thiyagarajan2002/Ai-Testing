# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Phase 2.17 implemented

Phase 2.17 connects approved AI execution to persistent history and makes historical execution data visible in generated reports. The history remains separate from normal regression totals.

### Automatic history recording

`AiGeneratedSuiteExecutor` can be constructed with an `AiExecutionHistoryService`. After an approved and attached AI-generated suite execution completes, the execution result is automatically converted to an `AiExecutionHistoryEntry` and persisted. Explicitly enabled negative AI-suite execution follows the same history path.

History persistence remains optional through constructor injection so existing callers that do not configure SQLite retain the previous behavior.

### Historical trend calculation

Added `AiExecutionHistoryTrend` and `AiExecutionHistoryTrendService`.

The trend service calculates:

1. Source suite ID.
2. Number of executions.
3. First pass rate.
4. Latest pass rate.
5. Pass-rate change.
6. First failed-test count.
7. Latest failed-test count.
8. Failed-test count change.
9. Overall trend: `IMPROVED`, `REGRESSED`, or `UNCHANGED`.

The pass-rate boundary uses the same `0.0001` threshold as execution comparison. Floating-point boundary behavior is covered by tests.

### History-aware report metadata

Added `AiHistoryReportMetadata` and `TestReportDto.aiHistoryMetadata`.

The metadata is a report-safe snapshot containing the calculated trend and historical execution records. It does not expose the SQLite connection or mutate the persistent store.

### ReportService integration

`ReportService.generateAllReports(...)` now supports:

```java
AiExecutionHistoryTrend historyTrend,
List<AiExecutionHistoryEntry> history
```

The existing report-generation overloads remain backward compatible. HTML, JSON, and CSV reports receive the same historical metadata snapshot.

### HTML historical dashboard

`AiHtmlReportEnhancer` now renders an `AI Historical Execution Dashboard` containing:

- source suite
- execution count
- overall trend
- first pass rate
- latest pass rate
- pass-rate change
- first failed-test count
- latest failed-test count
- failed-test change
- execution history table
- execution ID
- execution timestamp
- pass rate
- failed-test count
- execution status

Dynamic text is HTML escaped before insertion into the report.

### CSV historical columns

`CsvReportGenerator` now includes:

1. `AI History Source Suite ID`
2. `AI History Execution Count`
3. `AI History First Pass Rate`
4. `AI History Latest Pass Rate`
5. `AI History Pass Rate Change`
6. `AI History First Failed Tests`
7. `AI History Latest Failed Tests`
8. `AI History Failed Test Change`
9. `AI History Trend`
10. `AI History Records`

The values are repeated on report rows in the same way as the existing run-level AI metadata.

### Regression test

Added `src/test/java/org/ai/testing/report/HistoryAwareReportIntegrationTest.java`.

The test generates real HTML, JSON, and CSV reports using temporary files and verifies:

1. Historical metadata reaches `TestReportDto`.
2. Trend values are preserved.
3. HTML contains the historical dashboard and execution records.
4. JSON contains historical metadata and execution IDs.
5. CSV contains historical trend columns and execution records.
6. The `IMPROVED` trend is preserved across all report formats.

## Previous phases

Phase 2.16 introduced SQLite-backed persistent AI execution history. Phase 2.15 introduced historical execution tracking and comparison concepts. Phase 2.14 added AI execution reporting across HTML, JSON, and CSV. Phase 2.13 added controlled execution of approved AI-generated tests. Phase 2.12 added AI generation decision reporting. Phase 2.11 added generation review and approval. Phase 2.10 added controlled negative-suite expansion. Earlier phases added AI generation, response analysis, assertion suggestions, negative test data, failure analysis, provider abstraction, and per-test AI insights.

## Usage

History-enabled execution:

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

## Validation status

Phase 2.17 history persistence integration, trend calculation, HTML historical dashboard, CSV historical columns, and history-aware report integration tests are committed. Maven and GitHub Actions validation must complete before declaring the phase green.

## Known limitation

The historical dashboard is currently embedded in the generated HTML report. It is not yet a standalone interactive web application with filtering, charts, pagination, or date-range controls.

## Next planned phase

Run the complete Maven regression suite and GitHub Actions workflow, then fix any integration failures before adding interactive historical filtering and chart visualization.
