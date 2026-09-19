package org.ai.testing.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Masks credentials before they reach a report file.
 *
 * <p>Reports get committed to CI artefacts and shared in tickets, so bearer
 * tokens and API keys are replaced with a fingerprint that still lets you tell
 * two different tokens apart.</p>
 */
public final class Redaction {

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "apikey",
            "x-auth-token",
            "x-access-token",
            "x-csrf-token",
            "x-amz-security-token",
            "private-token");

    private Redaction() {
    }

    public static boolean isSensitive(String headerName) {
        if (headerName == null) {
            return false;
        }
        String name = headerName.toLowerCase(Locale.ROOT).trim();
        if (SENSITIVE_HEADERS.contains(name)) {
            return true;
        }
        return name.contains("token") || name.contains("secret") || name.contains("password");
    }

    public static String maskValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String prefix = "";
        String secret = value;
        int space = value.indexOf(' ');
        if (space > 0 && space < 12) {
            prefix = value.substring(0, space + 1);
            secret = value.substring(space + 1);
        }
        if (secret.length() <= 4) {
            return prefix + "****";
        }
        return prefix + "****" + secret.substring(secret.length() - 4)
                + " (" + secret.length() + " chars)";
    }

    /** Returns a copy of {@code headers} with sensitive values masked. */
    public static Map<String, String> maskHeaders(Map<String, String> headers, boolean enabled) {
        Map<String, String> masked = new LinkedHashMap<>();
        if (headers == null) {
            return masked;
        }
        headers.forEach((name, value) -> {
            if (enabled && isSensitive(name)) {
                masked.put(name, maskValue(value));
            } else {
                masked.put(name, value);
            }
        });
        return masked;
    }
}
