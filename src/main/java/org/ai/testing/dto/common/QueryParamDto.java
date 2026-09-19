package org.ai.testing.dto.common;

/** A query-string parameter or a form field. */
public class QueryParamDto {

    private String name;
    private String value;
    private boolean enabled = true;

    public QueryParamDto() {
    }

    public QueryParamDto(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public QueryParamDto copy() {
        QueryParamDto copy = new QueryParamDto(name, value);
        copy.enabled = enabled;
        return copy;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
