# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces AI-oriented test generation, response analysis, assertion suggestion, negative test-data generation, failure root-cause analysis, report insights, provider integration, executable AI negative tests, per-test failure insight orchestration, AI-aware reporting, controlled negative-suite expansion, and approved AI test-generation orchestration. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

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

`AiGeneratedNegativeTestSuite` groups generated negative `TestCaseDto` objects and records:

1. Source test case ID.
2. AI strategy.
3. Explicit enabled state.
4. Generated test cases.
5. Generated test case count.

The suite is disabled by default when constructed by callers unless they explicitly enable it.

### Negative suite builder

`AiNegativeTestSuiteBuilder` combines `AiNegativeTestDataResult` with `AiNegativeTestCaseBuilder`. This creates an isolated negative suite without modifying the original source test case.

The builder preserves the source test case relationship through `sourceTestCaseId` and preserves the generator strategy through the suite metadata.

### Safe execution policy

`AiNegativeTestSuiteExecutionPolicy` provides one deterministic gate for generated negative tests:

```text
suite exists
    +
suite enabled
    +
generated test cases exist
    =
executable
```

Generated negative tests are therefore not automatically injected into a normal regression run. A caller must explicitly enable the generated suite before it is considered executable.

This prevents AI-generated negative tests from unexpectedly changing an existing production regression run.

### Phase 2.10 tests

`AiNegativeTestSuiteBuilderTest` verifies:

1. Generated suites retain the source test case ID.
2. The deterministic AI strategy is retained.
3. Suites are disabled when explicitly requested as disabled.
4. Explicitly enabled suites become executable.
5. Generated test IDs remain traceable to the source test case.
6. Invalid builder inputs are rejected.

## Phase 2.11 implemented

Phase 2.11 adds a review and approval workflow for AI-generated tests before they are attached to a real test suite.

### Orchestration result

`AiTestGenerationOrchestrationResult` stores the complete reviewable generation result:

1. Target suite ID.
2. Source generated test case ID.
3. Positive generated suite.
4. Optional negative generated suite.
5. Review status and findings.
6. Explicit approval state.
7. Attachment state.
8. Total generated test count.

`getAllGeneratedTestCases()` combines positive and negative generated cases for review and controlled attachment.

### Generation workflow

`AiTestGenerationOrchestrator.generate(...)` coordinates:

```text
AI generation request
        |
        v
Positive test generation
        |
        +---- optional negative data generation
                         |
                         v
                  Negative suite
        |
        v
Reviewable orchestration result
```

Generation does not modify the target `TestSuiteDto`.

### Review workflow

`AiTestGenerationOrchestrator.review(...)` validates:

1. Generated test cases exist.
2. Generated test case IDs are present.
3. Generated IDs are unique.
4. Generated IDs do not already exist in the target suite.
5. Generated requests contain a URL.

Review findings are stored in the orchestration result. The result is marked `PASSED` only when all checks succeed.

### Approval workflow

`AiTestGenerationOrchestrator.approve(...)` requires a successful review before setting `approved=true`.

This makes approval an explicit application decision rather than an automatic side effect of AI generation.

### Controlled attachment

`AiTestGenerationOrchestrator.attachApproved(...)` requires both successful review and explicit approval. It then attaches generated positive and negative tests to the target suite, protects against duplicate IDs, marks the generated negative suite enabled, and records `attached=true`.

Calling generation or review alone cannot change the existing regression suite.

### Phase 2.11 tests

`AiTestGenerationOrchestratorTest` verifies:

1. Generation and review do not mutate the target suite.
2. Attachment is rejected without explicit approval.
3. Approved positive and negative tests are attached successfully.
4. The generated negative suite becomes enabled only after approved attachment.
5. Duplicate IDs fail review and prevent approval.

## Phase 1 + Phase 2 regression coverage

The project contains dedicated regression tests.

### Phase 1

`Phase1RegressionTest` executes GET, POST, PUT, PATCH and DELETE through the real executor chain against deterministic local HTTP endpoints.

### Phase 2

`Phase2RegressionTest` covers AI test generation, response analysis, assertion suggestions, negative test-data generation, failure analysis, the default heuristic provider, and provider request/response handling.

Phase 2.8 adds executable negative test conversion and failure insight orchestration.

Phase 2.9 adds per-test AI execution and report rendering coverage.

Phase 2.10 adds controlled negative-suite expansion and execution-policy coverage.

Phase 2.11 adds generation, review, explicit approval, duplicate protection, and controlled suite attachment coverage.

## Main.java and Petstore integration regression

`Main.java` executes the Swagger Petstore integration regression using:

`GET https://petstore3.swagger.io/api/v3/openapi.json`

The test expects HTTP 200, sends `Accept: application/json`, validates a non-empty response body, prints detailed failure diagnostics, and verifies HTML, JSON and CSV reports.

The Phase 1 HTTP method regression uses a deterministic local server for all five HTTP executor implementations. The Petstore OpenAPI GET remains the external integration check.

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
7. External AI providers are optional and must be explicitly configured.
8. Regression tests use deterministic local HTTP endpoints where external mutable behavior is unnecessary.

## Development rule

Every Phase 2 change must update this document with:

1. Implementation added or changed.
2. Affected classes and methods.
3. Test coverage.
4. CI or runtime validation results.
5. Known limitations or next-step integration work.

## Validation status

Phase 2.11 implementation and unit tests have been committed to `feature/02-ai-test-generation`. CI validation is required before declaring the latest phase fully green.

## Known limitation

Phase 2.11 provides the orchestration API and safe attachment boundary. It does not yet persist approval decisions or expose an interactive dashboard approval button. Those can be added after CI validation of this phase.

## Next planned phase

Add report visibility for AI generation decisions, including generated test counts, review findings, approval state, attachment state, and source traceability in HTML, JSON, and CSV outputs.
