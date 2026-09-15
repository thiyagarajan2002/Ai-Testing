package org.ai.testing.testcase.executor;

import lombok.Data;
import org.ai.testing.ai.AiFailureInsightService;
import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.executor.ExecutorDispatcher;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.factory.TestCaseRequestFactory;
import org.ai.testing.validation.AssertionType;
import org.ai.testing.validation.ValidationEngine;
import org.ai.testing.validation.dto.ValidationResultDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;

public class TestCaseExecutor {

    private final TestCaseRequestFactory requestFactory;
    private final ExecutorDispatcher executorDispatcher;
    private final ValidationEngine validationEngine;
    private final AiFailureInsightService aiFailureInsightService;
    private final long aiResponseTimeThresholdMs;

    public TestCaseExecutor() {
        this(new AiFailureInsightService(), 2000);
    }

    public TestCaseExecutor(
            AiFailureInsightService aiFailureInsightService,
            long aiResponseTimeThresholdMs) {
        if (aiFailureInsightService == null) {
            throw new IllegalArgumentException("AI failure insight service cannot be null");
        }
        if (aiResponseTimeThresholdMs < 0) {
            throw new IllegalArgumentException("AI response time threshold cannot be negative");
        }

        this.requestFactory = new TestCaseRequestFactory();
        this.executorDispatcher = new ExecutorDispatcher();
        this.validationEngine = new ValidationEngine();
        this.aiFailureInsightService = aiFailureInsightService;
        this.aiResponseTimeThresholdMs = aiResponseTimeThresholdMs;
    }

    public TestCaseExecutionResult execute(TestCaseDto testCase) {
        if (testCase == null) {
            throw new IllegalArgumentException("Test case cannot be null");
        }

        TestCaseExecutionResult executionResult = new TestCaseExecutionResult();
        executionResult.setTestCaseId(testCase.getTestCaseId());
        executionResult.setTestCaseName(testCase.getTestCaseName());

        if (!testCase.isEnabled()) {
            executionResult.setExecuted(false);
            executionResult.setPassed(false);
            executionResult.setMessage("Test case is disabled");
            return executionResult;
        }

        try {
            BaseRequestDto request = requestFactory.createRequest(testCase);
            normalizeRequest(request);
            executionResult.setRequest(copyRequest(request));

            ResponseDto response = executorDispatcher.execute(testCase.getMethod(), request);
            executionResult.setResponse(response);
            executionResult.setExecuted(true);

            ValidationSummaryDto validationSummary = validateResponse(testCase, response);
            executionResult.setValidationSummary(validationSummary);
            executionResult.setPassed(validationSummary.isPassed());
            executionResult.setMessage(validationSummary.isPassed()
                    ? "Test case passed"
                    : "Test case failed");

            AiFailureAnalysis aiFailureAnalysis = aiFailureInsightService.analyze(
                    executionResult,
                    testCase.getExpectedStatusCode(),
                    aiResponseTimeThresholdMs);
            executionResult.setAiFailureAnalysis(aiFailureAnalysis);

        } catch (Exception e) {
            executionResult.setExecuted(true);
            executionResult.setPassed(false);
            executionResult.setMessage("Test case execution failed: " + e.getMessage());
        }

        return executionResult;
    }

    private void normalizeRequest(BaseRequestDto request) {
        if (request.getHeaders() == null) {
            request.setHeaders(new java.util.HashMap<>());
        }
        if (request.getQueryParams() == null) {
            request.setQueryParams(new java.util.HashMap<>());
        }
        if (request.getPathParams() == null) {
            request.setPathParams(new java.util.HashMap<>());
        }
        if (request.getBody() != null
                && request.getBody().getContentType() != null
                && !request.getBody().getContentType().isBlank()
                && request.getHeaders().keySet().stream()
                .noneMatch(key -> key != null && key.equalsIgnoreCase("Content-Type"))) {
            request.getHeaders().put("Content-Type", request.getBody().getContentType());
        }
    }

    private BaseRequestDto copyRequest(BaseRequestDto source) {
        BaseRequestDto copy = new BaseRequestDto();
        copy.setUrl(source.getUrl());
        copy.setHeaders(source.getHeaders() == null
                ? new java.util.HashMap<>()
                : new java.util.HashMap<>(source.getHeaders()));
        copy.setQueryParams(source.getQueryParams() == null
                ? new java.util.HashMap<>()
                : new java.util.HashMap<>(source.getQueryParams()));
        copy.setPathParams(source.getPathParams() == null
                ? new java.util.HashMap<>()
                : new java.util.HashMap<>(source.getPathParams()));
        if (source.getBody() != null) {
            org.ai.testing.dto.common.RequestBodyDto body = new org.ai.testing.dto.common.RequestBodyDto();
            body.setContentType(source.getBody().getContentType());
            body.setRawBody(source.getBody().getRawBody());
            copy.setBody(body);
        }
        return copy;
    }

    private ValidationSummaryDto validateResponse(TestCaseDto testCase, ResponseDto response) {
        ValidationSummaryDto summary = new ValidationSummaryDto();
        boolean statusCodeAlreadyValidated = testCase.getExpectedStatusCode() != null;

        if (statusCodeAlreadyValidated) {
            ValidationResultDto result = validationEngine
                    .validateStatusCode(response, testCase.getExpectedStatusCode())
                    .getResults().get(0);
            addResult(summary, result);
        }

        if (testCase.getAssertions() != null) {
            for (AssertionDto assertion : testCase.getAssertions()) {
                if (assertion == null || assertion.getType() == null) {
                    continue;
                }

                AssertionType assertionType = assertion.getType();
                if (statusCodeAlreadyValidated && assertionType == AssertionType.STATUS_CODE) {
                    continue;
                }

                ValidationResultDto result = executeAssertion(response, assertion);
                addResult(summary, result);
            }
        }

        summary.setPassed(summary.getFailedCount() == 0);
        return summary;
    }

    private ValidationResultDto executeAssertion(ResponseDto response, AssertionDto assertion) {
        AssertionType type = assertion.getType();

        if (type == null) {
            return createFailedAssertionResult("UNKNOWN", assertion, "Assertion type cannot be null");
        }
        if (assertion.getOperator() == null) {
            return createFailedAssertionResult(type.name(), assertion, "Assertion operator cannot be null");
        }

        switch (type) {
            case RESPONSE_BODY:
                return validationEngine.validateBody(
                        response, assertion.getOperator(), assertion.getExpectedValue())
                        .getResults().get(0);
            case HEADER:
                return validationEngine.validateHeader(
                        response, assertion.getField(), assertion.getOperator(), assertion.getExpectedValue())
                        .getResults().get(0);
            case STATUS_CODE:
                return executeStatusCodeAssertion(response, assertion);
            default:
                return createFailedAssertionResult(
                        type.name(), assertion, "Unsupported assertion type: " + type);
        }
    }

    private ValidationResultDto executeStatusCodeAssertion(
            ResponseDto response,
            AssertionDto assertion) {
        String expectedValue = assertion.getExpectedValue();
        int expectedStatusCode;
        try {
            expectedStatusCode = Integer.parseInt(expectedValue);
        } catch (NumberFormatException e) {
            return createFailedAssertionResult(
                    AssertionType.STATUS_CODE.name(),
                    assertion,
                    "Invalid expected status code: " + expectedValue);
        }

        return validationEngine.validateStatusCode(response, expectedStatusCode)
                .getResults().get(0);
    }

    private ValidationResultDto createFailedAssertionResult(
            String validationType,
            AssertionDto assertion,
            String message) {
        ValidationResultDto result = new ValidationResultDto();
        result.setPassed(false);
        result.setValidationType(validationType);
        result.setField(assertion.getField());
        result.setExpected(assertion.getExpectedValue());
        result.setActual("");
        result.setMessage(message);
        return result;
    }

    private void addResult(ValidationSummaryDto summary, ValidationResultDto result) {
        summary.getResults().add(result);
        summary.setTotal(summary.getTotal() + 1);
        if (result.isPassed()) {
            summary.setPassedCount(summary.getPassedCount() + 1);
        } else {
            summary.setFailedCount(summary.getFailedCount() + 1);
        }
        summary.setPassed(summary.getFailedCount() == 0);
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
        private AiFailureAnalysis aiFailureAnalysis;
    }
}
