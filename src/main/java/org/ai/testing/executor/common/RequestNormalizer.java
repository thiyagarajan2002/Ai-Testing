package org.ai.testing.executor.common;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.BodyMode;
import org.ai.testing.dto.common.HeaderDto;
import org.ai.testing.dto.common.PathParamDto;
import org.ai.testing.dto.common.QueryParamDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.util.Strings;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collapses the imported representation of a request into what is actually sent.
 *
 * <p>Runs before authentication and before the URL is built:</p>
 * <ol>
 *   <li>enabled header, query and path items are folded into the flat maps;</li>
 *   <li>form bodies are encoded into {@code rawBody};</li>
 *   <li>a Content-Type is derived from the body mode when none was given;</li>
 *   <li>the Content-Type header is aligned with the body so servers that key off
 *       the header rather than sniffing the payload behave predictably.</li>
 * </ol>
 */
public class RequestNormalizer {

    public void normalize(BaseRequestDto request) {
        if (request == null) {
            return;
        }

        // The setters already guard against null, but a DTO built by
        // deserialisation may still carry nulls.
        request.setHeaders(request.getHeaders());
        request.setQueryParams(request.getQueryParams());
        request.setPathParams(request.getPathParams());

        mergeHeaders(request);
        mergeQueryParams(request);
        mergePathParams(request);
        normalizeBody(request);
        alignContentTypeHeader(request);
    }

    private void mergeHeaders(BaseRequestDto request) {
        for (HeaderDto header : request.getHeaderItems()) {
            if (header == null || !header.isEnabled() || Strings.isBlank(header.getName())) {
                continue;
            }
            request.getHeaders().put(header.getName().trim(),
                    Strings.nullToEmpty(header.getValue()));
        }
    }

    private void mergeQueryParams(BaseRequestDto request) {
        for (QueryParamDto param : request.getQueryParamItems()) {
            if (param == null || !param.isEnabled() || Strings.isBlank(param.getName())) {
                continue;
            }
            request.getQueryParams().put(param.getName().trim(),
                    Strings.nullToEmpty(param.getValue()));
        }
    }

    private void mergePathParams(BaseRequestDto request) {
        for (PathParamDto param : request.getPathParamItems()) {
            if (param == null || Strings.isBlank(param.getName())) {
                continue;
            }
            request.getPathParams().put(param.getName().trim(),
                    Strings.nullToEmpty(param.getValue()));
        }
    }

    private void normalizeBody(BaseRequestDto request) {
        RequestBodyDto body = request.getBody();
        if (body == null) {
            return;
        }

        BodyMode mode = body.getMode() == null ? BodyMode.RAW : body.getMode();

        if (mode == BodyMode.URLENCODED || mode == BodyMode.FORMDATA) {
            body.setRawBody(encodeForm(body.getFormFields()));
            if (Strings.isBlank(body.getContentType())) {
                // Multipart uploads are out of scope, so form-data is sent
                // url-encoded, which every server in this framework's remit
                // accepts for simple key/value fields.
                body.setContentType("application/x-www-form-urlencoded");
            }
            return;
        }

        if (Strings.isBlank(body.getContentType())) {
            switch (mode) {
                case JSON, GRAPHQL -> body.setContentType("application/json");
                case XML -> body.setContentType("application/xml");
                case TEXT -> body.setContentType("text/plain; charset=utf-8");
                default -> {
                    // A raw body of unknown shape gets no invented Content-Type,
                    // unless it obviously parses as JSON.
                    if (org.ai.testing.json.Json.isJson(body.getRawBody())) {
                        body.setContentType("application/json");
                    }
                }
            }
        }
    }

    private void alignContentTypeHeader(BaseRequestDto request) {
        RequestBodyDto body = request.getBody();
        if (body == null || Strings.isBlank(body.getContentType())) {
            return;
        }
        boolean alreadySet = request.getHeaders().keySet().stream()
                .anyMatch(key -> key != null && key.equalsIgnoreCase("Content-Type"));
        if (!alreadySet) {
            request.getHeaders().put("Content-Type", body.getContentType());
        }
    }

    private String encodeForm(List<QueryParamDto> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        return fields.stream()
                .filter(field -> field != null && field.isEnabled()
                        && Strings.hasText(field.getName()))
                .map(field -> RequestBuilder.encode(field.getName())
                        + "=" + RequestBuilder.encode(Strings.nullToEmpty(field.getValue())))
                .collect(Collectors.joining("&"));
    }

    /** Headers a caller should never set by hand; the client owns them. */
    public static boolean isRestrictedHeader(String name) {
        if (name == null) {
            return true;
        }
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "connection", "content-length", "expect", "host", "upgrade" -> true;
            default -> false;
        };
    }

    /** Effective header view used by reports. */
    public Map<String, String> effectiveHeaders(BaseRequestDto request) {
        return request == null ? Map.of() : request.getHeaders();
    }
}
