package org.ai.testing.executor;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.executor.common.RequestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("URL building")
class RequestBuilderTest {

    private final RequestBuilder builder = new RequestBuilder();

    @Test
    @DisplayName("percent-encodes query values")
    void encodesQueryValues() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test/search");
        request.query("q", "rex & milo");
        request.query("lang", "en/gb");
        String url = builder.buildUrl(request);
        assertTrue(url.contains("q=rex%20%26%20milo"), url);
        assertTrue(url.contains("lang=en%2Fgb"), url);
    }

    @Test
    @DisplayName("appends to a URL that already carries a query string")
    void mergesWithExistingQueryString() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test/search?page=1");
        request.query("size", "20");
        assertEquals("https://api.test/search?page=1&size=20", builder.buildUrl(request));
    }

    @Test
    @DisplayName("substitutes both brace and colon path placeholders")
    void substitutesPathParameters() {
        BaseRequestDto braces = new BaseRequestDto();
        braces.setUrl("https://api.test/pet/{petId}/photo");
        braces.pathParam("petId", "42");
        assertEquals("https://api.test/pet/42/photo", builder.buildUrl(braces));

        BaseRequestDto colons = new BaseRequestDto();
        colons.setUrl("https://api.test/pet/:petId");
        colons.pathParam("petId", "42");
        assertEquals("https://api.test/pet/42", builder.buildUrl(colons));
    }

    @Test
    @DisplayName("encodes a path value but keeps slashes intact")
    void encodesPathValues() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test/files/{path}");
        request.pathParam("path", "a b/c d");
        assertEquals("https://api.test/files/a%20b/c%20d", builder.buildUrl(request));
    }

    @Test
    @DisplayName("leaves a URL untouched when there are no parameters")
    void leavesPlainUrlAlone() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test/health");
        assertEquals("https://api.test/health", builder.buildUrl(request));
    }

    @Test
    @DisplayName("rejects a missing request or URL with a clear message")
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> builder.buildUrl(null));
        assertThrows(IllegalArgumentException.class,
                () -> builder.buildUrl(new BaseRequestDto()));
    }
}
