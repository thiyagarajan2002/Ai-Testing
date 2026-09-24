package org.ai.testing.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JSONPath subset")
class JsonPathReaderTest {

    private static final String BODY =
            "{\"data\":{\"items\":[{\"id\":1,\"name\":\"first\"},"
                    + "{\"id\":2,\"name\":\"second\"}],\"count\":2},"
                    + "\"ok\":true,\"empty\":\"\"}";

    @Test
    @DisplayName("reads object members and array indexes")
    void readsMembersAndIndexes() {
        assertEquals("first", JsonPathReader.readAsText(BODY, "$.data.items[0].name"));
        assertEquals("2", JsonPathReader.readAsText(BODY, "$.data.items[1].id"));
    }

    @Test
    @DisplayName("supports a negative index counting from the end")
    void supportsNegativeIndex() {
        assertEquals("second", JsonPathReader.readAsText(BODY, "$.data.items[-1].name"));
    }

    @Test
    @DisplayName("supports bracketed member names")
    void supportsBracketNotation() {
        assertEquals("2", JsonPathReader.readAsText(BODY, "$['data']['count']"));
    }

    @Test
    @DisplayName("counts elements with length()")
    void supportsLength() {
        assertEquals("2", JsonPathReader.readAsText(BODY, "$.data.items.length()"));
    }

    @Test
    @DisplayName("collects matches with recursive descent and wildcards")
    void supportsDescentAndWildcard() {
        assertEquals("[1,2]", JsonPathReader.readAsText(BODY, "$..id"));
        assertEquals("[{\"id\":1,\"name\":\"first\"},{\"id\":2,\"name\":\"second\"}]",
                JsonPathReader.readAsText(BODY, "$.data.items[*]"));
    }

    @Test
    @DisplayName("normalises Bruno and bare spellings to canonical paths")
    void normalisesShorthand() {
        assertEquals("$.info.title", JsonPathReader.normalize("res.body.info.title"));
        assertEquals("$.info.title", JsonPathReader.normalize("info.title"));
        assertEquals("$", JsonPathReader.normalize("res.body"));
        assertEquals("$.a", JsonPathReader.normalize("$.a"));
        assertEquals("true", JsonPathReader.readAsText(BODY, "res.body.ok"));
    }

    @Test
    @DisplayName("returns null for a path that matches nothing")
    void returnsNullWhenAbsent() {
        assertNull(JsonPathReader.readAsText(BODY, "$.nope.deeper"));
        assertNull(JsonPathReader.readAsText(BODY, "$.data.items[99].id"));
        assertNull(JsonPathReader.readAsText("not json", "$.a"));
        assertNull(JsonPathReader.readAsText("", "$.a"));
    }

    @Test
    @DisplayName("distinguishes an empty match from an absent one")
    void distinguishesEmptyFromAbsent() {
        assertEquals("", JsonPathReader.readAsText(BODY, "$.empty"));
        assertTrue(JsonPathReader.read(BODY, "$.nope").isMissing());
    }
}
