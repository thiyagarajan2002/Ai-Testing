package org.ai.testing.ai.provider;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AiProviderRequest {

    private String systemPrompt;
    private String userPrompt;
    private double temperature = 0.0;
    private Map<String, String> metadata = new LinkedHashMap<>();
}
