package org.ai.testing.extract;

import org.ai.testing.TestFixtures;
import org.ai.testing.dto.common.ExtractDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.env.VariableStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Response extraction")
class ResponseExtractorTest {

    private final ResponseExtractor extractor = new ResponseExtractor();

    @Test
    @DisplayName("captures body, header and status values into the runtime layer")
    void capturesFromEverySource() {
        ResponseDto response = TestFixtures.response(201, "{\"token\":\"abc\",\"id\":9}");
        VariableStore store = new VariableStore();

        List<ResponseExtractor.Capture> captures = extractor.extract(List.of(
                ExtractDto.fromBody("token", "$.token"),
                ExtractDto.fromBody("id", "$.id"),
                ExtractDto.fromHeader("trace", "X-Trace"),
                new ExtractDto("code", "", "STATUS")), response, store);

        assertEquals(4, captures.size());
        assertTrue(captures.stream().allMatch(ResponseExtractor.Capture::found));
        assertEquals("abc", store.get("token"));
        assertEquals("9", store.get("id"));
        assertEquals("abc-123", store.get("trace"));
        assertEquals("201", store.get("code"));
    }

    @Test
    @DisplayName("reports a miss instead of storing null")
    void reportsMisses() {
        ResponseDto response = TestFixtures.response(200, "{\"a\":1}");
        VariableStore store = new VariableStore();

        List<ResponseExtractor.Capture> captures = extractor.extract(
                List.of(ExtractDto.fromBody("gone", "$.missing")), response, store);

        assertFalse(captures.get(0).found());
        assertNull(store.get("gone"));
        assertTrue(extractor.capturedValues(captures).isEmpty());
    }

    @Test
    @DisplayName("ignores an extract without a variable name")
    void ignoresUnnamedExtracts() {
        VariableStore store = new VariableStore();
        List<ResponseExtractor.Capture> captures = extractor.extract(
                List.of(ExtractDto.fromBody("  ", "$.a")),
                TestFixtures.response(200, "{\"a\":1}"), store);
        assertTrue(captures.isEmpty());
    }
}
