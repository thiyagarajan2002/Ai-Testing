package org.ai.testing.dto.common;

/**
 * Captures a value from a response into the runtime variable store so a later
 * test case can reference it as <code>{{variableName}}</code>.
 */
public class ExtractDto {

    private String variableName;

    /** A JSONPath when source is {@code BODY}, a header name when {@code HEADER}. */
    private String expression;

    /** {@code BODY}, {@code HEADER} or {@code STATUS}. */
    private String source = "BODY";

    public ExtractDto() {
    }

    public ExtractDto(String variableName, String expression, String source) {
        this.variableName = variableName;
        this.expression = expression;
        this.source = source;
    }

    public static ExtractDto fromBody(String variableName, String jsonPath) {
        return new ExtractDto(variableName, jsonPath, "BODY");
    }

    public static ExtractDto fromHeader(String variableName, String headerName) {
        return new ExtractDto(variableName, headerName, "HEADER");
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return variableName + " <- " + source + ":" + expression;
    }
}
