package org.ai.testing.dto.common;

import lombok.Data;

@Data
public class HeaderDto {

    private String name;

    private String value;

    private boolean enabled = true;
}