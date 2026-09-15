package org.ai.testing.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpAiProvider implements AiProvider {

    private final String providerName;
    private final String modelName;
    private final URI endpoint;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public HttpAiProvider(
            String providerName,
            String modelName,
            URI endpoint,
            String apiKey) {
        this(providerName, modelName, endpoint, apiKey,
                HttpClient.newHttpClient(), new ObjectMapper(), Duration.ofSeconds(30));
    }

    public HttpAiProvider(
            String providerName,
            String modelName,
            URI endpoint,
            String apiKey,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Duration timeout) {
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("providerName is required");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName is required");
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        if (httpClient == null || objectMapper == null || timeout == null) {
            throw new IllegalArgumentException("HTTP provider dependencies are required");
        }
        this.providerName = providerName;
        this.modelName = modelName;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.timeout = timeout;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public AiProviderResponse generate(AiProviderRequest request) {
        long start = System.currentTimeMillis();
        if (request == null) {
            return failure("AI provider request cannot be null", start);
        }
        if (request.getUserPrompt() == null || request.getUserPrompt().isBlank()) {
            return failure("AI user prompt cannot be blank", start);
        }

        try {
            String payload = objectMapper.createObjectNode()
                    .put("model", modelName)
                    .put("systemPrompt", request.getSystemPrompt())
                    .put("userPrompt", request.getUserPrompt())
                    .put("temperature", request.getTemperature())
                    .toString();

            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return failure("Provider returned HTTP " + response.statusCode(), start);
            }

            String content = extractContent(response.body());
            return AiProviderResponse.success(
                    providerName, modelName, content, elapsed(start));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure("AI provider request was interrupted", start);
        } catch (IOException | RuntimeException e) {
            return failure("AI provider request failed: " + e.getMessage(), start);
        }
    }

    private String extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("content");
        if (!content.isMissingNode() && !content.isNull()) {
            return content.asText();
        }
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).path("message").path("content");
            if (!message.isMissingNode() && !message.isNull()) {
                return message.asText();
            }
        }
        return responseBody;
    }

    private AiProviderResponse failure(String message, long start) {
        return AiProviderResponse.failure(
                providerName, modelName, message, elapsed(start));
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
