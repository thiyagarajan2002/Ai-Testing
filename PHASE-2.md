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

The project contains dedicated regression tests:

### Phase 1

`Phase1RegressionTest` exercises the real `TestRunExecutor` flow against:

`GET https://petstore3.swagger.io/api/v3/openapi.json`

It validates test-run execution, suite execution, test-case execution, HTTP execution, status-code validation, and result aggregation.

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

## GitHub Actions regression flow

The workflow provides this regression chain:

```text
Phase 1 Core Regression
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
4. Verifies HTML, JSON, and CSV reports.
5. Uploads Surefire results.
6. Uploads generated API reports.

## CI issue resolution

The failed regression run was caused by Java test compilation errors in `Phase2RegressionTest`, not by the Maven setup or annotation-processing message.

The `Use -proc:none to disable annotation processing` text was only a javac informational warning. It was not the build failure.

The actual compilation failures were:

```text
AiNegativeTestDataResult.getCases() cannot be found
AiFailureAnalyzer.analyze(ResponseDto, AiResponseAnalysis) has the wrong argument count
```

Both were corrected to match the current production APIs.

## Development rule

Every Phase 2 change must update this document with:

1. Implementation added or changed.
2. Affected classes and methods.
3. Test coverage.
4. CI or runtime validation results.
5. Known limitations or next-step integration work.

## Next planned phase

Connect AI-generated negative test data and AI failure analysis directly into executable test cases and per-test report insights while preserving the deterministic regression path.
