package org.ai.testing.validation;


import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationResultDto;

public class StatusCodeValidator {

    public ValidationResultDto validate(
            ResponseDto response,
            int expectedStatusCode) {

        ValidationResultDto result =
                new ValidationResultDto();

        int actualStatusCode =
                response.getStatusCode();

        boolean passed =
                actualStatusCode == expectedStatusCode;

        result.setPassed(passed);

        result.setValidationType(
                "STATUS_CODE"
        );

        result.setField(
                "statusCode"
        );

        result.setExpected(
                String.valueOf(expectedStatusCode)
        );

        result.setActual(
                String.valueOf(actualStatusCode)
        );

        result.setMessage(
                passed
                        ? "Status code validation passed"
                        : "Expected "
                        + expectedStatusCode
                        + " but received "
                        + actualStatusCode
        );

        return result;
    }
}