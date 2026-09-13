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

    private boolean enabled = true;
}