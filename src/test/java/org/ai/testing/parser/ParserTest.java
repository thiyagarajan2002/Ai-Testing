package org.ai.testing.parser;

import org.ai.testing.dto.common.AuthType;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Collection importers")
class ParserTest {

    private static final String POSTMAN = """
            {
              "info": { "name": "Demo API" },
              "variable": [
                { "key": "baseUrl", "value": "https://api.test" },
                { "key": "unused", "value": "x", "disabled": true }
              ],
              "auth": { "type": "bearer", "bearer": [ { "key": "token", "value": "t0k" } ] },
              "item": [
                {
                  "name": "Folder One",
                  "item": [
                    {
                      "name": "Get thing",
                      "request": {
                        "method": "GET",
                        "header": [
                          { "key": "Accept", "value": "application/json" },
                          { "key": "X-Off", "value": "no", "disabled": true }
                        ],
                        "url": {
                          "raw": "{{baseUrl}}/things?page=1",
                          "query": [ { "key": "page", "value": "1" } ]
                        }
                      },
                      "event": [
                        {
                          "listen": "test",
                          "script": { "exec": [
                            "pm.response.to.have.status(201);",
                            "pm.response.to.have.header(\\"X-Trace\\");",
                            "pm.expect(pm.response.responseTime).to.be.below(800);",
                            "pm.expect(pm.response.text()).to.include(\\"ok\\");",
                            "var jsonData = pm.response.json();",
                            "pm.environment.set(\\"thingId\\", jsonData.data.id);"
                          ] }
                        }
                      ]
                    },
                    {
                      "name": "Create thing",
                      "request": {
                        "method": "POST",
                        "body": { "mode": "raw", "raw": "{\\"a\\":1}",
                                  "options": { "raw": { "language": "json" } } },
                        "url": { "raw": "{{baseUrl}}/things" }
                      }
                    }
                  ]
                }
              ]
            }
            """;

    private static final String BRU = """
            meta {
              name: Fetch thing
              seq: 3
              tags: smoke regression
            }

            get {
              url: {{baseUrl}}/things/{id}
            }

            headers {
              Accept: application/json
              ~X-Off: never
            }

            params:query {
              page: 1
            }

            params:path {
              id: 42
            }

            auth:bearer {
              token: {{authToken}}
            }

            body:json {
              {
                "nested": { "value": 1 }
              }
            }

            assert {
              res.status: eq 201
              res.headers.content-type: contains json
              res.body.data.id: isdefined
              res.responseTime: lt 900
              ~res.body.skipped: eq nope
            }

            vars:post-response {
              thingId: res.body.data.id
              trace: res.headers.x-trace
            }
            """;

    // ------------------------------------------------------------------

    @Test
    @DisplayName("Postman: reads folders, variables and collection auth")
    void postmanReadsStructure(@TempDir Path directory) throws IOException {
        TestRunDto run = load(directory, POSTMAN);

        assertEquals("Demo API", run.getRunName());
        assertEquals("https://api.test", run.getCollectionVariables().get("baseUrl"));
        assertFalse(run.getCollectionVariables().containsKey("unused"),
                "a disabled variable should not be imported");
        assertEquals(AuthType.BEARER, run.getAuth().getType());
        assertEquals("t0k", run.getAuth().getToken());
        assertEquals(1, run.getTestSuites().size());
        assertEquals("Folder One", run.getTestSuites().get(0).getSuiteName());
        assertEquals(2, run.getTestSuites().get(0).getTestCases().size());
    }

    @Test
    @DisplayName("Postman: strips the query string from the raw URL and keeps parameters")
    void postmanSeparatesQuery(@TempDir Path directory) throws IOException {
        TestCaseDto testCase = firstCase(load(directory, POSTMAN));
        assertEquals("{{baseUrl}}/things", testCase.getRequest().getUrl());
        assertEquals(1, testCase.getRequest().getQueryParamItems().size());
        assertEquals("page", testCase.getRequest().getQueryParamItems().get(0).getName());
    }

    @Test
    @DisplayName("Postman: keeps a disabled header but marks it disabled")
    void postmanMarksDisabledHeaders(@TempDir Path directory) throws IOException {
        TestCaseDto testCase = firstCase(load(directory, POSTMAN));
        assertEquals(2, testCase.getRequest().getHeaderItems().size());
        assertFalse(testCase.getRequest().getHeaderItems().get(1).isEnabled());
    }

    @Test
    @DisplayName("Postman: translates pm.* script idioms into native assertions")
    void postmanTranslatesScripts(@TempDir Path directory) throws IOException {
        TestCaseDto testCase = firstCase(load(directory, POSTMAN));

        assertEquals(201, testCase.getExpectedStatusCode());
        assertTrue(hasAssertion(testCase, AssertionType.HEADER, AssertionOperator.EXISTS));
        assertTrue(hasAssertion(testCase, AssertionType.RESPONSE_TIME,
                AssertionOperator.LESS_THAN));
        assertTrue(hasAssertion(testCase, AssertionType.RESPONSE_BODY,
                AssertionOperator.CONTAINS));
        assertEquals(1, testCase.getExtracts().size());
        assertEquals("thingId", testCase.getExtracts().get(0).getVariableName());
        assertEquals("$.data.id", testCase.getExtracts().get(0).getExpression());
    }

    @Test
    @DisplayName("Postman: recognises a JSON raw body")
    void postmanReadsJsonBody(@TempDir Path directory) throws IOException {
        TestCaseDto create = load(directory, POSTMAN)
                .getTestSuites().get(0).getTestCases().get(1);
        assertEquals("POST", create.getMethod());
        assertEquals("application/json", create.getRequest().getBody().getContentType());
        assertEquals("{\"a\":1}", create.getRequest().getBody().getRawBody());
    }

    @Test
    @DisplayName("Bruno: reads meta, method, params, body and auth")
    void brunoReadsStructure(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("fetch.bru");
        Files.writeString(file, BRU);
        TestCaseDto testCase = firstCase(new BrunoParser().parse(file));

        assertEquals("Fetch thing", testCase.getTestCaseName());
        assertEquals("GET", testCase.getMethod());
        assertEquals("{{baseUrl}}/things/{id}", testCase.getRequest().getUrl());
        assertEquals("42", testCase.getRequest().getPathParamItems().get(0).getValue());
        assertEquals("1", testCase.getRequest().getQueryParamItems().get(0).getValue());
        assertEquals(AuthType.BEARER, testCase.getAuth().getType());
        assertTrue(testCase.getRequest().getBody().getRawBody().contains("nested"));
        assertTrue(testCase.getTags().contains("smoke"));
        assertTrue(testCase.getTags().contains("regression"));
    }

    @Test
    @DisplayName("Bruno: maps the assert block, honouring the disabled prefix")
    void brunoReadsAssertions(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("fetch.bru");
        Files.writeString(file, BRU);
        TestCaseDto testCase = firstCase(new BrunoParser().parse(file));

        assertEquals(201, testCase.getExpectedStatusCode(),
                "res.status eq should become the expected status code");
        assertTrue(hasAssertion(testCase, AssertionType.HEADER, AssertionOperator.CONTAINS));
        assertTrue(hasAssertion(testCase, AssertionType.JSON_PATH, AssertionOperator.EXISTS));
        assertTrue(hasAssertion(testCase, AssertionType.RESPONSE_TIME,
                AssertionOperator.LESS_THAN));

        long disabled = testCase.getAssertions().stream()
                .filter(assertion -> !assertion.isEnabled()).count();
        assertEquals(1, disabled, "the ~ prefixed assertion should be disabled");
    }

    @Test
    @DisplayName("Bruno: header prefixed with ~ is imported but disabled")
    void brunoMarksDisabledHeaders(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("fetch.bru");
        Files.writeString(file, BRU);
        TestCaseDto testCase = firstCase(new BrunoParser().parse(file));

        assertEquals(2, testCase.getRequest().getHeaderItems().size());
        assertEquals("X-Off", testCase.getRequest().getHeaderItems().get(1).getName());
        assertFalse(testCase.getRequest().getHeaderItems().get(1).isEnabled());
    }

    @Test
    @DisplayName("Bruno: a directory becomes one suite per folder")
    void brunoReadsDirectory(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("collection.bru"),
                "meta {\n  name: Dir Collection\n}\n\nvars {\n  baseUrl: https://api.test\n}\n");
        Path folder = Files.createDirectories(directory.resolve("Things"));
        Files.writeString(folder.resolve("folder.bru"), "meta {\n  name: Things\n}\n");
        Files.writeString(folder.resolve("01-get.bru"), BRU);

        TestRunDto run = new BrunoParser().parse(directory);
        assertEquals("Dir Collection", run.getRunName());
        assertEquals("https://api.test", run.getCollectionVariables().get("baseUrl"));
        assertEquals(1, run.getTestSuites().size());
        assertEquals("Things", run.getTestSuites().get(0).getSuiteName());
    }

    @Test
    @DisplayName("Loader picks the parser from the file shape and layers an environment")
    void loaderSelectsParser(@TempDir Path directory) throws IOException {
        Path collection = directory.resolve("c.json");
        Files.writeString(collection, POSTMAN);
        Path environment = directory.resolve("env.json");
        Files.writeString(environment, """
                { "name": "staging", "values": [
                  { "key": "baseUrl", "value": "https://staging.test", "enabled": true },
                  { "key": "skipMe", "value": "x", "enabled": false } ] }
                """);

        TestRunDto run = new CollectionLoader().load(collection, environment);
        assertEquals("staging", run.getEnvironment());
        assertEquals("https://staging.test", run.getEnvironmentVariables().get("baseUrl"));
        assertFalse(run.getEnvironmentVariables().containsKey("skipMe"));
    }

    @Test
    @DisplayName("Loader reports a missing file clearly")
    void loaderReportsMissingFiles(@TempDir Path directory) {
        CollectionLoader loader = new CollectionLoader();
        assertThrows(CollectionParseException.class,
                () -> loader.load(directory.resolve("nope.json"), null));
        assertThrows(IllegalArgumentException.class, () -> loader.load(null, null));
    }

    @Test
    @DisplayName("A malformed collection raises a parse error, not a crash")
    void malformedCollectionIsReported(@TempDir Path directory) throws IOException {
        Path collection = directory.resolve("bad.json");
        Files.writeString(collection, "{ not json at all ");
        assertThrows(RuntimeException.class,
                () -> new CollectionLoader().load(collection, null));
    }

    // ------------------------------------------------------------------

    private TestRunDto load(Path directory, String json) throws IOException {
        Path file = directory.resolve("collection.json");
        Files.writeString(file, json);
        return new PostmanParser().parse(file);
    }

    private TestCaseDto firstCase(TestRunDto run) {
        TestSuiteDto suite = run.getTestSuites().get(0);
        assertNotNull(suite);
        return suite.getTestCases().get(0);
    }

    private boolean hasAssertion(TestCaseDto testCase, AssertionType type,
                                 AssertionOperator operator) {
        return testCase.getAssertions().stream().anyMatch(assertion ->
                assertion.getType() == type && assertion.getOperator() == operator);
    }
}
