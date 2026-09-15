# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces an AI-oriented test generation, response analysis, and assertion suggestion layer without coupling the framework to a specific external AI provider. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

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

### Example generated assertions

```text
STATUS_CODE  EQUALS      200
RESPONSE_BODY NOT_EMPTY   true
RESPONSE_BODY CONTAINS    {
HEADER       CONTAINS    application/json
```

The actual `AssertionDto` model contains type, field, operator, and expected value and uses Lombok rather than manually written getters and setters.

## Tests

`AiTestCaseGeneratorTest` verifies positive and negative generation, required URL validation, disabling negative-case generation, and generated HTTP method and expected status.

`AiResponseAnalyzerTest` verifies healthy JSON response analysis, unexpected status detection, invalid JSON detection, slow response detection, and missing response validation.

`AiAssertionSuggesterTest` verifies status, body, and header suggestion generation, expected-status override behavior, and missing-response validation.

## CI validation

The Phase 2 branch uses the existing Java 21 Maven regression workflow. The latest completed Phase 2 regression run passed all workflow steps, including Maven regression tests and artifact upload.

## Next Phase 2 steps

- AI negative test-data generation.
- Failure explanation and root-cause suggestions.
- AI information in HTML, JSON, and CSV reports.
- Provider abstraction for an external LLM.
- Additional integration coverage.

## Development rule

Every Phase 2 change must update this document with the implementation, behavior, tests, and usage impact.
