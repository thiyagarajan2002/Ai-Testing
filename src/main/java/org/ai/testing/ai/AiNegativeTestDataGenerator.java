package org.ai.testing.ai;

import org.ai.testing.ai.model.AiNegativeTestData;
import org.ai.testing.ai.model.AiNegativeTestDataResult;
import org.ai.testing.ai.model.AiTestGenerationRequest;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class AiNegativeTestDataGenerator {

    public AiNegativeTestDataResult generate(AiTestGenerationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            throw new IllegalArgumentException("url is required");
        }

        AiNegativeTestDataResult result = new AiNegativeTestDataResult();
        String method = request.getMethod() == null
                ? "POST"
                : request.getMethod().toUpperCase(Locale.ROOT);

        if (request.getRequestBody() != null && !request.getRequestBody().isBlank()) {
            addInvalidJsonTypeCase(result, request, method);
            addMissingRequiredFieldCase(result, request, method);
        }

        addEmptyBodyCase(result, request, method);
        addUnsupportedContentTypeCase(result, request, method);

        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            addMissingContentTypeCase(result, request, method);
        }

        return result;
    }

    private void addInvalidJsonTypeCase(
            AiNegativeTestDataResult result,
            AiTestGenerationRequest request,
            String method) {

        if (!looksLikeJson(request.getRequestBody())) {
            return;
        }

        String mutatedBody = request.getRequestBody()
                .replaceFirst(":\\s*([0-9]+(?:\\.[0-9]+)?)", ": \"invalid-number\"");

        if (mutatedBody.equals(request.getRequestBody())) {
            mutatedBody = request.getRequestBody()
                    .replaceFirst(":\\s*\"[^\"]*\"", ": 123456");
        }

        if (!mutatedBody.equals(request.getRequestBody())) {
            result.add(create(request, "INVALID_FIELD_TYPE",
                    "Replace a JSON field with an incompatible data type.",
                    mutatedBody, 400));
        }
    }

    private void addMissingRequiredFieldCase(
            AiNegativeTestDataResult result,
            AiTestGenerationRequest request,
            String method) {

        String body = request.getRequestBody();
        if (!looksLikeJsonObject(body)) {
            return;
        }

        String mutatedBody = removeFirstJsonField(body);
        if (!mutatedBody.equals(body)) {
            result.add(create(request, "MISSING_REQUIRED_FIELD",
                    "Remove the first JSON field to exercise required-field validation.",
                    mutatedBody, 400));
        }
    }

    private void addEmptyBodyCase(
            AiNegativeTestDataResult result,
            AiTestGenerationRequest request,
            String method) {

        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
            result.add(create(request, "EMPTY_REQUEST_BODY",
                    "Send an empty request body to verify input validation.",
                    "", 400));
        }
    }

    private void addUnsupportedContentTypeCase(
            AiNegativeTestDataResult result,
            AiTestGenerationRequest request,
            String method) {

        Map<String, String> headers = copy(request.getHeaders());
        headers.put("Content-Type", "text/plain");

        AiNegativeTestData data = create(request, "UNSUPPORTED_CONTENT_TYPE",
                "Use a content type that does not match the normal API contract.",
                request.getRequestBody(), 415);
        data.setHeaders(headers);
        result.add(data);
    }

    private void addMissingContentTypeCase(
            AiNegativeTestDataResult result,
            AiTestGenerationRequest request,
            String method) {

        Map<String, String> headers = copy(request.getHeaders());
        headers.entrySet().removeIf(entry -> entry.getKey() != null
                && entry.getKey().equalsIgnoreCase("Content-Type"));

        AiNegativeTestData data = create(request, "MISSING_CONTENT_TYPE",
                "Remove Content-Type to verify media-type validation.",
                request.getRequestBody(), 415);
        data.setHeaders(headers);
        result.add(data);
    }

    private AiNegativeTestData create(
            AiTestGenerationRequest request,
            String scenario,
            String reason,
            String body,
            Integer expectedStatus) {

        AiNegativeTestData data = new AiNegativeTestData();
        data.setScenario(scenario);
        data.setReason(reason);
        data.setRequestBody(body);
        data.setHeaders(copy(request.getHeaders()));
        data.setExpectedStatusCode(expectedStatus);
        return data;
    }

    private Map<String, String> copy(Map<String, String> source) {
        return source == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(source);
    }

    private boolean looksLikeJson(String body) {
        if (body == null) {
            return false;
        }
        String value = body.trim();
        return (value.startsWith("{") && value.endsWith("}"))
                || (value.startsWith("[") && value.endsWith("]"));
    }

    private boolean looksLikeJsonObject(String body) {
        if (body == null) {
            return false;
        }
        String value = body.trim();
        return value.startsWith("{") && value.endsWith("}")
                && value.length() > 2;
    }

    private String removeFirstJsonField(String body) {
        String value = body.trim();
        return value.replaceFirst(
                "\\{\\s*\"[^\"]+\"\\s*:\\s*(\"(?:\\\\.|[^\"])*\"|-?\\d+(?:\\.\\d+)?|true|false|null)(\\s*,)?",
                "{");
    }
}
