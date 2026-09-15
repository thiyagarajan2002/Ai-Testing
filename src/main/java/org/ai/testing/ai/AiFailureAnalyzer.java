package org.ai.testing.ai;

import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.ai.model.AiResponseAnalysis;
import org.ai.testing.dto.common.ResponseDto;

import java.util.Locale;
import java.util.Map;

/**
 * Produces deterministic failure explanations from an API response and its AI response analysis.
 */
public class AiFailureAnalyzer {

    public AiFailureAnalysis analyze(ResponseDto response,
                                     AiResponseAnalysis responseAnalysis,
                                     Integer expectedStatusCode) {
        if (response == null) {
            throw new IllegalArgumentException("API response is required");
        }
        if (responseAnalysis == null) {
            throw new IllegalArgumentException("response analysis is required");
        }

        AiFailureAnalysis result = new AiFailureAnalysis();
        result.setFailureDetected(!responseAnalysis.isHealthy());

        if (responseAnalysis.isHealthy()) {
            result.setSummary("No API failure was detected by the heuristic analysis.");
            result.setSeverity("INFO");
            result.setCategory("NONE");
            result.setLikelyRootCause("No failure indicators were detected.");
            return result;
        }

        int status = response.getStatusCode();
        if (status >= 500) {
            setServerFailure(result, response);
        } else if (status >= 400) {
            setClientFailure(result, response, expectedStatusCode);
        } else if (hasJsonFormatMismatch(response)) {
            setFormatFailure(result, response);
        } else {
            setGenericFailure(result, response, expectedStatusCode);
        }

        if (response.getResponseTimeMs() > 0) {
            result.getEvidence().add("Response time: " + response.getResponseTimeMs() + " ms.");
        }
        return result;
    }

    private void setServerFailure(AiFailureAnalysis result, ResponseDto response) {
        result.setSeverity("CRITICAL");
        result.setCategory("SERVER_ERROR");
        result.setSummary("The API failed with a server-side error.");
        result.setLikelyRootCause("The endpoint or one of its downstream dependencies likely failed while processing the request.");
        result.getEvidence().add("HTTP status was " + response.getStatusCode() + ".");
        result.getRecommendations().add("Inspect application and server logs for the failing endpoint.");
        result.getRecommendations().add("Check database, downstream service, and infrastructure dependencies.");
    }

    private void setClientFailure(AiFailureAnalysis result,
                                  ResponseDto response,
                                  Integer expectedStatusCode) {
        result.setSeverity("HIGH");
        result.setCategory("CLIENT_ERROR");
        result.setSummary("The request was rejected with a client-side HTTP error.");
        result.setLikelyRootCause("Request data, authentication, authorization, parameters, or endpoint expectations may not match the API contract.");
        result.getEvidence().add("HTTP status was " + response.getStatusCode() + ".");
        if (expectedStatusCode != null) {
            result.getEvidence().add("Expected HTTP status was " + expectedStatusCode + ".");
        }
        result.getRecommendations().add("Verify request URL, path parameters, query parameters, and request body.");
        result.getRecommendations().add("Verify authentication and authorization requirements.");
    }

    private void setFormatFailure(AiFailureAnalysis result, ResponseDto response) {
        result.setSeverity("HIGH");
        result.setCategory("RESPONSE_FORMAT");
        result.setSummary("The response format does not match its Content-Type declaration.");
        result.setLikelyRootCause("The endpoint may be returning incorrectly serialized content or an incorrect Content-Type header.");
        result.getEvidence().add("Content-Type indicates application/json but the body does not look like JSON.");
        result.getRecommendations().add("Validate response serialization in the API implementation.");
        result.getRecommendations().add("Verify that the Content-Type header matches the actual response body.");
    }

    private void setGenericFailure(AiFailureAnalysis result,
                                   ResponseDto response,
                                   Integer expectedStatusCode) {
        result.setSeverity("MEDIUM");
        result.setCategory("API_CONTRACT");
        result.setSummary("The API response did not satisfy the expected health conditions.");
        result.setLikelyRootCause("The endpoint response differs from the expected API contract or configured test expectations.");
        result.getEvidence().add("HTTP status was " + response.getStatusCode() + ".");
        if (expectedStatusCode != null && response.getStatusCode() != expectedStatusCode) {
            result.getEvidence().add("Expected HTTP status was " + expectedStatusCode + ".");
        }
        result.getRecommendations().add("Compare the response with the API contract and expected test data.");
        result.getRecommendations().add("Review the generated assertions and expected status code.");
    }

    private boolean hasJsonFormatMismatch(ResponseDto response) {
        String contentType = findHeader(response.getHeaders(), "Content-Type");
        String body = response.getBody();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains("application/json")
                && body != null
                && !body.isBlank()
                && !looksLikeJson(body);
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
}
