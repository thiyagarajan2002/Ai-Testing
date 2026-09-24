package org.ai.testing.json;

import java.util.ArrayList;
import java.util.List;

/**
 * A focused JSONPath subset, sufficient for API assertions and value capture.
 *
 * <p>Supported syntax:</p>
 * <pre>
 *   $                      root
 *   .name  ['name']        object member
 *   [0]    [-1]            array index, negative counts from the end
 *   [*]    .*              wildcard over an array or object
 *   ..name                 recursive descent
 *   .length()  .size()     element count of the current node
 * </pre>
 *
 * <p>Bruno and Postman spellings such as {@code res.body.id} and a bare
 * {@code data.items[0].id} are normalised to {@code $.…} automatically.</p>
 */
public final class JsonPathReader {

    private JsonPathReader() {
    }

    /** Evaluates {@code path}; returns {@link JsonValue#MISSING} when nothing matches. */
    public static JsonValue read(JsonValue root, String path) {
        if (root == null || root.isMissing()) {
            return JsonValue.MISSING;
        }
        List<Token> tokens = tokenize(normalize(path));
        List<JsonValue> current = new ArrayList<>();
        current.add(root);

        for (Token token : tokens) {
            List<JsonValue> next = new ArrayList<>();
            for (JsonValue node : current) {
                apply(node, token, next);
            }
            if (next.isEmpty()) {
                return JsonValue.MISSING;
            }
            current = next;
        }

        if (current.size() == 1) {
            return current.get(0);
        }
        return JsonValue.array(current);
    }

    /** Convenience overload that parses {@code body} first. */
    public static JsonValue read(String body, String path) {
        if (body == null || body.isBlank()) {
            return JsonValue.MISSING;
        }
        JsonValue root = Json.parseOrMissing(body);
        return root.isMissing() ? JsonValue.MISSING : read(root, path);
    }

    /** Reads a path and renders the match as display text, or {@code null}. */
    public static String readAsText(String body, String path) {
        JsonValue value = read(body, path);
        if (value.isMissing()) {
            return null;
        }
        return switch (value.kind()) {
            case STRING, NUMBER, BOOLEAN -> value.asText();
            case NULL -> "null";
            default -> Json.write(value, false);
        };
    }

    /**
     * Rewrites common shorthand into canonical JSONPath.
     * {@code res.body.id} and {@code id} both become {@code $.id}.
     */
    public static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "$";
        }
        String trimmed = path.trim();
        if (trimmed.equals("res.body") || trimmed.equals("response.body")) {
            return "$";
        }
        if (trimmed.startsWith("res.body.")) {
            return "$." + trimmed.substring("res.body.".length());
        }
        if (trimmed.startsWith("response.body.")) {
            return "$." + trimmed.substring("response.body.".length());
        }
        if (trimmed.startsWith("$")) {
            return trimmed;
        }
        if (trimmed.startsWith("[")) {
            return "$" + trimmed;
        }
        return "$." + trimmed;
    }

    // ------------------------------------------------------------------
    // Evaluation
    // ------------------------------------------------------------------

    private static void apply(JsonValue node, Token token, List<JsonValue> out) {
        switch (token.type) {
            case MEMBER -> {
                JsonValue child = node.path(token.name);
                if (!child.isMissing()) {
                    out.add(child);
                }
            }
            case INDEX -> {
                int index = token.index;
                if (index < 0) {
                    index += node.size();
                }
                JsonValue child = node.path(index);
                if (!child.isMissing()) {
                    out.add(child);
                }
            }
            case WILDCARD -> {
                if (node.isArray()) {
                    out.addAll(node.items());
                } else if (node.isObject()) {
                    out.addAll(node.fields().values());
                }
            }
            case DESCENDANT -> descend(node, token.name, out);
            case LENGTH -> out.add(JsonValue.of(node.size()));
            default -> {
            }
        }
    }

    private static void descend(JsonValue node, String name, List<JsonValue> out) {
        if (node.isObject()) {
            JsonValue direct = node.path(name);
            if (!direct.isMissing()) {
                out.add(direct);
            }
            for (JsonValue child : node.fields().values()) {
                descend(child, name, out);
            }
        } else if (node.isArray()) {
            for (JsonValue child : node.items()) {
                descend(child, name, out);
            }
        }
    }

    // ------------------------------------------------------------------
    // Tokenising
    // ------------------------------------------------------------------

    private enum Type { MEMBER, INDEX, WILDCARD, DESCENDANT, LENGTH }

    private record Token(Type type, String name, int index) {

        static Token member(String name) {
            return new Token(Type.MEMBER, name, 0);
        }

        static Token index(int index) {
            return new Token(Type.INDEX, null, index);
        }

        static Token wildcard() {
            return new Token(Type.WILDCARD, null, 0);
        }

        static Token descendant(String name) {
            return new Token(Type.DESCENDANT, name, 0);
        }

        static Token length() {
            return new Token(Type.LENGTH, null, 0);
        }
    }

    private static List<Token> tokenize(String path) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        if (path.startsWith("$")) {
            i = 1;
        }
        while (i < path.length()) {
            char c = path.charAt(i);
            if (c == '.') {
                if (i + 1 < path.length() && path.charAt(i + 1) == '.') {
                    i += 2;
                    int start = i;
                    while (i < path.length() && isNameChar(path.charAt(i))) {
                        i++;
                    }
                    if (i > start) {
                        tokens.add(Token.descendant(path.substring(start, i)));
                    }
                    continue;
                }
                i++;
                if (i < path.length() && path.charAt(i) == '*') {
                    tokens.add(Token.wildcard());
                    i++;
                    continue;
                }
                int start = i;
                while (i < path.length() && isNameChar(path.charAt(i))) {
                    i++;
                }
                String name = path.substring(start, i);
                if (i + 1 < path.length() && path.charAt(i) == '('
                        && path.indexOf(')', i) == i + 1) {
                    i += 2;
                    if (name.equals("length") || name.equals("size")) {
                        tokens.add(Token.length());
                        continue;
                    }
                }
                if (!name.isEmpty()) {
                    tokens.add(Token.member(name));
                }
                continue;
            }
            if (c == '[') {
                int close = path.indexOf(']', i);
                if (close < 0) {
                    throw new JsonException("Unclosed '[' in JSONPath: " + path);
                }
                String segment = path.substring(i + 1, close).trim();
                i = close + 1;
                if (segment.equals("*")) {
                    tokens.add(Token.wildcard());
                    continue;
                }
                if ((segment.startsWith("'") && segment.endsWith("'") && segment.length() >= 2)
                        || (segment.startsWith("\"") && segment.endsWith("\"")
                        && segment.length() >= 2)) {
                    tokens.add(Token.member(segment.substring(1, segment.length() - 1)));
                    continue;
                }
                try {
                    tokens.add(Token.index(Integer.parseInt(segment)));
                } catch (NumberFormatException e) {
                    tokens.add(Token.member(segment));
                }
                continue;
            }
            // Tolerate a leading bare name such as "data.id" after "$".
            int start = i;
            while (i < path.length() && isNameChar(path.charAt(i))) {
                i++;
            }
            if (i == start) {
                i++;
                continue;
            }
            tokens.add(Token.member(path.substring(start, i)));
        }
        return tokens;
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '@' || c == '$';
    }
}
