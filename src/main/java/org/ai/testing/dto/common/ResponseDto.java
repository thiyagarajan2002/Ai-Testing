package org.ai.testing.dto.common;

import java.util.LinkedHashMap;
import java.util.Map;

/** Everything captured from one HTTP exchange. */
public class ResponseDto {

    private int statusCode;
    private String statusMessage = "";
    private String body = "";
    private long responseTimeMs;
    private long bodySizeBytes;
    private int attempts = 1;
    private String finalUrl;
    private Map<String, String> headers = new LinkedHashMap<>();

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public long getBodySizeBytes() {
        return bodySizeBytes;
    }

    public void setBodySizeBytes(long bodySizeBytes) {
        this.bodySizeBytes = bodySizeBytes;
    }

    /** How many times the request was sent, including retries. */
    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    /** The URL that actually answered, after any redirects. */
    public String getFinalUrl() {
        return finalUrl;
    }

    public void setFinalUrl(String finalUrl) {
        this.finalUrl = finalUrl;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<>() : headers;
    }

    /** Case-insensitive header lookup; returns {@code null} when absent. */
    public String header(String name) {
        if (name == null || headers == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    @Override
    public String toString() {
        return statusCode + " " + statusMessage + " (" + responseTimeMs + " ms)";
    }
}
