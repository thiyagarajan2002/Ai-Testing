package org.ai.testing.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ai.testing.env.EnvironmentDto;

import java.io.IOException;
import java.nio.file.Path;

public class PostmanEnvironmentParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public EnvironmentDto parse(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Environment path cannot be null");
        }
        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            EnvironmentDto environment = new EnvironmentDto();
            environment.setName(root.path("name").asText("postman-env"));
            JsonNode values = root.path("values");
            if (values.isArray()) {
                for (JsonNode value : values) {
                    if (value.path("enabled").asBoolean(true)
                            && !value.path("key").asText("").isBlank()) {
                        environment.getValues().put(
                                value.path("key").asText(),
                                value.path("value").asText(""));
                    }
                }
            }
            return environment;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read Postman environment: " + path, e);
        }
    }
}
