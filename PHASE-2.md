# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces AI-oriented test generation, response analysis, assertion suggestion, negative test-data generation, failure root-cause analysis, report insights, provider integration, executable AI negative tests, per-test failure insight orchestration, AI-aware reporting, controlled negative-suite expansion, approved AI test-generation orchestration, and report visibility for AI generation decisions. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

## Phase 2.1 implemented

`AiTestGenerationRequest`, `AiGeneratedTestSuite`, and `AiTestCaseGenerator` generate positive and optional negative API test cases, status assertions, body assertions, and header assertions. The current deterministic strategy is `heuristic-ai-v1`.

## Phase 2.2 implemented

`AiResponseAnalysisRequest`, `AiResponseAnalysis`, and `AiResponseAnalyzer` analyze HTTP status, empty successful bodies, JSON response format, and response time. The analyzer is provider-neutral and deterministic.

## Phase 2.3 implemented

`AiAssertionSuggestion`, `AiAssertionSuggestionResult`, and `AiAssertionSuggester` create executable status, body, JSON, and Content-Type assertions using the existing validation model.

## Phase 2.4 implemented

`AiNegativeTestData`, `AiNegativeTestDataResult`, and `AiNegativeTestDataGenerator` create deterministic negative scenarios including invalid field type, missing required field, empty request body, unsupported content type, and missing content type. Generated data is stored in `AiNegativeTestDataResult.testData`.

## Phase 2.5 implemented

`AiFailureAnalysis` and `AiFailureAnalyzer` classify failures as `SERVER_ERROR`, `CLIENT_ERROR`, `RESPONSE_FORMAT`, `API_CONTRACT`, or `NONE`, with severity, evidence, likely root cause, and recommendations.

## Phase 2.6 implemented

`TestReportDto` contains run-level AI insight fields. `AiReportInsightBuilder` populates them through `ReportService` without changing the stable report name. HTML, JSON, and CSV reports expose the run-level AI insight data.

## Phase 2.7 implemented

The provider abstraction contains `AiProvider`, `AiProviderRequest`, `AiProviderResponse`, `HeuristicAiProvider`, `HttpAiProvider`, and `AiProviderFactory`. The default provider is deterministic `heuristic-ai-v1`. External HTTP providers can be configured without storing API keys in source control.

## Phase 2.8 implemented

`AiNegativeTestCaseBuilder` converts AI-generated negative data into executable `TestCaseDto` instances while preserving the source request contract. `AiFailureInsightService` connects an executed test response to response analysis and failure analysis.

## Phase 2.9 implemented

Per-test `AiFailureAnalysis` is attached to `TestCaseExecutionResult` after HTTP execution. HTML, JSON, and CSV reports expose per-test AI failure information while preserving the existing report contract.

## Phase 2.10 implemented

Phase 2.10 adds controlled AI-generated negative test-suite expansion.

### Negative suite model

`AiGeneratedNegativeTestSuite` groups generated negative `TestCaseDto` objects and records source test case ID, AI strategy, explicit enabled state, generated test cases, and generated test case count.

### Negative suite builder

`AiNegativeTestSuiteBuilder` combines `AiNegativeTestDataResult` with `AiNegativeTestCaseBuilder`. This creates an isolated negative suite without modifying the original source test case.

### Safe execution policy

`AiNegativeTestSuiteExecutionPolicy` provides one deterministic gate for generated negative tests. A suite must exist, be enabled, and contain generated test cases before it is executable.

### Phase 2.10 tests

`AiNegativeTestSuiteBuilderTest` verifies source traceability, deterministic strategy, disabled and enabled states, executable generated test IDs, and invalid builder inputs.

## Phase 2.11 implemented

Phase 2.11 adds a review and approval workflow for AI-generated tests before they are attached to a real test suite.

### Orchestration result

`AiTestGenerationOrchestrationResult` stores the source suite ID, source generated test case ID, positive generated suite, optional negative generated suite, review status and findings, approval state, attachment state, and total generated test count.

`getAllGeneratedTestCases()` combines positive and negative generated cases for review and controlled attachment.

### Generation workflow

`AiTestGenerationOrchestrator.generate(...)` coordinates positive generation, optional negative data generation, negative suite construction, and creation of a reviewable result. Generation does not modify the target `TestSuiteDto`.

### Review workflow

`AiTestGenerationOrchestrator.review(...)` validates generated cases, IDs, duplicate IDs, target-suite conflicts, and request URLs. Findings are stored in the orchestration result and review passes only when all checks succeed.

### Approval workflow

`AiTestGenerationOrchestrator.approve(...)` requires a successful review before setting `approved=true`.

### Controlled attachment

`AiTestGenerationOrchestrator.attachApproved(...)` requires successful review and explicit approval, protects against duplicate IDs, attaches generated positive and negative tests, enables the generated negative suite, and records `attached=true`.

### Phase 2.11 tests

`AiTestGenerationOrchestratorTest` verifies non-mutating generation and review, approval requirements, successful positive and negative attachment, negative-suite enablement, and duplicate-ID protection.

## Phase 2.12 implemented

Phase 2.12 adds report visibility for AI generation decisions across HTML, JSON, and CSV.

### `AiGenerationReportMetadata`

Added `src/main/java/org/ai/testing/ai/model/AiGenerationReportMetadata.java`.

The model records source suite ID, source test case ID, positive and negative generated test counts, AI strategy, review status, review result, approval state, attachment state, and review findings.

### `AiTestGenerationOrchestrationResult.toReportMetadata()`

Creates a report-safe snapshot of the generation decision and copies review findings to avoid mutation during report rendering.

### `TestReportDto`

Added `aiGenerationMetadata` so reports can expose generation decisions without changing the existing run-level AI insight fields.

### `ReportService`

Added `generateAllReports(TestRunResultDto, AiTestGenerationOrchestrationResult)`. It converts the orchestration result into metadata and passes the same metadata to HTML, JSON, and CSV generation. The existing method remains backward compatible.

### HTML

`AiHtmlReportEnhancer` now renders an `AI Generation Decision` section containing source traceability, positive and negative counts, strategy, review state, approval state, attachment state, and review findings.

### JSON

Jackson serializes `aiGenerationMetadata` automatically as part of `TestReportDto`.

### CSV

Added columns for strategy, source suite, source test case, positive count, negative count, review status, review result, approval state, attachment state, and review findings.

### Phase 2.12 tests

`AiReportRenderingTest` verifies AI generation decision metadata in HTML, JSON, and CSV.

## Phase 1 + Phase 2 regression coverage

`Phase1RegressionTest` executes GET, POST, PUT, PATCH and DELETE through the real executor chain against deterministic local HTTP endpoints.

`Phase2RegressionTest` covers AI test generation, response analysis, assertion suggestions, negative test-data generation, failure analysis, the default heuristic provider, and provider request/response handling.

## Main.java and Petstore integration regression

`Main.java` executes the Swagger Petstore integration regression using `GET https://petstore3.swagger.io/api/v3/openapi.json`. The test expects HTTP 200, sends `Accept: application/json`, validates a non-empty response body, prints detailed failure diagnostics, and verifies HTML, JSON and CSV reports.

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
7. Report metadata is a snapshot and does not mutate the orchestration result.
8. External AI providers are optional and must be explicitly configured.
9. Regression tests use deterministic local HTTP endpoints where external mutable behavior is unnecessary.

## Development rule

Every Phase 2 change must update this document with implementation changes, affected classes and methods, test coverage, CI or runtime validation, and known limitations or next-step integration work.

## Validation status

Phase 2.11 CI validation is green after the null source-request fix. Phase 2.12 implementation and unit test changes are committed, but the new commit requires CI validation before declaring Phase 2.12 green.

## Known limitation

Phase 2.12 exposes generation decisions when `ReportService.generateAllReports(TestRunResultDto, AiTestGenerationOrchestrationResult)` is used. Existing callers using the original method continue to produce reports without generation metadata. Interactive dashboard approval is still outside this phase.

## Next planned phase

Add an execution-aware AI generation workflow that can attach approved tests and optionally execute them as a separate AI-generated suite, while preserving source traceability, execution-policy controls, and report visibility.
