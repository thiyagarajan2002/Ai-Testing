# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces an AI-oriented test generation and response analysis layer without coupling the framework to a specific external AI provider. Deterministic heuristics keep the core regression suite reproducible in local development and CI.

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

`AiResponseAnalysis` contains:

- Healthy status
- HTTP status code
- Summary
- Findings
- Suggestions
- Severity
- Analysis strategy

### AI response analyzer

`AiResponseAnalyzer` performs deterministic analysis of the actual API response.

It currently detects:

1. Unexpected HTTP status codes.
2. 4xx client errors.
3. 5xx server errors.
4. Empty successful response bodies.
5. JSON Content-Type values with bodies that do not look like JSON.
6. Responses slower than the configured threshold.

The analyzer also provides actionable suggestions for investigation and improvement.

The implementation is provider-neutral and does not require an API key or network connection.

## Tests

`AiTestCaseGeneratorTest` verifies positive and negative generation, required URL validation, disabling negative-case generation, and generated HTTP method and expected status.

`AiResponseAnalyzerTest` verifies:

- Healthy JSON response analysis.
- Unexpected status detection.
- Invalid JSON detection.
- Slow response detection.
- Missing response validation.

## CI validation

The Phase 2 branch uses the existing Java 21 Maven regression workflow. The latest Phase 2.1 workflow completed successfully with 55 tests, 0 failures, and 0 errors.

## Next Phase 2 steps

- Automatic assertion suggestions.
- AI negative test-data generation.
- Failure explanation and root-cause suggestions.
- AI information in HTML, JSON, and CSV reports.
- Provider abstraction for an external LLM.
- Additional integration coverage.

## Development rule

Every Phase 2 change must update this document with the implementation, behavior, tests, and usage impact.
