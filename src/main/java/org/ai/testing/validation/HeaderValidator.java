package org.ai.testing.validation;


import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationResultDto;

public class HeaderValidator {

    public ValidationResultDto validate(
            ResponseDto response,
            String headerName,
            AssertionOperator operator,
            String expectedValue) {

        ValidationResultDto result =
                new ValidationResultDto();

        String actualValue =
                response.getHeaders()
                        .entrySet()
                        .stream()
                        .filter(entry ->
                                entry.getKey()
                                        .equalsIgnoreCase(headerName))
                        .map(entry -> entry.getValue())
                        .findFirst()
                        .orElse(null);

        if (operator == null) {

            result.setPassed(false);
            result.setValidationType("HEADER");
            result.setField(headerName);
            result.setExpected(expectedValue);
            result.setActual(actualValue);
            result.setMessage(
                    "Header validation operator cannot be null"
            );

            return result;
        }

        boolean passed;

        switch (operator) {

            case EXISTS:
                passed = actualValue != null;
                break;

            case NOT_EXISTS:
                passed = actualValue == null;
                break;

            case EQUALS:
                passed = actualValue != null
                        && actualValue.equals(expectedValue);
                break;

            case NOT_EQUALS:
                passed = actualValue == null
                        || !actualValue.equals(expectedValue);
                break;

            case CONTAINS:
                passed = actualValue != null
                        && expectedValue != null
                        && actualValue.contains(expectedValue);
                break;

            case NOT_CONTAINS:
                passed = actualValue == null
                        || expectedValue == null
                        || !actualValue.contains(expectedValue);
                break;

            case EMPTY:
                passed = actualValue != null
                        && actualValue.isBlank();
                break;

            case NOT_EMPTY:
                passed = actualValue != null
                        && !actualValue.isBlank();
                break;

            default:

                result.setPassed(false);
                result.setValidationType("HEADER");
                result.setField(headerName);
                result.setExpected(expectedValue);
                result.setActual(actualValue);
                result.setMessage(
                        "Unsupported header operator: "
                                + operator
                );

                return result;
        }

        result.setPassed(passed);
        result.setValidationType("HEADER");
        result.setField(headerName);
        result.setExpected(expectedValue);
        result.setActual(actualValue);

        result.setMessage(
                passed
                        ? "Header validation passed"
                        : "Header validation failed. "
                        + "Header: " + headerName
                        + ", Operator: " + operator
                        + ", Expected: " + expectedValue
                        + ", Actual: " + actualValue
        );

        return result;
    }
}