# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces AI-oriented test generation, response analysis, assertion suggestion, negative test-data generation, failure root-cause analysis, report insights, provider integration, executable AI negative tests, failure insight orchestration, and CI regression coverage. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

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

`TestReportDto` contains `aiSummary`, `aiSeverity`, `aiFindings`, and `aiRecommendations`. `AiReportInsightBuilder` populates these fields through `ReportService` without changing the stable report name. HTML, JSON, and CSV reports expose the AI insight data.

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

## Next planned phase

Integrate per-test AI failure insights directly into `TestCaseExecutionResult`, HTML, JSON and CSV report sections while keeping AI analysis deterministic by default and preserving the existing report contract.
