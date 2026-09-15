package org.ai.testing.ai;

import org.ai.testing.ai.model.AiNegativeTestDataResult;
import org.ai.testing.ai.model.AiTestGenerationRequest;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiNegativeTestCaseBuilderTest {

    @Test
    void shouldConvertGeneratedNegativeDataIntoExecutableTestCases() {
        TestCaseDto source = sourceTestCase();

        AiTestGenerationRequest generation = new AiTestGenerationRequest();
        generation.setUrl(source.getRequest().getUrl());
        generation.setMethod(source.getMethod());
        generation.setRequestBody(source.getRequest().getBody().getRawBody());
        generation.setHeaders(source.getRequest().getHeaders());

        AiNegativeTestDataResult generated =
                new AiNegativeTestDataGenerator().generate(generation);

        List<TestCaseDto> negativeCases =
                new AiNegativeTestCaseBuilder().build(source, generated);

        assertFalse(negativeCases.isEmpty());
        assertTrue(negativeCases.stream()
                .allMatch(testCase -> testCase.getTestCaseId().startsWith("TC-001-AI-NEG-")));
        assertTrue(negativeCases.stream()
                .allMatch(testCase -> "POST".equals(testCase.getMethod())));
        assertTrue(negativeCases.stream()
                .allMatch(testCase -> testCase.getExpectedStatusCode() != null));
        assertTrue(negativeCases.stream()
                .allMatch(testCase -> testCase.getAssertions().stream()
                        .anyMatch(assertion -> assertion.getType() == AssertionType.STATUS_CODE
                                && assertion.getOperator() == AssertionOperator.EQUALS)));
    }

    @Test
    void shouldPreserveGeneratedHeadersAndBodyWithoutMutatingSource() {
        TestCaseDto source = sourceTestCase();

        AiTestGenerationRequest generation = new AiTestGenerationRequest();
        generation.setUrl(source.getRequest().getUrl());
        generation.setMethod(source.getMethod());
        generation.setRequestBody(source.getRequest().getBody().getRawBody());
        generation.setHeaders(source.getRequest().getHeaders());

        AiNegativeTestDataResult generated =
                new AiNegativeTestDataGenerator().generate(generation);

        List<TestCaseDto> negativeCases =
                new AiNegativeTestCaseBuilder().build(source, generated);

        assertEquals("application/json", source.getRequest().getHeaders().get("Content-Type"));
        assertEquals("application/json", negativeCases.get(0).getRequest().getHeaders().get("Content-Type"));
        assertNotNull(negativeCases.get(0).getRequest().getBody());
    }

    private static TestCaseDto sourceTestCase() {
        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("TC-001");
        testCase.setTestCaseName("Create User");
        testCase.setMethod("POST");
        testCase.setEnabled(true);

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("http://localhost:8080/users");
        request.getHeaders().put("Content-Type", "application/json");

        org.ai.testing.dto.common.RequestBodyDto body =
                new org.ai.testing.dto.common.RequestBodyDto();
        body.setContentType("application/json");
        body.setRawBody("{\"name\":\"Thiyagarajan\",\"age\":25}");
        request.setBody(body);
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(201);
        return testCase;
    }
}
