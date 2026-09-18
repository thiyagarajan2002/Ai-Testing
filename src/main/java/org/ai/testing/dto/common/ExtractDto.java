package org.ai.testing.dto.common;

import lombok.Data;

@Data
public class ExtractDto {

    private String variableName;

    /**
     * JsonPath for the response body, or a header name when source is HEADER.
     */
    private String expression;

    /**
     * BODY or HEADER
     */
    private String source = "BODY";
}
