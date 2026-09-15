package org.ai.testing.ai;

import org.ai.testing.ai.model.AiResponseAnalysis;
import org.ai.testing.ai.model.AiResponseAnalysisRequest;
import org.ai.testing.dto.common.ResponseDto;

import java.util.Locale;
import java.util.Map;

/**
 * Performs deterministic AI-style analysis of an API response.
 * The service is provider-neutral and does not require an external AI API.
 */
public class AiResponseAnalyzer {

    public AiResponseAnalysis analyze(AiResponseAnalysisRequest input) {
        validate(input);

        ResponseDto response = input.getResponse();
        AiResponseAnalysis result = new AiResponseAnalysis();
        result.setStatusCode(response.getStatusCode());

        boolean healthy = true;

        if (input.getExpectedStatusCode() != null
                && response.getStatusCode() != input.getExpectedStatusCode()) {
            healthy = false;
            result.getFindings().add("Expected HTTP status " + input.getExpectedStatusCode()
                    + " but received " + response.getStatusCode() + ".");
            result.getSuggestions().add("Review the API behavior, request data, and expected status assertion.");
        }

        if (response.getStatusCode() >= 500) {
            healthy = false;
            result.getFindings().add("The API returned a server-side 5xx response.");
            result.getSuggestions().add("Inspect server logs and dependency failures for the endpoint.");
        } else if (response.getStatusCode() >= 400) {
            healthy = false;
            result.getFindings().add("The API returned a client-error 4xx response.");
            result.getSuggestions().add("Verify authentication, authorization, parameters, and request payload.");
        }

        String body = response.getBody();
        if (body == null || body.isBlank()) {
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                result.getFindings().add("Successful response contains an empty body.");
                result.getSuggestions().add("Confirm whether an empty response body is valid for this endpoint.");
            }
        }

        String contentType = findHeader(response.getHeaders(), "Content-Type");
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("application/json")
                && body != null && !body.isBlank() && !looksLikeJson(body)) {
            healthy = false;
            result.getFindings().add("Content-Type indicates JSON, but the response body does not look like JSON.");
            result.getSuggestions().add("Validate the response serialization and Content-Type header.");
        }

        if (input.getSlowResponseThresholdMs() > 0
                && response.getResponseTimeMs() > input.getSlowResponseThresholdMs()) {
            result.getFindings().add("Response time exceeded the configured slow-response threshold.");
            result.getSuggestions().add("Investigate endpoint latency, database queries, and downstream services.");
        }

        result.setHealthy(healthy);
        result.setSeverity(determineSeverity(result));
        result.setSummary(buildSummary(result));
        return result;
    }

    private String findHeader(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean looksLikeJson(String body) {
        String value = body.trim();
        return (value.startsWith("{") && value.endsWith("}"))
                || (value.startsWith("[") && value.endsWith("]"));
    }

    private String determineSeverity(AiResponseAnalysis result) {
        if (result.getFindings().isEmpty()) {
            return "INFO";
        }
        for (String finding : result.getFindings()) {
            if (finding.contains("5xx") || finding.contains("does not look like JSON")) {
                return "HIGH";
            }
        }
        return result.isHealthy() ? "MEDIUM" : "HIGH";
    }

    private String buildSummary(AiResponseAnalysis result) {
        if (result.isHealthy() && result.getFindings().isEmpty()) {
            return "API response passed heuristic health analysis.";
        }
        if (result.isHealthy()) {
            return "API response is acceptable, but improvement findings were detected.";
        }
        return "API response requires investigation based on heuristic findings.";
    }

    private void validate(AiResponseAnalysisRequest input) {
        if (input == null) {
            throw new IllegalArgumentException("AI response analysis request must not be null");
        }
        if (input.getResponse() == null) {
            throw new IllegalArgumentException("API response is required for analysis");
        }
    }
}
