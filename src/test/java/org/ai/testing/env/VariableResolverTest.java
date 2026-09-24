package org.ai.testing.env;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Variable resolution")
class VariableResolverTest {

    private final VariableResolver resolver = new VariableResolver();

    private VariableStore store() {
        VariableStore store = new VariableStore();
        store.putCollection("host", "api.example.com");
        store.putCollection("version", "v1");
        store.putEnvironment("version", "v2");
        return store;
    }

    @Test
    @DisplayName("applies runtime over environment over collection")
    void appliesLayerPrecedence() {
        VariableStore store = store();
        assertEquals("v2", store.get("version"));
        store.putRuntime("version", "v3");
        assertEquals("v3", store.get("version"));
        assertEquals("api.example.com", store.get("host"));
    }

    @Test
    @DisplayName("substitutes placeholders in a template")
    void substitutesPlaceholders() {
        assertEquals("https://api.example.com/v2/pets",
                resolver.resolve("https://{{host}}/{{version}}/pets", store()));
    }

    @Test
    @DisplayName("tolerates whitespace inside the braces")
    void toleratesWhitespace() {
        assertEquals("api.example.com", resolver.resolve("{{  host  }}", store()));
    }

    @Test
    @DisplayName("resolves a value that itself contains a placeholder")
    void resolvesNestedValues() {
        VariableStore store = store();
        store.putCollection("baseUrl", "https://{{host}}/{{version}}");
        assertEquals("https://api.example.com/v2/pets",
                resolver.resolve("{{baseUrl}}/pets", store));
    }

    @Test
    @DisplayName("stops rather than looping on a cyclic definition")
    void stopsOnCycle() {
        VariableStore store = new VariableStore();
        store.putCollection("a", "{{b}}");
        store.putCollection("b", "{{a}}");
        String resolved = resolver.resolve("{{a}}", store);
        assertTrue(resolved.contains("{{"), resolved);
    }

    @Test
    @DisplayName("leaves an unknown name in place so the gap is visible")
    void leavesUnknownNames() {
        assertEquals("https://{{unknown}}/x", resolver.resolve("https://{{unknown}}/x", store()));
        assertEquals(java.util.List.of("unknown"),
                resolver.unresolvedNames("https://{{unknown}}/x"));
    }

    @Test
    @DisplayName("generates Postman-style dynamic values")
    void generatesDynamicValues() {
        String guid = resolver.resolve("{{$guid}}", store());
        assertEquals(36, guid.length());
        assertFalse(resolver.resolve("{{$timestamp}}", store()).contains("{{"));
        assertFalse(resolver.resolve("{{$isoTimestamp}}", store()).contains("{{"));
    }

    @Test
    @DisplayName("resolves url, headers, query, path and body of a request")
    void resolvesWholeRequest() {
        VariableStore store = store();
        store.putRuntime("token", "abc123");
        store.putRuntime("petId", "7");

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://{{host}}/pet/{petId}");
        request.header("Authorization", "Bearer {{token}}");
        request.query("v", "{{version}}");
        request.pathParam("petId", "{{petId}}");
        request.setBody(RequestBodyDto.json("{\"id\":\"{{petId}}\"}"));

        resolver.resolveRequest(request, store);

        assertEquals("https://api.example.com/pet/{petId}", request.getUrl());
        assertEquals("Bearer abc123", request.getHeaders().get("Authorization"));
        assertEquals("v2", request.getQueryParams().get("v"));
        assertEquals("7", request.getPathParams().get("petId"));
        assertEquals("{\"id\":\"7\"}", request.getBody().getRawBody());
    }

    @Test
    @DisplayName("ignores null inputs")
    void ignoresNulls() {
        assertEquals(null, resolver.resolve(null, store()));
        assertEquals("x", resolver.resolve("x", null));
        assertTrue(resolver.resolveMap(null, store()).isEmpty());
    }
}
