# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces an AI-oriented test generation, response analysis, assertion suggestion, negative test-data generation, failure root-cause analysis, report-insight, provider integration, and CI report-generation layer. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

## Phase 2.1 implemented

### AI generation request

`AiTestGenerationRequest` describes an API operation that the generator can analyze.

Supported inputs include HTTP method, URL, request headers, request body metadata, expected status code, positive and negative case generation flags, and header and body assertion flags.

### Generated test suite

`AiGeneratedTestSuite` stores generated `TestCaseDto` objects and identifies the generator strategy.

Current generator identifier:

`heuristic-ai-v1`

### AI test case generator

`AiTestCaseGenerator` generates a positive API test case, an optional negative status test case, status-code assertions, response-body non-empty assertions, and header assertions for APIs that advertise JSON through the `Accept` request header. Required method and URL values are validated before generation.

## Phase 2.2 implemented

### AI response analysis request

`AiResponseAnalysisRequest` accepts the actual `ResponseDto`, an optional expected status code, and a configurable slow-response threshold.

### AI response analysis result

`AiResponseAnalysis` contains healthy status, HTTP status code, summary, findings, suggestions, severity, and analysis strategy.

### AI response analyzer

`AiResponseAnalyzer` performs deterministic analysis of the actual API response.

It currently detects unexpected HTTP status codes, 4xx client errors, 5xx server errors, empty successful response bodies, JSON Content-Type values with bodies that do not look like JSON, and responses slower than the configured threshold.

## Phase 2.3 implemented

### AI assertion suggestion model

`AiAssertionSuggestion` contains an executable `AssertionDto`, a human-readable reason, and a confidence level.

`AiAssertionSuggestionResult` groups generated suggestions and identifies the analysis strategy.

### Automatic assertion suggester

`AiAssertionSuggester` converts response characteristics into assertions compatible with the existing validation model.

It can currently suggest:

1. STATUS_CODE EQUALS expected status.
2. RESPONSE_BODY NOT_EMPTY when a response body exists.
3. RESPONSE_BODY CONTAINS `{` for JSON object responses.
4. HEADER CONTAINS the detected Content-Type media type.

The suggestions use the existing `AssertionType` and `AssertionOperator` enums and the existing `AssertionDto`, so they can be consumed by the normal validation engine without introducing a second assertion format.

The implementation handles response header name matching case-insensitively and removes Content-Type parameters such as charset before creating the assertion value.

## Phase 2.4 implemented

### AI negative test-data model

`AiNegativeTestData` stores a negative testing scenario, reason, request body, headers, query parameters, path parameters, expected status code, and generation strategy.

`AiNegativeTestDataResult` groups generated negative data cases.

### AI negative test-data generator

`AiNegativeTestDataGenerator` creates deterministic negative input variants from an existing `AiTestGenerationRequest`.

Current scenarios include:

1. `INVALID_FIELD_TYPE` for JSON request bodies.
2. `MISSING_REQUIRED_FIELD` by removing the first JSON field.
3. `EMPTY_REQUEST_BODY` for POST, PUT, and PATCH.
4. `UNSUPPORTED_CONTENT_TYPE` using `text/plain`.
5. `MISSING_CONTENT_TYPE` when Content-Type is present.

The generator validates that the request and URL exist before generation. It copies request headers instead of modifying the original input object.

The implementation is provider-neutral and does not call an external LLM. The generated cases are intended to become executable negative test cases in a later integration step.

## Phase 2.5 implemented

### AI failure analysis model

`AiFailureAnalysis` captures whether a failure was detected, severity, failure category, summary, likely root cause, evidence, recommendations, and the analysis strategy.

### AI failure analyzer

`AiFailureAnalyzer` combines the existing `ResponseDto` with `AiResponseAnalysis` and converts detected failures into an actionable explanation.

Current categories are:

1. `SERVER_ERROR` for HTTP 5xx responses.
2. `CLIENT_ERROR` for HTTP 4xx responses.
3. `RESPONSE_FORMAT` when JSON Content-Type does not match the response body format.
4. `API_CONTRACT` for other unhealthy response conditions.
5. `NONE` when no failure is detected.

The analyzer also provides evidence and recommended investigation steps. Server failures are marked `CRITICAL`, client and response-format failures are marked `HIGH`, and generic contract failures are marked `MEDIUM`.

This layer is deterministic and provider-neutral. It does not claim a definitive root cause. The `likelyRootCause` field represents the most likely category based on observable API response evidence.

## Phase 2.6 implemented

### AI report insight model

`TestReportDto` now carries:

- `aiSummary`
- `aiSeverity`
- `aiFindings`
- `aiRecommendations`

These fields are populated before report generation, so the same AI insight data is available to every report format. JSON serialization automatically includes the fields.

### AI report insight builder

`AiReportInsightBuilder` converts test-run statistics into deterministic report-level AI insights.

Current severity rules:

1. `HIGH` when failed suites or failed test cases exist.
2. `MEDIUM` when there are skipped suites or skipped test cases but no failures.
3. `INFO` when the run has no failures or skips.

The builder also creates actionable findings and recommendations. It is provider-neutral and does not call an external LLM.

### ReportService integration

`ReportService` invokes `AiReportInsightBuilder` while creating every report. The AI severity is stored in the dedicated `TestReportDto.aiSeverity` field and is not appended to `reportName`. This preserves the existing report-name contract and prevents breaking report consumers and tests that expect names such as `Sample API Run - Report`.

### Report name regression fix

The report-name regression introduced during AI insight integration was caused by replacing the stable report name with:

`<run name> - Report | AI Severity: <severity>`

The implementation now keeps the report name as:

`<run name> - Report`

AI severity remains available through `aiSeverity`, `aiSummary`, `aiFindings`, and `aiRecommendations`.

### HTML report

The HTML report keeps the stable report title and can use the dedicated AI fields for AI dashboard content. AI severity is therefore available without changing the report-name contract.

### JSON report

The JSON report contains the complete AI summary, severity, findings, and recommendations because these fields are part of `TestReportDto`.

### CSV report

The CSV report contains dedicated columns for AI severity, AI summary, AI findings, and AI recommendations.

## Phase 2.7 implemented

### Provider contract

`AiProvider` defines the provider-neutral contract:

- `getProviderName()` identifies the provider.
- `getModelName()` identifies the model.
- `generate(AiProviderRequest)` sends a normalized AI request.

### Provider request and response

`AiProviderRequest` contains system prompt, user prompt, temperature, and metadata.

`AiProviderResponse` contains success state, provider, model, generated content, error message, and latency.

### Deterministic fallback provider

`HeuristicAiProvider` remains the default provider. It does not require an API key or network access and continues to use `heuristic-ai-v1` so CI remains deterministic.

### Generic external HTTP provider

`HttpAiProvider` provides a generic Java HTTP integration point for an external LLM-compatible endpoint. It sends the model, system prompt, user prompt, and temperature as JSON and supports both a simple `content` response and a common `choices[0].message.content` response shape.

The API key is supplied at construction time and is never written into the repository. A real application should load it from an environment variable or secret manager.

The provider handles HTTP failures, invalid responses, I/O failures, and interrupted requests without exposing the secret in the returned error message.

### Provider factory

`AiProviderFactory` creates the deterministic default provider or a configured HTTP provider. This keeps the rest of the framework independent of a specific external AI vendor.

## Phase 1 + Phase 2 regression coverage

The project now has explicit regression tests for both completed phases:

### Phase 1 regression

`Phase1RegressionTest` exercises the real `TestRunExecutor` flow against the stable Swagger Petstore OpenAPI endpoint:

`GET https://petstore3.swagger.io/api/v3/openapi.json`

It verifies that the core test-run, suite, test-case, HTTP execution, status-code validation, and result aggregation flow succeeds.

### Phase 2 regression

`Phase2RegressionTest` executes one deterministic end-to-end AI regression path covering:

1. AI test-case generation.
2. AI response analysis.
3. AI assertion suggestions.
4. AI negative test-data generation.
5. AI failure analysis.
6. Default heuristic AI provider.
7. Provider request and response handling.

The Phase 2 regression uses the same Petstore OpenAPI URL as test context but uses a deterministic in-memory `ResponseDto` for AI analysis. This prevents Phase 2 unit/regression coverage from becoming dependent on network availability.

## Main.java and Petstore integration regression

`Main.java` is the executable integration regression entry point. It executes the Swagger Petstore GET regression through `TestRunExecutor`.

The regression endpoint is:

`GET https://petstore3.swagger.io/api/v3/openapi.json`

The regression intentionally does not use `GET /pet/1` or `GET /store/inventory`. A specific pet ID is mutable in the public sample service, and the inventory endpoint can depend on the public sample database. The OpenAPI definition endpoint avoids those mutable database dependencies while keeping the integration regression fully within Swagger Petstore.

The test expects HTTP status `200`, sends `Accept: application/json`, and validates that the response body is not empty.

`Main.java` also prints detailed failure diagnostics when a test fails, including the failed test case, execution message, HTTP status, response time, response body, and failed validation details.

The updated `Main.java` verifies that all three expected reports are actually created after the test run:

- `reports/test-report.html`
- `reports/test-report.json`
- `reports/test-report.csv`

It also verifies that each file is non-empty and prints the absolute path and file size.

## GitHub Actions regression flow

The GitHub Actions workflow now presents the regression as three explicit stages:

1. **Phase 1 and Phase 2 regression tests** using `mvn -B clean test -U`.
2. **Petstore integration regression** using `org.ai.testing.Main`.
3. **Report verification and artifact upload** for HTML, JSON, CSV, and Surefire results.

The workflow explicitly verifies that Surefire result files for `Phase1RegressionTest` and `Phase2RegressionTest` exist before executing the Petstore integration regression.

This gives the project a clear regression chain:

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

## Development rule

Every Phase 2 change must update this document with:

1. The implementation added or changed.
2. The affected classes and methods.
3. Test coverage.
4. CI or runtime validation results.
5. Any known limitations or next-step integration work.

## Next planned phase

The next implementation can connect AI-generated negative test data and AI failure analysis directly into executable test cases and per-test report insights while preserving the current deterministic regression path.
