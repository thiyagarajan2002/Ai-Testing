package org.ai.testing.dto.common;

import java.util.ArrayList;
import java.util.List;

/**
 * The payload of a request.
 *
 * <p>{@code rawBody} is what finally goes on the wire. For
 * {@link BodyMode#URLENCODED} and {@link BodyMode#FORMDATA} it is produced from
 * {@code formFields} by the request normaliser, so callers only supply fields.</p>
 */
public class RequestBodyDto {

    private BodyMode mode = BodyMode.RAW;
    private String contentType;
    private String rawBody;
    private List<QueryParamDto> formFields = new ArrayList<>();

    public RequestBodyDto() {
    }

    public static RequestBodyDto json(String rawBody) {
        RequestBodyDto body = new RequestBodyDto();
        body.mode = BodyMode.JSON;
        body.contentType = "application/json";
        body.rawBody = rawBody;
        return body;
    }

    public static RequestBodyDto text(String rawBody) {
        RequestBodyDto body = new RequestBodyDto();
        body.mode = BodyMode.TEXT;
        body.contentType = "text/plain";
        body.rawBody = rawBody;
        return body;
    }

    public static RequestBodyDto form(List<QueryParamDto> fields) {
        RequestBodyDto body = new RequestBodyDto();
        body.mode = BodyMode.URLENCODED;
        body.contentType = "application/x-www-form-urlencoded";
        body.setFormFields(fields);
        return body;
    }

    public BodyMode getMode() {
        return mode;
    }

    public void setMode(BodyMode mode) {
        this.mode = mode == null ? BodyMode.RAW : mode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody;
    }

    public List<QueryParamDto> getFormFields() {
        return formFields;
    }

    public void setFormFields(List<QueryParamDto> formFields) {
        this.formFields = formFields == null ? new ArrayList<>() : formFields;
    }

    public boolean isEmpty() {
        return (rawBody == null || rawBody.isEmpty())
                && (formFields == null || formFields.isEmpty());
    }

    public RequestBodyDto copy() {
        RequestBodyDto copy = new RequestBodyDto();
        copy.mode = mode;
        copy.contentType = contentType;
        copy.rawBody = rawBody;
        for (QueryParamDto field : formFields) {
            if (field != null) {
                copy.formFields.add(field.copy());
            }
        }
        return copy;
    }
}
