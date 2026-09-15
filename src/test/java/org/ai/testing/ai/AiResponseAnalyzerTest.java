package org.ai.testing.ai;

import org.ai.testing.ai.model.AiResponseAnalysis;
import org.ai.testing.ai.model.AiResponseAnalysisRequest;
import org.ai.testing.dto.common.ResponseDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiResponseAnalyzerTest {

    private final AiResponseAnalyzer analyzer = new AiResponseAnalyzer();

    @Test
    void shouldAcceptHealthyJsonResponse() {
        ResponseDto response = response(200, "{\"id\":1}", 120,
                Map.of("Content-Type", "application/json"));
        AiResponseAnalysisRequest request = new AiResponseAnalysisRequest();
        request.setResponse(response);
        request.setExpectedStatusCode(200);

        AiResponseAnalysis result = analyzer.analyze(request);

        assertTrue(result.isHealthy());
        assertEquals("INFO", result.getSeverity());
        assertTrue(result.getFindings().isEmpty());
    }

    @Test
    void shouldDetectUnexpectedStatus() {
        ResponseDto response = response(404, "{\"error\":\"not found\"}", 100, Map.of());
        AiResponseAnalysisRequest request = new AiResponseAnalysisRequest();
        request.setResponse(response);
        request.setExpectedStatusCode(200);

        AiResponseAnalysis result = analyzer.analyze(request);

        assertFalse(result.isHealthy());
        assertTrue(result.getFindings().stream().anyMatch(value -> value.contains("Expected HTTP status 200")));
    }

    @Test
    void shouldDetectInvalidJsonBody() {
        ResponseDto response = response(200, "not-json", 100,
                Map.of("Content-Type", "application/json"));
        AiResponseAnalysisRequest request = new AiResponseAnalysisRequest();
        request.setResponse(response);
        request.setExpectedStatusCode(200);

        AiResponseAnalysis result = analyzer.analyze(request);

        assertFalse(result.isHealthy());
        assertTrue(result.getFindings().stream().anyMatch(value -> value.contains("does not look like JSON")));
    }

    @Test
    void shouldDetectSlowResponse() {
        ResponseDto response = response(200, "{}", 3000, Map.of());
        AiResponseAnalysisRequest request = new AiResponseAnalysisRequest();
        request.setResponse(response);
        request.setSlowResponseThresholdMs(1000);

        AiResponseAnalysis result = analyzer.analyze(request);

        assertTrue(result.isHealthy());
        assertTrue(result.getFindings().stream().anyMatch(value -> value.contains("slow-response threshold")));
    }

    @Test
    void shouldRejectMissingResponse() {
        AiResponseAnalysisRequest request = new AiResponseAnalysisRequest();
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(request));
    }

    private ResponseDto response(int status, String body, long time, Map<String, String> headers) {
        ResponseDto response = new ResponseDto();
        response.setStatusCode(status);
        response.setBody(body);
        response.setResponseTimeMs(time);
        response.setHeaders(headers);
        return response;
    }
}
