package org.ai.testing.ai;

import org.ai.testing.ai.model.AiNegativeTestData;
import org.ai.testing.ai.model.AiNegativeTestDataResult;
import org.ai.testing.ai.model.AiTestGenerationRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiNegativeTestDataGeneratorTest {

    @Test
    void shouldGenerateNegativeJsonScenarios() {
        AiTestGenerationRequest request = new AiTestGenerationRequest();
        request.setMethod("POST");
        request.setUrl("https://example.com/users");
        request.setRequestBody("{\"name\":\"John\",\"age\":30}");
        request.setHeaders(Map.of("Content-Type", "application/json"));

        AiNegativeTestDataResult result =
                new AiNegativeTestDataGenerator().generate(request);

        assertTrue(result.getTestData().size() >= 5);
        assertTrue(hasScenario(result, "INVALID_FIELD_TYPE"));
        assertTrue(hasScenario(result, "MISSING_REQUIRED_FIELD"));
        assertTrue(hasScenario(result, "EMPTY_REQUEST_BODY"));
        assertTrue(hasScenario(result, "UNSUPPORTED_CONTENT_TYPE"));
        assertTrue(hasScenario(result, "MISSING_CONTENT_TYPE"));
    }

    @Test
    void shouldGenerateOnlyRelevantBodyCaseForGet() {
        AiTestGenerationRequest request = new AiTestGenerationRequest();
        request.setMethod("GET");
        request.setUrl("https://example.com/users/1");

        AiNegativeTestDataResult result =
                new AiNegativeTestDataGenerator().generate(request);

        assertTrue(hasScenario(result, "UNSUPPORTED_CONTENT_TYPE"));
        assertFalse(hasScenario(result, "EMPTY_REQUEST_BODY"));
    }

    @Test
    void shouldRemoveContentTypeForMissingHeaderScenario() {
        AiTestGenerationRequest request = new AiTestGenerationRequest();
        request.setMethod("POST");
        request.setUrl("https://example.com/users");
        request.setRequestBody("{\"name\":\"John\"}");
        request.setHeaders(Map.of("Content-Type", "application/json", "Accept", "application/json"));

        AiNegativeTestDataResult result =
                new AiNegativeTestDataGenerator().generate(request);

        AiNegativeTestData data = result.getTestData().stream()
                .filter(item -> "MISSING_CONTENT_TYPE".equals(item.getScenario()))
                .findFirst()
                .orElseThrow();

        assertFalse(data.getHeaders().keySet().stream()
                .anyMatch(key -> key.equalsIgnoreCase("Content-Type")));
        assertEquals("application/json", data.getHeaders().get("Accept"));
    }

    @Test
    void shouldRejectMissingRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiNegativeTestDataGenerator().generate(null));
    }

    @Test
    void shouldRejectMissingUrl() {
        AiTestGenerationRequest request = new AiTestGenerationRequest();

        assertThrows(IllegalArgumentException.class,
                () -> new AiNegativeTestDataGenerator().generate(request));
    }

    private boolean hasScenario(AiNegativeTestDataResult result, String scenario) {
        return result.getTestData().stream()
                .anyMatch(item -> scenario.equals(item.getScenario()));
    }
}
