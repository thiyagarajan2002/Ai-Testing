package org.ai.testing.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Minimal, strict-enough RFC 8259 reader and writer.
 *
 * <p>Replaces the previous Jackson dependency. The framework only ever reads
 * collection files and response bodies and only ever writes its own reports, so
 * a focused implementation is both smaller and more predictable than a general
 * purpose data-binding library.</p>
 */
public final class Json {

    private Json() {
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    public static JsonValue parse(String text) {
        if (text == null) {
            throw new JsonException("JSON input cannot be null");
        }
        Parser parser = new Parser(text);
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new JsonException("Unexpected trailing content at position " + parser.position());
        }
        return value;
    }

    /** Parses without throwing; returns {@link JsonValue#MISSING} on failure. */
    public static JsonValue parseOrMissing(String text) {
        try {
            return parse(text);
        } catch (JsonException e) {
            return JsonValue.MISSING;
        }
    }

    public static JsonValue read(Path path) throws IOException {
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    public static boolean isJson(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        char first = trimmed.charAt(0);
        if (first != '{' && first != '[') {
            return false;
        }
        try {
            parse(trimmed);
            return true;
        } catch (JsonException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    public static String write(JsonValue value, boolean pretty) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value == null ? JsonValue.NULL : value, pretty, 0);
        return out.toString();
    }

    /** Re-indents an arbitrary JSON document; returns the input when unparseable. */
    public static String prettyPrint(String text) {
        try {
            return write(parse(text), true);
        } catch (JsonException e) {
            return text;
        }
    }

    public static String quote(String text) {
        StringBuilder out = new StringBuilder();
        writeString(out, text);
        return out.toString();
    }

    static String formatNumber(double number) {
        if (number == Math.rint(number) && !Double.isInfinite(number)
                && Math.abs(number) < 1e15) {
            return Long.toString((long) number);
        }
        return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }

    private static void writeValue(StringBuilder out, JsonValue value,
                                   boolean pretty, int depth) {
        switch (value.kind()) {
            case NULL -> out.append("null");
            case BOOLEAN -> out.append(value.asBoolean(false));
            case NUMBER -> out.append(formatNumber(value.asDouble(0)));
            case STRING -> writeString(out, value.asText());
            case ARRAY -> writeArray(out, value.items(), pretty, depth);
            case OBJECT -> writeObject(out, value.fields(), pretty, depth);
            default -> out.append("null");
        }
    }

    private static void writeArray(StringBuilder out, List<JsonValue> items,
                                   boolean pretty, int depth) {
        if (items.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            newLine(out, pretty, depth + 1);
            writeValue(out, items.get(i), pretty, depth + 1);
        }
        newLine(out, pretty, depth);
        out.append(']');
    }

    private static void writeObject(StringBuilder out, Map<String, JsonValue> fields,
                                    boolean pretty, int depth) {
        if (fields.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : fields.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newLine(out, pretty, depth + 1);
            writeString(out, entry.getKey());
            out.append(':');
            if (pretty) {
                out.append(' ');
            }
            writeValue(out, entry.getValue(), pretty, depth + 1);
        }
        newLine(out, pretty, depth);
        out.append('}');
    }

    private static void newLine(StringBuilder out, boolean pretty, int depth) {
        if (!pretty) {
            return;
        }
        out.append('\n');
        out.append("  ".repeat(Math.max(0, depth)));
    }

    private static void writeString(StringBuilder out, String text) {
        if (text == null) {
            out.append("null");
            return;
        }
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    // ------------------------------------------------------------------
    // Parser
    // ------------------------------------------------------------------

    private static final class Parser {

        private final String source;
        private int index;

        Parser(String source) {
            this.source = source;
        }

        int position() {
            return index;
        }

        boolean atEnd() {
            return index >= source.length();
        }

        void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        JsonValue parseValue() {
            skipWhitespace();
            if (atEnd()) {
                throw new JsonException("Unexpected end of JSON input");
            }
            char c = source.charAt(index);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> JsonValue.of(parseString());
                case 't' -> parseLiteral("true", JsonValue.TRUE);
                case 'f' -> parseLiteral("false", JsonValue.FALSE);
                case 'n' -> parseLiteral("null", JsonValue.NULL);
                default -> parseNumber();
            };
        }

        private JsonValue parseLiteral(String literal, JsonValue value) {
            if (!source.startsWith(literal, index)) {
                throw new JsonException("Invalid literal at position " + index);
            }
            index += literal.length();
            return value;
        }

        private JsonValue parseObject() {
            expect('{');
            JsonValue object = JsonValue.object();
            skipWhitespace();
            if (peek() == '}') {
                index++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                JsonValue value = parseValue();
                object.put(key, value);
                skipWhitespace();
                char next = peek();
                if (next == ',') {
                    index++;
                    continue;
                }
                if (next == '}') {
                    index++;
                    return object;
                }
                throw new JsonException("Expected ',' or '}' at position " + index);
            }
        }

        private JsonValue parseArray() {
            expect('[');
            JsonValue array = JsonValue.array();
            skipWhitespace();
            if (peek() == ']') {
                index++;
                return array;
            }
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                char next = peek();
                if (next == ',') {
                    index++;
                    continue;
                }
                if (next == ']') {
                    index++;
                    return array;
                }
                throw new JsonException("Expected ',' or ']' at position " + index);
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder text = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new JsonException("Unterminated string literal");
                }
                char c = source.charAt(index++);
                if (c == '"') {
                    return text.toString();
                }
                if (c != '\\') {
                    text.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw new JsonException("Unterminated escape sequence");
                }
                char escape = source.charAt(index++);
                switch (escape) {
                    case '"' -> text.append('"');
                    case '\\' -> text.append('\\');
                    case '/' -> text.append('/');
                    case 'b' -> text.append('\b');
                    case 'f' -> text.append('\f');
                    case 'n' -> text.append('\n');
                    case 'r' -> text.append('\r');
                    case 't' -> text.append('\t');
                    case 'u' -> {
                        if (index + 4 > source.length()) {
                            throw new JsonException("Truncated unicode escape");
                        }
                        String hex = source.substring(index, index + 4);
                        index += 4;
                        try {
                            text.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw new JsonException("Invalid unicode escape: \\u" + hex);
                        }
                    }
                    default -> throw new JsonException("Invalid escape character: \\" + escape);
                }
            }
        }

        private JsonValue parseNumber() {
            int start = index;
            if (peek() == '-' || peek() == '+') {
                index++;
            }
            while (!atEnd() && (Character.isDigit(source.charAt(index))
                    || source.charAt(index) == '.'
                    || source.charAt(index) == 'e'
                    || source.charAt(index) == 'E'
                    || ((source.charAt(index) == '-' || source.charAt(index) == '+')
                    && (source.charAt(index - 1) == 'e' || source.charAt(index - 1) == 'E')))) {
                index++;
            }
            String literal = source.substring(start, index);
            if (literal.isEmpty()) {
                throw new JsonException("Unexpected character '" + peek()
                        + "' at position " + index);
            }
            try {
                return JsonValue.of(Double.parseDouble(literal));
            } catch (NumberFormatException e) {
                throw new JsonException("Invalid number literal: " + literal);
            }
        }

        private char peek() {
            return atEnd() ? '\0' : source.charAt(index);
        }

        private void expect(char expected) {
            skipWhitespace();
            if (atEnd() || source.charAt(index) != expected) {
                throw new JsonException("Expected '" + expected + "' at position " + index);
            }
            index++;
        }
    }
}
