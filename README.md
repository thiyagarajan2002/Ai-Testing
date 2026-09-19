# AI API Testing Agent — Master Guide

A single reference for installing, configuring, running, and extending the
project. If you only read one document, read this one.

---

## Table of contents

1. [What this project does](#1-what-this-project-does)
2. [Requirements](#2-requirements)
3. [Installing and building](#3-installing-and-building)
4. [Running your first test](#4-running-your-first-test)
5. [Command line reference](#5-command-line-reference)
6. [Project layout](#6-project-layout)
7. [Writing tests in Java](#7-writing-tests-in-java)
8. [Assertions](#8-assertions)
9. [Variables and response chaining](#9-variables-and-response-chaining)
10. [Authentication](#10-authentication)
11. [Importing Postman collections](#11-importing-postman-collections)
12. [Importing Bruno collections](#12-importing-bruno-collections)
13. [Reports](#13-reports)
14. [Parallel execution](#14-parallel-execution)
15. [Tag filtering](#15-tag-filtering)
16. [Retries, timeouts and redirects](#16-retries-timeouts-and-redirects)
17. [Redaction and secrets](#17-redaction-and-secrets)
18. [Exit codes and CI integration](#18-exit-codes-and-ci-integration)
19. [Architecture](#19-architecture)
20. [Running the unit tests](#20-running-the-unit-tests)
21. [Troubleshooting](#21-troubleshooting)
22. [FAQ](#22-faq)

---

## 1. What this project does

This is a Java 21 command-line tool that runs API test collections and
produces reports. It can:

- Run a test plan you build in Java (`TestRunDto`)
- Import and run a **Postman** collection (`.json`)
- Import and run a **Bruno** collection (a `.bru` file or a Bruno directory)
- Substitute `{{variables}}`, chain values from one response into the next
  request, apply authentication, retry on failure, and run suites in parallel
- Produce five report formats from a single run: HTML, JSON, CSV, Markdown,
  and JUnit XML

It has **no runtime dependencies** — the main source compiles against the JDK
alone. JUnit is used only to test the project itself.

---

## 2. Requirements

| Tool | Version |
|---|---|
| JDK | 21 or later |
| Maven | 3.9 or later |

Check what you have:

```bash
java -version
mvn -version
```

No other software, API keys, or network access is required to build or run
the demo.

---

## 3. Installing and building

```bash
# Unzip the project, then from its root:
mvn package
```

This produces a runnable jar at `target/ai-api-testing.jar`.

To build without running the unit tests (faster, e.g. for quick iteration):

```bash
mvn -DskipTests package
```

Or use the bundled helper scripts, which resolve the project root from their
own location so they work from any directory:

```bash
./scripts/run-demo.sh        # macOS / Linux — packages, then runs --demo
./scripts/run-tests.sh       # macOS / Linux — mvn clean test
```

```cmd
scripts\run-demo.cmd
scripts\run-tests.cmd
```

```powershell
scripts\run-demo.ps1
scripts\run-tests.ps1
```

---

## 4. Running your first test

The fastest way to see the tool work is the built-in demo, which needs no
files and hits a public test API:

```bash
java -jar target/ai-api-testing.jar --demo
```

You'll see a console summary, and five files appear under `reports/`:

```
reports/test-report.html     ← open this one in a browser
reports/test-report.json
reports/test-report.csv
reports/test-report.md
reports/junit-report.xml
```

To run one of your own collections instead:

```bash
java -jar target/ai-api-testing.jar \
  --collection path/to/collection.json \
  --env        path/to/environment.json \
  --out        build/reports
```

The `samples/` folder in the project ships a working Postman collection and a
working Bruno collection you can point at right away:

```bash
java -jar target/ai-api-testing.jar -c samples/postman-collection.json -e samples/postman-environment.json
java -jar target/ai-api-testing.jar -c samples/bruno
```

---

## 5. Command line reference

```
api-testing [options]
api-testing <collection> [options]
```

A bare path argument (no flag) is treated as `--collection`.

### Source

| Option | Meaning |
|---|---|
| `-c`, `--collection <path>` | Postman `.json`, a single Bruno `.bru` file, or a Bruno collection directory |
| `-e`, `--env <path>` | Postman or Bruno environment file |
| `--demo` | Run the built-in example plan. Also what happens if you pass no arguments at all. |

### Output

| Option | Meaning |
|---|---|
| `-o`, `--out <dir>` | Report directory. Default: `reports` |
| `-q`, `--quiet` | Print only the final summary line, not every suite/case |

### Execution

| Option | Default | Meaning |
|---|---|---|
| `--parallel` | off | Run suites concurrently |
| `--threads <n>` | 4 | Worker threads used by `--parallel` |
| `--timeout <ms>` | 60000 | Per-request timeout |
| `--connect-timeout <ms>` | 15000 | Connection timeout |
| `--retries <n>` | 0 | Retries after a transport error or retryable status |
| `--retry-delay <ms>` | 500 | Pause between retries |
| `--fail-fast` | off | Stop at the first failing test case |
| `--no-follow-redirects` | follows | Do not follow 3xx responses |
| `--max-body <chars>` | 200000 | Response body size kept in reports |

### Filtering and variables

| Option | Meaning |
|---|---|
| `--tag <a,b>` | Only run cases carrying one of these tags |
| `--exclude-tag <a,b>` | Never run cases carrying these tags (wins over `--tag`) |
| `--var name=value` | Set/override a variable. Repeatable. Highest precedence. |

### Security

| Option | Meaning |
|---|---|
| `--no-redact` | Write credentials into reports unmasked (off by default — redaction is on) |

### Examples

```bash
# Smoke tests only, stop at first failure, custom output directory
api-testing -c api.json --tag smoke --fail-fast --out build/reports

# Parallel run with retries, against a Bruno directory
api-testing -c samples/bruno --parallel --threads 8 --retries 2

# Override a variable at run time (e.g. a different environment host)
api-testing -c api.json --var baseUrl=https://staging.example.com
```

Exit codes: `0` = all executed cases passed, `1` = something failed or
errored, `2` = the command line or collection could not be understood. See
[§18](#18-exit-codes-and-ci-integration).

---

## 6. Project layout

```
Ai_Testing/
├── pom.xml
├── README.md
├── CHANGELOG.md
├── scripts/                       run-tests / run-demo (sh, cmd, ps1)
├── samples/
│   ├── postman-collection.json
│   ├── postman-environment.json
│   └── bruno/                     collection.bru, folders, requests
└── src/
    ├── main/java/org/ai/testing/
    │   ├── Main.java               entry point
    │   ├── cli/                    CliOptions, CliRunner, ConsolePrinter, DemoPlan
    │   ├── json/                   Json, JsonValue, JsonPathReader
    │   ├── dto/common/             request, response, assertion, auth types
    │   ├── env/                    VariableStore, VariableResolver
    │   ├── auth/                   AuthApplicator
    │   ├── extract/                ResponseExtractor
    │   ├── executor/               per-method executors, shared HTTP plumbing
    │   ├── validation/             ValidationEngine, Comparisons, operators
    │   ├── testcase/               case DTOs, factory, executor
    │   ├── testsuite/               suite DTOs, executor
    │   ├── testrun/                run DTOs, options, metrics, executor
    │   ├── report/                 5 report generators + ReportService
    │   ├── parser/                 Postman + Bruno importers
    │   └── util/                   Strings, HttpStatus, Redaction, CurlBuilder
    └── test/java/org/ai/testing/   108 unit tests
```

---

## 7. Writing tests in Java

You don't need a collection file at all — you can build a plan directly:

```java
import org.ai.testing.dto.common.*;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testrun.executor.TestRunExecutor;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.validation.AssertionOperator;

import java.nio.file.Path;

public class Example {
    public static void main(String[] args) {
        TestRunDto run = new TestRunDto();
        run.setRunName("Petstore smoke");
        run.getCollectionVariables().put("baseUrl", "https://petstore3.swagger.io/api/v3");

        TestSuiteDto suite = new TestSuiteDto("SUITE-1", "Contract");

        TestCaseDto openApi = new TestCaseDto("TC-1", "OpenAPI document is served", "GET");
        openApi.setRequest(new BaseRequestDto().header("Accept", "application/json"));
        openApi.getRequest().setUrl("{{baseUrl}}/openapi.json");
        openApi.setExpectedStatusCode(200);
        openApi.tag("smoke", "contract");
        openApi.assertion(AssertionDto.jsonPath("$.info.title",
                AssertionOperator.CONTAINS, "Petstore"));
        openApi.assertion(AssertionDto.responseTimeBelow(2000));
        openApi.extract(ExtractDto.fromBody("apiTitle", "$.info.title"));

        suite.add(openApi);
        run.add(suite);

        TestRunResultDto result = new TestRunExecutor(new ReportService(Path.of("reports")))
                .execute(run);

        System.out.println(result.getMessage());
    }
}
```

`TestRunExecutor.execute(...)` runs everything and writes the reports as part
of the call — you don't need to call the report service separately unless you
want a custom generator list.

### Key building blocks

| Class | Purpose |
|---|---|
| `TestRunDto` | The whole plan: suites, collection/environment variables, run-level auth, `RunOptions` |
| `TestSuiteDto` | A named group of cases; can carry its own auth, variables, and `stopOnFailure` |
| `TestCaseDto` | One request: method, `BaseRequestDto`, expected status, assertions, extracts, tags |
| `BaseRequestDto` | URL, headers, query params, path params, body, auth — has fluent helpers `.header()`, `.query()`, `.pathParam()`, `.jsonBody()` |
| `AssertionDto` | One check against the response (see [§8](#8-assertions)) |
| `ExtractDto` | One value captured from the response into the variable store (see [§9](#9-variables-and-response-chaining)) |

---

## 8. Assertions

An assertion is a **type + field + operator + expected value**, evaluated
independently and shown as its own row in every report.

### Types and how to build them

```java
AssertionDto.status(200);
AssertionDto.body(AssertionOperator.CONTAINS, "ok");
AssertionDto.header("Content-Type", AssertionOperator.CONTAINS, "json");
AssertionDto.jsonPath("$.data.id", AssertionOperator.EXISTS, "");
AssertionDto.responseTimeBelow(500);
new AssertionDto(AssertionType.RESPONSE_SIZE, "bodySizeBytes",
        AssertionOperator.LESS_THAN, "100000");
new AssertionDto(AssertionType.CONTENT_TYPE, "Content-Type",
        AssertionOperator.CONTAINS, "json");
```

| Type | What `field` means |
|---|---|
| `STATUS_CODE` | ignored |
| `RESPONSE_BODY` | ignored — checks the whole body as text |
| `HEADER` | a header name, matched case-insensitively |
| `JSON_PATH` | a JSONPath expression (see below) |
| `RESPONSE_TIME` | ignored — milliseconds |
| `RESPONSE_SIZE` | ignored — bytes |
| `CONTENT_TYPE` | shorthand for the `Content-Type` header |

### Operators

```
EQUALS  NOT_EQUALS  CONTAINS  NOT_CONTAINS
STARTS_WITH  ENDS_WITH  MATCHES (regex)
EXISTS  NOT_EXISTS  EMPTY  NOT_EMPTY
LESS_THAN  LESS_THAN_OR_EQUAL  GREATER_THAN  GREATER_THAN_OR_EQUAL
IN  NOT_IN   (comma-separated list, e.g. "200,201,204")
```

A numeric operator against a non-numeric value fails cleanly with a message
rather than throwing.

### Supported JSONPath subset

| Syntax | Meaning |
|---|---|
| `$.a.b` | member access |
| `$['a']['b']` | bracketed member access |
| `$.items[0]` | index |
| `$.items[-1]` | index from the end |
| `$.items[*]` | all elements |
| `$..id` | recursive descent — every `id` at any depth |
| `$.items.length()` | element/character count |

Shorthand from imported collections is normalized automatically:
`res.body.info.title` and a bare `info.title` both become `$.info.title`.

### Disabling an assertion without deleting it

```java
AssertionDto assertion = AssertionDto.body(AssertionOperator.EQUALS, "value");
assertion.setEnabled(false);
```

A disabled assertion is skipped and does not count toward pass/fail totals.

---

## 9. Variables and response chaining

### Three layers, later wins

1. **Collection** — set in the collection file or `run.getCollectionVariables()`
2. **Environment** — from the environment file, and from `--var name=value`
3. **Runtime** — captured from responses while the run executes

### Referencing a variable

Use `{{name}}` anywhere: URL, header value, query value, path parameter,
request body.

```java
request.setUrl("{{baseUrl}}/pet/{petId}");
request.header("Authorization", "Bearer {{authToken}}");
request.pathParam("petId", "{{petId}}");
```

Nested definitions (a variable whose value references another variable)
resolve up to 5 levels deep. An **unknown** variable name is left visible in
the output (e.g. the literal text `{{oops}}`) rather than silently becoming an
empty string — this is deliberate, because a blank substitution tends to
produce a confusing 404 instead of an obvious error. It's also logged as a run
warning.

### Built-in dynamic values

```
{{$guid}}          a random UUID
{{$timestamp}}     current Unix time in seconds
{{$isoTimestamp}}  current time, ISO-8601
{{$randomInt}}     a random integer
```

### Capturing a value and using it later

```java
// Login case — capture a token from the response body
login.extract(ExtractDto.fromBody("authToken", "$.token"));
login.extract(ExtractDto.fromHeader("traceId", "X-Trace-Id"));

// Any later case in the run can now use {{authToken}}
next.getRequest().setAuth(AuthDto.bearer("{{authToken}}"));
```

Extracts are written into the **runtime** layer, so any case that executes
after the extracting case — in the same suite or a later suite — can use the
value. An extract whose expression matches nothing is reported as a warning
and shown in the HTML report's "Captured variables" panel instead of being
silently dropped.

---

## 10. Authentication

Credentials resolve most-specific-first: **request → suite → run**.

```java
AuthDto.bearer("t0ken");
AuthDto.basic("username", "password");
AuthDto.apiKey("X-Api-Key", "value", "HEADER");   // or "QUERY"
```

| Type | Effect |
|---|---|
| `BEARER` | Sends `Authorization: Bearer <token>` |
| `BASIC` | Sends `Authorization: Basic <base64(user:pass)>` |
| `API_KEY` | Puts the value in a named header or query parameter |
| `NONE` | Explicitly no auth — stops inheritance. Use this on one public endpoint inside an otherwise-authenticated suite. |
| `INHERIT` | Defer to the enclosing scope (the default when auth is unset) |

Tokens can be variables, so a value captured with `ExtractDto` chains
straight into the next request's auth:

```java
next.getRequest().setAuth(AuthDto.bearer("{{authToken}}"));
```

---

## 11. Importing Postman collections

```bash
api-testing -c collection.json -e environment.json
```

Supports Postman Collection **v2.0** and **v2.1**. Folders become suites,
requests become test cases.

Postman test scripts are JavaScript, which this tool does not execute.
Instead, common `pm.*` patterns are recognized and translated:

| Script pattern | Becomes |
|---|---|
| `pm.response.to.have.status(201)` | expected status code 201 |
| `pm.expect(pm.response.code).to.eql(200)` | expected status code 200 |
| `pm.response.to.have.header("X")` | `HEADER X EXISTS` |
| `pm.expect(pm.response.responseTime).to.be.below(500)` | `RESPONSE_TIME LESS_THAN 500` |
| `pm.expect(pm.response.text()).to.include("ok")` | `RESPONSE_BODY CONTAINS ok` |
| `pm.expect(jsonData.a.b).to.eql("x")` | `JSON_PATH $.a.b EQUALS x` |
| `pm.environment.set("id", jsonData.data.id)` | extract `id` from `$.data.id` |

Anything not recognized is simply ignored — the import never fails because of
an unrecognized script line. Disabled headers/params in the collection are
imported but marked disabled (visible, not silently dropped).

---

## 12. Importing Bruno collections

```bash
api-testing -c ./my-bruno-collection      # a directory
api-testing -c single-request.bru         # one file
```

Reads `meta`, the method block (`get`/`post`/`put`/`patch`/`delete`),
`headers`, `params:query`, `params:path`, `body:json`/`body:text`/etc.,
`auth:bearer`/`auth:basic`/`auth:apikey`/`auth:none`, `assert`,
`vars:pre-request`, `vars:post-response`, and `docs`.

- A leading `~` on a header, parameter, or assertion line marks it **disabled**
  (imported, but not active) — e.g. `~X-Debug: true`.
- `folder.bru` supplies auth for everything in that folder.
- `collection.bru` supplies collection-level variables and auth.

Example `assert` block:

```
assert {
  res.status: eq 200
  res.headers.content-type: contains json
  res.body.data.id: isdefined
  res.responseTime: lt 900
  ~res.body.skipped: eq nope
}
```

---

## 13. Reports

Every run writes five files, all sharing **one report ID and timestamp** so
they can be correlated:

| File | Use it for |
|---|---|
| `test-report.html` | Reading the results yourself |
| `test-report.json` | Feeding another tool |
| `test-report.csv` | Spreadsheets — one row per assertion |
| `test-report.md` | Pasting into a PR comment or CI job summary |
| `junit-report.xml` | CI test-result tabs (Jenkins, GitHub Actions, GitLab, Azure Pipelines) |

### The HTML report

Single self-contained file — no CDN, no external font, no external script.
Opens the same whether you double-click it, email it, or publish it as a CI
artifact.

- Pass-rate ring + KPI cards (counts, duration, p95 latency, bytes)
- Bar charts: status-family distribution, requests by method
- Live search box, status filter chips, expand/collapse all
- Collapsible suites and cases (failures open by default)
- Per-case tabs: **Assertions / Request / Response / cURL**
- Pretty-printed JSON bodies with copy buttons
- Captured variables panel, and a list of any extract that matched nothing
- Slowest-tests chart
- Light/dark theme (remembered via `localStorage`)
- Print stylesheet that expands everything
- Press `/` to jump to the search box

Credentials are masked (`****`) unless you pass `--no-redact`.

### Wiring the JUnit XML report into CI

```yaml
# GitHub Actions example
- name: API tests
  run: java -jar target/ai-api-testing.jar -c api.json --out build/reports
- uses: actions/upload-artifact@v4
  if: always()
  with:
    name: api-report
    path: build/reports/
```

Most CI systems auto-detect `junit-report.xml` if pointed at the directory, or
you can wire it explicitly (e.g. `publishTestResults` in Azure Pipelines,
JUnit plugin in Jenkins).

---

## 14. Parallel execution

```bash
api-testing -c api.json --parallel --threads 8
```

- **Suites** run concurrently, on a bounded thread pool.
- **Cases within one suite always run in order**, because response chaining
  (§9) depends on order — parallelizing within a suite would silently break
  any test that extracts a value and uses it later.
- Results are collected in submission order, so the report layout is
  identical regardless of which suite happens to finish first.
- With only one suite, `--parallel` is a no-op (nothing to parallelize).

---

## 15. Tag filtering

```java
testCase.tag("smoke", "contract");
```

```bash
api-testing -c api.json --tag smoke
api-testing -c api.json --exclude-tag slow,flaky
api-testing -c api.json --tag smoke --exclude-tag flaky   # exclude wins on overlap
```

A case filtered out by tags is reported as **SKIPPED** with the reason
attached (e.g. "excluded by tag 'slow'"), rather than disappearing from the
report.

---

## 16. Retries, timeouts and redirects

```bash
api-testing -c api.json --retries 2 --retry-delay 500 \
  --timeout 30000 --connect-timeout 5000 \
  --no-follow-redirects
```

- `--retries` applies to transport errors (connection refused, timeout) and
  retryable HTTP status codes.
- `--max-body <chars>` caps how much of a response body is kept in reports —
  useful for endpoints that return very large payloads.

---

## 17. Redaction and secrets

By default, `Authorization` headers and other credential-looking values are
masked as `****` everywhere they'd otherwise appear in a report (request
headers, the generated `curl` command, the captured-variables panel).

```bash
api-testing -c api.json --no-redact   # write real credentials into the report
```

Only disable redaction for local debugging, and don't commit or share a report
generated with `--no-redact`.

---

## 18. Exit codes and CI integration

| Code | Meaning |
|---|---|
| `0` | Every executed test case passed |
| `1` | At least one test case failed or errored |
| `2` | The command line or the collection could not be understood |

Code `2` is intentionally distinct from `1` — a malformed flag or an unreadable
collection file is a different problem than a failing API, and your CI
pipeline may want to treat them differently (e.g. alert on `2` as a pipeline
misconfiguration, not a product regression).

Minimal CI step:

```bash
java -jar target/ai-api-testing.jar -c api.json --out build/reports --fail-fast
```

Exit code becomes the shell's `$?` / the step's result automatically.

---

## 19. Architecture

```
Main → CliRunner → CliOptions
                 → CollectionLoader ─┬─ PostmanParser ─┐
                 │                   └─ BrunoParser   ─┴→ TestRunDto
                 └→ TestRunExecutor
                       ├─ VariableStore (collection / environment / runtime)
                       ├─ TestSuiteExecutor          (tags, stop-on-failure)
                       │    └─ TestCaseExecutor      (9-step pipeline, see below)
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

### The nine-step test-case pipeline

1. Register the case's pre-request variables
2. Build a private deep copy of the request (so substitution never mutates
   the original test definition — a second run of the same plan must see the
   original placeholders again)
3. Substitute `{{variables}}`
4. Normalize headers, parameters, and the body
5. Resolve and apply credentials
6. Snapshot the request exactly as it will be sent (this is what the report
   shows and what the `curl` command reproduces)
7. Send it, with retries
8. Capture extracts into the variable store
9. Validate every assertion

---

## 20. Running the unit tests

```bash
mvn test
# or
./scripts/run-tests.sh
```

108 tests, no network access required — HTTP-dependent behavior is tested
against stubs, not live servers. A regression test specifically guards the
"disabled case shouldn't fail its suite" fix.

---

## 21. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| A variable shows up literally as `{{name}}` in the request/report | The name isn't defined in any layer (collection/environment/runtime/`--var`). Check spelling and check the run's warnings list. |
| `401 Unauthorized` even though auth looks right | Check scope: request-level auth overrides suite-level, which overrides run-level. An explicit `NONE` on the request stops inheritance entirely. |
| An extract shows "matched nothing" | The JSONPath expression doesn't match the actual response shape. Check the Response tab in the HTML report for the real body, and confirm you're not missing a leading `$.`. |
| A whole suite is `ERROR` | At least one case in it errored (transport failure, unreachable host, etc.) — check that case's Error Detail in the report, not just the suite line. |
| Reports directory doesn't appear | It's created relative to your current working directory unless `--out` is an absolute path. Use `--out $(pwd)/build/reports` if unsure. |
| `--parallel` doesn't seem faster | With only one suite there's nothing to parallelize — split cases across suites, or reduce case count per suite. |
| Credentials visible in a report | You passed `--no-redact`. Remove it, or make sure not to share/commit that specific report. |
| Path assertions fail on Windows in your own tests | Compare `Path` objects (`Paths.get(...)`), not `.toString()`, since separators differ by platform. |
| `mvn package` fails offline | The project has zero runtime dependencies, but Maven still needs JUnit for `test` scope the first time; run once with network access to populate `~/.m2`, or use `mvn -o -DskipTests package` afterward. |

---

## 22. FAQ

**Do I need a Postman or Bruno file to use this?**
No — you can build a `TestRunDto` directly in Java (§7). Importers are for
convenience when you already have collections elsewhere.

**Can I run against multiple environments in one invocation?**
Not in a single run — each run takes one `--env`. Script multiple invocations
with different `--env` files, or override specific values with `--var`.

**Does it execute Postman pre-request/test scripts as JavaScript?**
No. Common `pm.*` patterns are pattern-matched and translated into native
assertions/extracts (§11). Arbitrary script logic is not executed.

**Is the HTML report safe to open without internet access?**
Yes — it's fully self-contained, no external resources of any kind.

**How do I add a sixth report format?**
Implement `ReportGenerator`, then pass it alongside (or instead of) the
built-in five via `new ReportService(List<ReportGenerator>)`.

**Where do I change what counts as a "retryable" failure?**
`AbstractHttpExecutor` in `executor/common/` — retry eligibility is based on
transport exceptions and specific status codes.

**Can suites depend on each other's captured variables?**
Yes — runtime variables persist for the rest of the run once captured,
regardless of which suite captured them, as long as the consuming case runs
afterward in execution order.