package org.ai.testing.ai.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Input used by the Phase 2 AI test case generator.
 * The model is intentionally provider-neutral so a real LLM can be added later.
 */
@Data
public class AiTestGenerationRequest {
    private String testCaseIdPrefix = "AI-TC";
    private String testCaseNamePrefix = "AI Generated";
    private String description;
    private String method;
    private String url;
    private Map<String, String> headers = new LinkedHashMap<>();
    private String requestBody;
    private String contentType;
    private Integer expectedStatusCode;
    private boolean includeNegativeCases = true;
    private boolean includeHeaderAssertions = true;
    private boolean includeBodyAssertions = true;
}
