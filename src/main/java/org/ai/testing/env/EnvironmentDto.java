package org.ai.testing.env;

import java.util.LinkedHashMap;
import java.util.Map;

/** A named set of variables loaded from a Postman or Bruno environment file. */
public class EnvironmentDto {

    private String name = "default";
    private Map<String, String> values = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values == null ? new LinkedHashMap<>() : values;
    }

    public EnvironmentDto put(String key, String value) {
        values.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return name + " (" + values.size() + " variables)";
    }
}
