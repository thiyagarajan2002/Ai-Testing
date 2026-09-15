package org.ai.testing.ai;

import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.ai.model.AiResponseAnalysis;
import org.ai.testing.dto.common.ResponseDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiFailureAnalyzerTest {

    @Test
    void shouldExplainServerFailure() {
        ResponseDto response = response(500, "internal error", "text/plain");
        AiResponseAnalysis analysis = new AiResponseAnalyzer().analyze(request(response, 200));

        AiFailureAnalysis result = new AiFailureAnalyzer().analyze(response, analysis, 200);

        assertTrue(result.isFailureDetected());
        assertEquals("SERVER_ERROR", result.getCategory());
        assertEquals("CRITICAL", result.getSeverity());
        assertTrue(result.getLikelyRootCause().contains("downstream"));
    }

    @Test
    void shouldExplainClientFailure() {
        ResponseDto response = response(400, "bad request", "application/json");
        AiResponseAnalysis analysis = new AiResponseAnalyzer().analyze(request(response, 200));

        AiFailureAnalysis result = new AiFailureAnalyzer().analyze(response, analysis, 200);

        assertTrue(result.isFailureDetected());
        assertEquals("CLIENT_ERROR", result.getCategory());
        assertEquals("HIGH", result.getSeverity());
        assertFalse(result.getRecommendations().isEmpty());
    }

    @Test
    void shouldExplainJsonFormatFailure() {
        ResponseDto response = response(200, "not-json", "application/json");
        AiResponseAnalysis analysis = new AiResponseAnalyzer().analyze(request(response, 200));

        AiFailureAnalysis result = new AiFailureAnalyzer().analyze(response, analysis, 200);

        assertEquals("RESPONSE_FORMAT", result.getCategory());
        assertTrue(result.getLikelyRootCause().contains("Content-Type"));
    }

    @Test
    void shouldReturnNoFailureForHealthyResponse() {
        ResponseDto response = response(200, "{\"id\":1}", "application/json");
        AiResponseAnalysis analysis = new AiResponseAnalyzer().analyze(request(response, 200));

        AiFailureAnalysis result = new AiFailureAnalyzer().analyze(response, analysis, 200);

        assertFalse(result.isFailureDetected());
        assertEquals("NONE", result.getCategory());
        assertEquals("INFO", result.getSeverity());
    }

    @Test
    void shouldRejectMissingInputs() {
        AiFailureAnalyzer analyzer = new AiFailureAnalyzer();
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(null, new AiResponseAnalysis(), 200));
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(new ResponseDto(), null, 200));
    }

    private AiResponseAnalysisRequest request(ResponseDto response, int expectedStatus) {
        AiResponseAnalysisRequest request = new AiResponseAnalysisRequest();
        request.setResponse(response);
        request.setExpectedStatusCode(expectedStatus);
        return request;
    }

    private ResponseDto response(int status, String body, String contentType) {
        ResponseDto response = new ResponseDto();
        response.setStatusCode(status);
        response.setBody(body);
        response.setResponseTimeMs(100);
        response.setHeaders(Map.of("Content-Type", contentType));
        return response;
    }
}
