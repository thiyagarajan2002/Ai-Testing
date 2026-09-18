package org.ai.testing.extract;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.ai.testing.dto.common.ExtractDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.env.VariableStore;

import java.util.List;

public class ResponseExtractor {

    public void extract(List<ExtractDto> extracts, ResponseDto response, VariableStore store) {
        if (extracts == null || response == null || store == null) {
            return;
        }
        for (ExtractDto extract : extracts) {
            if (extract == null || extract.getVariableName() == null
                    || extract.getVariableName().isBlank()) {
                continue;
            }
            String value = read(extract, response);
            if (value != null) {
                store.putRuntime(extract.getVariableName(), value);
            }
        }
    }

    private String read(ExtractDto extract, ResponseDto response) {
        String source = extract.getSource() == null ? "BODY" : extract.getSource();
        if ("HEADER".equalsIgnoreCase(source)) {
            if (response.getHeaders() == null || extract.getExpression() == null) {
                return null;
            }
            return response.getHeaders().entrySet().stream()
                    .filter(entry -> entry.getKey() != null
                            && entry.getKey().equalsIgnoreCase(extract.getExpression()))
                    .map(entry -> entry.getValue())
                    .findFirst()
                    .orElse(null);
        }
        return readJsonPath(response.getBody(), extract.getExpression());
    }

    public String readJsonPath(String body, String expression) {
        if (body == null || body.isBlank() || expression == null || expression.isBlank()) {
            return null;
        }
        String path = expression.trim();
        if (path.startsWith("res.body.")) {
            path = "$." + path.substring("res.body.".length());
        } else if ("res.body".equals(path)) {
            return body;
        } else if (!path.startsWith("$")) {
            path = "$." + path;
        }
        try {
            Object value = JsonPath.read(body, path);
            return value == null ? null : String.valueOf(value);
        } catch (PathNotFoundException | IllegalArgumentException e) {
            return null;
        }
    }
}
