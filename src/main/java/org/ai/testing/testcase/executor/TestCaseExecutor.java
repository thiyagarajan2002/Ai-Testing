package org.ai.testing.testcase.executor;

import lombok.Data;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.executor.ExecutorDispatcher;
import org.ai.testing.executor.common.RequestBuilder;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.factory.TestCaseRequestFactory;
import org.ai.testing.validation.AssertionType;
import org.ai.testing.validation.ValidationEngine;
import org.ai.testing.validation.dto.ValidationResultDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;

import static org.ai.testing.validation.AssertionType.STATUS_CODE;

public class TestCaseExecutor {

    private final TestCaseRequestFactory requestFactory;
    private final ExecutorDispatcher executorDispatcher;
    private final ValidationEngine validationEngine;

    public TestCaseExecutor() {
        this.requestFactory =
                new TestCaseRequestFactory();

        this.executorDispatcher =
                new ExecutorDispatcher();

        this.validationEngine =
                new ValidationEngine();
    }

    public TestCaseExecutionResult execute(
            TestCaseDto testCase) {

        if (testCase == null) {
            throw new IllegalArgumentException(
                    "Test case cannot be null"
            );
        }

        TestCaseExecutionResult executionResult =
                new TestCaseExecutionResult();

        executionResult.setTestCaseId(
                testCase.getTestCaseId()
        );

        executionResult.setTestCaseName(
                testCase.getTestCaseName()
        );

        // ---------------------------------------------
        // Disabled test case
        // ---------------------------------------------

        if (!testCase.isEnabled()) {

            executionResult.setExecuted(false);
            executionResult.setPassed(false);
            executionResult.setMessage(
                    "Test case is disabled"
            );

            return executionResult;
        }

        try {

            // -----------------------------------------
            // Build request
            // -----------------------------------------

            BaseRequestDto request =
                    requestFactory.createRequest(testCase);

            // -----------------------------------------
            // Execute HTTP request
            // -----------------------------------------

            executionResult.setRequest(request);
            request.setUrl(new RequestBuilder().buildUrl(request));

            ResponseDto response =
                    executorDispatcher.execute(
                            testCase.getMethod(),
                            request
                    );

            executionResult.setResponse(response);
            executionResult.setExecuted(true);

            // -----------------------------------------
            // Validate response
            // -----------------------------------------

            ValidationSummaryDto validationSummary =
                    validateResponse(
                            testCase,
                            response
                    );

            executionResult.setValidationSummary(
                    validationSummary
            );

            executionResult.setPassed(
                    validationSummary.isPassed()
            );

            executionResult.setMessage(
                    validationSummary.isPassed()
                            ? "Test case passed"
                            : "Test case failed"
            );

        } catch (Exception e) {

            executionResult.setExecuted(true);
            executionResult.setPassed(false);

            executionResult.setMessage(
                    "Test case execution failed: "
                            + e.getMessage()
            );
        }

        return executionResult;
    }

    private ValidationSummaryDto validateResponse(
            TestCaseDto testCase,
            ResponseDto response) {

        ValidationSummaryDto summary =
                new ValidationSummaryDto();

        // ---------------------------------------------
        // Built-in expected status code
        // ---------------------------------------------

        boolean statusCodeAlreadyValidated =
                testCase.getExpectedStatusCode() != null;

        if (statusCodeAlreadyValidated) {

            ValidationResultDto result =
                    validationEngine
                            .validateStatusCode(
                                    response,
                                    testCase
                                            .getExpectedStatusCode()
                            )
                            .getResults()
                            .get(0);

            addResult(summary, result);
        }

        // ---------------------------------------------
        // Assertions
        // ---------------------------------------------

        if (testCase.getAssertions() != null) {

            for (AssertionDto assertion :
                    testCase.getAssertions()) {

                if (assertion == null
                        || assertion.getType() == null) {
                    continue;
                }

                AssertionType assertionType =
                        assertion.getType();

                // -------------------------------------
                // Avoid duplicate status validation
                // -------------------------------------

                if (statusCodeAlreadyValidated
                        && assertionType ==
                        AssertionType.STATUS_CODE) {

                    continue;
                }

                ValidationResultDto result =
                        executeAssertion(
                                response,
                                assertion
                        );

                addResult(summary, result);
            }
        }

        summary.setPassed(
                summary.getFailedCount() == 0
        );

        return summary;
    }

    private ValidationResultDto executeAssertion(
            ResponseDto response,
            AssertionDto assertion) {

        AssertionType type =
                assertion.getType();

        // ---------------------------------------------
        // Validate assertion type
        // ---------------------------------------------

        if (type == null) {

            return createFailedAssertionResult(
                    "UNKNOWN",
                    assertion,
                    "Assertion type cannot be null"
            );
        }

        // ---------------------------------------------
        // Validate operator
        // ---------------------------------------------

        if (assertion.getOperator() == null) {

            return createFailedAssertionResult(
                    type.name(),
                    assertion,
                    "Assertion operator cannot be null"
            );
        }

        // ---------------------------------------------
        // Execute assertion
        // ---------------------------------------------

        switch (type) {

            case RESPONSE_BODY:

                return validationEngine
                        .validateBody(
                                response,
                                assertion.getOperator(),
                                assertion.getExpectedValue()
                        )
                        .getResults()
                        .get(0);

            case HEADER:

                return validationEngine
                        .validateHeader(
                                response,
                                assertion.getField(),
                                assertion.getOperator(),
                                assertion.getExpectedValue()
                        )
                        .getResults()
                        .get(0);

            case STATUS_CODE:

                return executeStatusCodeAssertion(
                        response,
                        assertion
                );

            default:

                return createFailedAssertionResult(
                        type.name(),
                        assertion,
                        "Unsupported assertion type: "
                                + type
                );
        }
    }

    private ValidationResultDto executeStatusCodeAssertion(
            ResponseDto response,
            AssertionDto assertion) {

        String expectedValue =
                assertion.getExpectedValue();

        int expectedStatusCode;

        try {

            expectedStatusCode =
                    Integer.parseInt(
                            expectedValue
                    );

        } catch (NumberFormatException e) {

            return createFailedAssertionResult(
                    AssertionType.STATUS_CODE.name(),
                    assertion,
                    "Invalid expected status code: "
                            + expectedValue
            );
        }

        return validationEngine
                .validateStatusCode(
                        response,
                        expectedStatusCode
                )
                .getResults()
                .get(0);
    }

    private ValidationResultDto createFailedAssertionResult(
            String validationType,
            AssertionDto assertion,
            String message) {

        ValidationResultDto result =
                new ValidationResultDto();

        result.setPassed(false);

        result.setValidationType(
                validationType
        );

        result.setField(
                assertion.getField()
        );

        result.setExpected(
                assertion.getExpectedValue()
        );

        result.setActual("");

        result.setMessage(message);

        return result;
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

    @Data
    public static class TestCaseExecutionResult {

        private String testCaseId;

        private String testCaseName;

        private boolean executed;

        private boolean passed;

        private String message;

        private BaseRequestDto request;

        private ResponseDto response;

        private ValidationSummaryDto validationSummary;
    }
}