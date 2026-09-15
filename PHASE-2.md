# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces AI-oriented test generation, response analysis, assertion suggestion, negative test-data generation, failure root-cause analysis, report insights, provider integration, executable AI negative tests, per-test failure insight orchestration, AI-aware reporting, controlled negative-suite expansion, approved AI test-generation orchestration, report visibility for AI generation decisions, isolated execution of approved AI-generated tests, and isolated execution reporting. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

## Phase 2.14 implemented

Phase 2.14 adds report visibility for isolated AI-generated suite execution across HTML, JSON, and CSV while keeping generated execution separate from normal regression totals.

### `AiExecutionReportMetadata`

Added `src/main/java/org/ai/testing/ai/model/AiExecutionReportMetadata.java`.

The report-safe snapshot records source suite ID, source test case ID, execution state, overall result, message, total tests, passed tests, failed tests, skipped tests, and failed AI test case IDs. The `from(...)` factory copies values from `AiGeneratedSuiteExecutionResult` without exposing the mutable execution object directly.

### `TestReportDto`

Added `aiExecutionMetadata` so the report model can expose isolated AI execution information independently from normal regression statistics.

### `ReportService`

Added `generateAllReports(TestRunResultDto, AiTestGenerationOrchestrationResult, AiGeneratedSuiteExecutionResult)`.

The method creates report-safe generation and execution metadata and sends the same metadata snapshot to HTML, JSON, and CSV report generation. Existing report methods remain backward compatible.

### HTML dashboard

`AiHtmlReportEnhancer` now renders an `AI Generated Execution Dashboard` section containing execution status, pass/fail state, source traceability, total/passed/failed/skipped counts, execution message, and failed AI test case IDs.

### JSON

Jackson automatically serializes `aiExecutionMetadata` as part of `TestReportDto`.

### CSV

Added execution columns:

1. `AI Execution Status`
2. `AI Execution Passed`
3. `AI Execution Message`
4. `AI Execution Source Suite ID`
5. `AI Execution Source Test Case ID`
6. `AI Execution Total Tests`
7. `AI Execution Passed Tests`
8. `AI Execution Failed Tests`
9. `AI Execution Skipped Tests`
10. `AI Execution Failed Test IDs`

### Phase 2.14 tests

`AiExecutionReportMetadataTest` verifies report-safe snapshot creation, source traceability, execution counts, failed test IDs, and null input handling.

## Phase 2.13 implemented

Phase 2.13 adds controlled execution of approved AI-generated tests without changing normal regression execution.

### `AiGeneratedSuiteExecutionResult`

The result stores source suite and source test case traceability, execution state, pass/fail/skipped counts, message, and individual `TestCaseExecutionResult` objects.

### `AiGeneratedSuiteExecutor`

`execute(...)` requires review success, explicit approval, and attachment before executing generated positive and negative test cases. It reuses the existing `TestCaseExecutor`, so HTTP dispatch, validation, and per-test AI failure analysis remain consistent with normal API execution.

`executeNegativeSuite(...)` additionally requires `AiNegativeTestSuiteExecutionPolicy` to allow execution.

### Isolation rules

AI-generated execution results remain separate from normal `TestRunResultDto` regression totals.

## Phase 2.12 implemented

Phase 2.12 adds report visibility for AI generation decisions across HTML, JSON, and CSV.

`AiGenerationReportMetadata` records source traceability, generated counts, strategy, review state, approval state, attachment state, and review findings. `ReportService`, HTML, JSON, and CSV expose this metadata.

## Phase 2.11 implemented

Phase 2.11 adds generation review, approval, and controlled attachment. Generated tests are not attached until review passes and explicit approval is recorded.

## Phase 2.10 implemented

Phase 2.10 adds controlled AI-generated negative test-suite expansion. Generated negative tests remain disabled until explicit enablement.

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
7. Report metadata is a snapshot and does not mutate the orchestration or execution result.
8. AI-generated execution is isolated from normal regression totals.
9. Negative generated execution requires the explicit negative-suite execution policy.
10. External AI providers are optional and must be explicitly configured.
11. Regression tests use deterministic local HTTP endpoints where external mutable behavior is unnecessary.

## Development rule

Every Phase 2 change must update this document with implementation changes, affected classes and methods, test coverage, CI or runtime validation, and known limitations or next-step integration work.

## Validation status

Phase 2.14 implementation and unit test changes are committed. GitHub Actions validation must complete before declaring this phase green.

## Known limitation

The AI execution dashboard is currently embedded into the generated HTML report. A separate interactive web dashboard and historical AI execution comparison are future work.

## Next planned phase

Add historical AI execution tracking and comparison so multiple generated-suite executions can be compared without changing the normal regression baseline.
