# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

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

Added `AiExecutionHistoryStoreTest` covering SQLite persistence, source-suite filtering, pass-rate calculation, latest comparison, improvement detection, and insufficient-history protection.

## Step 23: History data integrity validation

Added `src/test/java/org/ai/testing/ai/history/AiExecutionHistoryEntryDataIntegrityTest.java`.

The tests verify that historical snapshots preserve execution data without changing its meaning:

1. Zero test cases produce a zero pass rate and zero counts.
2. Pass rate is calculated as passed tests divided by total tests multiplied by 100.
3. Failed and skipped test counts are preserved correctly.
4. Only executed failed test cases are captured in `failedTestCaseIds`.
5. Skipped test cases are not incorrectly classified as failures.

The validation uses `AiGeneratedSuiteExecutionResult.addResult(...)` and `AiExecutionHistoryEntry.from(...)`, so the test covers the actual history snapshot conversion path.

## Usage

```java
try (AiExecutionHistoryStore store =
         new AiExecutionHistoryStore("jdbc:sqlite:data/ai-execution-history.db")) {
    AiExecutionHistoryService historyService = new AiExecutionHistoryService(store);
    historyService.record(executionResult);
    List<AiExecutionHistoryEntry> history =
            historyService.getBySourceSuite("SUITE-01");
}
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

## Development rule

Every Phase 2 change must update this document with implementation changes, affected classes and methods, test coverage, CI or runtime validation, and known limitations or next-step integration work.

## Validation status

Phase 2.16 implementation, SQLite persistence code, and history integrity validation are committed. GitHub Actions validation must complete before declaring this phase green.

## Known limitation

The persistent store is currently a local SQLite database selected by the application through its JDBC URL. Automatic wiring into every AI execution and a dedicated interactive historical trend dashboard are future integration work.

## Next planned phase

Validate history isolation across source suites, including same execution IDs in different suites and suite-specific latest/comparison behavior.
