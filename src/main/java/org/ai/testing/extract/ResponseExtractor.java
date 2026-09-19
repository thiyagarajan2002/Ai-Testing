package org.ai.testing.extract;

import org.ai.testing.dto.common.ExtractDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.env.VariableStore;
import org.ai.testing.json.JsonPathReader;
import org.ai.testing.util.Strings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures values out of a response and into the runtime variable layer so that
 * later test cases can chain on them, for example a login returning a token
 * that the next request sends as a bearer credential.
 */
public class ResponseExtractor {

    /** The outcome of one capture, kept so the report can explain a miss. */
    public record Capture(String variableName, String expression, String source,
                          String value, boolean found) {
    }

    /**
     * Runs every capture and writes the hits into {@code store}.
     *
     * @return one {@link Capture} per configured extract, in declaration order
     */
    public List<Capture> extract(List<ExtractDto> extracts,
                                 ResponseDto response,
                                 VariableStore store) {

        List<Capture> captures = new ArrayList<>();
        if (extracts == null || extracts.isEmpty() || response == null || store == null) {
            return captures;
        }

        for (ExtractDto extract : extracts) {
            if (extract == null || Strings.isBlank(extract.getVariableName())) {
                continue;
            }
            String source = Strings.defaultIfBlank(extract.getSource(), "BODY")
                    .toUpperCase(java.util.Locale.ROOT);
            String value = read(source, extract.getExpression(), response);
            boolean found = value != null;
            if (found) {
                store.putRuntime(extract.getVariableName(), value);
            }
            captures.add(new Capture(extract.getVariableName(),
                    extract.getExpression(), source, value, found));
        }
        return captures;
    }

    /** Convenience view of successful captures. */
    public Map<String, String> capturedValues(List<Capture> captures) {
        Map<String, String> values = new LinkedHashMap<>();
        if (captures == null) {
            return values;
        }
        for (Capture capture : captures) {
            if (capture.found()) {
                values.put(capture.variableName(), capture.value());
            }
        }
        return values;
    }

    private String read(String source, String expression, ResponseDto response) {
        return switch (source) {
            case "HEADER" -> response.header(expression);
            case "STATUS" -> String.valueOf(response.getStatusCode());
            default -> JsonPathReader.readAsText(response.getBody(), expression);
        };
    }
}
