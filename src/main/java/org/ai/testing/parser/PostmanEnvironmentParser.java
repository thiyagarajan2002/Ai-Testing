package org.ai.testing.parser;

import org.ai.testing.env.EnvironmentDto;
import org.ai.testing.json.Json;
import org.ai.testing.json.JsonValue;
import org.ai.testing.util.Strings;

import java.io.IOException;
import java.nio.file.Path;

/** Reads a Postman environment export. */
public class PostmanEnvironmentParser {

    public EnvironmentDto parse(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Environment path cannot be null");
        }
        try {
            JsonValue root = Json.read(path);
            EnvironmentDto environment = new EnvironmentDto();
            environment.setName(root.path("name").asText("postman-env"));

            JsonValue values = root.path("values");
            if (values.isArray()) {
                for (JsonValue value : values.items()) {
                    String key = value.path("key").asText("");
                    if (value.path("enabled").asBoolean(true) && Strings.hasText(key)) {
                        environment.put(key, value.path("value").asText(""));
                    }
                }
            }
            return environment;
        } catch (IOException e) {
            throw new CollectionParseException(
                    "Unable to read Postman environment: " + path, e);
        }
    }
}
