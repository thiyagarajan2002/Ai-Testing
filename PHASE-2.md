# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Phase 2.17 implemented

Phase 2.17 connects approved AI execution history to reporting and adds a historical execution dashboard in the HTML report.

### Automatic history recording

`AiExecutionHistoryService.recordApprovedExecution(...)` records an execution only after the AI generation result has passed review, received explicit approval, and been attached.

`AiGeneratedSuiteExecutor` supports an optional `AiExecutionHistoryService`. When supplied, approved positive and explicitly enabled negative AI suite executions are automatically persisted after execution.

Existing constructors remain backward compatible and do not create a database implicitly.

### `AiExecutionHistoryTrend`

Added `src/main/java/org/ai/testing/ai/history/AiExecutionHistoryTrend.java`.

The trend contains source suite ID, execution count, first and latest pass rates, pass-rate change, first and latest failed-test counts, failure-count change, and an overall trend of `IMPROVED`, `REGRESSED`, or `UNCHANGED`.

The pass-rate boundary uses `0.0001` and treats values at the boundary as unchanged.

### `AiExecutionHistoryTrendService`

Added `src/main/java/org/ai/testing/ai/history/AiExecutionHistoryTrendService.java`.

The service calculates historical trend data either from a source suite ID or from an already loaded history list. Empty history is rejected explicitly.

### Historical report metadata

Added `src/main/java/org/ai/testing/ai/model/AiHistoryReportMetadata.java`.

`TestReportDto` now supports `aiHistoryMetadata`, containing the aggregated trend and a copied list of historical execution snapshots. This keeps report data separate from the live history store.

`ReportService.generateAllReports(...)` now has an overload that accepts trend and historical execution data while preserving all existing overloads.

### HTML historical dashboard

`AiHtmlReportEnhancer` now renders `AI Historical Execution Dashboard` when history metadata is supplied.

The dashboard includes:

1. Source suite ID.
2. Number of historical executions.
3. Overall trend.
4. First pass rate.
5. Latest pass rate.
6. Pass-rate change.
7. First failed-test count.
8. Latest failed-test count.
9. Failure-count change.
10. Execution history table with execution ID, timestamp, pass rate, failed count, and status.

All dynamic values are HTML escaped.

### SQLite lifecycle validation

`AiExecutionHistoryStore` now explicitly tracks its lifecycle. Operations after close throw `IllegalStateException`, the first close succeeds, and a second close throws `IllegalStateException`. SQLite persistence behavior is unchanged.

## Phase 2.16 implemented

Phase 2.16 adds SQLite-backed persistent storage for isolated AI-generated suite execution history. Records survive application restarts and remain separate from normal regression totals.

### `AiExecutionHistoryEntry`

Added `src/main/java/org/ai/testing/ai/history/AiExecutionHistoryEntry.java`.

The snapshot stores execution ID, timestamp, source suite ID, source test case ID, execution state, pass/fail state, message, total/passed/failed/skipped counts, pass rate, and failed AI test case IDs. `from(...)` creates a historical snapshot from `AiGeneratedSuiteExecutionResult`.

### `AiExecutionHistoryStore`

Added `src/main/java/org/ai/testing/ai/history/AiExecutionHistoryStore.java`.

The store uses SQLite through JDBC. The schema is created automatically and includes an index on source suite and execution time. It supports `save(...)`, `findAll()`, `findBySourceSuite(...)`, `findLatest(...)`, and `close()`.

Prepared statements are used for database operations. History is isolated from `TestRunResultDto` and normal regression totals.

### `AiExecutionHistoryComparison`

Added `src/main/java/org/ai/testing/ai/history/AiExecutionHistoryComparison.java`.

It compares two historical snapshots and reports previous pass rate, current pass rate, pass-rate change, and trend: `IMPROVED`, `REGRESSED`, or `UNCHANGED`.

### `AiExecutionHistoryService`

Added `src/main/java/org/ai/testing/ai/history/AiExecutionHistoryService.java`.

The service coordinates snapshot creation, persistence, history retrieval, source-suite filtering, latest execution lookup, and comparison of the latest two executions.

### Maven dependency

`pom.xml` now includes:

`org.xerial:sqlite-jdbc:3.46.1.3`

The existing Java 21, Lombok, JUnit 5.12.2, Jackson, and Surefire configuration remains unchanged.

### Tests

History tests cover SQLite persistence, source-suite filtering, pass-rate calculation, latest comparison, improvement detection, insufficient-history protection, lifecycle behavior, trend boundaries, and report metadata.

## Usage

```java
try (AiExecutionHistoryStore store =
         new AiExecutionHistoryStore("jdbc:sqlite:data/ai-execution-history.db")) {
    AiExecutionHistoryService historyService = new AiExecutionHistoryService(store);
    historyService.record(executionResult);
    List<AiExecutionHistoryEntry> history =
            historyService.getBySourceSuite("SUITE-01");

    AiExecutionHistoryTrendService trendService =
            new AiExecutionHistoryTrendService(historyService);
    AiExecutionHistoryTrend trend = trendService.calculate("SUITE-01");
}
```

For automatic recording during approved AI execution:

```java
AiExecutionHistoryStore store =
        new AiExecutionHistoryStore("jdbc:sqlite:data/ai-execution-history.db");
AiExecutionHistoryService historyService = new AiExecutionHistoryService(store);
AiGeneratedSuiteExecutor executor =
        new AiGeneratedSuiteExecutor(new TestCaseExecutor(), historyService);
```

Reopening the same SQLite JDBC URL reads records from the previous application process.

## Previous phases

Phase 2.15 introduced historical AI execution tracking and comparison concepts. Phase 2.14 added AI execution reporting across HTML, JSON, and CSV. Phase 2.13 added controlled execution of approved AI-generated tests. Phase 2.12 added AI generation decision reporting. Phase 2.11 added generation review and approval. Phase 2.10 added controlled negative-suite expansion. Earlier phases added AI generation, response analysis, assertion suggestions, negative test data, failure analysis, provider abstraction, and per-test AI insights.

## Regression coverage

`Phase1RegressionTest` exercises GET, POST, PUT, PATCH, and DELETE through the real executor chain against deterministic local HTTP endpoints. `Phase2RegressionTest` covers the AI generation and analysis workflow.

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
15. Automatic history recording is opt-in through constructor injection, so existing callers do not create database files unexpectedly.
16. Historical report metadata is copied before being attached to reports.
17. Historical HTML values are escaped before insertion into the report.

## Development rule

Every Phase 2 change must update this document with implementation changes, affected classes and methods, test coverage, CI or runtime validation, and known limitations or next-step integration work.

## Validation status

Phase 2.17 implementation is committed. GitHub Actions validation must complete before declaring the phase green.

## Known limitation

The historical dashboard is currently rendered in the generated HTML report when history metadata is supplied. JSON receives the history metadata through `TestReportDto`. CSV historical columns and a standalone interactive dashboard page are future integration work.

## Next planned phase

Add automated history-aware report generation from the persisted SQLite store, extend CSV with historical trend columns, and add regression tests that validate the complete history-to-report flow.
