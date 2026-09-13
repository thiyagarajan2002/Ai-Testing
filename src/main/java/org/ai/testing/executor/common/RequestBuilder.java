package org.ai.testing.executor.common;

import org.ai.testing.dto.common.BaseRequestDto;

import java.util.Map;

public class RequestBuilder {

    public String buildUrl(BaseRequestDto request) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            throw new IllegalArgumentException("Request URL cannot be null or empty");
        }

        String url = request.getUrl();

        Map<String, String> pathParams = request.getPathParams() == null
                ? Map.of()
                : request.getPathParams();

        Map<String, String> queryParams = request.getQueryParams() == null
                ? Map.of()
                : request.getQueryParams();

        // Replace path parameters
        for (Map.Entry<String, String> entry :
                pathParams.entrySet()) {

            String placeholder = "{" + entry.getKey() + "}";

            url = url.replace(
                    placeholder,
                    entry.getValue()
            );
        }

        // Add query parameters
        if (!queryParams.isEmpty()) {

            StringBuilder query = new StringBuilder();

            for (Map.Entry<String, String> entry :
                    queryParams.entrySet()) {

                if (!query.isEmpty()) {
                    query.append("&");
                }

                query.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());
            }

            url += "?" + query;
        }

        return url;
    }
}