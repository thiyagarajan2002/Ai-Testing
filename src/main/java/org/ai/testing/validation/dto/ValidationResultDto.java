package org.ai.testing.validation.dto;

import lombok.Data;

@Data
public class ValidationResultDto {

    private boolean passed;

    private String validationType;

    private String field;

    private String expected;

    private String actual;

    private String message;
}