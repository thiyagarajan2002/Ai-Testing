package org.ai.testing.ai;

import org.ai.testing.ai.model.AiAssertionSuggestion;
import org.ai.testing.ai.model.AiAssertionSuggestionResult;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

import java.util.Locale;

public class AiAssertionSuggester {

    public AiAssertionSuggestionResult suggest(ResponseDto response, Integer expectedStatusCode) {
        if (response == null) {
            throw new IllegalArgumentException("response is required");
        }

        AiAssertionSuggestionResult result = new AiAssertionSuggestionResult();

        int expected = expectedStatusCode == null ? response.getStatusCode() : expectedStatusCode;
        result.add(statusSuggestion(expected));

        String body = response.getBody();
        if (body != null && !body.isBlank()) {
            AssertionDto bodyAssertion = new AssertionDto();
            bodyAssertion.setType(AssertionType.RESPONSE_BODY);
            bodyAssertion.setField("body");
            bodyAssertion.setOperator(AssertionOperator.NOT_EMPTY);
            bodyAssertion.setExpectedValue(null);
            result.add(new AiAssertionSuggestion(bodyAssertion,
                    "The API returned a non-empty response body.", "HIGH"));

            if (looksLikeJson(body)) {
                AssertionDto jsonAssertion = new AssertionDto();
                jsonAssertion.setType(AssertionType.RESPONSE_BODY);
                jsonAssertion.setField("body");
                jsonAssertion.setOperator(AssertionOperator.CONTAINS);
                jsonAssertion.setExpectedValue("{");
                result.add(new AiAssertionSuggestion(jsonAssertion,
                        "The response appears to be a JSON object.", "MEDIUM"));
            }
        }

        String contentType = findHeader(response, "content-type");
        if (contentType != null) {
            AssertionDto headerAssertion = new AssertionDto();
            headerAssertion.setType(AssertionType.HEADER);
            headerAssertion.setField("Content-Type");
            headerAssertion.setOperator(AssertionOperator.CONTAINS);
            headerAssertion.setExpectedValue(contentType.split(";", 2)[0].trim());
            result.add(new AiAssertionSuggestion(headerAssertion,
                    "The response advertises a Content-Type header.", "HIGH"));
        }

        return result;
    }

    private AiAssertionSuggestion statusSuggestion(int expectedStatusCode) {
        AssertionDto assertion = new AssertionDto();
        assertion.setType(AssertionType.STATUS_CODE);
        assertion.setField("statusCode");
        assertion.setOperator(AssertionOperator.EQUALS);
        assertion.setExpectedValue(String.valueOf(expectedStatusCode));
        return new AiAssertionSuggestion(assertion,
                "The expected HTTP status should be explicitly validated.", "HIGH");
    }

    private boolean looksLikeJson(String body) {
        String trimmed = body.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private String findHeader(ResponseDto response, String name) {
        if (response.getHeaders() == null) {
            return null;
        }
        return response.getHeaders().entrySet().stream()
                .filter(entry -> entry.getKey() != null
                        && entry.getKey().toLowerCase(Locale.ROOT).equals(name))
                .map(entry -> entry.getValue())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
