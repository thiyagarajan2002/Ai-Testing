package org.ai.testing.validation;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationResultDto;

public class JsonPathValidator {

    public ValidationResultDto validate(
            ResponseDto response,
            String jsonPath,
            AssertionOperator operator,
            String expectedValue) {

        ValidationResultDto result = new ValidationResultDto();
        result.setValidationType("JSON_PATH");
        result.setField(jsonPath);
        result.setExpected(expectedValue);

        String body = response == null || response.getBody() == null ? "" : response.getBody();
        String path = normalizePath(jsonPath);
        String actual;
        boolean pathFound;
        try {
            Object value = JsonPath.read(body, path);
            pathFound = true;
            actual = value == null ? "" : String.valueOf(value);
        } catch (PathNotFoundException | IllegalArgumentException e) {
            pathFound = false;
            actual = "";
        }
        result.setActual(actual);

        if (operator == null) {
            result.setPassed(false);
            result.setMessage("JSONPath operator cannot be null");
            return result;
        }

        boolean passed = switch (operator) {
            case EXISTS -> pathFound;
            case NOT_EXISTS -> !pathFound;
            case EMPTY -> pathFound && actual.isBlank();
            case NOT_EMPTY -> pathFound && !actual.isBlank();
            case EQUALS -> pathFound && actual.equals(expectedValue);
            case NOT_EQUALS -> pathFound && !actual.equals(expectedValue);
            case CONTAINS -> pathFound && expectedValue != null && actual.contains(expectedValue);
            case NOT_CONTAINS -> pathFound && (expectedValue == null || !actual.contains(expectedValue));
            case MATCHES -> pathFound && expectedValue != null && actual.matches(expectedValue);
            default -> false;
        };

        result.setPassed(passed);
        result.setMessage(passed
                ? "JSONPath validation passed"
                : "JSONPath validation failed. Path: " + path
                + ", Operator: " + operator
                + ", Expected: " + expectedValue
                + ", Actual: " + actual);
        return result;
    }

    private String normalizePath(String jsonPath) {
        if (jsonPath == null || jsonPath.isBlank()) {
            return "$";
        }
        String path = jsonPath.trim();
        if (path.startsWith("res.body.")) {
            return "$." + path.substring("res.body.".length());
        }
        if (!path.startsWith("$")) {
            return "$." + path;
        }
        return path;
    }
}
