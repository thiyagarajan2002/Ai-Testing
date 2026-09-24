package org.ai.testing.util;

import org.ai.testing.dto.common.BaseRequestDto;

import java.util.Map;

/**
 * Renders a request as a {@code curl} command.
 *
 * <p>Pasting this into a terminal is usually the fastest way to confirm whether
 * a failure is in the API or in the test, so every case result carries one.</p>
 */
public final class CurlBuilder {

    private CurlBuilder() {
    }

    public static String build(String method, String url,
                               BaseRequestDto request, boolean redact) {

        StringBuilder curl = new StringBuilder("curl -i -sS");
        curl.append(" -X ").append(Strings.defaultIfBlank(method, "GET"));

        if (request != null) {
            Map<String, String> headers = request.getHeaders();
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (Strings.isBlank(entry.getKey())) {
                        continue;
                    }
                    String value = entry.getValue();
                    if (redact && Redaction.isSensitive(entry.getKey())) {
                        value = Redaction.maskValue(value);
                    }
                    curl.append(" \\\n  -H ")
                            .append(quote(entry.getKey() + ": " + Strings.nullToEmpty(value)));
                }
            }

            if (request.getBody() != null && Strings.hasText(request.getBody().getRawBody())) {
                curl.append(" \\\n  --data ").append(quote(request.getBody().getRawBody()));
            }
        }

        curl.append(" \\\n  ").append(quote(Strings.nullToEmpty(url)));
        return curl.toString();
    }

    /** Single-quotes a value the way a POSIX shell expects. */
    private static String quote(String value) {
        return "'" + Strings.nullToEmpty(value).replace("'", "'\\''") + "'";
    }
}
