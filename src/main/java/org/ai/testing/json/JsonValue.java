package org.ai.testing.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable-by-convention JSON node used across the framework.
 *
 * <p>The framework deliberately ships without Jackson, Gson or JSONPath so the
 * build never breaks because of a transitive dependency clash. This class plus
 * {@link Json} and {@link JsonPathReader} covers everything the parsers,
 * validators and report generators need.</p>
 */
public final class JsonValue {

    public enum Kind { NULL, BOOLEAN, NUMBER, STRING, ARRAY, OBJECT }

    public static final JsonValue NULL = new JsonValue(Kind.NULL, null);
    public static final JsonValue TRUE = new JsonValue(Kind.BOOLEAN, Boolean.TRUE);
    public static final JsonValue FALSE = new JsonValue(Kind.BOOLEAN, Boolean.FALSE);

    /** Returned instead of {@code null} so callers can chain safely. */
    public static final JsonValue MISSING = new JsonValue(Kind.NULL, null);

    private final Kind kind;
    private final Object value;

    private JsonValue(Kind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    public static JsonValue of(String text) {
        return text == null ? NULL : new JsonValue(Kind.STRING, text);
    }

    public static JsonValue of(double number) {
        return new JsonValue(Kind.NUMBER, number);
    }

    public static JsonValue of(long number) {
        return new JsonValue(Kind.NUMBER, (double) number);
    }

    public static JsonValue of(boolean flag) {
        return flag ? TRUE : FALSE;
    }

    public static JsonValue array() {
        return new JsonValue(Kind.ARRAY, new ArrayList<JsonValue>());
    }

    public static JsonValue array(List<JsonValue> items) {
        return new JsonValue(Kind.ARRAY, items == null ? new ArrayList<JsonValue>() : items);
    }

    public static JsonValue object() {
        return new JsonValue(Kind.OBJECT, new LinkedHashMap<String, JsonValue>());
    }

    public static JsonValue object(Map<String, JsonValue> fields) {
        return new JsonValue(Kind.OBJECT,
                fields == null ? new LinkedHashMap<String, JsonValue>() : fields);
    }

    public Kind kind() {
        return kind;
    }

    public boolean isMissing() {
        return this == MISSING;
    }

    public boolean isNull() {
        return kind == Kind.NULL;
    }

    public boolean isObject() {
        return kind == Kind.OBJECT;
    }

    public boolean isArray() {
        return kind == Kind.ARRAY;
    }

    public boolean isString() {
        return kind == Kind.STRING;
    }

    public boolean isNumber() {
        return kind == Kind.NUMBER;
    }

    public boolean isBoolean() {
        return kind == Kind.BOOLEAN;
    }

    /** Never returns {@code null}; unknown fields yield {@link #MISSING}. */
    public JsonValue path(String field) {
        if (kind != Kind.OBJECT || field == null) {
            return MISSING;
        }
        JsonValue found = fields().get(field);
        return found == null ? MISSING : found;
    }

    /** Never returns {@code null}; out-of-range indexes yield {@link #MISSING}. */
    public JsonValue path(int index) {
        if (kind != Kind.ARRAY || index < 0 || index >= items().size()) {
            return MISSING;
        }
        return items().get(index);
    }

    public boolean has(String field) {
        return kind == Kind.OBJECT && field != null && fields().containsKey(field);
    }

    @SuppressWarnings("unchecked")
    public List<JsonValue> items() {
        if (kind != Kind.ARRAY) {
            return Collections.emptyList();
        }
        return (List<JsonValue>) value;
    }

    @SuppressWarnings("unchecked")
    public Map<String, JsonValue> fields() {
        if (kind != Kind.OBJECT) {
            return Collections.emptyMap();
        }
        return (Map<String, JsonValue>) value;
    }

    public JsonValue put(String field, JsonValue child) {
        if (kind == Kind.OBJECT && field != null) {
            fields().put(field, child == null ? NULL : child);
        }
        return this;
    }

    public JsonValue put(String field, String child) {
        return put(field, of(child));
    }

    public JsonValue add(JsonValue child) {
        if (kind == Kind.ARRAY) {
            items().add(child == null ? NULL : child);
        }
        return this;
    }

    public int size() {
        if (kind == Kind.ARRAY) {
            return items().size();
        }
        if (kind == Kind.OBJECT) {
            return fields().size();
        }
        return 0;
    }

    public String asText(String fallback) {
        return switch (kind) {
            case STRING -> (String) value;
            case NUMBER -> Json.formatNumber((Double) value);
            case BOOLEAN -> String.valueOf(value);
            case NULL -> this == MISSING ? fallback : fallback;
            default -> Json.write(this, false);
        };
    }

    public String asText() {
        return asText("");
    }

    public boolean asBoolean(boolean fallback) {
        if (kind == Kind.BOOLEAN) {
            return (Boolean) value;
        }
        if (kind == Kind.STRING) {
            String text = ((String) value).trim();
            if (text.equalsIgnoreCase("true")) {
                return true;
            }
            if (text.equalsIgnoreCase("false")) {
                return false;
            }
        }
        return fallback;
    }

    public double asDouble(double fallback) {
        if (kind == Kind.NUMBER) {
            return (Double) value;
        }
        if (kind == Kind.STRING) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public int asInt(int fallback) {
        return (int) asDouble(fallback);
    }

    /** The scalar carried by this node, or {@code null} for containers. */
    public Object raw() {
        return value;
    }

    @Override
    public String toString() {
        return Json.write(this, false);
    }
}
