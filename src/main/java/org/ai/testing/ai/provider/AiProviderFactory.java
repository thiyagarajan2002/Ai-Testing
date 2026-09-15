package org.ai.testing.ai.provider;

import java.net.URI;

public final class AiProviderFactory {

    private AiProviderFactory() {
    }

    public static AiProvider createDefault() {
        return new HeuristicAiProvider();
    }

    public static AiProvider createHttpProvider(
            String providerName,
            String modelName,
            String endpoint,
            String apiKey) {
        return new HttpAiProvider(
                providerName,
                modelName,
                URI.create(endpoint),
                apiKey);
    }
}
