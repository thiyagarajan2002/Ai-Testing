package org.ai.testing.ai.provider;

import java.util.Locale;

public class HeuristicAiProvider implements AiProvider {

    public static final String PROVIDER_NAME = "heuristic";
    public static final String MODEL_NAME = "heuristic-ai-v1";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public AiProviderResponse generate(AiProviderRequest request) {
        long start = System.currentTimeMillis();

        if (request == null) {
            return AiProviderResponse.failure(
                    PROVIDER_NAME,
                    MODEL_NAME,
                    "AI provider request cannot be null",
                    elapsed(start));
        }

        String prompt = request.getUserPrompt();
        if (prompt == null || prompt.isBlank()) {
            return AiProviderResponse.failure(
                    PROVIDER_NAME,
                    MODEL_NAME,
                    "AI user prompt cannot be blank",
                    elapsed(start));
        }

        String normalized = prompt.toLowerCase(Locale.ROOT);
        String content;

        if (normalized.contains("failure") || normalized.contains("error")) {
            content = "Review the HTTP status, response body, headers, and validation failures before investigating downstream dependencies.";
        } else if (normalized.contains("test")) {
            content = "Validate the expected status, response body, important headers, and negative input behavior.";
        } else {
            content = "Analyze the supplied API context using observable request and response evidence.";
        }

        return AiProviderResponse.success(
                PROVIDER_NAME,
                MODEL_NAME,
                content,
                elapsed(start));
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
