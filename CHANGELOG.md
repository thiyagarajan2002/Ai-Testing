# Changelog

All notable changes to this project are documented here.

## [2.0.0]

A rebuild. The 1.x source did not compile, and roughly half of it was never
reachable from any entry point; this release makes the framework work end to
end and adds the pieces the README already claimed.

### Fixed

- **The project now compiles.** `BaseRequestDto` was missing `headerItems`,
  `pathParamItems` and `auth`; `getQueryParamItems()` was a stub returning
  `null` typed as the wrong array; `RequestBodyDto.setMode()` was a no-op and
  `getFormFields()` returned a null array. `BrunoParser`, `PostmanParser`,
  `RequestNormalizer` and `VariableResolver` all called that missing API.
- **Variables are applied.** `VariableResolver` and `VariableStore` existed but
  were never invoked, so `{{placeholders}}` were sent to the server literally.
- **Credentials are applied.** `AuthApplicator` was likewise dead code.
- **Response chaining works.** `ResponseExtractor` was never called, so no value
  could be carried from one response into a later request.
- **Collections can be loaded.** `CollectionLoader` had no caller, leaving both
  importers unreachable.
- **JSON\_PATH and RESPONSE\_TIME assertions are evaluated.** Both were rejected
  as "Unsupported assertion type" despite their validators being present.
- **A disabled test case no longer fails its suite.** Suite status required
  `skippedTestCases == 0`.
- **URLs are encoded correctly.** `RequestBuilder` never percent-encoded and
  appended `?` even when there were no query parameters. It now encodes
  properly, merges with an existing query string, and supports `{brace}` and
  `:colon` path parameters.
- **`statusMessage` is populated** from the status code instead of always being
  an empty string.
- **One HTTP client per run**, not one per executor per test case.
- **`executionMode` is honoured.** It was previously stored and ignored.
- **Reports share one envelope.** `generateAllReports` built a separate envelope
  per format, each with its own report ID and timestamp, so the files from a
  single run could not be correlated.
- **A failure in one report format no longer aborts the others.**
- **Exit codes replace a thrown exception** on test failure.

### Added

- Command-line runner (`CliOptions`, `CliRunner`, `ConsolePrinter`) covering
  collections, environments, output directory, parallelism, timeouts, retries,
  fail-fast, tag filtering, variable overrides and redaction.
- JUnit XML report, for CI test tabs.
- Markdown report, for pull request comments and job summaries.
- Rewritten HTML dashboard: pass-rate ring, KPI cards, SVG bar charts, live
  search, status filter chips, collapsible suites and cases, per-case
  Assertions/Request/Response/cURL tabs, pretty-printed JSON, copy buttons,
  captured-variable panel, slowest-tests chart, light/dark theme, print styles.
  Entirely self-contained — no external stylesheet, font or script.
- Retries with configurable delay, applied to transport errors and retryable
  status codes.
- Tag include/exclude filtering.
- Fail-fast at suite and run level.
- `RESPONSE_TIME`, `RESPONSE_SIZE` and `CONTENT_TYPE` assertion types.
- Seventeen assertion operators, including `STARTS_WITH`, `ENDS_WITH`,
  `MATCHES`, `IN` and `NOT_IN`.
- Credential redaction, enabled by default.
- A copyable `curl` reproduction per test case.
- Latency percentiles (median, p90, p95), throughput and bytes-transferred
  metrics.
- Built-in demo plan, so the tool produces a populated report with no input.
- Sample Postman and Bruno collections under `samples/`.
- `run-tests` and `run-demo` scripts for bash, cmd and PowerShell.
- 108 unit tests.

### Changed

- **Removed all runtime dependencies.** Lombok, Jackson and json-path are gone;
  the main source set compiles against the JDK alone. This removes the
  annotation-processor fragility, the missing JSR-310 module that would have
  broken `LocalDateTime` serialisation, and a transitive SLF4J binding.
- `TestCaseExecutor.TestCaseExecutionResult` promoted to
  `org.ai.testing.testcase.dto.TestCaseResultDto`.
- `ReportService` now accepts an output `Path` or an explicit generator list.
- Suite and run status derive from a single `tally()` on each result object.
- Response bodies are truncated to a configurable cap before being stored.
- `README.md` rewritten; it previously documented six test classes and four
  scripts that were not in the repository.

## [1.0-SNAPSHOT]

Initial version.
