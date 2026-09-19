package org.ai.testing.dto.common;

import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

/** One expectation checked against a captured response. */
public class AssertionDto {

    private AssertionType type;

    /**
     * What the assertion targets: a header name for {@link AssertionType#HEADER},
     * a JSONPath for {@link AssertionType#JSON_PATH}, otherwise a label.
     */
    private String field;

    private AssertionOperator operator;
    private String expectedValue;

    /** Human-readable intent, shown in reports when present. */
    private String description;

    private boolean enabled = true;

    public AssertionDto() {
    }

    public AssertionDto(AssertionType type, String field,
                        AssertionOperator operator, String expectedValue) {
        this.type = type;
        this.field = field;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    public static AssertionDto status(int expected) {
        return new AssertionDto(AssertionType.STATUS_CODE, "statusCode",
                AssertionOperator.EQUALS, String.valueOf(expected));
    }

    public static AssertionDto body(AssertionOperator operator, String expected) {
        return new AssertionDto(AssertionType.RESPONSE_BODY, "body", operator, expected);
    }

    public static AssertionDto header(String name, AssertionOperator operator, String expected) {
        return new AssertionDto(AssertionType.HEADER, name, operator, expected);
    }

    public static AssertionDto jsonPath(String path, AssertionOperator operator, String expected) {
        return new AssertionDto(AssertionType.JSON_PATH, path, operator, expected);
    }

    public static AssertionDto responseTimeBelow(long milliseconds) {
        return new AssertionDto(AssertionType.RESPONSE_TIME, "responseTimeMs",
                AssertionOperator.LESS_THAN, String.valueOf(milliseconds));
    }

    public AssertionType getType() {
        return type;
    }

    public void setType(AssertionType type) {
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public AssertionOperator getOperator() {
        return operator;
    }

    public void setOperator(AssertionOperator operator) {
        this.operator = operator;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AssertionDto describedAs(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        return type + " " + field + " " + operator + " " + expectedValue;
    }
}
