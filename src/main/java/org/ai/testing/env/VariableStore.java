package org.ai.testing.env;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Postman/Bruno-style variable layers: runtime overrides environment, which
 * overrides collection.
 */
public class VariableStore {

    private final Map<String, String> collection = new LinkedHashMap<>();
    private final Map<String, String> environment = new LinkedHashMap<>();
    private final Map<String, String> runtime = new LinkedHashMap<>();

    public void putCollection(String key, String value) {
        if (key != null && !key.isBlank() && value != null) {
            collection.put(key, value);
        }
    }

    public void putAllCollection(Map<String, String> values) {
        if (values != null) {
            values.forEach(this::putCollection);
        }
    }

    public void putEnvironment(String key, String value) {
        if (key != null && !key.isBlank() && value != null) {
            environment.put(key, value);
        }
    }

    public void putAllEnvironment(Map<String, String> values) {
        if (values != null) {
            values.forEach(this::putEnvironment);
        }
    }

    public void putRuntime(String key, String value) {
        if (key != null && !key.isBlank() && value != null) {
            runtime.put(key, value);
        }
    }

    public void putAllRuntime(Map<String, String> values) {
        if (values != null) {
            values.forEach(this::putRuntime);
        }
    }

    public String get(String key) {
        if (key == null) {
            return null;
        }
        if (runtime.containsKey(key)) {
            return runtime.get(key);
        }
        if (environment.containsKey(key)) {
            return environment.get(key);
        }
        return collection.get(key);
    }

    public Map<String, String> snapshot() {
        Map<String, String> merged = new LinkedHashMap<>(collection);
        merged.putAll(environment);
        merged.putAll(runtime);
        return merged;
    }
}
