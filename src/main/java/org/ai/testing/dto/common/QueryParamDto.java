package org.ai.testing.dto.common;

import lombok.Data;

@Data
public class QueryParamDto {

    private String name;

    private String value;

    private boolean enabled = true;
}