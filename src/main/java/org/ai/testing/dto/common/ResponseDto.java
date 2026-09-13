package org.ai.testing.dto.common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ResponseDto {

    private int statusCode;

    private String statusMessage;

    private String body;

    private long responseTimeMs;

    private Map<String, String> headers = new HashMap<>();
}