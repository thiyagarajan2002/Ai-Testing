package org.ai.testing.regression;

import org.ai.testing.ai.AiAssertionSuggester;
import org.ai.testing.ai.AiFailureAnalyzer;
import org.ai.testing.ai.AiNegativeTestDataGenerator;
import org.ai.testing.ai.AiResponseAnalyzer;
import org.ai.testing.ai.AiTestCaseGenerator;
import org.ai.testing.ai.model.AiResponseAnalysis;
import org.ai.testing.ai.model.AiResponseAnalysisRequest;
import org.ai.testing.ai.model.AiTestGenerationRequest;
import org.ai.testing.ai.provider.AiProvider;
import org.ai.testing.ai.provider.AiProviderFactory;
import org.ai.testing.ai.provider.AiProviderRequest;
import org.ai.testing.ai.provider.AiProviderResponse;
import org.ai.testing.dto.common.ResponseDto;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Phase2RegressionTest {

    @Test
    void shouldRunCompletePhase2AiRegressionFlow() {
        AiTestGenerationRequest generation = new AiTestGenerationRequest();
        generation.setMethod("GET");
        generation.setUrl("https://petstore3.swagger.io/api/v3/openapi.json");
        generation.setHeaders(new LinkedHashMap<>(Map.of("Accept", "application/json")));
        generation.setExpectedStatusCode(200);
        generation.setIncludeNegativeCases(true);
        generation.setIncludeBodyAssertions(true);
        generation.setIncludeHeaderAssertions(true);

        var generated = new AiTestCaseGenerator().generate(generation);
        assertEquals("heuristic-ai-v1", generated.getGenerator());
        assertEquals(2, generated.getTestCases().size());
        assertFalse(generated.getTestCases().get(0).getAssertions().isEmpty());

        ResponseDto response = new ResponseDto();
        response.setStatusCode(200);
        response.setBody("{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"Swagger Petstore\"}}");
        response.setResponseTimeMs(150);
        response.setHeaders(new LinkedHashMap<>(Map.of("Content-Type", "application/json")));

        AiResponseAnalysisRequest analysisRequest = new AiResponseAnalysisRequest();
        analysisRequest.setResponse(response);
        analysisRequest.setExpectedStatusCode(200);
        analysisRequest.setSlowResponseThresholdMs(2000);

        AiResponseAnalysis analysis = new AiResponseAnalyzer().analyze(analysisRequest);
        assertTrue(analysis.isHealthy());
        assertEquals("INFO", analysis.getSeverity());

        var suggestions = new AiAssertionSuggester().suggest(response, 200);
        assertFalse(suggestions.getSuggestions().isEmpty());

        var negativeData = new AiNegativeTestDataGenerator().generate(generation);
        assertFalse(negativeData.getTestData().isEmpty());

        var failure = new AiFailureAnalyzer().analyze(response, analysis, 200);
        assertFalse(failure.isFailureDetected());
        assertEquals("NONE", failure.getCategory());

        AiProvider provider = AiProviderFactory.createDefault();
        AiProviderRequest providerRequest = new AiProviderRequest();
        providerRequest.setSystemPrompt("Analyze API regression");
        providerRequest.setUserPrompt("Check the Petstore response");
        AiProviderResponse providerResponse = provider.generate(providerRequest);

        assertTrue(providerResponse.isSuccessful());
        assertEquals("heuristic", provider.getProviderName());
        assertEquals("heuristic-ai-v1", provider.getModelName());
        assertNotNull(providerResponse.getContent());
    }
}
