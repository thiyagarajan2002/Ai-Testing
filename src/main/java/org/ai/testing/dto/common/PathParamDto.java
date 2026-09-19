package org.ai.testing.dto.common;

/** A {@code {placeholder}} value substituted into the request URL. */
public class PathParamDto {

    private String name;
    private String value;

    public PathParamDto() {
    }

    public PathParamDto(String name, String value) {
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

    public PathParamDto copy() {
        return new PathParamDto(name, value);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
