package org.ai.testing.ai;

import org.ai.testing.ai.model.AiGeneratedTestSuite;
import org.ai.testing.ai.model.AiTestGenerationRequest;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiTestCaseGeneratorTest {

    @Test
    void shouldGeneratePositiveAndNegativeCases() {
        AiTestGenerationRequest request = new AiTestGenerationRequest();
        request.setMethod("GET");
        request.setUrl("https://example.test/users/1");
        request.setHeaders(Map.of("Accept", "application/json"));
        request.setExpectedStatusCode(200);

        AiGeneratedTestSuite suite = new AiTestCaseGenerator().generate(request);

        assertEquals("heuristic-ai-v1", suite.getGenerator());
        assertEquals(2, suite.getTestCases().size());
        assertEquals("GET", suite.getTestCases().get(0).getMethod());
        assertFalse(suite.getTestCases().get(0).getAssertions().isEmpty());
    }

    @Test
    void shouldRejectMissingUrl() {
        AiTestGenerationRequest request = new AiTestGenerationRequest();
        request.setMethod("GET");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new AiTestCaseGenerator().generate(request));

        assertEquals("API URL is required", error.getMessage());
    }

    @Test
    void shouldGenerateOnlyPositiveCaseWhenNegativeGenerationIsDisabled() {
        AiTestGenerationRequest request = new AiTestGenerationRequest();
        request.setMethod("POST");
        request.setUrl("https://example.test/users");
        request.setExpectedStatusCode(201);
        request.setIncludeNegativeCases(false);

        AiGeneratedTestSuite suite = new AiTestCaseGenerator().generate(request);

        assertEquals(1, suite.getTestCases().size());
        TestCaseDto testCase = suite.getTestCases().get(0);
        assertEquals(201, testCase.getExpectedStatusCode());
    }
}
