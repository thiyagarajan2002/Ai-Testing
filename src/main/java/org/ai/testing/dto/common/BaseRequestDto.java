package org.ai.testing.dto.common;


import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class BaseRequestDto {

    private String url;

    private Map<String, String> headers =
            new HashMap<>();

    private Map<String, String> queryParams =
            new HashMap<>();

    private Map<String, String> pathParams =
            new HashMap<>();

    private RequestBodyDto body;
}