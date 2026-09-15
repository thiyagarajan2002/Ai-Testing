# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces an AI-oriented test generation, response analysis, assertion suggestion, and negative test-data generation layer without coupling the framework to a specific external AI provider. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

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

The implementation is provider-neutral and does not call an external LLM. The generated cases are intended to become executable negative test cases in the next integration step.

## Tests

`AiTestCaseGeneratorTest` verifies positive and negative generation, required URL validation, disabling negative-case generation, and generated HTTP method and expected status.

`AiResponseAnalyzerTest` verifies healthy JSON response analysis, unexpected status detection, invalid JSON detection, slow response detection, and missing response validation.

`AiAssertionSuggesterTest` verifies status, body, and header suggestion generation, expected-status override behavior, and missing-response validation.

`AiNegativeTestDataGeneratorTest` verifies JSON mutation scenarios, GET behavior, Content-Type removal, missing request validation, and missing URL validation.

## CI validation

The Phase 2 branch uses the existing Java 21 Maven regression workflow. The latest completed Phase 2 regression run passed all workflow steps, including Maven regression tests and artifact upload.

The new Phase 2.4 commits have triggered a new regression workflow. Its final result must be checked before marking Phase 2.4 CI validation as passed.

## Next Phase 2 steps

- Failure explanation and root-cause suggestions.
- AI information in HTML, JSON, and CSV reports.
- Provider abstraction for an external LLM.
- Additional integration coverage.
- Connect generated negative data to executable `TestCaseDto` instances.

## Development rule

Every Phase 2 change must update this document with the implementation, behavior, tests, and usage impact.
