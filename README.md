# AI API Testing Agent

A dependency-free API test runner for Java 21. It imports Postman and Bruno
collections, executes them with variables, authentication and response
chaining, and writes five report formats — including a self-contained
interactive HTML dashboard.

```
mvn package
java -jar target/ai-api-testing.jar --demo
```

---

## Table of contents

- [What changed in 2.0](#what-changed-in-20)
- [Why there are no dependencies](#why-there-are-no-dependencies)
- [Quick start](#quick-start)
- [Command line reference](#command-line-reference)
- [Writing tests in Java](#writing-tests-in-java)
- [Assertions](#assertions)
- [Variables and chaining](#variables-and-chaining)
- [Authentication](#authentication)
- [Importing collections](#importing-collections)
- [Reports](#reports)
- [Parallel execution](#parallel-execution)
- [Tag filtering](#tag-filtering)
- [Exit codes and CI](#exit-codes-and-ci)
- [Architecture](#architecture)
- [Migrating from 1.x](#migrating-from-1x)
- [Project layout](#project-layout)

---

## What changed in 2.0

### The project did not compile

`BaseRequestDto` was missing `headerItems`, `pathParamItems` and `auth`, and
`getQueryParamItems()` was a stub returning `null` typed as the wrong array.
`RequestBodyDto.setMode()` did nothing and `getFormFields()` returned a null
array. Four classes — `BrunoParser`, `PostmanParser`, `RequestNormalizer` and
`VariableResolver` — called the API those methods were supposed to expose, so
`mvn compile` failed before a single test could run. All of it is now
implemented and covered by tests.

### Half the codebase was never called

`AuthApplicator`, `VariableResolver`, `VariableStore`, `ResponseExtractor`,
`RequestNormalizer` and `CollectionLoader` existed but nothing invoked them.
In practice that meant `{{variables}}` were sent to the server literally,
credentials were never attached, values could not be carried from one response
into the next request, and neither importer was reachable. `TestCaseExecutor`
now runs an explicit nine-step pipeline that wires all of them together.

### Assertions were rejected despite working validators

JSON\_PATH and RESPONSE\_TIME assertions were answered with "Unsupported
assertion type" even though the validation code for both was present. Every
assertion type now routes through one `ValidationEngine`, and the operator
logic lives in a single `Comparisons` class rather than being duplicated per
type.

### A disabled test failed the whole suite

Suite status required `skippedTestCases == 0`, so a single disabled case turned
a green suite red. Skipped cases are now neutral: they never cause a failure,
and a suite containing nothing but skipped cases reports as SKIPPED rather than
PASSED. There is a regression test for this.

### Other fixes

| Problem | Resolution |
|---|---|
| `RequestBuilder` never URL-encoded and always appended `?`, even with no parameters | Proper percent-encoding; merges with an existing query string; supports `{brace}` and `:colon` path parameters |
| `statusMessage` was always an empty string | Reason phrases resolved from the status code |
| A new `HttpClient` was created per executor, per test case | One shared, connection-pooled client for the whole run |
| `executionMode` was stored and ignored | `PARALLEL` runs suites on a bounded pool; cases stay ordered inside a suite so chaining still works |
| `generateAllReports` built three envelopes with different IDs and timestamps | One shared envelope, so every file from a run correlates |
| Failure in one report format aborted the rest | Each format is attempted; failures are collected and reported together |
| `Main` threw on failure, producing a stack trace | Exit codes 0 / 1 / 2, suitable for CI |
| README documented six test classes and four scripts that were not in the repository | Both now exist, and the test suite runs |

### New features

- Command-line runner covering every capability
- Bruno and Postman importers wired to the runner via `CollectionLoader`
- JUnit XML and Markdown reports
- Rewritten interactive HTML dashboard
- Retries with configurable delay
- Tag include/exclude filtering
- Fail-fast, at suite and run level
- Response-time, response-size and content-type assertions
- Seventeen assertion operators
- Credential redaction, on by default
- A copyable `curl` reproduction for every test case
- Latency percentiles (median, p90, p95) and throughput metrics

---

## Why there are no dependencies

The main source set compiles against the JDK alone. Lombok, Jackson and
json-path have all been removed:

- **Lombok** required an IDE plugin and an annotation processor that broke on
  JDK upgrades. Accessors are now written out.
- **Jackson** needed a separate JSR-310 module kept in version lockstep to
  serialise `LocalDateTime`, which the previous build did not have. Reports are
  serialised by a small hand-written JSON writer instead.
- **json-path** pulled in a JSON provider and an SLF4J binding for one feature.
  A focused JSONPath subset now covers what API assertions actually use.

The practical effect is that `mvn package` works on a machine with no artifacts
cached, the jar is a few hundred kilobytes, and there is no transitive CVE
surface. JUnit is still used, but only in `test` scope.

---

## Quick start

Requires **JDK 21** and **Maven 3.9+**.

```bash
mvn package                                   # build target/ai-api-testing.jar
java -jar target/ai-api-testing.jar --demo    # run the built-in example plan
```

Run a collection of your own:

```bash
java -jar target/ai-api-testing.jar \
  --collection samples/postman-collection.json \
  --env        samples/postman-environment.json \
  --out        build/reports
```

Or use the helper scripts, which work from any directory:

```bash
./scripts/run-tests.sh                        # mvn clean test
./scripts/run-demo.sh -c samples/bruno        # package, then run
```

Windows equivalents are `scripts\run-tests.cmd` and `scripts\run-demo.cmd`, plus
PowerShell versions.

---

## Command line reference

```
api-testing [options]
api-testing <collection> [options]
```

**Source**

| Option | Meaning |
|---|---|
| `-c`, `--collection <path>` | Postman `.json`, Bruno `.bru`, or a Bruno collection directory |
| `-e`, `--env <path>` | Postman or Bruno environment file |
| `--demo` | Run the built-in example plan (so does passing no arguments) |

**Output**

| Option | Meaning |
|---|---|
| `-o`, `--out <dir>` | Report directory (default `reports`) |
| `-q`, `--quiet` | Print only the final summary |

**Execution**

| Option | Default | Meaning |
|---|---|---|
| `--parallel` | off | Run suites concurrently |
| `--threads <n>` | 4 | Workers for `--parallel` |
| `--timeout <ms>` | 60000 | Per-request timeout |
| `--connect-timeout <ms>` | 15000 | Connect timeout |
| `--retries <n>` | 0 | Retries after a transport error or a retryable status |
| `--retry-delay <ms>` | 500 | Pause between retries |
| `--fail-fast` | off | Stop at the first failing case |
| `--no-follow-redirects` | follows | Do not follow 3xx |
| `--max-body <chars>` | 200000 | Response body cap kept in reports |

**Filtering and variables**

| Option | Meaning |
|---|---|
| `--tag <a,b>` | Only run cases carrying one of these tags |
| `--exclude-tag <a,b>` | Never run cases carrying these tags |
| `--var name=value` | Set a variable; repeatable, and overrides the collection and environment |
| `--no-redact` | Write credentials to reports unmasked |

A bare path argument is treated as the collection, so
`api-testing api.json --tag smoke` works.

---

## Writing tests in Java

A plan can be built directly, without any collection file:

```java
TestRunDto run = new TestRunDto();
run.setRunName("Petstore smoke");
run.getCollectionVariables().put("baseUrl", "https://petstore3.swagger.io/api/v3");

TestSuiteDto suite = new TestSuiteDto("SUITE-1", "Contract");

TestCaseDto openApi = new TestCaseDto("TC-1", "OpenAPI document is served", "GET");
openApi.setRequest(new BaseRequestDto().header("Accept", "application/json"));
openApi.getRequest().setUrl("{{baseUrl}}/openapi.json");
openApi.setExpectedStatusCode(200);
openApi.tag("smoke", "contract");
openApi.assertion(AssertionDto.jsonPath("$.info.title", AssertionOperator.CONTAINS, "Petstore"));
openApi.assertion(AssertionDto.responseTimeBelow(2000));
openApi.extract(ExtractDto.fromBody("apiTitle", "$.info.title"));

suite.add(openApi);
run.add(suite);

TestRunResultDto result = new TestRunExecutor(new ReportService(Path.of("reports")))
        .execute(run);
```

`TestRunExecutor` generates the reports as part of `execute`.

---

## Assertions

An assertion is a **type**, a **field**, an **operator** and an **expected
value**. Each one is evaluated independently and appears as its own row in the
report.

### Types

| Type | Field means | Example |
|---|---|---|
| `STATUS_CODE` | — | `AssertionDto.status(200)` |
| `RESPONSE_BODY` | — | `AssertionDto.body(CONTAINS, "ok")` |
| `HEADER` | header name (case-insensitive) | `AssertionDto.header("Content-Type", CONTAINS, "json")` |
| `JSON_PATH` | a JSONPath expression | `AssertionDto.jsonPath("$.data.id", EXISTS, "")` |
| `RESPONSE_TIME` | — | `AssertionDto.responseTimeBelow(500)` |
| `RESPONSE_SIZE` | — | body size in bytes |
| `CONTENT_TYPE` | — | shorthand for the `Content-Type` header |

### Operators

`EQUALS`, `NOT_EQUALS`, `CONTAINS`, `NOT_CONTAINS`, `STARTS_WITH`, `ENDS_WITH`,
`MATCHES` (regex), `EXISTS`, `NOT_EXISTS`, `EMPTY`, `NOT_EMPTY`, `LESS_THAN`,
`LESS_THAN_OR_EQUAL`, `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `IN`, `NOT_IN`.

`IN` and `NOT_IN` take a comma-separated list. Numeric operators report a clear
failure rather than throwing when the value is not a number.

### Supported JSONPath

| Syntax | Meaning |
|---|---|
| `$.a.b` | Member access |
| `$['a']['b']` | Bracketed member access |
| `$.items[0]` | Index |
| `$.items[-1]` | Index from the end |
| `$.items[*]` | All elements |
| `$..id` | Recursive descent |
| `$.items.length()` | Element or character count |

`res.body.x` and a bare `info.title` are both normalised to `$.x` and
`$.info.title`, so expressions copied from Bruno work unchanged.

---

## Variables and chaining

Variables resolve from three layers. Later layers win:

1. **Collection** — defined in the collection file
2. **Environment** — from the environment file, and from `--var`
3. **Runtime** — captured from responses during the run

Reference one as `{{name}}` in a URL, header, query value, path parameter or
body. Nested definitions resolve up to five levels deep. An unknown name is
**left visible in the output** rather than replaced with an empty string, and is
recorded as a run warning — a blank substitution tends to produce a confusing
404 instead of an obvious error.

Built-in dynamic values: `{{$guid}}`, `{{$timestamp}}`, `{{$isoTimestamp}}`,
`{{$randomInt}}`.

### Carrying a value from one response to the next

```java
login.extract(ExtractDto.fromBody("authToken", "$.token"));
login.extract(ExtractDto.fromHeader("traceId", "X-Trace-Id"));

next.getRequest().setAuth(AuthDto.bearer("{{authToken}}"));
```

Extracts land in the runtime layer, so every later case in the run can use them.
An extract that matches nothing is reported as a warning and shown in the HTML
report instead of silently storing a null.

---

## Authentication

Credentials are resolved most-specific-first: **request → suite → run**.

| Type | Effect |
|---|---|
| `BEARER` | `Authorization: Bearer <token>` |
| `BASIC` | `Authorization: Basic <base64>` |
| `API_KEY` | A named header, or a query parameter |
| `NONE` | Stops inheritance — use for a public endpoint inside an authenticated suite |
| `INHERIT` | Defer to the enclosing scope |

Tokens may themselves be variables, so `AuthDto.bearer("{{authToken}}")` picks
up a value captured earlier in the run.

---

## Importing collections

```bash
api-testing -c collection.json -e environment.json   # Postman v2.0 / v2.1
api-testing -c ./bruno-collection                    # a Bruno directory
api-testing -c request.bru                           # a single Bruno request
```

Format is detected from the file shape, not a flag.

### Postman

Folders become suites, requests become cases. Postman tests are JavaScript,
which this framework does not execute; instead the common `pm.*` idioms are
translated into native assertions:

| Script | Becomes |
|---|---|
| `pm.response.to.have.status(201)` | expected status code |
| `pm.expect(pm.response.code).to.eql(200)` | expected status code |
| `pm.response.to.have.header("X")` | `HEADER X EXISTS` |
| `pm.expect(pm.response.responseTime).to.be.below(500)` | `RESPONSE_TIME LESS_THAN 500` |
| `pm.expect(pm.response.text()).to.include("ok")` | `RESPONSE_BODY CONTAINS ok` |
| `pm.expect(jsonData.a.b).to.eql("x")` | `JSON_PATH $.a.b EQUALS x` |
| `pm.environment.set("id", jsonData.data.id)` | extract `id` from `$.data.id` |

Anything unrecognised is ignored rather than failing the import. Disabled
headers and parameters are imported but marked disabled, so you can see what was
switched off.

### Bruno

`meta`, `get`/`post`/…, `headers`, `params:query`, `params:path`, `body:*`,
`auth:*`, `assert`, `vars:pre-request`, `vars:post-response` and `docs` blocks
are all read. A leading `~` marks a header, parameter or assertion as disabled.
`folder.bru` supplies folder-level authentication; `collection.bru` supplies
collection variables and credentials.

---

## Reports

Five files are written per run, all sharing one report ID and timestamp:

| File | Purpose |
|---|---|
| `test-report.html` | Interactive dashboard for humans |
| `test-report.json` | Full structured result for tooling |
| `test-report.csv` | One row per assertion, for spreadsheets |
| `test-report.md` | Pull request comments and CI job summaries |
| `junit-report.xml` | Surefire format, for CI test tabs |

### The HTML dashboard

A single file with no external stylesheet, font or script, so it renders
identically when emailed or published as a CI artefact.

- Pass-rate ring, and KPI cards for counts, duration, p95 latency and bytes
- Bar charts for status-family and per-method distribution
- Live search plus status filter chips, with expand/collapse all
- Collapsible suites and cases; failures open by default
- Per-case tabs: **Assertions**, **Request**, **Response**, **cURL**
- Pretty-printed JSON bodies with copy buttons
- Captured variables, and a list of extracts that matched nothing
- Slowest-tests chart
- Light and dark themes, remembered between visits
- Print stylesheet that expands everything
- `/` focuses the filter box

Credentials are masked unless `--no-redact` is passed.

### JUnit XML

Jenkins, GitLab CI, GitHub Actions and Azure Pipelines all ingest this natively,
so an API run appears in the build's test tab alongside the unit tests.

```yaml
- name: API tests
  run: java -jar target/ai-api-testing.jar -c api.json --out build/reports
- uses: actions/upload-artifact@v4
  if: always()
  with:
    name: api-report
    path: build/reports/
```

---

## Parallel execution

```bash
api-testing -c api.json --parallel --threads 8
```

Suites run concurrently; cases stay ordered within a suite, because response
chaining makes their order meaningful. Results are collected in submission
order, so the report layout is identical whichever suite finishes first. The
pool is skipped when a run has only one suite.

---

## Tag filtering

```java
testCase.tag("smoke", "contract");
```

```bash
api-testing -c api.json --tag smoke
api-testing -c api.json --exclude-tag slow,flaky
```

Excluded tags win over included ones. A filtered-out case is reported as
SKIPPED with the reason attached, rather than vanishing from the report.

---

## Exit codes and CI

| Code | Meaning |
|---|---|
| `0` | Every executed test case passed |
| `1` | At least one case failed or errored |
| `2` | The command line or the collection could not be understood |

Code `2` is distinct on purpose: a typo in a flag should not look like a failing
API.

---

## Architecture

```
Main → CliRunner → CliOptions
                 → CollectionLoader ─┬─ PostmanParser ─┐
                 │                   └─ BrunoParser   ─┴→ TestRunDto
                 └→ TestRunExecutor
                       ├─ VariableStore (collection / environment / runtime)
                       ├─ TestSuiteExecutor          (tags, stop-on-failure)
                       │    └─ TestCaseExecutor      (the nine-step pipeline)
                       │         ├─ VariableResolver
                       │         ├─ RequestNormalizer
                       │         ├─ AuthApplicator
                       │         ├─ ExecutorDispatcher → Get/Post/Put/Patch/Delete
                       │         │      └─ AbstractHttpExecutor (shared HttpClient, retries)
                       │         ├─ ResponseExtractor
                       │         └─ ValidationEngine → Comparisons
                       └─ ReportService
                            ├─ HtmlReportGenerator
                            ├─ JsonReportGenerator
                            ├─ CsvReportGenerator
                            ├─ MarkdownReportGenerator
                            └─ JUnitXmlReportGenerator
```

### The test case pipeline

1. Register the case's pre-request variables
2. Build a private deep copy of the request
3. Substitute `{{variables}}`
4. Normalise headers, parameters and the body
5. Resolve and apply credentials
6. Snapshot the request exactly as it will be sent
7. Send it, with retries
8. Capture extracts into the variable store
9. Validate every assertion

The copy in step 2 matters: without it, substitution would mutate the test
definition and a second run of the same plan would find no placeholders left.

---

## Migrating from 1.x

| 1.x | 2.0 |
|---|---|
| `TestCaseExecutor.TestCaseExecutionResult` (nested) | `org.ai.testing.testcase.dto.TestCaseResultDto` |
| `new ReportService()` writing to a fixed path | `new ReportService(Path)` or `new ReportService(List<ReportGenerator>)` |
| Lombok-generated accessors | Written out; the IDE plugin is no longer needed |
| Jackson annotations on DTOs | Removed; serialisation is handled by the report generators |
| `Main` throwing on failure | `CliRunner.run(String[])` returning an exit code |
| `testRun.setExecutionMode(...)` | Still works, and now delegates to `RunOptions` |

The old `ReportService()` no-argument constructor still exists and writes to
`reports/`.

---

## Project layout

```
Ai_Testing/
├── pom.xml
├── README.md
├── CHANGELOG.md
├── scripts/                       run-tests and run-demo (sh, cmd, ps1)
├── samples/
│   ├── postman-collection.json
│   ├── postman-environment.json
│   └── bruno/                     collection.bru, folders, requests
└── src/
    ├── main/java/org/ai/testing/
    │   ├── Main.java
    │   ├── cli/                   CliOptions, CliRunner, ConsolePrinter, DemoPlan
    │   ├── json/                  Json, JsonValue, JsonPathReader
    │   ├── dto/common/            request, response, assertion and auth types
    │   ├── env/                   VariableStore, VariableResolver
    │   ├── auth/                  AuthApplicator
    │   ├── extract/               ResponseExtractor
    │   ├── executor/              per-method executors and shared HTTP plumbing
    │   ├── validation/            ValidationEngine, Comparisons, operators
    │   ├── testcase/              case DTOs, factory and executor
    │   ├── testsuite/             suite DTOs and executor
    │   ├── testrun/               run DTOs, options, metrics and executor
    │   ├── report/                five generators and the report service
    │   ├── parser/                Postman and Bruno importers
    │   └── util/                  Strings, HttpStatus, Redaction, CurlBuilder
    └── test/java/org/ai/testing/  108 unit tests
```

Run them with `mvn test` or `./scripts/run-tests.sh`.
