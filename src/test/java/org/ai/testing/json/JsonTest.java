package org.ai.testing.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Json parser and writer")
class JsonTest {

    @Test
    @DisplayName("reads nested objects and arrays")
    void readsNestedStructures() {
        JsonValue root = Json.parse("{\"a\":{\"b\":[1,2,3]},\"c\":true,\"d\":null}");
        assertEquals(3, root.path("a").path("b").size());
        assertEquals(2, root.path("a").path("b").path(1).asInt(0));
        assertTrue(root.path("c").asBoolean(false));
        assertTrue(root.path("d").isNull());
        assertTrue(root.path("missing").isMissing());
    }

    @Test
    @DisplayName("decodes every escape sequence")
    void decodesEscapes() {
        JsonValue root = Json.parse("{\"k\":\"line\\nquote\\\"slash\\\\tab\\tu\\u00e9\"}");
        assertEquals("line\nquote\"slash\\tab\tu\u00e9", root.path("k").asText());
    }

    @Test
    @DisplayName("round-trips a document through write and parse")
    void roundTrips() {
        String original = "{\"name\":\"Rex & Milo\",\"ids\":[1,2],\"nested\":{\"ok\":false}}";
        assertEquals(original, Json.write(Json.parse(original), false));
    }

    @Test
    @DisplayName("renders whole numbers without a decimal point")
    void formatsNumbers() {
        assertEquals("{\"n\":150}", Json.write(Json.parse("{\"n\":1.5e2}"), false));
        assertEquals("{\"n\":1.25}", Json.write(Json.parse("{\"n\":1.25}"), false));
        assertEquals("{\"n\":-7}", Json.write(Json.parse("{\"n\":-7}"), false));
    }

    @Test
    @DisplayName("indents when asked to pretty-print")
    void prettyPrints() {
        assertEquals("{\n  \"a\": 1\n}", Json.prettyPrint("{\"a\":1}"));
    }

    @Test
    @DisplayName("returns the input unchanged when it is not JSON")
    void prettyPrintLeavesNonJsonAlone() {
        assertEquals("not json", Json.prettyPrint("not json"));
    }

    @Test
    @DisplayName("rejects malformed input")
    void rejectsMalformedInput() {
        assertThrows(JsonException.class, () -> Json.parse("{\"a\":}"));
        assertThrows(JsonException.class, () -> Json.parse("{\"a\":1}extra"));
        assertThrows(JsonException.class, () -> Json.parse("[1,2"));
    }

    @Test
    @DisplayName("detects whether text is JSON without throwing")
    void detectsJson() {
        assertTrue(Json.isJson("{\"a\":1}"));
        assertTrue(Json.isJson("[1,2]"));
        assertFalse(Json.isJson("plain text"));
        assertFalse(Json.isJson(null));
        assertFalse(Json.isJson(""));
    }

    @Test
    @DisplayName("parseOrMissing never throws")
    void parseOrMissingIsSafe() {
        assertTrue(Json.parseOrMissing("nonsense").isMissing());
    }
}
