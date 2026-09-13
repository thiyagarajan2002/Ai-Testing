package org.ai.testing.validation;

import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationResultDto;

public class ResponseBodyValidator {

    public ValidationResultDto validate(
            ResponseDto response,
            AssertionOperator operator,
            String expectedValue) {

        ValidationResultDto result =
                new ValidationResultDto();

        String actualBody =
                response.getBody();

        if (actualBody == null) {
            actualBody = "";
        }

        if (operator == null) {

            result.setPassed(false);
            result.setValidationType("RESPONSE_BODY");
            result.setField("body");
            result.setExpected(expectedValue);
            result.setActual(actualBody);
            result.setMessage(
                    "Response body validation operator cannot be null"
            );

            return result;
        }

        boolean passed;

        switch (operator) {

            case EQUALS:
                passed = actualBody.equals(expectedValue);
                break;

            case NOT_EQUALS:
                passed = !actualBody.equals(expectedValue);
                break;

            case CONTAINS:
                passed = expectedValue != null
                        && actualBody.contains(expectedValue);
                break;

            case NOT_CONTAINS:
                passed = expectedValue == null
                        || !actualBody.contains(expectedValue);
                break;

            case EMPTY:
                passed = actualBody.isBlank();
                break;

            case NOT_EMPTY:
                passed = !actualBody.isBlank();
                break;

            default:

                result.setPassed(false);
                result.setValidationType("RESPONSE_BODY");
                result.setField("body");
                result.setExpected(expectedValue);
                result.setActual(actualBody);
                result.setMessage(
                        "Unsupported response body operator: "
                                + operator
                );

                return result;
        }

        result.setPassed(passed);
        result.setValidationType("RESPONSE_BODY");
        result.setField("body");
        result.setExpected(expectedValue);
        result.setActual(actualBody);

        result.setMessage(
                passed
                        ? "Response body validation passed"
                        : "Response body validation failed. "
                        + "Operator: " + operator
                        + ", Expected: " + expectedValue
                        + ", Actual: " + actualBody
        );

        return result;
    }
}