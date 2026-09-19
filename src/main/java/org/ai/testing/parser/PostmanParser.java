package org.ai.testing.parser;

import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.AuthType;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.BodyMode;
import org.ai.testing.dto.common.ExtractDto;
import org.ai.testing.dto.common.HeaderDto;
import org.ai.testing.dto.common.PathParamDto;
import org.ai.testing.dto.common.QueryParamDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.json.Json;
import org.ai.testing.json.JsonValue;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.util.Strings;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports Postman Collection v2.0 and v2.1 JSON.
 *
 * <p>Postman test scripts are JavaScript, which this framework does not
 * execute. Instead the common {@code pm.*} idioms are recognised by pattern and
 * translated into native assertions and value captures. Anything unrecognised
 * is ignored rather than failing the import, so a collection always loads.</p>
 */
public class PostmanParser {

    private static final Pattern STATUS_TEST =
            Pattern.compile("pm\\.response\\.to\\.have\\.status\\((\\d+)\\)");
    private static final Pattern EXPECT_CODE =
            Pattern.compile("pm\\.expect\\(pm\\.response\\.code\\)\\.to\\.(?:eql|equal)\\((\\d+)\\)");
    private static final Pattern HEADER_TEST =
            Pattern.compile("pm\\.response\\.to\\.have\\.header\\([\"']([^\"']+)[\"']\\)");
    private static final Pattern RESPONSE_TIME =
            Pattern.compile("pm\\.expect\\(pm\\.response\\.responseTime\\)"
                    + "\\.to\\.be\\.(?:below|lessThan)\\((\\d+)\\)");
    private static final Pattern BODY_CONTAINS =
            Pattern.compile("pm\\.expect\\(pm\\.response\\.text\\(\\)\\)"
                    + "\\.to\\.include\\([\"']([^\"']*)[\"']\\)");
    private static final Pattern JSON_EQUALS =
            Pattern.compile("pm\\.expect\\(jsonData\\.([A-Za-z0-9_.\\[\\]]+)\\)"
                    + "\\.to\\.(?:eql|equal)\\([\"']?([^\"')]*)[\"']?\\)");
    private static final Pattern VAR_SET_DOT =
            Pattern.compile("pm\\.(?:environment|collectionVariables|globals)\\.set\\("
                    + "[\"']([^\"']+)[\"']\\s*,\\s*"
                    + "(?:jsonData|pm\\.response\\.json\\(\\))\\.([A-Za-z0-9_.]+)\\)");
    private static final Pattern VAR_SET_BRACKET =
            Pattern.compile("pm\\.(?:environment|collectionVariables|globals)\\.set\\("
                    + "[\"']([^\"']+)[\"']\\s*,\\s*"
                    + "(?:jsonData|pm\\.response\\.json\\(\\))\\[[\"']([^\"']+)[\"']\\]\\)");

    public TestRunDto parse(Path collectionPath) {
        if (collectionPath == null) {
            throw new IllegalArgumentException("Collection path cannot be null");
        }
        try {
            return parse(Json.read(collectionPath));
        } catch (IOException e) {
            throw new CollectionParseException(
                    "Unable to read Postman collection: " + collectionPath, e);
        }
    }

    public TestRunDto parse(JsonValue root) {
        if (root == null || root.isMissing() || !root.isObject()) {
            throw new CollectionParseException("Postman collection JSON cannot be empty");
        }

        String name = text(root.path("info"), "name", "Postman Collection");

        TestRunDto run = new TestRunDto();
        run.setRunId("POSTMAN-" + Strings.slug(name));
        run.setRunName(name);
        run.setDescription(description(root.path("info").path("description")));
        run.setCollectionVariables(readVariables(root.path("variable")));
        run.setAuth(readAuth(root.path("auth")));

        JsonValue items = root.path("item");
        if (!items.isArray() || items.size() == 0) {
            return run;
        }

        boolean hasFolders = items.items().stream().anyMatch(item -> item.has("item"));

        if (!hasFolders) {
            TestSuiteDto suite = new TestSuiteDto("SUITE-" + Strings.slug(name), name);
            suite.setAuth(run.getAuth());
            for (JsonValue item : items.items()) {
                TestCaseDto testCase = readRequestItem(item, name);
                if (testCase != null) {
                    suite.add(testCase);
                }
            }
            run.add(suite);
            return run;
        }

        int index = 1;
        List<TestCaseDto> rootRequests = new ArrayList<>();
        for (JsonValue item : items.items()) {
            if (item.has("item")) {
                run.add(readFolder(item, index++));
            } else {
                TestCaseDto testCase = readRequestItem(item, name);
                if (testCase != null) {
                    rootRequests.add(testCase);
                }
            }
        }
        if (!rootRequests.isEmpty()) {
            TestSuiteDto suite = new TestSuiteDto("SUITE-ROOT", name + " (root)");
            suite.setTestCases(rootRequests);
            run.getTestSuites().add(0, suite);
        }
        return run;
    }

    // ------------------------------------------------------------------

    private TestSuiteDto readFolder(JsonValue folder, int index) {
        String folderName = text(folder, "name", "Folder " + index);
        TestSuiteDto suite = new TestSuiteDto(
                "SUITE-" + index + "-" + Strings.slug(folderName), folderName);
        suite.setDescription(description(folder.path("description")));
        suite.setAuth(readAuth(folder.path("auth")));
        collectRequests(folder.path("item"), suite.getTestCases(), folderName);
        return suite;
    }

    private void collectRequests(JsonValue items, List<TestCaseDto> target, String folderName) {
        if (!items.isArray()) {
            return;
        }
        for (JsonValue item : items.items()) {
            if (item.has("item")) {
                // Nested folders are flattened into the parent suite so the
                // report hierarchy stays two levels deep and readable.
                collectRequests(item.path("item"), target, folderName);
            } else {
                TestCaseDto testCase = readRequestItem(item, folderName);
                if (testCase != null) {
                    target.add(testCase);
                }
            }
        }
    }

    private TestCaseDto readRequestItem(JsonValue item, String folderName) {
        JsonValue requestNode = item.path("request");
        if (requestNode.isMissing() || requestNode.isNull()) {
            return null;
        }

        String requestName = text(item, "name", "Request");
        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("TC-" + Strings.slug(folderName)
                + "-" + Strings.slug(requestName));
        testCase.setTestCaseName(requestName);
        testCase.setEnabled(!item.path("disabled").asBoolean(false));

        if (requestNode.isString()) {
            testCase.setMethod("GET");
            BaseRequestDto request = new BaseRequestDto();
            request.setUrl(requestNode.asText());
            testCase.setRequest(request);
        } else {
            testCase.setMethod(text(requestNode, "method", "GET"));
            testCase.setDescription(description(requestNode.path("description")));
            testCase.setRequest(readRequest(requestNode));
            testCase.setAuth(readAuth(requestNode.path("auth")));
        }

        parseEvents(item.path("event"), testCase);

        if (testCase.getExpectedStatusCode() == null && testCase.getAssertions().isEmpty()) {
            // Without any scripted expectation, treat 2xx as the contract.
            testCase.setExpectedStatusCode(200);
        }
        return testCase;
    }

    private BaseRequestDto readRequest(JsonValue requestNode) {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(readUrl(requestNode.path("url")));
        request.setHeaderItems(readHeaders(requestNode.path("header")));
        request.setQueryParamItems(readQuery(requestNode.path("url").path("query")));
        request.setPathParamItems(readPathVariables(requestNode.path("url").path("variable")));
        request.setBody(readBody(requestNode.path("body")));
        request.setAuth(readAuth(requestNode.path("auth")));
        return request;
    }

    private String readUrl(JsonValue urlNode) {
        if (urlNode.isMissing() || urlNode.isNull()) {
            return "";
        }
        if (urlNode.isString()) {
            return urlNode.asText();
        }
        String raw = text(urlNode, "raw", "");
        if (Strings.hasText(raw)) {
            // Strip the query string; parameters are carried separately and
            // re-encoded, so keeping both would duplicate them.
            int question = raw.indexOf('?');
            return question < 0 ? raw : raw.substring(0, question);
        }
        return rebuildUrl(urlNode);
    }

    /** Reassembles a URL from Postman's exploded host/path form. */
    private String rebuildUrl(JsonValue urlNode) {
        StringBuilder url = new StringBuilder();
        String protocol = text(urlNode, "protocol", "https");
        url.append(protocol).append("://");

        JsonValue host = urlNode.path("host");
        if (host.isArray()) {
            url.append(String.join(".", host.items().stream().map(JsonValue::asText).toList()));
        } else if (host.isString()) {
            url.append(host.asText());
        }

        String port = text(urlNode, "port", "");
        if (Strings.hasText(port)) {
            url.append(':').append(port);
        }

        JsonValue path = urlNode.path("path");
        if (path.isArray()) {
            for (JsonValue segment : path.items()) {
                url.append('/').append(segment.asText());
            }
        } else if (path.isString()) {
            String value = path.asText();
            url.append(value.startsWith("/") ? value : "/" + value);
        }
        return url.toString();
    }

    private List<HeaderDto> readHeaders(JsonValue headers) {
        List<HeaderDto> items = new ArrayList<>();
        if (!headers.isArray()) {
            return items;
        }
        for (JsonValue header : headers.items()) {
            HeaderDto dto = new HeaderDto(text(header, "key", ""), text(header, "value", ""));
            dto.setEnabled(!header.path("disabled").asBoolean(false));
            items.add(dto);
        }
        return items;
    }

    private List<QueryParamDto> readQuery(JsonValue query) {
        List<QueryParamDto> items = new ArrayList<>();
        if (!query.isArray()) {
            return items;
        }
        for (JsonValue param : query.items()) {
            QueryParamDto dto = new QueryParamDto(text(param, "key", ""), text(param, "value", ""));
            dto.setEnabled(!param.path("disabled").asBoolean(false));
            items.add(dto);
        }
        return items;
    }

    private List<PathParamDto> readPathVariables(JsonValue variables) {
        List<PathParamDto> items = new ArrayList<>();
        if (!variables.isArray()) {
            return items;
        }
        for (JsonValue variable : variables.items()) {
            items.add(new PathParamDto(text(variable, "key", ""), text(variable, "value", "")));
        }
        return items;
    }

    private RequestBodyDto readBody(JsonValue bodyNode) {
        if (bodyNode.isMissing() || bodyNode.isNull() || !bodyNode.isObject()) {
            return null;
        }
        RequestBodyDto body = new RequestBodyDto();
        switch (text(bodyNode, "mode", "raw")) {
            case "urlencoded" -> {
                body.setMode(BodyMode.URLENCODED);
                body.setFormFields(readForm(bodyNode.path("urlencoded")));
                body.setContentType("application/x-www-form-urlencoded");
            }
            case "formdata" -> {
                body.setMode(BodyMode.FORMDATA);
                body.setFormFields(readForm(bodyNode.path("formdata")));
            }
            case "graphql" -> {
                body.setMode(BodyMode.GRAPHQL);
                JsonValue graphql = bodyNode.path("graphql");
                String variables = text(graphql, "variables", "");
                body.setRawBody("{\"query\":" + Json.quote(text(graphql, "query", ""))
                        + ",\"variables\":" + (Strings.isBlank(variables) ? "{}" : variables) + "}");
                body.setContentType("application/json");
            }
            case "file" -> {
                // File uploads are not supported; keep the case importable.
                body.setMode(BodyMode.NONE);
                body.setRawBody("");
            }
            default -> {
                body.setMode(BodyMode.RAW);
                body.setRawBody(text(bodyNode, "raw", ""));
                String language = text(bodyNode.path("options").path("raw"), "language", "");
                if ("json".equalsIgnoreCase(language)) {
                    body.setMode(BodyMode.JSON);
                    body.setContentType("application/json");
                } else if ("xml".equalsIgnoreCase(language)) {
                    body.setMode(BodyMode.XML);
                    body.setContentType("application/xml");
                }
            }
        }
        return body;
    }

    private List<QueryParamDto> readForm(JsonValue form) {
        List<QueryParamDto> fields = new ArrayList<>();
        if (!form.isArray()) {
            return fields;
        }
        for (JsonValue field : form.items()) {
            if ("file".equalsIgnoreCase(text(field, "type", ""))) {
                continue;
            }
            QueryParamDto dto = new QueryParamDto(text(field, "key", ""), text(field, "value", ""));
            dto.setEnabled(!field.path("disabled").asBoolean(false));
            fields.add(dto);
        }
        return fields;
    }

    private AuthDto readAuth(JsonValue authNode) {
        if (authNode.isMissing() || authNode.isNull() || !authNode.isObject()) {
            return null;
        }
        AuthDto auth = new AuthDto();
        switch (text(authNode, "type", "noauth").toLowerCase(Locale.ROOT)) {
            case "bearer" -> {
                auth.setType(AuthType.BEARER);
                auth.setToken(authValue(authNode.path("bearer"), "token"));
            }
            case "basic" -> {
                auth.setType(AuthType.BASIC);
                auth.setUsername(authValue(authNode.path("basic"), "username"));
                auth.setPassword(authValue(authNode.path("basic"), "password"));
            }
            case "apikey" -> {
                auth.setType(AuthType.API_KEY);
                auth.setApiKeyName(authValue(authNode.path("apikey"), "key"));
                auth.setApiKeyValue(authValue(authNode.path("apikey"), "value"));
                auth.setApiKeyIn("query".equalsIgnoreCase(
                        authValue(authNode.path("apikey"), "in")) ? "QUERY" : "HEADER");
            }
            case "noauth" -> auth.setType(AuthType.NONE);
            default -> auth.setType(AuthType.INHERIT);
        }
        return auth;
    }

    private String authValue(JsonValue node, String key) {
        if (node.isArray()) {
            for (JsonValue entry : node.items()) {
                if (key.equals(text(entry, "key", ""))) {
                    return text(entry, "value", "");
                }
            }
        }
        if (node.isObject()) {
            return text(node, key, "");
        }
        return "";
    }

    private Map<String, String> readVariables(JsonValue variables) {
        Map<String, String> values = new LinkedHashMap<>();
        if (!variables.isArray()) {
            return values;
        }
        for (JsonValue variable : variables.items()) {
            if (variable.path("disabled").asBoolean(false)) {
                continue;
            }
            String key = text(variable, "key", "");
            if (Strings.hasText(key)) {
                values.put(key, text(variable, "value", ""));
            }
        }
        return values;
    }

    // ------------------------------------------------------------------
    // Script translation
    // ------------------------------------------------------------------

    private void parseEvents(JsonValue events, TestCaseDto testCase) {
        if (!events.isArray()) {
            return;
        }
        for (JsonValue event : events.items()) {
            String script = joinScript(event.path("script").path("exec"));
            if (Strings.isBlank(script)) {
                continue;
            }
            if ("test".equals(text(event, "listen", ""))) {
                parseTests(script, testCase);
                parseCaptures(script, testCase);
            }
        }
    }

    private void parseTests(String script, TestCaseDto testCase) {
        Matcher status = STATUS_TEST.matcher(script);
        while (status.find()) {
            testCase.setExpectedStatusCode(Integer.parseInt(status.group(1)));
        }
        Matcher expectCode = EXPECT_CODE.matcher(script);
        while (expectCode.find()) {
            testCase.setExpectedStatusCode(Integer.parseInt(expectCode.group(1)));
        }
        Matcher header = HEADER_TEST.matcher(script);
        while (header.find()) {
            testCase.assertion(AssertionDto.header(header.group(1),
                            AssertionOperator.EXISTS, "")
                    .describedAs("Imported from a Postman test script"));
        }
        Matcher time = RESPONSE_TIME.matcher(script);
        while (time.find()) {
            testCase.assertion(AssertionDto.responseTimeBelow(Long.parseLong(time.group(1))));
        }
        Matcher contains = BODY_CONTAINS.matcher(script);
        while (contains.find()) {
            testCase.assertion(AssertionDto.body(AssertionOperator.CONTAINS, contains.group(1)));
        }
        Matcher jsonEquals = JSON_EQUALS.matcher(script);
        while (jsonEquals.find()) {
            testCase.assertion(AssertionDto.jsonPath("$." + jsonEquals.group(1),
                    AssertionOperator.EQUALS, jsonEquals.group(2).trim()));
        }
        if (script.contains("pm.response.to.be.ok")) {
            testCase.setExpectedStatusCode(200);
        }
    }

    private void parseCaptures(String script, TestCaseDto testCase) {
        Matcher dotted = VAR_SET_DOT.matcher(script);
        while (dotted.find()) {
            testCase.extract(ExtractDto.fromBody(dotted.group(1), "$." + dotted.group(2)));
        }
        Matcher bracketed = VAR_SET_BRACKET.matcher(script);
        while (bracketed.find()) {
            testCase.extract(ExtractDto.fromBody(bracketed.group(1), "$." + bracketed.group(2)));
        }
    }

    private String joinScript(JsonValue exec) {
        if (exec.isString()) {
            return exec.asText();
        }
        if (!exec.isArray()) {
            return "";
        }
        StringBuilder script = new StringBuilder();
        for (JsonValue line : exec.items()) {
            script.append(line.asText()).append('\n');
        }
        return script.toString();
    }

    // ------------------------------------------------------------------

    private String description(JsonValue node) {
        if (node.isString()) {
            return node.asText();
        }
        return text(node, "content", "");
    }

    private String text(JsonValue node, String field, String fallback) {
        JsonValue value = node.path(field);
        if (value.isMissing() || value.isNull()) {
            return fallback;
        }
        return value.asText(fallback);
    }
}
