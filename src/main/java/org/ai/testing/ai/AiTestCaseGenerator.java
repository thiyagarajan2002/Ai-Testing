package org.ai.testing.ai;

import org.ai.testing.ai.model.AiGeneratedTestSuite;
import org.ai.testing.ai.model.AiTestGenerationRequest;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates executable API test cases from a compact API description.
 *
 * Phase 2 starts with deterministic AI-style heuristics. This keeps the framework
 * offline and reproducible while exposing a stable service boundary for a future
 * LLM provider.
 */
public class AiTestCaseGenerator {

    public AiGeneratedTestSuite generate(AiTestGenerationRequest input) {
        validate(input);

        AiGeneratedTestSuite suite = new AiGeneratedTestSuite();
        List<TestCaseDto> cases = suite.getTestCases();

        cases.add(createPositiveCase(input));

        if (input.isIncludeNegativeCases()) {
            cases.add(createNegativeStatusCase(input));
        }

        return suite;
    }

    private TestCaseDto createPositiveCase(AiTestGenerationRequest input) {
        TestCaseDto testCase = baseCase(input, "Positive", input.getExpectedStatusCode());
        List<AssertionDto> assertions = new ArrayList<>();

        if (input.getExpectedStatusCode() != null) {
            assertions.add(assertion(
                    AssertionType.STATUS_CODE,
                    "statusCode",
                    AssertionOperator.EQUALS,
                    String.valueOf(input.getExpectedStatusCode())));
        }

        if (input.isIncludeBodyAssertions() && input.getRequestBody() != null && !input.getRequestBody().isBlank()) {
            assertions.add(assertion(
                    AssertionType.RESPONSE_BODY,
                    "body",
                    AssertionOperator.NOT_EMPTY,
                    "true"));
        }

        if (input.isIncludeHeaderAssertions()) {
            for (String header : input.getHeaders().keySet()) {
                if ("accept".equalsIgnoreCase(header)) {
                    assertions.add(assertion(
                            AssertionType.HEADER,
                            "Content-Type",
                            AssertionOperator.NOT_EMPTY,
                            "true"));
                    break;
                }
            }
        }

        testCase.setAssertions(assertions);
        return testCase;
    }

    private TestCaseDto createNegativeStatusCase(AiTestGenerationRequest input) {
        int negativeStatus = input.getExpectedStatusCode() != null && input.getExpectedStatusCode() >= 400
                ? input.getExpectedStatusCode()
                : 400;

        TestCaseDto testCase = baseCase(input, "Negative Status", negativeStatus);
        testCase.setDescription("AI generated negative-path case. Expected status is used as a configurable baseline.");
        testCase.setAssertions(List.of(assertion(
                AssertionType.STATUS_CODE,
                "statusCode",
                AssertionOperator.EQUALS,
                String.valueOf(negativeStatus))));
        return testCase;
    }

    private TestCaseDto baseCase(AiTestGenerationRequest input, String suffix, Integer expectedStatus) {
        TestCaseDto testCase = new TestCaseDto();
        String prefix = input.getTestCaseIdPrefix();
        testCase.setTestCaseId(prefix + "-" + suffix.replace(' ', '-').toUpperCase(Locale.ROOT));
        testCase.setTestCaseName(input.getTestCaseNamePrefix() + " - " + suffix);
        testCase.setDescription(input.getDescription());
        testCase.setMethod(input.getMethod().toUpperCase(Locale.ROOT));
        testCase.setEnabled(true);

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(input.getUrl());
        if (input.getHeaders() != null) {
            request.setHeaders(new java.util.LinkedHashMap<>(input.getHeaders()));
        }
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(expectedStatus);
        return testCase;
    }

    private AssertionDto assertion(AssertionType type, String field, AssertionOperator operator, String expected) {
        AssertionDto dto = new AssertionDto();
        dto.setType(type);
        dto.setField(field);
        dto.setOperator(operator);
        dto.setExpectedValue(expected);
        return dto;
    }

    private void validate(AiTestGenerationRequest input) {
        if (input == null) {
            throw new IllegalArgumentException("AI generation request must not be null");
        }
        if (input.getMethod() == null || input.getMethod().isBlank()) {
            throw new IllegalArgumentException("API method is required");
        }
        if (input.getUrl() == null || input.getUrl().isBlank()) {
            throw new IllegalArgumentException("API URL is required");
        }
        if (input.getHeaders() == null) {
            input.setHeaders(new java.util.LinkedHashMap<>());
        }
    }
}
