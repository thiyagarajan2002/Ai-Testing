package org.ai.testing.executor.common;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;

public abstract class AbstractHttpExecutor<T extends BaseRequestDto>
        implements ApiExecutor<T> {

    private final HttpClient httpClient;
    private final RequestBuilder requestBuilder;

    protected AbstractHttpExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.requestBuilder = new RequestBuilder();
    }

    protected abstract String httpMethod();

    @Override
    public ResponseDto execute(T request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        try {
            String url = requestBuilder.buildUrl(request);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60));

            if (request.getHeaders() != null) {
                request.getHeaders().forEach((name, value) -> {
                    if (name != null && value != null) {
                        builder.header(name, value);
                    }
                });
            }

            String body = request.getBody() == null
                    || request.getBody().getRawBody() == null
                    ? ""
                    : request.getBody().getRawBody();

            if (request.getBody() != null
                    && request.getBody().getContentType() != null
                    && !request.getBody().getContentType().isBlank()
                    && (request.getHeaders() == null
                    || request.getHeaders().keySet().stream()
                    .noneMatch(key -> key.equalsIgnoreCase("Content-Type")))) {
                builder.header("Content-Type", request.getBody().getContentType());
            }

            HttpRequest.BodyPublisher publisher = body.isEmpty()
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);

            builder.method(httpMethod(), publisher);

            long start = System.nanoTime();
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();

            ResponseDto result = new ResponseDto();
            result.setStatusCode(response.statusCode());
            result.setStatusMessage("");
            result.setBody(response.body());
            result.setResponseTimeMs(elapsed);
            result.setHeaders(new HashMap<>());
            response.headers().map().forEach((name, values) ->
                    result.getHeaders().put(name,
                            String.join(", ", values)));
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "HTTP request interrupted: " + e.getMessage(), e);
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(
                    "HTTP request failed: " + e.getMessage(), e);
        }
    }
}
