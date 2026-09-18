package org.ai.testing.executor.common;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.BodyMode;
import org.ai.testing.dto.common.HeaderDto;
import org.ai.testing.dto.common.PathParamDto;
import org.ai.testing.dto.common.QueryParamDto;
import org.ai.testing.dto.common.RequestBodyDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class RequestNormalizer {

    public void normalize(BaseRequestDto request) {
        if (request == null) {
            return;
        }
        if (request.getHeaders() == null) {
            request.setHeaders(new HashMap<>());
        }
        if (request.getQueryParams() == null) {
            request.setQueryParams(new HashMap<>());
        }
        if (request.getPathParams() == null) {
            request.setPathParams(new HashMap<>());
        }

        mergeHeaders(request);
        mergeQueryParams(request);
        mergePathParams(request);
        encodeBody(request);
    }

    private void mergeHeaders(BaseRequestDto request) {
        if (request.getHeaderItems() == null) {
            return;
        }
        for (HeaderDto header : request.getHeaderItems()) {
            if (header != null && header.isEnabled()
                    && header.getName() != null && !header.getName().isBlank()
                    && header.getValue() != null) {
                request.getHeaders().put(header.getName(), header.getValue());
            }
        }
    }

    private void mergeQueryParams(BaseRequestDto request) {
        if (request.getQueryParamItems() == null) {
            return;
        }
        for (QueryParamDto param : request.getQueryParamItems()) {
            if (param != null && param.isEnabled()
                    && param.getName() != null && !param.getName().isBlank()) {
                request.getQueryParams().put(param.getName(),
                        param.getValue() == null ? "" : param.getValue());
            }
        }
    }

    private void mergePathParams(BaseRequestDto request) {
        if (request.getPathParamItems() == null) {
            return;
        }
        for (PathParamDto param : request.getPathParamItems()) {
            if (param != null && param.getName() != null && !param.getName().isBlank()) {
                request.getPathParams().put(param.getName(),
                        param.getValue() == null ? "" : param.getValue());
            }
        }
    }

    private void encodeBody(BaseRequestDto request) {
        RequestBodyDto body = request.getBody();
        if (body == null) {
            return;
        }
        BodyMode mode = body.getMode();
        if (mode == BodyMode.URLENCODED || mode == BodyMode.FORMDATA) {
            body.setRawBody(encodeForm(body.getFormFields()));
            if (mode == BodyMode.URLENCODED
                    && (body.getContentType() == null || body.getContentType().isBlank())) {
                body.setContentType("application/x-www-form-urlencoded");
            }
            if (mode == BodyMode.FORMDATA
                    && (body.getContentType() == null || body.getContentType().isBlank())) {
                body.setContentType("application/x-www-form-urlencoded");
            }
        } else if (mode == BodyMode.JSON
                && (body.getContentType() == null || body.getContentType().isBlank())) {
            body.setContentType("application/json");
        } else if (mode == BodyMode.XML
                && (body.getContentType() == null || body.getContentType().isBlank())) {
            body.setContentType("application/xml");
        } else if (mode == BodyMode.GRAPHQL
                && (body.getContentType() == null || body.getContentType().isBlank())) {
            body.setContentType("application/json");
        }
    }

    private String encodeForm(List<QueryParamDto> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        return fields.stream()
                .filter(field -> field != null && field.isEnabled() && field.getName() != null)
                .map(field -> encode(field.getName()) + "="
                        + encode(field.getValue() == null ? "" : field.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
