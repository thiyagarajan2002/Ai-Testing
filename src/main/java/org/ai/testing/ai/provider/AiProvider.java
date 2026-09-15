package org.ai.testing.ai.provider;

public interface AiProvider {

    String getProviderName();

    String getModelName();

    AiProviderResponse generate(AiProviderRequest request);
}
