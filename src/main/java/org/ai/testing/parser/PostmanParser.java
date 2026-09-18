package org.ai.testing.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports Postman Collection v2.0 / v2.1 JSON into the native run model.
 */
public class PostmanParser {

    private static final Pattern STATUS_TEST =
            Pattern.compile("pm\\.response\\.to\\.have\\.status\\((\\d+)\\)");
    private static final Pattern EXPECT_CODE =
            Pattern.compile("pm\\.expect\\(pm\\.response\\.code\\)\\.to\\.(?:eql|equal)\\((\\d+)\\)");
    private static final Pattern HEADER_TEST =
            Pattern.compile("pm\\.response\\.to\\.have\\.header\\([\"']([^\"']+)[\"']\\)");
    private static final Pattern RESPONSE_TIME =
            Pattern.compile("pm\\.expect\\(pm\\.response\\.responseTime\\)\\.to\\.be\\.(?:below|lessThan)\\((\\d+)\\)");
    private static final Pattern ENV_SET_JSON =
            Pattern.compile("pm\\.environment\\.set\\([\"']([^\"']+)[\"']\\s*,\\s*pm\\.response\\.json\\(\\)\\.([A-Za-z0-9_]+)\\)");
    private static final Pattern ENV_SET_JSONPATH =
            Pattern.compile("pm\\.environment\\.set\\([\"']([^\"']+)[\"']\\s*,\\s*pm\\.response\\.json\\(\\)\\[\"']([^\"']+)[\"']\\]\\)");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestRunDto parse(Path collectionPath) {
        if (collectionPath == null) {
            throw new IllegalArgumentException("Collection path cannot be null");
        }
        try {
            JsonNode root = objectMapper.readTree(collectionPath.toFile());
            return parse(root);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Unable to read Postman collection: " + collectionPath, e);
        }
    }

    public TestRunDto parse(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            throw new IllegalArgumentException("Postman collection JSON cannot be empty");
        }

        JsonNode info = root.path("info");
        String name = text(info, "name", "Postman Collection");

        TestRunDto run = new TestRunDto();
        run.setRunId("POSTMAN-" + sanitize(name));
        run.setRunName(name);
        run.setExecutionMode("SEQUENTIAL");
        run.setCollectionVariables(readVariables(root.path("variable")));
        run.setAuth(readAuth(root.path("auth")));

        JsonNode items = root.path("item");
        if (!items.isArray() || items.isEmpty()) {
            return run;
        }

        boolean hasFolder = false;
        for (JsonNode item : items) {
            if (item.has("item")) {
                hasFolder = true;
                break;
            }
        }

        if (!hasFolder) {
            TestSuiteDto suite = new TestSuiteDto();
            suite.setSuiteId("SUITE-" + sanitize(name));
            suite.setSuiteName(name);
            suite.setAuth(run.getAuth());
            for (JsonNode item : items) {
                TestCaseDto testCase = readRequestItem(item, name);
                if (testCase != null) {
                    suite.getTestCases().add(testCase);
                }
            }
            run.getTestSuites().add(suite);
            return run;
        }

        int index = 1;
        List<TestCaseDto> rootRequests = new ArrayList<>();
        for (JsonNode item : items) {
            if (item.has("item")) {
                TestSuiteDto suite = readFolder(item, index++);
                run.getTestSuites().add(suite);
            } else {
                TestCaseDto testCase = readRequestItem(item, name);
                if (testCase != null) {
                    rootRequests.add(testCase);
                }
            }
        }
        if (!rootRequests.isEmpty()) {
            TestSuiteDto suite = new TestSuiteDto();
            suite.setSuiteId("SUITE-ROOT");
            suite.setSuiteName(name + " (root)");
            suite.setTestCases(rootRequests);
            run.getTestSuites().add(0, suite);
        }
        return run;
    }

    private TestSuiteDto readFolder(JsonNode folder, int index) {
        String folderName = text(folder, "name", "Folder " + index);
        TestSuiteDto suite = new TestSuiteDto();
        suite.setSuiteId("SUITE-" + index + "-" + sanitize(folderName));
        suite.setSuiteName(folderName);
        suite.setAuth(readAuth(folder.path("auth")));
        collectRequests(folder.path("item"), suite.getTestCases(), folderName);
        return suite;
    }

    private void collectRequests(JsonNode items, List<TestCaseDto> target, String folderName) {
        if (!items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            if (item.has("item")) {
                collectRequests(item.path("item"), target, folderName);
            } else {
                TestCaseDto testCase = readRequestItem(item, folderName);
                if (testCase != null) {
                    target.add(testCase);
                }
            }
        }
    }

    private TestCaseDto readRequestItem(JsonNode item, String folderName) {
        JsonNode requestNode = item.path("request");
        if (requestNode.isMissingNode() || requestNode.isNull()) {
            return null;
        }

        String requestName = text(item, "name", "Request");
        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("TC-" + sanitize(folderName) + "-" + sanitize(requestName));
        testCase.setTestCaseName(requestName);
        testCase.setDescription(readDescription(item.path("request").path("description")));
        testCase.setEnabled(!item.path("disabled").asBoolean(false));

        String method;
        if (requestNode.isTextual()) {
            method = "GET";
            BaseRequestDto request = new BaseRequestDto();
            request.setUrl(requestNode.asText());
            testCase.setRequest(request);
        } else {
            method = text(requestNode, "method", "GET");
            testCase.setRequest(readRequest(requestNode));
            testCase.setAuth(readAuth(requestNode.path("auth")));
        }
        testCase.setMethod(method);

        parseEvents(item.path("event"), testCase);
        if (testCase.getExpectedStatusCode() == null) {
            testCase.setExpectedStatusCode(200);
        }
        return testCase;
    }

    private BaseRequestDto readRequest(JsonNode requestNode) {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(readUrl(requestNode.path("url")));
        request.setHeaderItems(readHeaders(requestNode.path("header")));
        request.setQueryParamItems(readQuery(requestNode.path("url").path("query")));
        request.setPathParamItems(readPathVariables(requestNode.path("url").path("variable")));
        request.setBody(readBody(requestNode.path("body")));
        request.setAuth(readAuth(requestNode.path("auth")));
        return request;
    }

    private String readUrl(JsonNode urlNode) {
        if (urlNode == null || urlNode.isMissingNode() || urlNode.isNull()) {
            return "";
        }
        if (urlNode.isTextual()) {
            return urlNode.asText();
        }
        return text(urlNode, "raw", "");
    }

    private List<HeaderDto> readHeaders(JsonNode headers) {
        List<HeaderDto> items = new ArrayList<>();
        if (!headers.isArray()) {
            return items;
        }
        for (JsonNode header : headers) {
            HeaderDto dto = new HeaderDto();
            dto.setName(text(header, "key", ""));
            dto.setValue(text(header, "value", ""));
            dto.setEnabled(!header.path("disabled").asBoolean(false));
            items.add(dto);
        }
        return items;
    }

    private List<QueryParamDto> readQuery(JsonNode query) {
        List<QueryParamDto> items = new ArrayList<>();
        if (!query.isArray()) {
            return items;
        }
        for (JsonNode param : query) {
            QueryParamDto dto = new QueryParamDto();
            dto.setName(text(param, "key", ""));
            dto.setValue(text(param, "value", ""));
            dto.setEnabled(!param.path("disabled").asBoolean(false));
            items.add(dto);
        }
        return items;
    }

    private List<PathParamDto> readPathVariables(JsonNode variables) {
        List<PathParamDto> items = new ArrayList<>();
        if (!variables.isArray()) {
            return items;
        }
        for (JsonNode variable : variables) {
            PathParamDto dto = new PathParamDto();
            dto.setName(text(variable, "key", ""));
            dto.setValue(text(variable, "value", ""));
            items.add(dto);
        }
        return items;
    }

    private RequestBodyDto readBody(JsonNode bodyNode) {
        if (bodyNode == null || bodyNode.isMissingNode() || bodyNode.isNull()) {
            return null;
        }
        RequestBodyDto body = new RequestBodyDto();
        String mode = text(bodyNode, "mode", "raw");
        switch (mode) {
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
                JsonNode graphql = bodyNode.path("graphql");
                String query = text(graphql, "query", "");
                String variables = text(graphql, "variables", "");
                body.setRawBody("{\"query\":"
                        + quote(query)
                        + ",\"variables\":"
                        + (variables.isBlank() ? "{}" : variables)
                        + "}");
                body.setContentType("application/json");
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

    private List<QueryParamDto> readForm(JsonNode form) {
        List<QueryParamDto> fields = new ArrayList<>();
        if (!form.isArray()) {
            return fields;
        }
        for (JsonNode field : form) {
            if ("file".equalsIgnoreCase(text(field, "type", ""))) {
                continue;
            }
            QueryParamDto dto = new QueryParamDto();
            dto.setName(text(field, "key", ""));
            dto.setValue(text(field, "value", ""));
            dto.setEnabled(!field.path("disabled").asBoolean(false));
            fields.add(dto);
        }
        return fields;
    }

    private AuthDto readAuth(JsonNode authNode) {
        if (authNode == null || authNode.isMissingNode() || authNode.isNull()) {
            return null;
        }
        String type = text(authNode, "type", "noauth");
        AuthDto auth = new AuthDto();
        switch (type.toLowerCase()) {
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
                String in = authValue(authNode.path("apikey"), "in");
                auth.setApiKeyIn("query".equalsIgnoreCase(in) ? "QUERY" : "HEADER");
            }
            case "noauth" -> auth.setType(AuthType.NONE);
            default -> auth.setType(AuthType.INHERIT);
        }
        return auth;
    }

    private String authValue(JsonNode array, String key) {
        if (array != null && array.isArray()) {
            for (JsonNode entry : array) {
                if (key.equals(text(entry, "key", ""))) {
                    return text(entry, "value", "");
                }
            }
        }
        if (array != null && array.isObject()) {
            return text(array, key, "");
        }
        return "";
    }

    private Map<String, String> readVariables(JsonNode variables) {
        Map<String, String> values = new LinkedHashMap<>();
        if (!variables.isArray()) {
            return values;
        }
        for (JsonNode variable : variables) {
            if (variable.path("disabled").asBoolean(false)) {
                continue;
            }
            String key = text(variable, "key", "");
            if (!key.isBlank()) {
                values.put(key, text(variable, "value", ""));
            }
        }
        return values;
    }

    private void parseEvents(JsonNode events, TestCaseDto testCase) {
        if (!events.isArray()) {
            return;
        }
        for (JsonNode event : events) {
            String listen = text(event, "listen", "");
            String script = joinScript(event.path("script").path("exec"));
            if ("test".equals(listen)) {
                parseTests(script, testCase);
            }
            if ("prerequest".equals(listen) || "test".equals(listen)) {
                parseExtracts(script, testCase);
            }
        }
    }

    private void parseTests(String script, TestCaseDto testCase) {
        Matcher status = STATUS_TEST.matcher(script);
        while (status.find()) {
            int code = Integer.parseInt(status.group(1));
            testCase.setExpectedStatusCode(code);
            addAssertion(testCase, AssertionType.STATUS_CODE, "statusCode",
                    AssertionOperator.EQUALS, String.valueOf(code));
        }
        Matcher expectCode = EXPECT_CODE.matcher(script);
        while (expectCode.find()) {
            int code = Integer.parseInt(expectCode.group(1));
            testCase.setExpectedStatusCode(code);
        }
        Matcher header = HEADER_TEST.matcher(script);
        while (header.find()) {
            addAssertion(testCase, AssertionType.HEADER, header.group(1),
                    AssertionOperator.EXISTS, "");
        }
        Matcher time = RESPONSE_TIME.matcher(script);
        while (time.find()) {
            addAssertion(testCase, AssertionType.RESPONSE_TIME, "responseTimeMs",
                    AssertionOperator.LESS_THAN, time.group(1));
        }
        if (script.contains("pm.response.to.be.ok")) {
            testCase.setExpectedStatusCode(200);
        }
    }

    private void parseExtracts(String script, TestCaseDto testCase) {
        Matcher env = ENV_SET_JSON.matcher(script);
        while (env.find()) {
            ExtractDto extract = new ExtractDto();
            extract.setVariableName(env.group(1));
            extract.setExpression("$." + env.group(2));
            extract.setSource("BODY");
            testCase.getExtracts().add(extract);
        }
        Matcher path = ENV_SET_JSONPATH.matcher(script);
        while (path.find()) {
            ExtractDto extract = new ExtractDto();
            extract.setVariableName(path.group(1));
            extract.setExpression("$." + path.group(2));
            extract.setSource("BODY");
            testCase.getExtracts().add(extract);
        }
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

    private String joinScript(JsonNode exec) {
        if (exec.isTextual()) {
            return exec.asText();
        }
        if (!exec.isArray()) {
            return "";
        }
        StringBuilder script = new StringBuilder();
        for (JsonNode line : exec) {
            script.append(line.asText()).append('\n');
        }
        return script.toString();
    }

    private String readDescription(JsonNode description) {
        if (description.isTextual()) {
            return description.asText();
        }
        return text(description, "content", "");
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        return value.asText(fallback);
    }

    private String sanitize(String value) {
        return value == null ? "item" : value.replaceAll("[^A-Za-z0-9]+", "-");
    }

    private String quote(String value) {
        return objectMapper.valueToTree(value).toString();
    }
}
