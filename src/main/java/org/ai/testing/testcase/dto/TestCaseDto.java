package org.ai.testing.testcase.dto;


import lombok.Data;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestCaseDto {

    private String testCaseId;

    private String testCaseName;

    private String description;

    private String method;

    private BaseRequestDto request;

    private Integer expectedStatusCode;

    private List<AssertionDto> assertions =
            new ArrayList<>();

    private java.util.List<org.ai.testing.dto.common.ExtractDto> extracts =
            new ArrayList<>();
    private java.util.Map<String, String> preRequestVariables =
            new java.util.LinkedHashMap<>();
    private org.ai.testing.dto.common.AuthDto auth;

    private boolean enabled = true;
}