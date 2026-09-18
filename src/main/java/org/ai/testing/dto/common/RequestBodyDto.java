package org.ai.testing.dto.common;

import lombok.Data;

@Data
public class RequestBodyDto {

    private String contentType;

    private String rawBody;

    public void setMode(BodyMode bodyMode) {
    }

    public QueryParamDto[] getFormFields() {
        return null;
    }
}