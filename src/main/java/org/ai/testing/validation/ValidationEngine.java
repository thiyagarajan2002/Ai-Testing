package org.ai.testing.validation;

import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationResultDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;

import java.util.List;


public class ValidationEngine {

    private final StatusCodeValidator statusCodeValidator;
    private final ResponseBodyValidator responseBodyValidator;
    private final HeaderValidator headerValidator;

    public ValidationEngine() {
        this.statusCodeValidator =
                new StatusCodeValidator();

        this.responseBodyValidator =
                new ResponseBodyValidator();

        this.headerValidator =
                new HeaderValidator();
    }

    public ValidationSummaryDto validateStatusCode(
            ResponseDto response,
            int expectedStatusCode) {

        ValidationSummaryDto summary =
                new ValidationSummaryDto();

        ValidationResultDto result =
                statusCodeValidator.validate(
                        response,
                        expectedStatusCode
                );

        addResult(summary, result);

        return summary;
    }

    public ValidationSummaryDto validateBody(
            ResponseDto response,
            AssertionOperator operator,
            String expectedValue) {

        ValidationSummaryDto summary =
                new ValidationSummaryDto();

        ValidationResultDto result =
                responseBodyValidator.validate(
                        response,
                        operator,
                        expectedValue
                );

        addResult(summary, result);

        return summary;
    }

    public ValidationSummaryDto validateHeader(
            ResponseDto response,
            String headerName,
            AssertionOperator operator,
            String expectedValue) {

        ValidationSummaryDto summary =
                new ValidationSummaryDto();

        ValidationResultDto result =
                headerValidator.validate(
                        response,
                        headerName,
                        operator,
                        expectedValue
                );

        addResult(summary, result);

        return summary;
    }

    public ValidationSummaryDto validateAll(
            ResponseDto response,
            Integer expectedStatusCode,
            List<BodyValidation> bodyValidations,
            List<HeaderValidation> headerValidations) {

        ValidationSummaryDto summary =
                new ValidationSummaryDto();

        if (expectedStatusCode != null) {

            ValidationResultDto result =
                    statusCodeValidator.validate(
                            response,
                            expectedStatusCode
                    );

            addResult(summary, result);
        }

        if (bodyValidations != null) {

            for (BodyValidation validation :
                    bodyValidations) {

                ValidationResultDto result =
                        responseBodyValidator.validate(
                                response,
                                validation.operator(),
                                validation.expectedValue()
                        );

                addResult(summary, result);
            }
        }

        if (headerValidations != null) {

            for (HeaderValidation validation :
                    headerValidations) {

                ValidationResultDto result =
                        headerValidator.validate(
                                response,
                                validation.headerName(),
                                validation.operator(),
                                validation.expectedValue()
                        );

                addResult(summary, result);
            }
        }

        summary.setPassed(
                summary.getFailedCount() == 0
        );

        return summary;
    }

    private void addResult(
            ValidationSummaryDto summary,
            ValidationResultDto result) {

        summary.getResults().add(result);

        summary.setTotal(
                summary.getTotal() + 1
        );

        if (result.isPassed()) {

            summary.setPassedCount(
                    summary.getPassedCount() + 1
            );

        } else {

            summary.setFailedCount(
                    summary.getFailedCount() + 1
            );
        }

        summary.setPassed(
                summary.getFailedCount() == 0
        );
    }

    public record BodyValidation(
            AssertionOperator operator,
            String expectedValue) {
    }

    public record HeaderValidation(
            String headerName,
            AssertionOperator operator,
            String expectedValue) {
    }
}