package org.ai.testing.ai;

import org.ai.testing.ai.model.AiNegativeTestData;
import org.ai.testing.ai.model.AiNegativeTestDataResult;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts AI-generated negative test data into executable framework test cases.
 */
public class AiNegativeTestCaseBuilder {

    public List<TestCaseDto> build(
            TestCaseDto sourceTestCase,
            AiNegativeTestDataResult generatedData) {

        if (sourceTestCase == null) {
            throw new IllegalArgumentException("source test case is required");
        }
        if (generatedData == null) {
            throw new IllegalArgumentException("generated negative test data is required");
        }

        List<TestCaseDto> testCases = new ArrayList<>();
        if (generatedData.getTestData() == null) {
            return testCases;
        }

        int index = 1;
        for (AiNegativeTestData data : generatedData.getTestData()) {
            if (data == null) {
                continue;
            }

            TestCaseDto negativeTestCase = new TestCaseDto();
            String scenario = data.getScenario() == null || data.getScenario().isBlank()
                    ? "NEGATIVE"
                    : data.getScenario();

            negativeTestCase.setTestCaseId(
                    sourceTestCase.getTestCaseId() + "-AI-NEG-" + index);
            negativeTestCase.setTestCaseName(
                    sourceTestCase.getTestCaseName() + " - AI " + scenario);
            negativeTestCase.setDescription(data.getReason());
            negativeTestCase.setMethod(sourceTestCase.getMethod());
            negativeTestCase.setEnabled(sourceTestCase.isEnabled());

            BaseRequestDto sourceRequest = sourceTestCase.getRequest();
            BaseRequestDto request = new BaseRequestDto();
            if (sourceRequest != null) {
                request.setUrl(sourceRequest.getUrl());
                request.setQueryParams(copy(sourceRequest.getQueryParams()));
                request.setPathParams(copy(sourceRequest.getPathParams()));
            }
            request.setHeaders(copy(data.getHeaders()));

            if (data.getRequestBody() != null) {
                RequestBodyDto body = new RequestBodyDto();
                String contentType = findHeader(request.getHeaders(), "Content-Type");
                body.setContentType(contentType);
                body.setRawBody(data.getRequestBody());
                request.setBody(body);
            }

            negativeTestCase.setRequest(request);
            negativeTestCase.setExpectedStatusCode(data.getExpectedStatusCode());
            negativeTestCase.setAssertions(buildAssertions(data));
            testCases.add(negativeTestCase);
            index++;
        }

        return testCases;
    }

    private List<AssertionDto> buildAssertions(AiNegativeTestData data) {
        List<AssertionDto> assertions = new ArrayList<>();
        if (data.getExpectedStatusCode() != null) {
            AssertionDto assertion = new AssertionDto();
            assertion.setType(AssertionType.STATUS_CODE);
            assertion.setField("statusCode");
            assertion.setOperator(AssertionOperator.EQUALS);
            assertion.setExpectedValue(String.valueOf(data.getExpectedStatusCode()));
            assertions.add(assertion);
        }
        return assertions;
    }

    private Map<String, String> copy(Map<String, String> source) {
        return source == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(source);
    }

    private String findHeader(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
