package org.ai.testing.parser;

import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.AuthType;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.BodyMode;
import org.ai.testing.dto.common.ExtractDto;
import org.ai.testing.dto.common.HeaderDto;
import org.ai.testing.dto.common.QueryParamDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.env.EnvironmentDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Imports Bruno {@code .bru} requests, folders, and env files.
 */
public class BrunoParser {

    public TestRunDto parse(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Bruno path cannot be null");
        }
        try {
            if (Files.isDirectory(path)) {
                return parseDirectory(path);
            }
            return parseRequestFile(path, path.getFileName().toString(), 1);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read Bruno collection: " + path, e);
        }
    }

    public EnvironmentDto parseEnvironment(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Environment path cannot be null");
        }
        try {
            Map<String, String> blocks = BruBlockReader.read(Files.readString(path));
            EnvironmentDto environment = new EnvironmentDto();
            environment.setName(textValue(blocks.getOrDefault("meta", ""), "name",
                    path.getFileName().toString().replace(".bru", "")));
            environment.getValues().putAll(parseMap(firstPresent(blocks,
                    "vars", "vars:env", "vars:secret")));
            return environment;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read Bruno environment: " + path, e);
        }
    }

    private TestRunDto parseDirectory(Path directory) throws IOException {
        TestRunDto run = new TestRunDto();
        run.setRunId("BRUNO-" + sanitize(directory.getFileName().toString()));
        run.setRunName(directory.getFileName().toString());
        run.setExecutionMode("SEQUENTIAL");

        Path collectionFile = directory.resolve("collection.bru");
        if (Files.isRegularFile(collectionFile)) {
            Map<String, String> blocks = BruBlockReader.read(Files.readString(collectionFile));
            String name = textValue(blocks.getOrDefault("meta", ""), "name", run.getRunName());
            run.setRunName(name);
            run.setAuth(parseAuth(blocks));
            run.setCollectionVariables(parseMap(firstPresent(blocks, "vars", "vars:pre-request")));
        }

        Map<String, TestSuiteDto> suites = new LinkedHashMap<>();
        List<Path> files;
        try (Stream<Path> stream = Files.walk(directory)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".bru"))
                    .filter(file -> !file.getFileName().toString().equals("collection.bru"))
                    .filter(file -> !file.getFileName().toString().equals("folder.bru"))
                    .sorted()
                    .toList();
        }

        int sequence = 1;
        for (Path file : files) {
            Path relative = directory.relativize(file);
            String suiteName = relative.getNameCount() > 1
                    ? relative.getName(0).toString()
                    : run.getRunName();
            TestSuiteDto suite = suites.computeIfAbsent(suiteName, name -> {
                TestSuiteDto created = new TestSuiteDto();
                created.setSuiteId("SUITE-" + sanitize(name));
                created.setSuiteName(name);
                Path folderBru = directory.resolve(name).resolve("folder.bru");
                if (Files.isRegularFile(folderBru)) {
                    try {
                        created.setAuth(parseAuth(BruBlockReader.read(Files.readString(folderBru))));
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
                return created;
            });
            TestRunDto single = parseRequestFile(file, suiteName, sequence++);
            if (!single.getTestSuites().isEmpty()) {
                suite.getTestCases().addAll(single.getTestSuites().get(0).getTestCases());
            }
        }

        List<TestSuiteDto> ordered = new ArrayList<>(suites.values());
        ordered.sort(Comparator.comparing(TestSuiteDto::getSuiteName));
        run.setTestSuites(ordered);
        return run;
    }

    private TestRunDto parseRequestFile(Path file, String suiteName, int sequence) throws IOException {
        Map<String, String> blocks = BruBlockReader.read(Files.readString(file));
        String meta = blocks.getOrDefault("meta", "");
        String name = textValue(meta, "name", file.getFileName().toString().replace(".bru", ""));
        int seq = intValue(meta, "seq", sequence);

        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("TC-" + seq + "-" + sanitize(name));
        testCase.setTestCaseName(name);
        testCase.setEnabled(!"false".equalsIgnoreCase(textValue(meta, "enabled", "true")));
        testCase.setDescription(blocks.getOrDefault("docs", "").trim());
        testCase.setAuth(parseAuth(blocks));
        testCase.getPreRequestVariables().putAll(parseMap(blocks.get("vars:pre-request")));
        testCase.getExtracts().addAll(parseExtracts(blocks.get("vars:post-response")));

        HttpLine http = readHttp(blocks);
        testCase.setMethod(http.method);
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(http.url);
        request.setHeaderItems(parseHeaders(blocks.get("headers")));
        request.setQueryParamItems(parseQuery(blocks.get("params:query")));
        request.setBody(parseBody(blocks));
        request.setAuth(testCase.getAuth());
        testCase.setRequest(request);

        parseAssertBlock(blocks.get("assert"), testCase);
        if (testCase.getExpectedStatusCode() == null) {
            testCase.setExpectedStatusCode(200);
        }

        TestSuiteDto suite = new TestSuiteDto();
        suite.setSuiteId("SUITE-" + sanitize(suiteName));
        suite.setSuiteName(suiteName);
        suite.getTestCases().add(testCase);

        TestRunDto run = new TestRunDto();
        run.setRunId("BRUNO-" + sanitize(name));
        run.setRunName(name);
        run.setExecutionMode("SEQUENTIAL");
        run.getTestSuites().add(suite);
        return run;
    }

    private HttpLine readHttp(Map<String, String> blocks) {
        for (String method : List.of("get", "post", "put", "patch", "delete", "options", "head")) {
            if (blocks.containsKey(method)) {
                String url = textValue(blocks.get(method), "url", blocks.get(method).trim());
                if (url.contains("\n")) {
                    url = textValue(blocks.get(method), "url", "");
                }
                return new HttpLine(method.toUpperCase(Locale.ROOT), url.trim());
            }
        }
        return new HttpLine("GET", "");
    }

    private List<HeaderDto> parseHeaders(String block) {
        List<HeaderDto> headers = new ArrayList<>();
        parseMap(block).forEach((key, value) -> {
            HeaderDto header = new HeaderDto();
            header.setName(stripDisabled(key));
            header.setValue(value);
            header.setEnabled(!key.startsWith("~"));
            headers.add(header);
        });
        return headers;
    }

    private List<QueryParamDto> parseQuery(String block) {
        List<QueryParamDto> params = new ArrayList<>();
        parseMap(block).forEach((key, value) -> {
            QueryParamDto param = new QueryParamDto();
            param.setName(stripDisabled(key));
            param.setValue(value);
            param.setEnabled(!key.startsWith("~"));
            params.add(param);
        });
        return params;
    }

    private RequestBodyDto parseBody(Map<String, String> blocks) {
        if (blocks.containsKey("body:json")) {
            RequestBodyDto body = new RequestBodyDto();
            body.setMode(BodyMode.JSON);
            body.setContentType("application/json");
            body.setRawBody(blocks.get("body:json").trim());
            return body;
        }
        if (blocks.containsKey("body:text")) {
            RequestBodyDto body = new RequestBodyDto();
            body.setMode(BodyMode.TEXT);
            body.setRawBody(blocks.get("body:text").trim());
            return body;
        }
        if (blocks.containsKey("body:xml")) {
            RequestBodyDto body = new RequestBodyDto();
            body.setMode(BodyMode.XML);
            body.setContentType("application/xml");
            body.setRawBody(blocks.get("body:xml").trim());
            return body;
        }
        if (blocks.containsKey("body:graphql")) {
            RequestBodyDto body = new RequestBodyDto();
            body.setMode(BodyMode.GRAPHQL);
            body.setContentType("application/json");
            body.setRawBody(blocks.get("body:graphql").trim());
            return body;
        }
        if (blocks.containsKey("body:form-urlencoded")) {
            RequestBodyDto body = new RequestBodyDto();
            body.setMode(BodyMode.URLENCODED);
            body.setFormFields(parseQuery(blocks.get("body:form-urlencoded")));
            return body;
        }
        if (blocks.containsKey("body:multipart-form")) {
            RequestBodyDto body = new RequestBodyDto();
            body.setMode(BodyMode.FORMDATA);
            body.setFormFields(parseQuery(blocks.get("body:multipart-form")));
            return body;
        }
        return null;
    }

    private AuthDto parseAuth(Map<String, String> blocks) {
        if (blocks.containsKey("auth:bearer")) {
            AuthDto auth = new AuthDto();
            auth.setType(AuthType.BEARER);
            auth.setToken(textValue(blocks.get("auth:bearer"), "token",
                    blocks.get("auth:bearer").trim()));
            return auth;
        }
        if (blocks.containsKey("auth:basic")) {
            AuthDto auth = new AuthDto();
            auth.setType(AuthType.BASIC);
            auth.setUsername(textValue(blocks.get("auth:basic"), "username", ""));
            auth.setPassword(textValue(blocks.get("auth:basic"), "password", ""));
            return auth;
        }
        if (blocks.containsKey("auth:apikey")) {
            AuthDto auth = new AuthDto();
            auth.setType(AuthType.API_KEY);
            auth.setApiKeyName(textValue(blocks.get("auth:apikey"), "key", ""));
            auth.setApiKeyValue(textValue(blocks.get("auth:apikey"), "value", ""));
            String placement = textValue(blocks.get("auth:apikey"), "placement", "header");
            auth.setApiKeyIn(placement.toLowerCase(Locale.ROOT).contains("query") ? "QUERY" : "HEADER");
            return auth;
        }
        if (blocks.containsKey("auth:none")) {
            AuthDto auth = new AuthDto();
            auth.setType(AuthType.NONE);
            return auth;
        }
        return null;
    }

    private List<ExtractDto> parseExtracts(String block) {
        List<ExtractDto> extracts = new ArrayList<>();
        parseMap(block).forEach((name, expression) -> {
            ExtractDto extract = new ExtractDto();
            extract.setVariableName(name);
            extract.setExpression(expression);
            extract.setSource(expression != null && expression.startsWith("res.headers")
                    ? "HEADER" : "BODY");
            if (expression != null && expression.startsWith("res.headers.")) {
                extract.setExpression(expression.substring("res.headers.".length()));
            }
            extracts.add(extract);
        });
        return extracts;
    }

    private void parseAssertBlock(String block, TestCaseDto testCase) {
        if (block == null || block.isBlank()) {
            return;
        }
        for (String line : block.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains(":")) {
                continue;
            }
            String[] parts = trimmed.split(":", 2);
            if (parts.length < 2) {
                continue;
            }
            String left = parts[0].trim();
            String right = parts[1].trim();
            String operatorToken = right;
            String expected = "";
            int space = right.indexOf(' ');
            if (space > 0) {
                operatorToken = right.substring(0, space).trim();
                expected = unquote(right.substring(space + 1).trim());
            }
            AssertionOperator operator = mapOperator(operatorToken);
            if (left.equals("res.status") || left.equals("status")) {
                if (operator == AssertionOperator.EQUALS && expected.matches("\\d+")) {
                    testCase.setExpectedStatusCode(Integer.parseInt(expected));
                }
                addAssertion(testCase, AssertionType.STATUS_CODE, "statusCode", operator, expected);
            } else if (left.equals("res.responseTime") || left.equals("res.time")) {
                addAssertion(testCase, AssertionType.RESPONSE_TIME, "responseTimeMs", operator, expected);
            } else if (left.startsWith("res.headers.") || left.startsWith("res.header.")) {
                String headerName = left.substring(left.indexOf('.', 4) + 1);
                addAssertion(testCase, AssertionType.HEADER, headerName, operator, expected);
            } else if (left.equals("res.body")) {
                addAssertion(testCase, AssertionType.RESPONSE_BODY, "body", operator, expected);
            } else if (left.startsWith("res.body.")) {
                addAssertion(testCase, AssertionType.JSON_PATH, left, operator, expected);
            } else {
                addAssertion(testCase, AssertionType.JSON_PATH, left, operator, expected);
            }
        }
    }

    private AssertionOperator mapOperator(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "eq", "equal", "equals", "is" -> AssertionOperator.EQUALS;
            case "neq", "not", "isnot" -> AssertionOperator.NOT_EQUALS;
            case "contains" -> AssertionOperator.CONTAINS;
            case "notcontains" -> AssertionOperator.NOT_CONTAINS;
            case "isempty" -> AssertionOperator.EMPTY;
            case "isnotempty" -> AssertionOperator.NOT_EMPTY;
            case "isdefined", "exists" -> AssertionOperator.EXISTS;
            case "isundefined", "notexists" -> AssertionOperator.NOT_EXISTS;
            case "matches" -> AssertionOperator.MATCHES;
            case "lt", "lte", "lessthan" -> AssertionOperator.LESS_THAN;
            case "gt", "gte", "greaterthan" -> AssertionOperator.GREATER_THAN;
            default -> AssertionOperator.EQUALS;
        };
    }

    private void addAssertion(
            TestCaseDto testCase,
            AssertionType type,
            String field,
            AssertionOperator operator,
            String expected) {
        AssertionDto assertion = new AssertionDto();
        assertion.setType(type);
        assertion.setField(field);
        assertion.setOperator(operator);
        assertion.setExpectedValue(expected);
        testCase.getAssertions().add(assertion);
    }

    private Map<String, String> parseMap(String block) {
        Map<String, String> values = new LinkedHashMap<>();
        if (block == null || block.isBlank()) {
            return values;
        }
        for (String line : block.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains(":")) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            String key = trimmed.substring(0, colon).trim();
            String value = unquote(trimmed.substring(colon + 1).trim());
            values.put(key, value);
        }
        return values;
    }

    private String firstPresent(Map<String, String> blocks, String... keys) {
        for (String key : keys) {
            if (blocks.containsKey(key)) {
                return blocks.get(key);
            }
        }
        return "";
    }

    private String textValue(String block, String key, String fallback) {
        Map<String, String> values = parseMap(block);
        return values.getOrDefault(key, fallback);
    }

    private int intValue(String block, String key, int fallback) {
        try {
            return Integer.parseInt(textValue(block, key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String stripDisabled(String key) {
        return key.startsWith("~") ? key.substring(1) : key;
    }

    private String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String sanitize(String value) {
        return value == null ? "item" : value.replaceAll("[^A-Za-z0-9]+", "-");
    }

    private record HttpLine(String method, String url) {
    }
}
