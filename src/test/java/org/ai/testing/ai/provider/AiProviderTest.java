package org.ai.testing.ai.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiProviderTest {

    @Test
    void shouldCreateDeterministicDefaultProvider() {
        AiProvider provider = AiProviderFactory.createDefault();

        assertEquals("heuristic", provider.getProviderName());
        assertEquals("heuristic-ai-v1", provider.getModelName());
    }

    @Test
    void shouldGenerateHeuristicResponse() {
        AiProviderRequest request = new AiProviderRequest();
        request.setUserPrompt("Analyze this API test failure");

        AiProviderResponse response =
                new HeuristicAiProvider().generate(request);

        assertTrue(response.isSuccessful());
        assertEquals("heuristic", response.getProvider());
        assertFalse(response.getContent().isBlank());
    }

    @Test
    void shouldRejectBlankPrompt() {
        AiProviderRequest request = new AiProviderRequest();

        AiProviderResponse response =
                new HeuristicAiProvider().generate(request);

        assertFalse(response.isSuccessful());
        assertEquals("heuristic", response.getProvider());
        assertTrue(response.getErrorMessage().contains("prompt"));
    }

    @Test
    void shouldRejectNullRequest() {
        AiProviderResponse response =
                new HeuristicAiProvider().generate(null);

        assertFalse(response.isSuccessful());
        assertTrue(response.getErrorMessage().contains("request"));
    }

    @Test
    void shouldValidateHttpProviderConfiguration() {
        assertThrows(IllegalArgumentException.class, () ->
                AiProviderFactory.createHttpProvider(
                        "test", "model", "https://example.com", ""));
    }
}
