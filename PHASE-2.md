# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces AI-oriented test generation, response analysis, assertion suggestion, negative test-data generation, failure root-cause analysis, report insights, provider integration, executable AI negative tests, per-test failure insight orchestration, and AI-aware reporting. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

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

`AiFailureAnalyzer.analyze(...)` accepts three arguments: `ResponseDto`, `AiResponseAnalysis`, and expected HTTP status code.

## Phase 2.6 implemented

`TestReportDto` contains `aiSummary`, `aiSeverity`, `aiFindings`, and `aiRecommendations`. `AiReportInsightBuilder` populates these fields through `ReportService` without changing the stable report name. HTML, JSON, and CSV reports expose the run-level AI insight data.

## Phase 2.7 implemented

The provider abstraction contains `AiProvider`, `AiProviderRequest`, `AiProviderResponse`, `HeuristicAiProvider`, `HttpAiProvider`, and `AiProviderFactory`. The default provider is deterministic `heuristic-ai-v1`. External HTTP providers can be configured without storing API keys in source control.

## Phase 2.8 implemented

Phase 2.8 connects the previously independent AI components to the executable testing flow.

### Executable negative test cases

`AiNegativeTestCaseBuilder` converts each `AiNegativeTestData` item into a normal `TestCaseDto` that can be executed by the existing framework. It preserves:

1. HTTP method.
2. URL.
3. Headers.
4. Query parameters.
5. Path parameters.
6. Generated request body.
7. Expected negative HTTP status code.
8. A status-code assertion.
9. Scenario and reason in the generated test case ID, name and description.

This means generated negative data no longer stops at an AI model object. It can enter the same `TestRunExecutor -> TestSuiteExecutor -> TestCaseExecutor -> ExecutorDispatcher` pipeline as manually authored tests.

### Failure insight orchestration

`AiFailureInsightService` connects an executed `TestCaseExecutionResult` to:

`AiResponseAnalyzer -> AiFailureAnalyzer`

The service uses the actual `ResponseDto`, expected HTTP status code, and configurable response-time threshold. It returns `AiFailureAnalysis` with failure detection, category, severity, evidence, root cause, and recommendations.

The service validates that an execution result and response are available before analysis.

### Phase 2.8 tests

`AiNegativeTestCaseBuilderTest` verifies:

1. AI negative data is converted into executable test cases.
2. Generated IDs use the AI negative-test naming convention.
3. HTTP methods are preserved.
4. Expected status codes are preserved.
5. Status-code assertions are generated.
6. Generated headers and request bodies are preserved without mutating the source test case.

`AiFailureInsightServiceTest` verifies:

1. HTTP 500 responses are classified as `SERVER_ERROR` with `CRITICAL` severity.
2. Healthy HTTP 200 responses produce `NONE` and `INFO`.
3. Missing response data is rejected with a clear validation exception.

## Phase 2.9 implemented

Phase 2.9 attaches AI failure insights to every executed test case that receives an HTTP response and exposes those insights in all report formats without changing the existing report name contract.

### Per-test execution integration

`TestCaseExecutor.TestCaseExecutionResult` now contains:

`AiFailureAnalysis aiFailureAnalysis`

After normal HTTP execution and validation, `TestCaseExecutor` invokes `AiFailureInsightService` using the actual execution result, expected status code, and a default 2000 ms response-time threshold. The resulting analysis is stored directly on the test result.

The default behavior remains deterministic and uses the existing heuristic AI implementation. Tests with no HTTP response, disabled tests, or execution failures without a response keep the AI field empty rather than fabricating an analysis.

### HTML reporting

`AiHtmlReportGenerator` wraps the existing `HtmlReportGenerator` and then applies `AiHtmlReportEnhancer`.

The enhancer adds an `AI Test Insights` section containing, per test case:

1. Failure detected flag.
2. Severity.
3. Failure category.
4. Summary.
5. Likely root cause.
6. Evidence.
7. Recommendations.

All dynamic AI text is HTML escaped before rendering.

### JSON reporting

The existing JSON report automatically serializes the new `aiFailureAnalysis` field inside each `TestCaseExecutionResult` because the project already uses Jackson serialization with Lombok DTO accessors.

No existing report fields were renamed or removed.

### CSV reporting

`CsvReportGenerator` now includes dedicated per-test columns:

1. `AI Failure Detected`
2. `AI Severity Per Test`
3. `AI Category`
4. `AI Summary Per Test`
5. `AI Root Cause`
6. `AI Evidence`
7. `AI Recommendations Per Test`

Run-level AI fields remain unchanged and continue to appear separately.

### Phase 2.9 tests

`AiPerTestInsightIntegrationTest` verifies the complete HTTP execution path against a local deterministic HTTP 500 endpoint and confirms that the resulting test execution object contains a `SERVER_ERROR` AI insight with evidence and recommendations.

`AiReportRenderingTest` verifies that:

1. HTML contains the per-test AI insight section.
2. JSON contains the serialized `aiFailureAnalysis` object and failure data.
3. CSV contains the dedicated AI columns and failure data.

## Phase 1 + Phase 2 regression coverage

The project contains dedicated regression tests.

### Phase 1

`Phase1RegressionTest` executes the complete HTTP executor matrix through the real `TestRunExecutor` flow:

1. `GET` against a deterministic local endpoint.
2. `POST` against a deterministic local endpoint.
3. `PUT` against a deterministic local endpoint.
4. `PATCH` against a deterministic local endpoint.
5. `DELETE` against a deterministic local endpoint.

The test starts an in-process Java `HttpServer` on an automatically assigned local port. Each HTTP method has its own endpoint and returns a deterministic HTTP 200 JSON response. This removes CI dependence on the mutable public Swagger Petstore POST, PUT, PATCH and DELETE endpoints while preserving real HTTP execution.

### Phase 2

`Phase2RegressionTest` covers AI test-case generation, response analysis, assertion suggestions, negative test-data generation, failure analysis, the default heuristic provider, and provider request/response handling.

Phase 2.8 adds dedicated unit coverage for executable negative test-case conversion and integrated failure insight analysis.

Phase 2.9 adds per-test execution and report rendering coverage.

## Main.java and Petstore integration regression

`Main.java` executes the Swagger Petstore integration regression through `TestRunExecutor` using:

`GET https://petstore3.swagger.io/api/v3/openapi.json`

The test expects HTTP 200, sends `Accept: application/json`, and validates a non-empty response body. It also prints detailed failure diagnostics and verifies that HTML, JSON, and CSV reports exist and are non-empty.

The OpenAPI endpoint is used instead of `/pet/1` or `/store/inventory` to avoid mutable sample data and public sample database dependency issues.

The Phase 1 HTTP method regression uses a deterministic local server for all five HTTP executor implementations. The stable Petstore OpenAPI GET remains in `Main.java` as the external integration check.

## GitHub Actions regression flow

The workflow provides this regression chain:

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
        +--> AI negative test data
        +--> Executable negative test cases
        +--> AI response analysis
        +--> AI failure insights
        |
        v
Petstore Integration Regression
        |
        v
HTML + JSON + CSV Reports
        |
        +--> Run-level AI insights
        +--> Per-test AI failure insights
```

The workflow runs:

1. `mvn -B clean test -U`
2. Verifies Phase 1 and Phase 2 Surefire reports.
3. Runs `org.ai.testing.Main`.
4. Verifies HTML, JSON and CSV reports.
5. Uploads Surefire results.
6. Uploads generated API reports.

## CI issue resolution

The Phase 1 regression originally used public Swagger Petstore POST, PUT, PATCH and DELETE endpoints. Those operations can fail independently of the test framework because they depend on external service behavior and mutable sample data.

The regression was changed to start an in-process Java `HttpServer` and execute all five HTTP methods against deterministic local endpoints. The production executor chain and assertions remain unchanged.

This resolves the CI failure where `Phase1RegressionTest.shouldExecuteAllHttpMethodsAndGenerateAllReports` reported:

```text
Test run failed. Passed suites: 0, Failed suites: 1
```

The fix does not weaken the assertion and does not skip any HTTP method.

## Development rule

Every Phase 2 change must update this document with:

1. Implementation added or changed.
2. Affected classes and methods.
3. Test coverage.
4. CI or runtime validation results.
5. Known limitations or next-step integration work.

## Validation status

Phase 2.9 implementation and tests have been committed to `feature/02-ai-test-generation`. The latest commit is awaiting GitHub Actions validation before the phase can be declared fully green. The GitHub commit status currently reports `pending` with no completed status checks yet.

## Next planned phase

Add AI-driven test-suite expansion and execution controls around generated negative tests, including safe enablement, tagging, traceability to the source test case, and deterministic regression coverage.
