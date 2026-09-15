# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces AI-oriented test generation, response analysis, assertion suggestion, negative test-data generation, failure root-cause analysis, report insights, provider integration, and CI regression coverage. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

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

The regression covers the following Phase 1 components:

1. `TestRunExecutor` executes the complete test-run lifecycle and invokes report generation.
2. `TestSuiteExecutor` is exercised by `TestRunExecutor` while executing the configured suite.
3. `TestCaseExecutor` is exercised for all five HTTP test cases.
4. `ExecutorDispatcher` dispatches GET, POST, PUT, PATCH and DELETE requests.
5. `GetExecutor` performs the local GET request.
6. `PostExecutor` performs the local POST request.
7. `PutExecutor` performs the local PUT request.
8. `PatchExecutor` performs the local PATCH request.
9. `DeleteExecutor` performs the local DELETE request.
10. `AbstractHttpExecutor` provides the shared HTTP execution behavior for all five executors.
11. `ValidationEngine` validates status-code and response-body assertions.
12. `ReportService` generates all report formats for the completed run.
13. `HtmlReportGenerator` generates `reports/test-report.html`.
14. `JsonReportGenerator` generates `reports/test-report.json`.
15. `CsvReportGenerator` generates `reports/test-report.csv`.

The regression expects one suite with five passing test cases and verifies that all three report files exist and are non-empty. The local server makes this regression deterministic and independent of external API availability.

### Phase 2

`Phase2RegressionTest` covers:

1. AI test-case generation.
2. AI response analysis.
3. AI assertion suggestions.
4. AI negative test-data generation.
5. AI failure analysis.
6. Default heuristic AI provider.
7. Provider request and response handling.

The Phase 2 regression uses an in-memory `ResponseDto`, so AI analysis does not depend on network availability.

### Phase 2 regression API alignment fix

The first CI implementation of `Phase2RegressionTest` used two APIs that did not match the current production classes:

- `AiNegativeTestDataResult.getCases()` did not exist. The model exposes `getTestData()`.
- `AiFailureAnalyzer.analyze(response, analysis)` was missing the required expected-status argument. The current signature is `analyze(response, analysis, expectedStatusCode)`.

The regression test was corrected to:

```java
assertFalse(negativeData.getTestData().isEmpty());

var failure = new AiFailureAnalyzer().analyze(response, analysis, 200);
```

This change modifies only the regression test. Production AI behavior is unchanged.

## Main.java and Petstore integration regression

`Main.java` executes the Swagger Petstore integration regression through `TestRunExecutor` using:

`GET https://petstore3.swagger.io/api/v3/openapi.json`

The test expects HTTP 200, sends `Accept: application/json`, and validates a non-empty response body. It also prints detailed failure diagnostics and verifies that HTML, JSON, and CSV reports exist and are non-empty.

The OpenAPI endpoint is used instead of `/pet/1` or `/store/inventory` to avoid mutable sample data and public sample database dependency issues.

The Phase 1 HTTP method regression intentionally uses a deterministic local server for all five HTTP executor implementations. The stable Petstore OpenAPI GET remains in `Main.java` as the external integration check.

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

The regression was changed to start an in-process Java `HttpServer` and execute all five HTTP methods against deterministic local endpoints. The production executor chain and assertions remain unchanged. Only the regression test data source changed from an external service to a local deterministic server.

This resolves the CI failure where `Phase1RegressionTest.shouldExecuteAllHttpMethodsAndGenerateAllReports` reported:

```text
Test run failed. Passed suites: 0, Failed suites: 1
```

The fix does not weaken the assertion and does not skip any HTTP method.

The `Use -proc:none to disable annotation processing` text from earlier builds was only a javac informational warning. It was not the build failure.

## Latest Phase 1 HTTP method regression update

The Phase 1 regression now executes all five HTTP methods through `ExecutorDispatcher` using a deterministic local HTTP server. The test contains five test cases in one suite and verifies five passed test cases plus HTML, JSON and CSV report generation.

Affected test file:

`src/test/java/org/ai/testing/regression/Phase1RegressionTest.java`

The production HTTP executor classes were not changed. The update removes the external Petstore dependency from this unit/regression test while retaining real HTTP requests through every executor implementation.

## Development rule

Every Phase 2 change must update this document with:

1. Implementation added or changed.
2. Affected classes and methods.
3. Test coverage.
4. CI or runtime validation results.
5. Known limitations or next-step integration work.

## Next planned phase

Connect AI-generated negative test data and AI failure analysis directly into executable test cases and per-test report insights while preserving the deterministic regression path.
