package org.ai.testing.dto.common;

/** A request header, keeping the enabled flag used by Postman and Bruno. */
public class HeaderDto {

    private String name;
    private String value;
    private boolean enabled = true;

    public HeaderDto() {
    }

    public HeaderDto(String name, String value) {
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

    public HeaderDto copy() {
        HeaderDto copy = new HeaderDto(name, value);
        copy.enabled = enabled;
        return copy;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
