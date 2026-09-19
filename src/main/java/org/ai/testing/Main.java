package org.ai.testing;

import org.ai.testing.dto.common.*;
import org.ai.testing.env.VariableStore;
import org.ai.testing.parser.CollectionLoader;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.RunOptions;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testrun.executor.TestRunExecutor;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * ExamplesMain — a single, self-contained tour of the AI API Testing Agent.
 *
 * <p>This file is not part of the library; it is a reference you can read top
 * to bottom, or run directly, to see every major feature exercised in one
 * place: building a plan in Java, every assertion type and operator, variable
 * layering and response chaining, every authentication mode, importing
 * Postman and Bruno collections, tag filtering, parallel execution, retries,
 * redaction, and report generation.</p>
 *
 * <p>Run it with:</p>
 * <pre>
 *   javac -cp target/classes -d /tmp/out ExamplesMain.java
 *   java -cp target/classes:/tmp/out org.ai.testing.examples.ExamplesMain
 * </pre>
 *
 * <p>Or drop it into {@code src/main/java/org/ai/testing/examples/} and build
 * with Maven as usual. Each {@code exampleN} method is independent — read
 * whichever section you need and ignore the rest.</p>
 */
public final class Main {

    /** Public demo API used throughout so every example runs without setup. */
    private static final String BASE_URL = "https://petstore3.swagger.io/api/v3";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Minimal request, one assertion ===");
        example1_minimalRequest();

//        System.out.println("\n=== 2. Every assertion type ===");
//        example2_everyAssertionType();
//
//        System.out.println("\n=== 3. Every assertion operator ===");
//        example3_everyOperator();
//
//        System.out.println("\n=== 4. Variables: collection / environment / runtime layers ===");
//        example4_variableLayers();
//
//        System.out.println("\n=== 5. Response chaining: capture a token, use it later ===");
//        example5_responseChaining();
//
//        System.out.println("\n=== 6. Every authentication mode ===");
//        example6_everyAuthMode();
//
//        System.out.println("\n=== 7. Multiple suites, tags, and stop-on-failure ===");
//        example7_suitesAndTags();
//
//        System.out.println("\n=== 8. Tag filtering at the run level (--tag / --exclude-tag) ===");
//        example8_tagFiltering();
//
//        System.out.println("\n=== 9. Parallel execution, retries, timeouts ===");
//        example9_parallelAndRetries();
//
//        System.out.println("\n=== 10. Redaction on and off ===");
//        example10_redaction();
//
//        System.out.println("\n=== 11. Importing a Postman collection ===");
//        example11_importPostman();
//
//        System.out.println("\n=== 12. Importing a Bruno collection ===");
//        example12_importBruno();
//
//        System.out.println("\n=== 13. Generating a custom subset of reports ===");
//        example13_customReports();
//
//        System.out.println("\n=== 14. Reading results back out (pass rate, slowest tests, failures) ===");
//        example14_inspectingResults();
//
//        System.out.println("\nAll examples completed. See ./reports and ./examples-output for output files.");
    }

    // ==================================================================
    // 1. Minimal request
    // ==================================================================

    /**
     * The smallest possible test: one case, one expected status code, run,
     * and reported. Everything else in this file builds on this shape.
     */
    private static void example1_minimalRequest() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 1 - Minimal request");

        TestSuiteDto suite = new TestSuiteDto("SUITE-1", "Health check");

        TestCaseDto openApi = new TestCaseDto("TC-1", "OpenAPI document is served", "GET");
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(BASE_URL + "/openapi.json");
        openApi.setRequest(request);
        openApi.setExpectedStatusCode(200);

        suite.add(openApi);
        run.add(suite);

        TestRunResultDto result = execute(run, "examples-output/01-minimal");
        printSummary(result);
    }

    // ==================================================================
    // 2. Every assertion type
    // ==================================================================

    /**
     * STATUS_CODE, RESPONSE_BODY, HEADER, JSON_PATH, RESPONSE_TIME,
     * RESPONSE_SIZE and CONTENT_TYPE — one case exercising each.
     */
    private static void example2_everyAssertionType() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 2 - Every assertion type");

        TestSuiteDto suite = new TestSuiteDto("SUITE-2", "Assertion types");

        TestCaseDto testCase = new TestCaseDto("TC-2", "Inventory endpoint, fully checked", "GET");
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(BASE_URL + "/store/inventory");
        request.header("Accept", "application/json");
        testCase.setRequest(request);

        // STATUS_CODE — the expected status is a first-class field, not a
        // separate assertion, but the same check can be spelled out too.
        testCase.setExpectedStatusCode(200);

        // RESPONSE_BODY — checks the raw body text.
        testCase.assertion(AssertionDto.body(AssertionOperator.NOT_EMPTY, ""));

        // HEADER — matched case-insensitively.
        testCase.assertion(AssertionDto.header("Content-Type",
                AssertionOperator.CONTAINS, "json"));

        // JSON_PATH — see the JSONPath subset in example 3 / the guide.
        testCase.assertion(AssertionDto.jsonPath("$.available",
                AssertionOperator.EXISTS, ""));

        // RESPONSE_TIME — milliseconds.
        testCase.assertion(AssertionDto.responseTimeBelow(15_000));

        // RESPONSE_SIZE — bytes. Built with the general constructor because
        // there is no dedicated factory method for this shorthand type.
        testCase.assertion(new AssertionDto(AssertionType.RESPONSE_SIZE, "bodySizeBytes",
                AssertionOperator.GREATER_THAN, "2"));

        // CONTENT_TYPE — shorthand for the Content-Type header.
        testCase.assertion(new AssertionDto(AssertionType.CONTENT_TYPE, "Content-Type",
                AssertionOperator.CONTAINS, "application"));

        suite.add(testCase);
        run.add(suite);

        TestRunResultDto result = execute(run, "examples-output/02-assertion-types");
        printSummary(result);
    }

    // ==================================================================
    // 3. Every assertion operator
    // ==================================================================

    /**
     * All seventeen operators, demonstrated against a JSONPath field so they
     * can be compared side by side. In real tests you would pick the one
     * operator that fits; this is a reference sheet, not a style to copy.
     */
    private static void example3_everyOperator() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 3 - Every operator");

        TestSuiteDto suite = new TestSuiteDto("SUITE-3", "Operators");

        TestCaseDto testCase = new TestCaseDto("TC-3", "Pet lookup with full operator sweep", "GET");
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(BASE_URL + "/pet/findByStatus");
        request.query("status", "available");
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);

        // The response is a JSON array; $[0].status is the first pet's status.
        String field = "$[0].status";

        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.EQUALS, "available"));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.NOT_EQUALS, "sold"));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.CONTAINS, "avail"));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.NOT_CONTAINS, "zzz"));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.STARTS_WITH, "avail"));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.ENDS_WITH, "able"));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.MATCHES, "^[a-z]+$"));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.EXISTS, ""));
        testCase.assertion(AssertionDto.jsonPath("$[0].nope", AssertionOperator.NOT_EXISTS, ""));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.NOT_EMPTY, ""));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.IN,
                "available,pending,sold"));
        testCase.assertion(AssertionDto.jsonPath(field, AssertionOperator.NOT_IN,
                "discontinued,recalled"));

        // Numeric operators against the array length.
        String countField = "$.length()";
        testCase.assertion(AssertionDto.jsonPath(countField, AssertionOperator.GREATER_THAN, "0"));
        testCase.assertion(AssertionDto.jsonPath(countField,
                AssertionOperator.GREATER_THAN_OR_EQUAL, "1"));
        testCase.assertion(AssertionDto.jsonPath(countField, AssertionOperator.LESS_THAN, "100000"));
        testCase.assertion(AssertionDto.jsonPath(countField,
                AssertionOperator.LESS_THAN_OR_EQUAL, "100000"));

        suite.add(testCase);
        run.add(suite);

        TestRunResultDto result = execute(run, "examples-output/03-operators");
        printSummary(result);
    }

    // ==================================================================
    // 4. Variable layers
    // ==================================================================

    /**
     * Collection variables are the default; environment variables (and
     * {@code --var} on the command line) override them; values captured at
     * runtime override everything. This example sets the same variable name
     * at two layers to show which one wins, then demonstrates a nested
     * placeholder and an unresolved one.
     */
    private static void example4_variableLayers() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 4 - Variable layers");

        // Collection layer: the default host and version.
        run.getCollectionVariables().put("host", "petstore3.swagger.io");
        run.getCollectionVariables().put("apiPath", "/api/v3");
        run.getCollectionVariables().put("status", "collection-value");

        // Environment layer overrides the same key.
        run.getEnvironmentVariables().put("status", "environment-value");

        // A nested placeholder: baseUrl is built from host and apiPath.
        run.getCollectionVariables().put("baseUrl", "https://{{host}}{{apiPath}}");

        TestSuiteDto suite = new TestSuiteDto("SUITE-4", "Variable resolution");

        TestCaseDto testCase = new TestCaseDto("TC-4", "Nested and overridden variables", "GET");
        BaseRequestDto request = new BaseRequestDto();
        // Resolves through host + apiPath, then substitutes into the URL.
        request.setUrl("{{baseUrl}}/openapi.json");
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);
        testCase.assertion(AssertionDto.jsonPath("$.openapi",
                AssertionOperator.STARTS_WITH, "3."));

        suite.add(testCase);
        run.add(suite);

        TestRunResultDto result = execute(run, "examples-output/04-variables");
        printSummary(result);

        // "status" resolved to "environment-value" because the environment
        // layer overrides the collection layer. An unknown variable such as
        // {{doesNotExist}} would be left visible in the request rather than
        // silently becoming an empty string — check result.getWarnings() for
        // any such case.
        System.out.println("  status resolved to: "
                + result.getVariables().getOrDefault("status", "(not captured)"));
    }

    // ==================================================================
    // 5. Response chaining
    // ==================================================================

    /**
     * Captures a value from one response and uses it in a later request.
     * Since the public Petstore API has no real login endpoint, this example
     * captures a harmless value (the API title) instead of a token, but the
     * mechanism is identical to capturing an auth token from a login call.
     */
    private static void example5_responseChaining() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 5 - Response chaining");
        run.getCollectionVariables().put("baseUrl", BASE_URL);

        TestSuiteDto suite = new TestSuiteDto("SUITE-5", "Chaining");

        // Step 1: call an endpoint and capture a value from its response.
        TestCaseDto first = new TestCaseDto("TC-5A", "Fetch and capture the API title", "GET");
        BaseRequestDto firstRequest = new BaseRequestDto();
        firstRequest.setUrl("{{baseUrl}}/openapi.json");
        first.setRequest(firstRequest);
        first.setExpectedStatusCode(200);
        first.extract(ExtractDto.fromBody("apiTitle", "$.info.title"));
        first.extract(ExtractDto.fromHeader("contentType", "Content-Type"));
        suite.add(first);

        // Step 2: a later case references {{apiTitle}}. Here we just prove it
        // resolved, by asserting the captured variable landed in the run's
        // variable snapshot; in a real test you'd use it in a header, a query
        // parameter, or an auth token (see example 6).
        TestCaseDto second = new TestCaseDto("TC-5B", "Use the captured value", "GET");
        BaseRequestDto secondRequest = new BaseRequestDto();
        secondRequest.setUrl("{{baseUrl}}/store/inventory");
        secondRequest.header("X-Previous-Api-Title", "{{apiTitle}}");
        second.setRequest(secondRequest);
        second.setExpectedStatusCode(200);
        suite.add(second);

        run.add(suite);

        TestRunResultDto result = execute(run, "examples-output/05-chaining");
        printSummary(result);
        System.out.println("  captured apiTitle = "
                + result.getVariables().getOrDefault("apiTitle", "(not captured)"));
    }

    // ==================================================================
    // 6. Every authentication mode
    // ==================================================================

    /**
     * BEARER, BASIC, API_KEY (header and query placement), NONE, and INHERIT.
     * These requests are built but not necessarily meaningful against the
     * public Petstore API (which does not require auth) — the point is to
     * show exactly how each AuthDto is constructed and attached.
     */
    private static void example6_everyAuthMode() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 6 - Authentication modes");
        run.getCollectionVariables().put("baseUrl", BASE_URL);

        // Run-level auth: the default for every suite/case that doesn't
        // override it and doesn't set NONE.
        run.setAuth(AuthDto.bearer("run-level-token"));

        TestSuiteDto suite = new TestSuiteDto("SUITE-6", "Auth modes");
        // Suite-level auth overrides the run-level default for every case in
        // this suite, unless a case sets its own.
        suite.setAuth(AuthDto.basic("suite-user", "suite-pass"));

        // BEARER at the request level — most specific, wins over suite and run.
        TestCaseDto bearerCase = caseWithAuth("TC-6A", "Bearer token",
                AuthDto.bearer("request-level-token"));
        suite.add(bearerCase);

        // BASIC at the request level.
        TestCaseDto basicCase = caseWithAuth("TC-6B", "Basic credentials",
                AuthDto.basic("alice", "s3cret"));
        suite.add(basicCase);

        // API_KEY sent as a header.
        TestCaseDto apiKeyHeaderCase = caseWithAuth("TC-6C", "API key in a header",
                AuthDto.apiKey("X-Api-Key", "key-value-123", "HEADER"));
        suite.add(apiKeyHeaderCase);

        // API_KEY sent as a query parameter.
        TestCaseDto apiKeyQueryCase = caseWithAuth("TC-6D", "API key in the query string",
                AuthDto.apiKey("api_key", "key-value-123", "QUERY"));
        suite.add(apiKeyQueryCase);

        // NONE — explicitly unauthenticated, stops inheritance from the
        // suite's Basic auth and the run's Bearer auth.
        AuthDto none = new AuthDto();
        none.setType(AuthType.NONE);
        TestCaseDto noneCase = caseWithAuth("TC-6E", "Explicitly no auth", none);
        suite.add(noneCase);

        // INHERIT — falls through to the suite (Basic), since no request
        // override is set. Simply omit setAuth() to get this behavior; shown
        // here explicitly for clarity.
        AuthDto inherit = new AuthDto();
        inherit.setType(AuthType.INHERIT);
        TestCaseDto inheritCase = caseWithAuth("TC-6F", "Inherits suite auth", inherit);
        suite.add(inheritCase);

        run.add(suite);

        TestRunResultDto result = execute(run, "examples-output/06-auth");
        printSummary(result);
    }

    private static TestCaseDto caseWithAuth(String id, String name, AuthDto auth) {
        TestCaseDto testCase = new TestCaseDto(id, name, "GET");
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("{{baseUrl}}/store/inventory");
        request.setAuth(auth);
        testCase.setRequest(request);
        // The public Petstore API ignores auth entirely, so every mode still
        // returns 200 — this example is about construction, not enforcement.
        testCase.setExpectedStatusCode(200);
        return testCase;
    }

    // ==================================================================
    // 7. Multiple suites, tags, stop-on-failure
    // ==================================================================

    private static void example7_suitesAndTags() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 7 - Suites and tags");
        run.getCollectionVariables().put("baseUrl", BASE_URL);

        TestSuiteDto contract = new TestSuiteDto("SUITE-7A", "Contract");
        TestCaseDto openApi = new TestCaseDto("TC-7A1", "OpenAPI document", "GET");
        BaseRequestDto openApiRequest = new BaseRequestDto();
        openApiRequest.setUrl("{{baseUrl}}/openapi.json");
        openApi.setRequest(openApiRequest);
        openApi.setExpectedStatusCode(200);
        openApi.tag("smoke", "contract");
        contract.add(openApi);

        TestSuiteDto inventory = new TestSuiteDto("SUITE-7B", "Inventory");
        // stopOnFailure: if the first case in this suite fails, the rest are
        // reported as skipped instead of being attempted.
        inventory.setStopOnFailure(true);

        TestCaseDto readInventory = new TestCaseDto("TC-7B1", "Read inventory", "GET");
        BaseRequestDto inventoryRequest = new BaseRequestDto();
        inventoryRequest.setUrl("{{baseUrl}}/store/inventory");
        readInventory.setRequest(inventoryRequest);
        readInventory.setExpectedStatusCode(200);
        readInventory.tag("smoke", "regression");
        inventory.add(readInventory);

        TestCaseDto findByStatus = new TestCaseDto("TC-7B2", "Find pets by status", "GET");
        BaseRequestDto findRequest = new BaseRequestDto();
        findRequest.setUrl("{{baseUrl}}/pet/findByStatus");
        findRequest.query("status", "available");
        findByStatus.setRequest(findRequest);
        findByStatus.setExpectedStatusCode(200);
        findByStatus.tag("regression");
        inventory.add(findByStatus);

        // A disabled case: reported as SKIPPED, never counted as a failure,
        // and does not stop the suite.
        TestCaseDto disabled = new TestCaseDto("TC-7B3", "Deliberately disabled", "DELETE");
        BaseRequestDto disabledRequest = new BaseRequestDto();
        disabledRequest.setUrl("{{baseUrl}}/pet/1");
        disabled.setRequest(disabledRequest);
        disabled.setEnabled(false);
        inventory.add(disabled);

        run.add(contract);
        run.add(inventory);

        TestRunResultDto result = execute(run, "examples-output/07-suites-and-tags");
        printSummary(result);
    }

    // ==================================================================
    // 8. Tag filtering at the run level
    // ==================================================================

    /**
     * Same plan as example 7, but this time RunOptions restricts execution
     * to cases tagged "smoke" — mirroring what {@code --tag smoke} does on
     * the command line.
     */
    private static void example8_tagFiltering() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 8 - Tag filtering");
        run.getCollectionVariables().put("baseUrl", BASE_URL);

        TestSuiteDto suite = new TestSuiteDto("SUITE-8", "Mixed tags");

        TestCaseDto smokeCase = new TestCaseDto("TC-8A", "Smoke: OpenAPI document", "GET");
        BaseRequestDto smokeRequest = new BaseRequestDto();
        smokeRequest.setUrl("{{baseUrl}}/openapi.json");
        smokeCase.setRequest(smokeRequest);
        smokeCase.setExpectedStatusCode(200);
        smokeCase.tag("smoke");
        suite.add(smokeCase);

        TestCaseDto regressionCase = new TestCaseDto("TC-8B", "Regression: inventory", "GET");
        BaseRequestDto regressionRequest = new BaseRequestDto();
        regressionRequest.setUrl("{{baseUrl}}/store/inventory");
        regressionCase.setRequest(regressionRequest);
        regressionCase.setExpectedStatusCode(200);
        regressionCase.tag("regression", "slow");
        suite.add(regressionCase);

        run.add(suite);

        // Equivalent to: api-testing -c ... --tag smoke --exclude-tag slow
        run.getOptions().setIncludeTags(Set.of("smoke"));
        run.getOptions().setExcludeTags(Set.of("slow"));

        TestRunResultDto result = execute(run, "examples-output/08-tag-filtering");
        printSummary(result);
        System.out.println("  regression case should show as SKIPPED (filtered by tag)");
    }

    // ==================================================================
    // 9. Parallel execution, retries, timeouts
    // ==================================================================

    private static void example9_parallelAndRetries() {
        TestRunDto run = new TestRunDto();
        run.setRunName("Example 9 - Parallel and retries");
        run.getCollectionVariables().put("baseUrl", BASE_URL);

        RunOptions options = run.getOptions();
        // Equivalent to: --parallel --threads 4 --retries 2 --retry-delay 500
        //                --timeout 30000 --connect-timeout 5000
        options.setExecutionMode("PARALLEL");
        options.setThreads(4);
        options.setRetries(2);
        options.setRetryDelayMs(500);
        options.setRequestTimeoutMs(30_000);
        options.setConnectTimeoutMs(5_000);
        options.setFollowRedirects(true);
        options.setMaxBodyChars(50_000);

        // Multiple suites so PARALLEL actually has something to parallelize —
        // a single-suite run executes sequentially regardless of this flag.
        for (int i = 1; i <= 3; i++) {
            TestSuiteDto suite = new TestSuiteDto("SUITE-9-" + i, "Parallel suite " + i);
            TestCaseDto testCase = new TestCaseDto("TC-9-" + i, "Inventory check " + i, "GET");
            BaseRequestDto request = new BaseRequestDto();
            request.setUrl("{{baseUrl}}/store/inventory");
            testCase.setRequest(request);
            testCase.setExpectedStatusCode(200);
            suite.add(testCase);
            run.add(suite);
        }

        TestRunResultDto result = execute(run, "examples-output/09-parallel");
        printSummary(result);
        System.out.println("  execution mode: " + result.getExecutionMode());
        System.out.println("  requests/sec:   "
                + String.format("%.2f", result.getMetrics().getRequestsPerSecond()));
    }

    // ==================================================================
    // 10. Redaction
    // ==================================================================

    /**
     * By default, credentials are masked everywhere they would otherwise
     * appear in a report. This example runs the same authenticated request
     * twice — once with redaction on (the default) and once with it off — so
     * you can open both HTML reports and compare the Authorization header.
     */
    private static void example10_redaction() {
        TestSuiteDto suite = new TestSuiteDto("SUITE-10", "Redaction");
        TestCaseDto testCase = new TestCaseDto("TC-10", "Authenticated request", "GET");
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(BASE_URL + "/store/inventory");
        request.setAuth(AuthDto.bearer("super-secret-token-do-not-leak"));
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);
        suite.add(testCase);

        TestRunDto redactedRun = new TestRunDto();
        redactedRun.setRunName("Example 10a - Redacted (default)");
        redactedRun.add(cloneSuite(suite));
        redactedRun.getOptions().setRedactSecrets(true);
        TestRunResultDto redactedResult = execute(redactedRun, "examples-output/10a-redacted");
        printSummary(redactedResult);

        TestRunDto unredactedRun = new TestRunDto();
        unredactedRun.setRunName("Example 10b - Unredacted (--no-redact)");
        unredactedRun.add(cloneSuite(suite));
        unredactedRun.getOptions().setRedactSecrets(false);
        TestRunResultDto unredactedResult = execute(unredactedRun, "examples-output/10b-unredacted");
        printSummary(unredactedResult);

        System.out.println("  compare examples-output/10a-redacted/test-report.html");
        System.out.println("      with examples-output/10b-unredacted/test-report.html");
    }

    /** A fresh TestSuiteDto with the same single case, so each run gets its own copy. */
    private static TestSuiteDto cloneSuite(TestSuiteDto original) {
        TestSuiteDto copy = new TestSuiteDto(original.getSuiteId(), original.getSuiteName());
        for (TestCaseDto testCase : original.getTestCases()) {
            TestCaseDto testCaseCopy = new TestCaseDto(
                    testCase.getTestCaseId(), testCase.getTestCaseName(), testCase.getMethod());
            testCaseCopy.setRequest(testCase.getRequest().copy());
            testCaseCopy.setExpectedStatusCode(testCase.getExpectedStatusCode());
            copy.add(testCaseCopy);
        }
        return copy;
    }

    // ==================================================================
    // 11. Importing a Postman collection
    // ==================================================================

    /**
     * Writes a minimal Postman v2.1 collection to a temp file, then imports
     * and runs it exactly the way {@code -c collection.json} would from the
     * command line. See the master guide for the full pm.* script mapping
     * table this importer understands.
     */
    private static void example11_importPostman() throws Exception {
        String json = """
                {
                  "info": { "name": "Example Postman Collection" },
                  "variable": [ { "key": "baseUrl", "value": "%s" } ],
                  "item": [
                    {
                      "name": "Health",
                      "item": [
                        {
                          "name": "Fetch OpenAPI document",
                          "request": {
                            "method": "GET",
                            "header": [ { "key": "Accept", "value": "application/json" } ],
                            "url": { "raw": "{{baseUrl}}/openapi.json" }
                          },
                          "event": [
                            {
                              "listen": "test",
                              "script": { "exec": [
                                "pm.response.to.have.status(200);",
                                "pm.expect(pm.response.responseTime).to.be.below(15000);",
                                "var jsonData = pm.response.json();",
                                "pm.environment.set(\\"apiTitle\\", jsonData.info.title);"
                              ] }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.formatted(BASE_URL);

        Path directory = Paths.get("examples-output/11-postman-import");
        java.nio.file.Files.createDirectories(directory);
        Path collectionFile = directory.resolve("collection.json");
        java.nio.file.Files.writeString(collectionFile, json);

        TestRunDto run = new CollectionLoader().load(collectionFile, null);
        System.out.println("  imported run: " + run.getRunName()
                + " (" + run.totalTestCases() + " test case(s))");

        TestRunResultDto result = execute(run, "examples-output/11-postman-import");
        printSummary(result);
    }

    // ==================================================================
    // 12. Importing a Bruno collection
    // ==================================================================

    /**
     * Writes a minimal single-file Bruno request and imports it the way
     * {@code -c request.bru} would. A directory-based Bruno collection
     * (with collection.bru, folder.bru, and multiple *.bru requests) is
     * imported the same way — just pass the directory path instead of a file.
     */
    private static void example12_importBruno() throws Exception {
        String bru = """
                meta {
                  name: Fetch OpenAPI document
                  seq: 1
                  tags: smoke contract
                }

                get {
                  url: %s/openapi.json
                }

                headers {
                  Accept: application/json
                }

                assert {
                  res.status: eq 200
                  res.headers.content-type: contains json
                  res.body.openapi: startswith 3.
                  res.responseTime: lt 15000
                }

                vars:post-response {
                  apiTitle: res.body.info.title
                }
                """.formatted(BASE_URL);

        Path directory = Paths.get("examples-output/12-bruno-import");
        java.nio.file.Files.createDirectories(directory);
        Path requestFile = directory.resolve("fetch-openapi.bru");
        java.nio.file.Files.writeString(requestFile, bru);

        TestRunDto run = new CollectionLoader().load(requestFile, null);
        System.out.println("  imported run: " + run.getRunName()
                + " (" + run.totalTestCases() + " test case(s))");

        TestRunResultDto result = execute(run, "examples-output/12-bruno-import");
        printSummary(result);
    }

    // ==================================================================
    // 13. Custom report subset
    // ==================================================================

    /**
     * ReportService normally writes all five formats. Here only HTML and
     * JUnit XML are generated — useful in CI where you want the human-facing
     * dashboard as a build artifact and the XML for the test-results tab, but
     * don't need CSV or Markdown every time.
     */
    private static void example13_customReports() {
        TestSuiteDto suite = new TestSuiteDto("SUITE-13", "Custom report subset");
        TestCaseDto testCase = new TestCaseDto("TC-13", "OpenAPI document", "GET");
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(BASE_URL + "/openapi.json");
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);
        suite.add(testCase);

        TestRunDto run = new TestRunDto();
        run.setRunName("Example 13 - Custom report subset");
        run.add(suite);

        Path directory = Paths.get("examples-output/13-custom-reports");
        ReportService htmlAndXmlOnly = new ReportService(java.util.List.of(
                new org.ai.testing.report.generator.HtmlReportGenerator(
                        directory.resolve("test-report.html")),
                new org.ai.testing.report.generator.JUnitXmlReportGenerator(
                        directory.resolve("junit-report.xml"))));

        TestRunResultDto result = new TestRunExecutor(htmlAndXmlOnly).execute(run);
        printSummary(result);
        System.out.println("  formats written: " + htmlAndXmlOnly.formats());
    }

    // ==================================================================
    // 14. Inspecting results programmatically
    // ==================================================================

    /**
     * Everything the HTML report shows is also available on
     * {@code TestRunResultDto} directly — useful for a custom dashboard, a
     * Slack notifier, or a build script that wants to fail only on a specific
     * condition (e.g. "fail only if pass rate drops below 90%").
     */
    private static void example14_inspectingResults() {
        TestSuiteDto suite = new TestSuiteDto("SUITE-14", "Inspection");

        TestCaseDto passing = new TestCaseDto("TC-14A", "Passing case", "GET");
        BaseRequestDto passingRequest = new BaseRequestDto();
        passingRequest.setUrl(BASE_URL + "/openapi.json");
        passing.setRequest(passingRequest);
        passing.setExpectedStatusCode(200);
        suite.add(passing);

        TestCaseDto failing = new TestCaseDto("TC-14B", "Intentionally failing case", "GET");
        BaseRequestDto failingRequest = new BaseRequestDto();
        failingRequest.setUrl(BASE_URL + "/store/inventory");
        failing.setRequest(failingRequest);
        // Wrong on purpose, so example 14 has something in "failures()" to show.
        failing.setExpectedStatusCode(999);
        suite.add(failing);

        TestRunDto run = new TestRunDto();
        run.setRunName("Example 14 - Inspecting results");
        run.add(suite);

        TestRunResultDto result = execute(run, "examples-output/14-inspection");

        System.out.println("  status:        " + result.getStatus());
        System.out.println("  pass rate:     "
                + String.format("%.1f%%", result.getMetrics().getPassRate()));
        System.out.println("  median latency:" + result.getMetrics().getMedianResponseTimeMs() + " ms");
        System.out.println("  p95 latency:   " + result.getMetrics().getP95ResponseTimeMs() + " ms");
        System.out.println("  failures:");
        for (var failure : result.failures()) {
            System.out.println("    - " + failure.getTestCaseName() + ": " + failure.getMessage());
        }
        System.out.println("  slowest tests:");
        for (var slow : result.slowestTests(3)) {
            System.out.println("    - " + slow.getTestCaseName() + ": "
                    + slow.responseTimeMs() + " ms");
        }
        if (!result.getWarnings().isEmpty()) {
            System.out.println("  warnings:");
            result.getWarnings().forEach(warning -> System.out.println("    - " + warning));
        }
    }

    // ==================================================================
    // Shared helpers
    // ==================================================================

    /** Runs a plan and writes its reports to {@code outputDirectory}/reports semantics. */
    private static TestRunResultDto execute(TestRunDto run, String outputDirectory) {
        ReportService reportService = new ReportService(Paths.get(outputDirectory));
        return new TestRunExecutor(reportService).execute(run);
    }

    private static void printSummary(TestRunResultDto result) {
        System.out.println("  " + result.getMessage());
    }
}