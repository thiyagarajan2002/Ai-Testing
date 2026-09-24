package org.ai.testing.validation;

import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationResultDto;

public class ResponseTimeValidator {

    public ValidationResultDto validate(
            ResponseDto response,
            AssertionOperator operator,
            String expectedValue) {

        ValidationResultDto result = new ValidationResultDto();
        result.setValidationType("RESPONSE_TIME");
        result.setField("responseTimeMs");
        result.setExpected(expectedValue);

        long actual = response == null ? 0 : response.getResponseTimeMs();
        result.setActual(String.valueOf(actual));

        if (operator == null) {
            result.setPassed(false);
            result.setMessage("Response time operator cannot be null");
            return result;
        }

        long expected;
        try {
            expected = Long.parseLong(expectedValue == null ? "" : expectedValue.trim());
        } catch (NumberFormatException e) {
            result.setPassed(false);
            result.setMessage("Invalid expected response time: " + expectedValue);
            return result;
        }

        boolean passed = switch (operator) {
            case EQUALS -> actual == expected;
            case NOT_EQUALS -> actual != expected;
            case LESS_THAN -> actual < expected;
            case GREATER_THAN -> actual > expected;
            default -> false;
        };

        result.setPassed(passed);
        result.setMessage(passed
                ? "Response time validation passed"
                : "Response time validation failed. Operator: " + operator
                + ", Expected: " + expected
                + ", Actual: " + actual);
        return result;
    }
}
