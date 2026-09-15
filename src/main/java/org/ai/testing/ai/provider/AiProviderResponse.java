package org.ai.testing.ai.provider;

import lombok.Data;

@Data
public class AiProviderResponse {

    private boolean successful;
    private String provider;
    private String model;
    private String content;
    private String errorMessage;
    private long latencyMs;

    public static AiProviderResponse success(
            String provider,
            String model,
            String content,
            long latencyMs) {
        AiProviderResponse response = new AiProviderResponse();
        response.setSuccessful(true);
        response.setProvider(provider);
        response.setModel(model);
        response.setContent(content);
        response.setLatencyMs(latencyMs);
        return response;
    }

    public static AiProviderResponse failure(
            String provider,
            String model,
            String errorMessage,
            long latencyMs) {
        AiProviderResponse response = new AiProviderResponse();
        response.setSuccessful(false);
        response.setProvider(provider);
        response.setModel(model);
        response.setErrorMessage(errorMessage);
        response.setLatencyMs(latencyMs);
        return response;
    }
}
