package org.ai.testing.env;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Layered variables, in Postman and Bruno precedence order.
 *
 * <pre>
 *   runtime      captured from earlier responses or --var on the command line
 *     overrides
 *   environment  loaded from an environment file
 *     overrides
 *   collection   declared inside the collection itself
 * </pre>
 *
 * <p>The runtime layer is a concurrent map because suites may run in parallel
 * and a captured value written by one test case must be visible to others.</p>
 */
public class VariableStore {

    private final Map<String, String> collection = new ConcurrentHashMap<>();
    private final Map<String, String> environment = new ConcurrentHashMap<>();
    private final Map<String, String> runtime = new ConcurrentHashMap<>();

    public void putCollection(String key, String value) {
        put(collection, key, value);
    }

    public void putAllCollection(Map<String, String> values) {
        putAll(collection, values);
    }

    public void putEnvironment(String key, String value) {
        put(environment, key, value);
    }

    public void putAllEnvironment(Map<String, String> values) {
        putAll(environment, values);
    }

    public void putRuntime(String key, String value) {
        put(runtime, key, value);
    }

    public void putAllRuntime(Map<String, String> values) {
        putAll(runtime, values);
    }

    /** Resolves one name across all layers; {@code null} when undefined. */
    public String get(String key) {
        if (key == null) {
            return null;
        }
        String value = runtime.get(key);
        if (value != null) {
            return value;
        }
        value = environment.get(key);
        if (value != null) {
            return value;
        }
        return collection.get(key);
    }

    public boolean contains(String key) {
        return get(key) != null;
    }

    /** A flattened view, lowest precedence first. Useful for reports. */
    public Map<String, String> snapshot() {
        Map<String, String> merged = new LinkedHashMap<>(collection);
        merged.putAll(environment);
        merged.putAll(runtime);
        return merged;
    }

    public Map<String, String> runtimeSnapshot() {
        return new LinkedHashMap<>(runtime);
    }

    public int size() {
        return snapshot().size();
    }

    private void put(Map<String, String> target, String key, String value) {
        if (key != null && !key.isBlank() && value != null) {
            target.put(key.trim(), value);
        }
    }

    private void putAll(Map<String, String> target, Map<String, String> values) {
        if (values != null) {
            values.forEach((key, value) -> put(target, key, value));
        }
    }
}
