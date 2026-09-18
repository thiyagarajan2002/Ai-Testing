# AI API Testing Agent

Version: 1.0-SNAPSHOT
Java: 21
Build: Maven
Test framework: JUnit Jupiter 5.12.2

## 1. Project purpose

AI API Testing Agent is a Java 21 Maven framework for executing API test cases and producing detailed execution reports. It supports GET, POST, PUT, PATCH, and DELETE requests, request parameters, request headers, request bodies, response capture, assertions, test suites, test runs, and HTML, JSON, and CSV reports.

The framework is designed so that the request and response captured during execution are available to every report generator. This prevents the report layer from losing request headers, response headers, bodies, status information, or timing data.

## 2. Supported features

- GET, POST, PUT, PATCH, DELETE execution
- Common request DTO model with HTTP-specific DTO subclasses
- Lombok `@Data` DTOs instead of manually written getters and setters
- URL path parameter replacement using `{parameter}` placeholders
- Query parameter support
- Request headers
- Request body and content type
- Automatic JSON Content-Type for body requests when a content type is not supplied
- Response status code
- Response status message
- Response headers
- Response body
- Response time in milliseconds
- Status code assertions
- Response body assertions
- Response header assertions
- EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, EMPTY, NOT_EMPTY, EXISTS, NOT_EXISTS operators
- Test case execution
- Test suite execution
- Test run execution
- Enabled/disabled test case and suite handling
- HTML dashboard report
- JSON report
- CSV report
- Request and response information in all report formats
- Postman parser entry point
- Bruno parser entry point
- JUnit 5 automated tests
- Windows PowerShell and CMD run scripts

## 3. Project structure

```text
Ai Testing/
├── pom.xml
├── README.md
├── .gitignore
├── run-tests.ps1
├── run-demo.ps1
├── run-tests.cmd
├── run-demo.cmd
└── src/
    ├── main/java/org/ai/testing/
    │   ├── Main.java
    │   ├── dto/common/
    │   ├── dto/get/
    │   ├── dto/post/
    │   ├── dto/put/
    │   ├── dto/patch/
    │   ├── dto/delete/
    │   ├── executor/
    │   ├── executor/common/
    │   ├── parser/
    │   ├── report/
    │   ├── testcase/
    │   ├── testsuite/
    │   ├── testrun/
    │   └── validation/
    └── test/java/org/ai/testing/
        ├── report/
        ├── testrun/
        └── validation/
```

`target/` and `reports/` are generated directories and are ignored by Git. Test classes exist only under `src/test/java`.

## 4. Requirements

Install:

- JDK 21
- Maven 3.9 or newer

Check Java:

```powershell
java -version
```

Check Maven:

```powershell
mvn -version
```

The compiler is configured for Java release 21.

## 5. Maven issue fixed

A previous execution was started from a directory similar to:

```text
Ai Testing/src/main/java/org/ai/testing/executor
```

Maven then reported:

```text
The goal you specified requires a project to execute but there is no POM in this directory
```

This is a working-directory problem. Maven needs the project `pom.xml`.

### Recommended Windows command

From any PowerShell directory:

```powershell
cd "C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing"
mvn -f .\pom.xml clean test -U
```

Or use the included script from any directory:

```powershell
& "C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing\run-tests.ps1"
```

The script always resolves the POM from its own project directory, so it does not depend on the current terminal directory.

CMD alternative:

```cmd
"C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing\run-tests.cmd"
```

### If PowerShell blocks scripts

Run:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

Then run `run-tests.ps1` again. This changes the policy only for the current PowerShell process.

## 6. JUnit Platform issue fixed

The project previously contained incompatible JUnit versions. The final POM aligns the test stack as follows:

```text
JUnit Jupiter       5.12.2
JUnit Platform      1.12.2
Surefire             3.5.2
```

The POM no longer mixes JUnit 5 with JUnit 6 API artifacts or TestNG for the same test suite.

The Surefire plugin explicitly uses the JUnit Jupiter 5.12.2 engine.

## 7. Jackson LocalDateTime issue fixed

Reports contain `LocalDateTime` fields. Jackson core databind alone is not sufficient for Java time types.

The final POM includes:

```text
jackson-databind       2.18.2
jackson-datatype-jsr310 2.18.2
```

The JSON generator also registers Jackson modules. This prevents JSON report generation failures caused by `LocalDateTime` serialization.

## 8. Build and test

### Option A: normal Maven command

Run from the directory containing `pom.xml`:

```powershell
mvn clean test -U
```

### Option B: Maven with an explicit POM

This is the safest command when the terminal may be in another directory:

```powershell
mvn -f "C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing\pom.xml" clean test -U
```

### Option C: included PowerShell script

```powershell
& "C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing\run-tests.ps1"
```

### Option D: included CMD script

```cmd
"C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing\run-tests.cmd"
```

### Clean stale build files manually if required

```powershell
Remove-Item -Recurse -Force .\target -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .\reports -ErrorAction SilentlyContinue
mvn clean test -U
```

Do not copy old `target/` files into the project ZIP. Stale compiled test classes can make debugging confusing.

## 9. Run the demo

The demo creates a GET test against JSONPlaceholder and expects HTTP 200.

Normal command:

```powershell
mvn compile exec:java -Dexec.mainClass="org.ai.testing.Main"
```

Recommended script:

```powershell
& "C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing\run-demo.ps1"
```

CMD:

```cmd
"C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing\run-demo.cmd"
```

The demo prints run metadata, suite counts, test case counts, execution time, status, and report paths.

## 10. Reports

A successful application run creates:

```text
reports/
├── test-report.html
├── test-report.json
└── test-report.csv
```

### HTML report

The HTML dashboard contains:

- Report name
- Run ID
- Run name
- Environment
- Execution mode
- Start and end information
- Execution time
- Suite counts
- Test case counts
- Passed, failed, and skipped status
- Request URL
- Query parameters
- Path parameters
- Request headers
- Request body
- Response status code
- Response status message
- Response time
- Response headers
- Response body
- Validation results

HTML values are escaped before insertion into the document. This includes run names, report names, headers, bodies, and other user-controlled values.

### JSON report

The JSON report serializes the complete `TestReportDto`, including the run, suites, test cases, request information, response information, and validation results.

### CSV report

The CSV report contains request, response, execution, and validation data. Values are quoted and quotes are doubled so commas, quotes, and multiline content do not corrupt the CSV structure.

The CSV includes `Run Name` as a dedicated column.

## 11. Execution architecture

```text
Main
  |
  v
TestRunExecutor
  |
  v
TestSuiteExecutor
  |
  v
TestCaseExecutor
  |
  v
TestCaseRequestFactory
  |
  v
ExecutorDispatcher
  |
  +--> GetExecutor
  +--> PostExecutor
  +--> PutExecutor
  +--> PatchExecutor
  +--> DeleteExecutor
  |
  v
ResponseDto
  |
  v
ValidationEngine
  |
  v
TestRunResultDto
  |
  v
ReportService
  |
  +--> HtmlReportGenerator
  +--> JsonReportGenerator
  +--> CsvReportGenerator
```

## 12. Request data flow

```text
TestCaseDto
  |
  v
TestCaseRequestFactory
  |
  v
HTTP-specific RequestDto
  |
  v
TestCaseExecutor
  |
  +--> normalize request
  +--> copy request for reporting
  |
  v
ExecutorDispatcher
  |
  v
HTTP Executor
```

The executed request is copied into the test result before the HTTP call. The response is stored after the HTTP call. Therefore the report layer receives both sides of the transaction.

## 13. Request DTO design

`BaseRequestDto` is the common request model. HTTP-specific DTOs extend it:

```text
BaseRequestDto
├── GetRequestDto
├── PostRequestDto
├── PutRequestDto
├── PatchRequestDto
└── DeleteRequestDto
```

Common request fields:

```text
url
headers
queryParams
pathParams
body
```

`RequestBodyDto` contains:

```text
contentType
rawBody
```

All DTO boilerplate is handled with Lombok `@Data`.

## 14. Test case model

A `TestCaseDto` contains:

```text
testCaseId
testCaseName
description
method
request
expectedStatusCode
assertions
enabled
```

A test case can have multiple assertions.

## 15. Assertion model

Assertion types:

```text
STATUS_CODE
RESPONSE_BODY
HEADER
```

Assertion operators:

```text
EQUALS
NOT_EQUALS
CONTAINS
NOT_CONTAINS
EMPTY
NOT_EMPTY
EXISTS
NOT_EXISTS
```

Examples:

```text
Status code EQUALS 200
Response body CONTAINS "id"
Response header EXISTS Content-Type
Response header CONTAINS application/json
```

## 16. Method reference

This section documents the implemented methods so the project can be maintained without inspecting every class first.

### Main

| Method | Purpose |
|---|---|
| `main(String[] args)` | Creates a sample test run, executes it, and prints the result and report paths. |

### RequestBuilder

| Method | Purpose |
|---|---|
| `buildUrl(BaseRequestDto request)` | Validates the request, replaces `{path}` placeholders, and appends query parameters. |

### ExecutorDispatcher

| Method | Purpose |
|---|---|
| `execute(String method, BaseRequestDto request)` | Selects the correct HTTP executor. |
| `convertToGetRequest(BaseRequestDto)` | Converts a common request to GET DTO. |
| `convertToPostRequest(BaseRequestDto)` | Converts a common request to POST DTO. |
| `convertToPutRequest(BaseRequestDto)` | Converts a common request to PUT DTO. |
| `convertToPatchRequest(BaseRequestDto)` | Converts a common request to PATCH DTO. |
| `convertToDeleteRequest(BaseRequestDto)` | Converts a common request to DELETE DTO. |
| `copyFields(BaseRequestDto, BaseRequestDto)` | Copies common request fields between DTOs. |

### HTTP executors

| Class | Method | Purpose |
|---|---|---|
| `GetExecutor` | `execute(GetRequestDto)` | Executes GET and captures response data. |
| `PostExecutor` | `execute(PostRequestDto)` | Executes POST and captures response data. |
| `PutExecutor` | `execute(PutRequestDto)` | Executes PUT and captures response data. |
| `PatchExecutor` | `execute(PatchRequestDto)` | Executes PATCH and captures response data. |
| `DeleteExecutor` | `execute(DeleteRequestDto)` | Executes DELETE and captures response data. |

### TestCaseRequestFactory

| Method | Purpose |
|---|---|
| `createRequest(TestCaseDto)` | Creates the HTTP-specific request DTO for the test case method. |
| `createGetRequest(TestCaseDto)` | Creates GET request DTO. |
| `createPostRequest(TestCaseDto)` | Creates POST request DTO. |
| `createPutRequest(TestCaseDto)` | Creates PUT request DTO. |
| `createPatchRequest(TestCaseDto)` | Creates PATCH request DTO. |
| `createDeleteRequest(TestCaseDto)` | Creates DELETE request DTO. |
| `copyBaseFields(TestCaseDto, BaseRequestDto)` | Copies URL, headers, parameters, and body data. |

### TestCaseExecutor

| Method | Purpose |
|---|---|
| `execute(TestCaseDto)` | Executes one test case, captures request and response, validates assertions, and creates the execution result. |
| `normalizeRequest(BaseRequestDto)` | Ensures mutable request maps and body Content-Type defaults are available. |
| `copyRequest(BaseRequestDto)` | Creates a report-safe copy of the executed request. |
| `validateResponse(ResponseDto, TestCaseDto)` | Runs all configured assertions and creates a validation summary. |
| `executeAssertion(AssertionDto, ResponseDto)` | Dispatches an assertion to the appropriate validator. |
| `executeStatusCodeAssertion(AssertionDto, ResponseDto)` | Validates a status code assertion. |
| `createFailedAssertionResult(String)` | Creates a failed validation result when assertion execution cannot continue. |
| `addResult(ValidationSummaryDto, ValidationResultDto)` | Adds one validation result and updates summary state. |

`TestCaseExecutionResult` is a nested result model containing test case status, request, response, message, and validation summary.

### TestSuiteExecutor

| Method | Purpose |
|---|---|
| `execute(TestSuiteDto)` | Executes enabled test cases in a suite and calculates suite counts. |
| `buildSummaryMessage(...)` | Builds the suite execution summary text. |

### TestRunExecutor

| Method | Purpose |
|---|---|
| `execute(TestRunDto)` | Executes all suites, calculates run counts, records timing, and generates all reports. |
| `updateSuiteCounts(...)` | Updates passed, failed, and skipped suite counts. |
| `updateTestCaseCounts(...)` | Updates passed, failed, and skipped test case counts. |
| `generateReport(...)` | Generates HTML, JSON, and CSV reports and records a report-generation error in the run message if generation fails. |
| `elapsedMilliseconds(long)` | Converts elapsed nanoseconds to milliseconds. |
| `buildSummaryMessage(...)` | Creates the final run summary message. |

### ValidationEngine

| Method | Purpose |
|---|---|
| `validateStatusCode(...)` | Validates status code assertions. |
| `validateBody(...)` | Validates response body assertions. |
| `validateHeader(...)` | Validates response header assertions. |
| `validateAll(...)` | Executes the configured assertion groups and combines results. |
| `addResult(...)` | Adds validation output to a summary. |

Nested records:

```text
ValidationEngine.BodyValidation
ValidationEngine.HeaderValidation
```

### Validators

| Class | Method | Purpose |
|---|---|---|
| `StatusCodeValidator` | `validate(...)` | Performs status code comparison. |
| `ResponseBodyValidator` | `validate(...)` | Performs response body comparison. |
| `HeaderValidator` | `validate(...)` | Performs response header comparison. |

### ReportService

| Method | Purpose |
|---|---|
| `generateHtmlReport(TestRunResultDto)` | Creates metadata and writes HTML. |
| `generateJsonReport(TestRunResultDto)` | Creates metadata and writes JSON. |
| `generateCsvReport(TestRunResultDto)` | Creates metadata and writes CSV. |
| `generateAllReports(TestRunResultDto)` | Creates and writes all three report formats in one operation. |
| `createReport(...)` | Builds the report DTO shared by generators. |
| `validateTestRunResult(...)` | Rejects a null test run result. |
| `generateReportId(...)` | Creates a unique report identifier. |
| `buildReportName(...)` | Creates the report display name from the run name. |

### HtmlReportGenerator

| Method | Purpose |
|---|---|
| `generate(TestReportDto)` | Creates parent directories and writes the HTML file. |
| `buildHtml(...)` | Builds the complete dashboard HTML. |
| `appendHeader(...)` | Adds report and run metadata. |
| `appendSummary(...)` | Adds run-level summary cards. |
| `appendSuites(...)` | Adds all suite sections. |
| `appendSuite(...)` | Adds one suite and its status. |
| `appendTestCases(...)` | Adds test cases inside a suite. |
| `appendTestCase(...)` | Adds one test case and its details. |
| `appendRequest(...)` | Adds request URL, parameters, headers, and body. |
| `appendHeaders(...)` | Adds request or response headers. |
| `formatMap(...)` | Formats map values for display. |
| `appendResponse(...)` | Adds response status, headers, body, and timing. |
| `appendValidationResults(...)` | Adds assertion results. |
| `formatDate(...)` | Formats report timestamps. |
| `nullToEmpty(...)` | Converts null strings to empty strings. |
| `escapeHtml(...)` | Escapes HTML-sensitive characters to keep report content safe. |

### JsonReportGenerator

| Method | Purpose |
|---|---|
| `generate(TestReportDto)` | Creates parent directories and serializes the report to JSON. |
| Constructors | Configure the default or custom JSON output path. |

### CsvReportGenerator

| Method | Purpose |
|---|---|
| `generate(TestReportDto)` | Creates parent directories and writes CSV. |
| `buildCsv(...)` | Builds CSV text from the complete run result. |
| `appendSuiteRows(...)` | Adds suite-level rows. |
| `appendTestCaseRows(...)` | Adds test case and validation rows. |
| `baseValues(...)` | Builds common report columns, including Run Name. |
| `combine(...)` | Combines groups of CSV values. |
| `writeRow(...)` | Writes one escaped CSV row. |
| `formatMap(...)` | Converts map values to readable CSV content. |
| `testCaseStatus(...)` | Converts test case execution state to PASSED, FAILED, or SKIPPED. |
| `suiteStatus(...)` | Converts suite execution state to PASSED, FAILED, or SKIPPED. |
| `nullToEmpty(...)` | Converts null values to empty strings. |
| `csvValue(...)` | Quotes and escapes CSV values. |

### Parsers

`PostmanParser` and `BrunoParser` are parser entry points reserved for collection/file import integration. Their current source classes are intentionally lightweight in this version.

## 17. Report generation contract

`ReportGenerator` defines the common generator operation:

```text
generate(TestReportDto report)
```

`HtmlReportGenerator`, `JsonReportGenerator`, and `CsvReportGenerator` implement that contract.

`ReportService.generateAllReports(...)` invokes all three generators. The test run executor calls this combined method, not only the HTML generator.

## 18. Error handling

The framework validates null or empty inputs at important boundaries.

Examples:

```text
Request cannot be null
Request URL cannot be null or empty
HTTP method cannot be null or empty
Unsupported HTTP method: ...
Test run cannot be null
Test run result cannot be null
Report cannot be null
```

Report generation exceptions are captured by `TestRunExecutor.generateReport(...)` and added to the run message so the execution result still explains what happened.

## 19. Tests included

The final source layout keeps tests only in `src/test/java`.

Test classes:

```text
CsvReportGeneratorTest
HtmlReportGeneratorTest
JsonReportGeneratorTest
ReportServiceTest
TestRunExecutorTest
ValidationEngineTest
```

The report tests cover important regression cases including:

- Parent directory creation
- JSON serialization
- Java time serialization
- CSV escaping
- HTML escaping
- Run name rendering
- Report service delegation
- Combined report generation

## 20. Expected Maven result

After installing JDK 21 and Maven, run:

```powershell
mvn clean test -U
```

The expected Maven test phase should execute the JUnit tests instead of showing `Tests run: 0`. The previous JUnit Platform `OutputDirectoryCreator` failure was caused by dependency/version mismatch and is addressed in this project POM.

## 21. If Maven still fails locally

First verify versions:

```powershell
java -version
mvn -version
```

Then clean stale files:

```powershell
Remove-Item -Recurse -Force .\target -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .\reports -ErrorAction SilentlyContinue
```

Run:

```powershell
mvn -f .\pom.xml clean test -U
```

If Maven is being run from a child directory, use `-f` with the absolute POM path.

If dependency download is interrupted, run again with `-U` after network access is restored.

## 22. Development rules for future changes

When changing this project:

1. Keep production Java files under `src/main/java`.
2. Keep JUnit tests under `src/test/java` only.
3. Keep all JUnit versions aligned.
4. Keep Jackson core and Java Time module versions aligned.
5. Run `mvn clean test` after source changes.
6. Run the demo after execution/reporting changes.
7. Verify HTML, JSON, and CSV files after report changes.
8. Update this README whenever a method, feature, dependency, execution flow, report field, or usage command changes.
9. Do not commit `target/` or generated `reports/` files.

## 23. Current fixed issues summary

```text
[FIXED] Maven command documented and project-root scripts added
[FIXED] Wrong-directory Maven failure explained and prevented by -f scripts
[FIXED] JUnit dependency mismatch
[FIXED] JUnit Platform OutputDirectoryCreator startup failure
[FIXED] Duplicate test classes under src/main/java removed
[FIXED] Jackson Java Time module added
[FIXED] JSON report parent directory creation supported
[FIXED] CSV Run Name column added
[FIXED] CSV special-character escaping regression fixed
[FIXED] HTML Run Name rendering added
[FIXED] HTML content escaping regression fixed
[FIXED] TestRunExecutor generates HTML + JSON + CSV together
[FIXED] Request details retained for reporting
[FIXED] Response headers retained for reporting
[FIXED] Request headers retained for reporting
[FIXED] Stale target artifacts excluded from final source package
[ADDED] PowerShell build/test script
[ADDED] PowerShell demo script
[ADDED] CMD build/test script
[ADDED] CMD demo script
[UPDATED] Complete project documentation and method reference
```

## 24. Final execution commands

For the simplest workflow on Windows:

```powershell
cd "C:\Users\traja\OneDrive\Desktop\api-test-automation\Ai Testing"
mvn clean test -U
mvn compile exec:java -Dexec.mainClass="org.ai.testing.Main"
```

Or run the scripts:

```powershell
.\run-tests.ps1
.\run-demo.ps1
```

The scripts resolve the project root from their own location, so the Maven working-directory problem does not recur.
