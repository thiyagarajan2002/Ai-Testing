package org.ai.testing.executor.common;

import org.ai.testing.dto.common.BaseRequestDto;

import java.util.Map;

public class RequestBuilder {

    public String buildUrl(BaseRequestDto request) {

        String url = request.getUrl();

        // Replace path parameters
        for (Map.Entry<String, String> entry :
                request.getPathParams().entrySet()) {

            String placeholder = "{" + entry.getKey() + "}";

            url = url.replace(
                    placeholder,
                    entry.getValue()
            );
        }

        // Add query parameters
        if (!request.getQueryParams().isEmpty()) {

            StringBuilder query = new StringBuilder();

            for (Map.Entry<String, String> entry :
                    request.getQueryParams().entrySet()) {

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