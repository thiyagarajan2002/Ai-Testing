package org.ai.testing.executor.common;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.util.Strings;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Turns a request model into the final URL string.
 *
 * <p>Three defects in the previous implementation are fixed here:</p>
 * <ul>
 *   <li>query values were concatenated raw, so a space or an ampersand produced
 *       a malformed or silently truncated URL;</li>
 *   <li>a {@code ?} was always appended, corrupting a URL that already carried
 *       a query string;</li>
 *   <li>path placeholders were substituted unencoded.</li>
 * </ul>
 */
public class RequestBuilder {

    public String buildUrl(BaseRequestDto request) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (Strings.isBlank(request.getUrl())) {
            throw new IllegalArgumentException("Request URL cannot be null or empty");
        }

        String url = request.getUrl().trim();

        Map<String, String> pathParams = request.getPathParams();
        if (pathParams != null) {
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String value = encodePathSegment(Strings.nullToEmpty(entry.getValue()));
                // Both Postman ({{x}} already resolved, :id) and Bruno ({id}) spellings.
                url = url.replace("{" + entry.getKey() + "}", value);
                url = url.replace(":" + entry.getKey(), value);
            }
        }

        Map<String, String> queryParams = request.getQueryParams();
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(Strings.nullToEmpty(entry.getValue())));
        }

        if (query.length() == 0) {
            return url;
        }

        // Respect a query string that is already part of the URL.
        char separator = url.indexOf('?') >= 0 ? '&' : '?';
        if (url.endsWith("?") || url.endsWith("&")) {
            return url + query;
        }
        return url + separator + query;
    }

    /** Percent-encodes a value for use in a query string. */
    public static String encode(String value) {
        return URLEncoder.encode(Strings.nullToEmpty(value), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    /**
     * Encodes a path segment. Unlike a query value, {@code /} is preserved so a
     * multi-segment path parameter still works.
     */
    public static String encodePathSegment(String value) {
        String encoded = encode(value);
        return encoded.replace("%2F", "/");
    }
}
