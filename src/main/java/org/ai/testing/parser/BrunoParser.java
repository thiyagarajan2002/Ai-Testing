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
import org.ai.testing.util.Strings;
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

/** Imports Bruno {@code .bru} requests, folders and environment files. */
public class BrunoParser {

    private static final List<String> METHOD_BLOCKS =
            List.of("get", "post", "put", "patch", "delete", "options", "head");

    public TestRunDto parse(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Bruno path cannot be null");
        }
        try {
            if (Files.isDirectory(path)) {
                return parseDirectory(path);
            }
            return parseSingleFile(path);
        } catch (IOException e) {
            throw new CollectionParseException("Unable to read Bruno collection: " + path, e);
        }
    }

    public EnvironmentDto parseEnvironment(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Environment path cannot be null");
        }
        try {
            Map<String, String> blocks = BruBlockReader.read(Files.readString(path));
            EnvironmentDto environment = new EnvironmentDto();
            environment.setName(value(blocks.get("meta"), "name",
                    path.getFileName().toString().replace(".bru", "")));
            environment.getValues().putAll(parseMap(firstPresent(blocks,
                    "vars", "vars:env", "vars:secret")));
            return environment;
        } catch (IOException e) {
            throw new CollectionParseException("Unable to read Bruno environment: " + path, e);
        }
    }

    // ------------------------------------------------------------------

    private TestRunDto parseDirectory(Path directory) throws IOException {
        TestRunDto run = new TestRunDto();
        String directoryName = directory.getFileName() == null
                ? "bruno" : directory.getFileName().toString();
        run.setRunId("BRUNO-" + Strings.slug(directoryName));
        run.setRunName(directoryName);

        Path collectionFile = directory.resolve("collection.bru");
        if (Files.isRegularFile(collectionFile)) {
            Map<String, String> blocks = BruBlockReader.read(Files.readString(collectionFile));
            run.setRunName(value(blocks.get("meta"), "name", run.getRunName()));
            run.setAuth(parseAuth(blocks));
            run.setCollectionVariables(parseMap(firstPresent(blocks, "vars", "vars:pre-request")));
        }

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

        Map<String, TestSuiteDto> suites = new LinkedHashMap<>();
        int sequence = 1;

        for (Path file : files) {
            Path relative = directory.relativize(file);
            String suiteName = relative.getNameCount() > 1
                    ? relative.getName(0).toString()
                    : run.getRunName();

            TestSuiteDto suite = suites.get(suiteName);
            if (suite == null) {
                suite = new TestSuiteDto("SUITE-" + Strings.slug(suiteName), suiteName);
                suite.setAuth(readFolderAuth(directory.resolve(suiteName).resolve("folder.bru")));
                suites.put(suiteName, suite);
            }
            suite.add(parseTestCase(file, sequence++));
        }

        List<TestSuiteDto> ordered = new ArrayList<>(suites.values());
        ordered.sort(Comparator.comparing(suite ->
                Strings.nullToEmpty(suite.getSuiteName())));
        run.setTestSuites(ordered);
        return run;
    }

    private TestRunDto parseSingleFile(Path file) throws IOException {
        TestCaseDto testCase = parseTestCase(file, 1);

        TestSuiteDto suite = new TestSuiteDto(
                "SUITE-" + Strings.slug(testCase.getTestCaseName()),
                testCase.getTestCaseName());
        suite.add(testCase);

        TestRunDto run = new TestRunDto();
        run.setRunId("BRUNO-" + Strings.slug(testCase.getTestCaseName()));
        run.setRunName(testCase.getTestCaseName());
        run.add(suite);
        return run;
    }

    private AuthDto readFolderAuth(Path folderFile) {
        if (!Files.isRegularFile(folderFile)) {
            return null;
        }
        try {
            return parseAuth(BruBlockReader.read(Files.readString(folderFile)));
        } catch (IOException e) {
            // A folder-level file that cannot be read should not abort the
            // whole import; the folder simply inherits collection credentials.
            return null;
        }
    }

    private TestCaseDto parseTestCase(Path file, int sequence) throws IOException {
        Map<String, String> blocks = BruBlockReader.read(Files.readString(file));
        String meta = blocks.getOrDefault("meta", "");
        String name = value(meta, "name", file.getFileName().toString().replace(".bru", ""));
        int order = intValue(meta, "seq", sequence);

        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("TC-" + order + "-" + Strings.slug(name));
        testCase.setTestCaseName(name);
        testCase.setEnabled(!"false".equalsIgnoreCase(value(meta, "enabled", "true")));
        testCase.setDescription(blocks.getOrDefault("docs", "").trim());
        testCase.setAuth(parseAuth(blocks));
        testCase.getPreRequestVariables().putAll(parseMap(blocks.get("vars:pre-request")));
        testCase.getExtracts().addAll(parseExtracts(blocks.get("vars:post-response")));

        String tags = value(meta, "tags", "");
        if (Strings.hasText(tags)) {
            testCase.tag(tags.split("[,\\s]+"));
        }

        MethodAndUrl http = readMethodAndUrl(blocks);
        testCase.setMethod(http.method());

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(http.url());
        request.setHeaderItems(parseHeaders(blocks.get("headers")));
        request.setQueryParamItems(parseParams(blocks.get("params:query")));
        request.setPathParamItems(parsePathParams(blocks.get("params:path")));
        request.setBody(parseBody(blocks));
        request.setAuth(testCase.getAuth());
        testCase.setRequest(request);

        parseAssertions(blocks.get("assert"), testCase);

        if (testCase.getExpectedStatusCode() == null && testCase.getAssertions().isEmpty()) {
            testCase.setExpectedStatusCode(200);
        }
        return testCase;
    }

    private record MethodAndUrl(String method, String url) {
    }

    private MethodAndUrl readMethodAndUrl(Map<String, String> blocks) {
        for (String method : METHOD_BLOCKS) {
            if (blocks.containsKey(method)) {
                String url = value(blocks.get(method), "url", "").trim();
                return new MethodAndUrl(method.toUpperCase(Locale.ROOT), url);
            }
        }
        return new MethodAndUrl("GET", "");
    }

    private List<HeaderDto> parseHeaders(String block) {
        List<HeaderDto> headers = new ArrayList<>();
        parseMapKeepingDisabled(block).forEach((key, value) -> {
            HeaderDto header = new HeaderDto(stripDisabled(key), value);
            header.setEnabled(!key.startsWith("~"));
            headers.add(header);
        });
        return headers;
    }

    private List<QueryParamDto> parseParams(String block) {
        List<QueryParamDto> params = new ArrayList<>();
        parseMapKeepingDisabled(block).forEach((key, value) -> {
            QueryParamDto param = new QueryParamDto(stripDisabled(key), value);
            param.setEnabled(!key.startsWith("~"));
            params.add(param);
        });
        return params;
    }

    private List<org.ai.testing.dto.common.PathParamDto> parsePathParams(String block) {
        List<org.ai.testing.dto.common.PathParamDto> params = new ArrayList<>();
        parseMap(block).forEach((key, value) ->
                params.add(new org.ai.testing.dto.common.PathParamDto(key, value)));
        return params;
    }

    private RequestBodyDto parseBody(Map<String, String> blocks) {
        if (blocks.containsKey("body:json")) {
            RequestBodyDto body = RequestBodyDto.json(blocks.get("body:json").trim());
            return body;
        }
        if (blocks.containsKey("body:text")) {
            return RequestBodyDto.text(blocks.get("body:text").trim());
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
            return RequestBodyDto.form(parseParams(blocks.get("body:form-urlencoded")));
        }
        if (blocks.containsKey("body:multipart-form")) {
            RequestBodyDto body = new RequestBodyDto();
            body.setMode(BodyMode.FORMDATA);
            body.setFormFields(parseParams(blocks.get("body:multipart-form")));
            return body;
        }
        return null;
    }

    private AuthDto parseAuth(Map<String, String> blocks) {
        if (blocks.containsKey("auth:bearer")) {
            return AuthDto.bearer(value(blocks.get("auth:bearer"), "token", ""));
        }
        if (blocks.containsKey("auth:basic")) {
            return AuthDto.basic(value(blocks.get("auth:basic"), "username", ""),
                    value(blocks.get("auth:basic"), "password", ""));
        }
        if (blocks.containsKey("auth:apikey")) {
            String placement = value(blocks.get("auth:apikey"), "placement", "header");
            return AuthDto.apiKey(
                    value(blocks.get("auth:apikey"), "key", ""),
                    value(blocks.get("auth:apikey"), "value", ""),
                    placement.toLowerCase(Locale.ROOT).contains("query") ? "QUERY" : "HEADER");
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
            if (expression != null && expression.startsWith("res.headers.")) {
                extracts.add(ExtractDto.fromHeader(name,
                        expression.substring("res.headers.".length())));
            } else {
                extracts.add(ExtractDto.fromBody(name, expression));
            }
        });
        return extracts;
    }

    private void parseAssertions(String block, TestCaseDto testCase) {
        if (Strings.isBlank(block)) {
            return;
        }
        for (String line : block.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains(":")) {
                continue;
            }
            boolean disabled = trimmed.startsWith("~");
            if (disabled) {
                trimmed = trimmed.substring(1).trim();
            }

            String[] parts = trimmed.split(":", 2);
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
            AssertionDto assertion = buildAssertion(left, operator, expected, testCase);
            if (assertion != null) {
                assertion.setEnabled(!disabled);
                testCase.assertion(assertion);
            }
        }
    }

    private AssertionDto buildAssertion(String left, AssertionOperator operator,
                                        String expected, TestCaseDto testCase) {

        if (left.equals("res.status") || left.equals("status")) {
            if (operator == AssertionOperator.EQUALS && expected.matches("\\d+")) {
                testCase.setExpectedStatusCode(Integer.parseInt(expected));
                return null;
            }
            return new AssertionDto(AssertionType.STATUS_CODE, "statusCode", operator, expected);
        }
        if (left.equals("res.responseTime") || left.equals("res.time")) {
            return new AssertionDto(AssertionType.RESPONSE_TIME, "responseTimeMs",
                    operator, expected);
        }
        if (left.startsWith("res.headers.") || left.startsWith("res.header.")) {
            return AssertionDto.header(left.substring(left.indexOf('.', 4) + 1),
                    operator, expected);
        }
        if (left.equals("res.body")) {
            return AssertionDto.body(operator, expected);
        }
        return AssertionDto.jsonPath(left, operator, expected);
    }

    private AssertionOperator mapOperator(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "eq", "equal", "equals", "is" -> AssertionOperator.EQUALS;
            case "neq", "not", "isnot" -> AssertionOperator.NOT_EQUALS;
            case "contains" -> AssertionOperator.CONTAINS;
            case "notcontains" -> AssertionOperator.NOT_CONTAINS;
            case "startswith" -> AssertionOperator.STARTS_WITH;
            case "endswith" -> AssertionOperator.ENDS_WITH;
            case "isempty" -> AssertionOperator.EMPTY;
            case "isnotempty" -> AssertionOperator.NOT_EMPTY;
            case "isdefined", "exists" -> AssertionOperator.EXISTS;
            case "isundefined", "notexists" -> AssertionOperator.NOT_EXISTS;
            case "matches" -> AssertionOperator.MATCHES;
            case "lt", "lessthan" -> AssertionOperator.LESS_THAN;
            case "lte" -> AssertionOperator.LESS_THAN_OR_EQUAL;
            case "gt", "greaterthan" -> AssertionOperator.GREATER_THAN;
            case "gte" -> AssertionOperator.GREATER_THAN_OR_EQUAL;
            case "in" -> AssertionOperator.IN;
            case "notin" -> AssertionOperator.NOT_IN;
            default -> AssertionOperator.EQUALS;
        };
    }

    // ------------------------------------------------------------------

    private Map<String, String> parseMap(String block) {
        Map<String, String> values = new LinkedHashMap<>();
        parseMapKeepingDisabled(block).forEach((key, value) ->
                values.put(stripDisabled(key), value));
        return values;
    }

    private Map<String, String> parseMapKeepingDisabled(String block) {
        Map<String, String> values = new LinkedHashMap<>();
        if (Strings.isBlank(block)) {
            return values;
        }
        for (String line : block.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains(":")) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            values.put(trimmed.substring(0, colon).trim(),
                    unquote(trimmed.substring(colon + 1).trim()));
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

    private String value(String block, String key, String fallback) {
        String found = parseMap(block).get(key);
        return found == null ? fallback : found;
    }

    private int intValue(String block, String key, int fallback) {
        try {
            return Integer.parseInt(value(block, key, String.valueOf(fallback)));
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
}
