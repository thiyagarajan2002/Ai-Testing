package org.ai.testing.validation.dto;

/** The outcome of one assertion. */
public class ValidationResultDto {

    private boolean passed;
    private String validationType;
    private String field;
    private String operator;
    private String expected;
    private String actual;
    private String message;
    private String description;

    public ValidationResultDto() {
    }

    public static ValidationResultDto failed(String type, String field, String message) {
        ValidationResultDto result = new ValidationResultDto();
        result.passed = false;
        result.validationType = type;
        result.field = field;
        result.message = message;
        return result;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return (passed ? "PASS " : "FAIL ") + validationType + " " + field;
    }
}
