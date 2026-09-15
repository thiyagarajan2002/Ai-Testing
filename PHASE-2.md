# Phase 2: AI API Test Generation

## Branch

`feature/02-ai-test-generation`

## Objective

Phase 2 introduces an AI-oriented test generation layer without coupling the framework to a specific external AI provider. The first implementation uses deterministic heuristics so generated tests are reproducible in local development and CI.

## Phase 2.1 implemented

### AI generation request

`AiTestGenerationRequest` describes an API operation that the generator can analyze.

Supported inputs include:

- HTTP method
- URL
- Request headers
- Request body metadata
- Expected status code
- Positive and negative case generation flags
- Header and body assertion flags

### Generated test suite

`AiGeneratedTestSuite` stores generated `TestCaseDto` objects and identifies the generator strategy.

Current generator identifier:

`heuristic-ai-v1`

### AI test case generator

`AiTestCaseGenerator` currently generates:

1. A positive API test case.
2. An optional negative status test case.
3. Status-code assertions when an expected status is available.
4. Response-body non-empty assertions when body generation is enabled and a request body is supplied.
5. Header assertions for APIs that advertise JSON through the `Accept` request header.

The implementation validates required method and URL values before generation.

## Why deterministic heuristics first

The project should not require an API key, network connection, or paid AI provider for its core regression suite. The generator therefore exposes a provider-neutral service boundary first. A future LLM provider can consume the same request model and return the same `TestCaseDto` model.

## Tests

`AiTestCaseGeneratorTest` verifies:

- Positive and negative test generation.
- Required URL validation.
- Disabling negative-case generation.
- Generated HTTP method and expected status.

## Next Phase 2 steps

- AI response analyzer.
- Automatic assertion suggestions.
- AI negative test-data generation.
- Failure explanation and root-cause suggestions.
- AI information in HTML, JSON, and CSV reports.
- Provider abstraction for an external LLM.
- Additional unit and integration coverage.

## Development rule

Every Phase 2 change must update this document with the implementation, behavior, tests, and usage impact.
