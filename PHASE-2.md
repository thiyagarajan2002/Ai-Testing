# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces AI-oriented test generation, response analysis, assertion suggestion, negative test-data generation, failure root-cause analysis, report insights, provider integration, executable AI negative tests, per-test failure insight orchestration, AI-aware reporting, controlled negative-suite expansion, approved AI test-generation orchestration, report visibility for AI generation decisions, isolated execution of approved AI-generated tests, isolated execution reporting, and historical AI execution tracking. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

## Phase 2.15 implemented

Phase 2.15 adds isolated historical tracking and comparison for AI-generated suite executions. Historical records never modify normal regression totals.

### `AiExecutionHistoryEntry`

Added `src/main/java/org/ai/testing/ai/model/AiExecutionHistoryEntry.java`.

The model stores an execution ID, timestamp, source suite and test case traceability, execution state, pass/fail/skipped counts, execution message, and failed AI test case IDs. The `from(...)` factory creates a snapshot from `AiGeneratedSuiteExecutionResult`.

### `AiExecutionHistoryStore`

Added `src/main/java/org/ai/testing/ai/AiExecutionHistoryStore.java`.

The store keeps isolated in-memory execution history. It supports recording executions, retrieving all history, filtering by source suite, and clearing history. Returned lists are defensive snapshots.

### `AiExecutionHistoryComparison`

Added `src/main/java/org/ai/testing/ai/model/AiExecutionHistoryComparison.java`.

The comparison model records baseline and latest execution IDs, test counts, pass rates, pass-rate change, and whether the latest execution improved or regressed.

Pass rate is calculated as:

`passed tests / total tests * 100`

### `AiExecutionHistoryService`

Added `src/main/java/org/ai/testing/ai/AiExecutionHistoryService.java`.

The service coordinates recording, history retrieval, source-suite filtering, latest-execution comparison, and history clearing. `compareLatest(...)` requires at least two executions for the requested source suite.

### Phase 2.15 tests

`AiExecutionHistoryServiceTest` verifies recording, source-suite filtering, latest-execution comparison, pass-rate change calculation, improvement detection, and insufficient-history protection.

## Phase 2.14 implemented

Phase 2.14 adds report visibility for isolated AI-generated suite execution across HTML, JSON, and CSV while keeping generated execution separate from normal regression totals.

### `AiExecutionReportMetadata`

The report-safe snapshot records source suite ID, source test case ID, execution state, overall result, message, total tests, passed tests, failed tests, skipped tests, and failed AI test case IDs.

### `TestReportDto`

Added `aiExecutionMetadata` so the report model can expose isolated AI execution information independently from normal regression statistics.

### `ReportService`

Added `generateAllReports(TestRunResultDto, AiTestGenerationOrchestrationResult, AiGeneratedSuiteExecutionResult)`.

### HTML dashboard

`AiHtmlReportEnhancer` renders an `AI Generated Execution Dashboard` section containing execution status, pass/fail state, source traceability, total/passed/failed/skipped counts, execution message, and failed AI test case IDs.

### JSON and CSV

JSON serializes `aiExecutionMetadata`. CSV exposes execution status, result, source IDs, counts, message, and failed test IDs.

## Phase 2.13 implemented

Phase 2.13 adds controlled execution of approved AI-generated tests without changing normal regression execution. Review, approval, attachment, and negative-suite execution policy gates are required.

## Phase 2.12 implemented

Phase 2.12 adds report visibility for AI generation decisions across HTML, JSON, and CSV.

## Phase 2.11 implemented

Phase 2.11 adds generation review, approval, and controlled attachment.

## Phase 2.10 implemented

Phase 2.10 adds controlled AI-generated negative test-suite expansion.

## Phase 2.9 implemented

Per-test `AiFailureAnalysis` is attached after HTTP execution and exposed through HTML, JSON, and CSV.

## Phase 2.8 implemented

`AiNegativeTestCaseBuilder` converts generated negative data into executable `TestCaseDto` instances. `AiFailureInsightService` connects response analysis and failure analysis.

## Phase 2.7 implemented

The provider abstraction contains deterministic heuristic and optional HTTP AI providers.

## Phase 2.6 implemented

Run-level AI insight fields were added to reports.

## Phase 2.5 implemented

AI failure analysis classifies server, client, response-format, API-contract, and no-failure conditions.

## Phase 2.4 implemented

Deterministic negative test-data generation covers invalid field type, missing required field, empty body, unsupported content type, and missing content type.

## Phase 2.3 implemented

AI assertion suggestion creates executable status, body, JSON, and Content-Type assertions.

## Phase 2.2 implemented

AI response analysis evaluates status, body, JSON format, and response time.

## Phase 2.1 implemented

AI positive and optional negative test generation uses the deterministic `heuristic-ai-v1` strategy.

## Phase 1 + Phase 2 regression coverage

`Phase1RegressionTest` executes GET, POST, PUT, PATCH and DELETE through the real executor chain against deterministic local HTTP endpoints.

`Phase2RegressionTest` covers AI generation, response analysis, assertion suggestions, negative data generation, failure analysis, provider behavior, and related AI workflows.

## Main.java and Petstore integration regression

`Main.java` executes the Swagger Petstore integration regression using `GET https://petstore3.swagger.io/api/v3/openapi.json`. The test expects HTTP 200, sends `Accept: application/json`, validates a non-empty response body, prints detailed diagnostics, and verifies HTML, JSON and CSV reports.

## GitHub Actions regression flow

```text
Phase 1 HTTP Method Regression
        |
        +--> Local GET
        +--> Local POST
        +--> Local PUT
        +--> Local PATCH
        +--> Local DELETE
        |
        v
Phase 2 AI Regression
        |
        +--> AI positive generation
        +--> AI negative test data
        +--> Executable negative test cases
        +--> Controlled negative suite
        +--> AI generation review
        +--> Explicit AI approval
        +--> Controlled suite attachment
        +--> AI generation decision metadata
        +--> Isolated AI-generated execution
        +--> AI execution report metadata
        +--> AI execution dashboard
        +--> Historical AI execution tracking
        +--> AI execution comparison
        +--> AI response analysis
        +--> AI failure insights
        |
        v
Petstore Integration Regression
        |
        v
HTML + JSON + CSV Reports
```

## Safety and determinism rules

1. The default AI provider remains `heuristic-ai-v1`.
2. Generated negative suites are not enabled automatically.
3. Existing manually authored test cases are not modified by generation or review.
4. Generated test IDs retain source test case information for traceability.
5. AI-generated tests are not attached until review passes and explicit approval is recorded.
6. Duplicate test IDs are rejected before attachment.
7. Report and history metadata are snapshots and do not mutate the source execution objects.
8. AI-generated execution is isolated from normal regression totals.
9. Negative generated execution requires the explicit negative-suite execution policy.
10. External AI providers are optional and must be explicitly configured.
11. Regression tests use deterministic local HTTP endpoints where external mutable behavior is unnecessary.
12. Historical AI execution data is isolated from normal regression history.

## Development rule

Every Phase 2 change must update this document with implementation changes, affected classes and methods, test coverage, CI or runtime validation, and known limitations or next-step integration work.

## Validation status

Phase 2.15 implementation, tests, and documentation are committed. GitHub Actions validation must complete before declaring this phase green.

## Known limitation

Phase 2.15 uses an in-memory history store. History is lost when the application process stops. Persistent storage and a dedicated interactive historical dashboard remain future work.

## Next planned phase

Add persistent AI execution history and a dedicated trend/reporting view while preserving the separation between AI-generated execution history and normal regression results.
