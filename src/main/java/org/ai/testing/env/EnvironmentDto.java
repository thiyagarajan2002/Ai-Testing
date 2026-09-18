package org.ai.testing.env;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class EnvironmentDto {

    private String name = "default";

    private Map<String, String> values = new LinkedHashMap<>();
}
