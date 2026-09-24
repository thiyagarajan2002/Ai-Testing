package org.ai.testing.executor.common;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.util.HttpStatus;
import org.ai.testing.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared transport for every HTTP verb.
 *
 * <p>Improvements over the earlier version:</p>
 * <ul>
 *   <li>one {@link HttpClient} per {@link ExecutionOptions} instead of a fresh
 *       client for every executor instance, which previously meant a new
 *       connection pool and TLS context per test case;</li>
 *   <li>configurable retries with a delay, covering transport failures and
 *       retryable status codes;</li>
 *   <li>the reason phrase, byte size, attempt count and post-redirect URL are
 *       captured, so reports are no longer missing the status message;</li>
 *   <li>restricted headers are dropped instead of throwing, and an oversized
 *       body is truncated rather than held in memory in full.</li>
 * </ul>
 */
public abstract class AbstractHttpExecutor<T extends BaseRequestDto>
        implements ApiExecutor<T> {

    private final ExecutionOptions options;
    private final HttpClient httpClient;
    private final RequestBuilder requestBuilder = new RequestBuilder();

    protected AbstractHttpExecutor() {
        this(ExecutionOptions.defaults());
    }

    protected AbstractHttpExecutor(ExecutionOptions options) {
        this.options = options == null ? ExecutionOptions.defaults() : options;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.options.connectTimeout())
                .followRedirects(this.options.followRedirects()
                        ? HttpClient.Redirect.NORMAL
                        : HttpClient.Redirect.NEVER)
                .build();
    }

    /** The HTTP verb sent on the wire. */
    protected abstract String httpMethod();

    @Override
    public String method() {
        return httpMethod();
    }

    public ExecutionOptions options() {
        return options;
    }

    @Override
    public ResponseDto execute(T request) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        String url = requestBuilder.buildUrl(request);
        URI uri = toUri(url);
        HttpRequest httpRequest = buildHttpRequest(request, uri);

        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= options.totalAttempts(); attempt++) {
            long start = System.nanoTime();
            try {
                HttpResponse<String> response = httpClient.send(
                        httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                ResponseDto result = toResponseDto(response, elapsedMs, attempt);

                boolean canRetry = attempt < options.totalAttempts()
                        && HttpStatus.isRetryable(result.getStatusCode());
                if (!canRetry) {
                    return result;
                }
                pauseBeforeRetry();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApiExecutionException(
                        "HTTP request interrupted: " + describe(uri), e);

            } catch (IOException e) {
                lastFailure = new ApiExecutionException(
                        "HTTP request failed after attempt " + attempt + " for "
                                + describe(uri) + ": " + rootMessage(e), e);
                if (attempt >= options.totalAttempts()) {
                    break;
                }
                pauseBeforeRetry();
            }
        }

        throw lastFailure != null
                ? lastFailure
                : new ApiExecutionException("HTTP request failed: " + describe(uri));
    }

    // ------------------------------------------------------------------
    // Request assembly
    // ------------------------------------------------------------------

    private HttpRequest buildHttpRequest(T request, URI uri) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(options.requestTimeout());

        Map<String, String> headers = request.getHeaders();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (Strings.isBlank(name) || value == null) {
                    return;
                }
                if (RequestNormalizer.isRestrictedHeader(name)) {
                    // The JDK client manages these; setting them throws.
                    return;
                }
                builder.header(name, value);
            });
        }

        String body = request.getBody() == null
                ? null
                : request.getBody().getRawBody();

        HttpRequest.BodyPublisher publisher = (body == null || body.isEmpty())
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

        builder.method(httpMethod(), publisher);
        return builder.build();
    }

    private URI toUri(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new ApiExecutionException("Invalid request URL: " + url, e);
        }
    }

    // ------------------------------------------------------------------
    // Response capture
    // ------------------------------------------------------------------

    private ResponseDto toResponseDto(HttpResponse<String> response,
                                      long elapsedMs, int attempt) {

        ResponseDto result = new ResponseDto();
        result.setStatusCode(response.statusCode());
        result.setStatusMessage(HttpStatus.reasonPhrase(response.statusCode()));
        result.setResponseTimeMs(elapsedMs);
        result.setAttempts(attempt);
        result.setFinalUrl(response.uri() == null ? null : response.uri().toString());

        String body = response.body() == null ? "" : response.body();
        result.setBodySizeBytes(body.getBytes(StandardCharsets.UTF_8).length);
        if (body.length() > options.maxBodyChars()) {
            result.setBody(body.substring(0, options.maxBodyChars())
                    + "\n… truncated, " + body.length() + " characters total");
        } else {
            result.setBody(body);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        response.headers().map().forEach((name, values) -> {
            if (name != null && !name.startsWith(":")) {
                headers.put(name, joinHeaderValues(values));
            }
        });
        result.setHeaders(headers);
        return result;
    }

    private String joinHeaderValues(List<String> values) {
        return values == null ? "" : String.join(", ", values);
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(Math.max(0, options.retryDelay().toMillis()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String describe(URI uri) {
        return httpMethod() + " " + uri;
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return Strings.defaultIfBlank(cause.getMessage(), cause.getClass().getSimpleName());
    }
}
