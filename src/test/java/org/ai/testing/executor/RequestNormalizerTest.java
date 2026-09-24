package org.ai.testing.executor;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.BodyMode;
import org.ai.testing.dto.common.HeaderDto;
import org.ai.testing.dto.common.QueryParamDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.executor.common.RequestNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Request normalisation")
class RequestNormalizerTest {

    private final RequestNormalizer normalizer = new RequestNormalizer();

    @Test
    @DisplayName("folds enabled items into the effective maps and drops disabled ones")
    void mergesItemsAndDropsDisabled() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test");

        HeaderDto enabled = new HeaderDto("Accept", "application/json");
        HeaderDto disabled = new HeaderDto("X-Skip", "no");
        disabled.setEnabled(false);
        request.setHeaderItems(List.of(enabled, disabled));

        QueryParamDto onParam = new QueryParamDto("page", "1");
        QueryParamDto offParam = new QueryParamDto("debug", "true");
        offParam.setEnabled(false);
        request.setQueryParamItems(List.of(onParam, offParam));

        normalizer.normalize(request);

        assertEquals("application/json", request.getHeaders().get("Accept"));
        assertFalse(request.getHeaders().containsKey("X-Skip"));
        assertEquals("1", request.getQueryParams().get("page"));
        assertFalse(request.getQueryParams().containsKey("debug"));
    }

    @Test
    @DisplayName("URL-encodes a form body into rawBody")
    void encodesFormBodies() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test");
        request.setBody(RequestBodyDto.form(List.of(
                new QueryParamDto("name", "Rex & Milo"),
                new QueryParamDto("note", "a b"))));

        normalizer.normalize(request);

        assertEquals("name=Rex%20%26%20Milo&note=a%20b", request.getBody().getRawBody());
        assertEquals("application/x-www-form-urlencoded",
                request.getHeaders().get("Content-Type"));
    }

    @Test
    @DisplayName("derives a Content-Type from the body mode")
    void derivesContentType() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test");
        RequestBodyDto body = new RequestBodyDto();
        body.setMode(BodyMode.XML);
        body.setRawBody("<pet/>");
        request.setBody(body);

        normalizer.normalize(request);
        assertEquals("application/xml", request.getBody().getContentType());
    }

    @Test
    @DisplayName("detects JSON in a raw body when no type was declared")
    void detectsJsonRawBody() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test");
        RequestBodyDto body = new RequestBodyDto();
        body.setMode(BodyMode.RAW);
        body.setRawBody("{\"a\":1}");
        request.setBody(body);

        normalizer.normalize(request);
        assertEquals("application/json", request.getBody().getContentType());
    }

    @Test
    @DisplayName("does not overwrite a Content-Type the caller set explicitly")
    void respectsExplicitContentType() {
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test");
        request.header("content-type", "application/vnd.api+json");
        request.setBody(RequestBodyDto.json("{}"));

        normalizer.normalize(request);
        assertEquals("application/vnd.api+json", request.getHeaders().get("content-type"));
        assertFalse(request.getHeaders().containsKey("Content-Type"));
    }

    @Test
    @DisplayName("identifies headers the HTTP client owns")
    void identifiesRestrictedHeaders() {
        assertTrue(RequestNormalizer.isRestrictedHeader("Host"));
        assertTrue(RequestNormalizer.isRestrictedHeader("content-length"));
        assertFalse(RequestNormalizer.isRestrictedHeader("Authorization"));
    }
}
