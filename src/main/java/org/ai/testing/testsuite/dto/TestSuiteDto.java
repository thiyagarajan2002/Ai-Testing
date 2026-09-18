package org.ai.testing.testsuite.dto;


import lombok.Data;
import org.ai.testing.testcase.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestSuiteDto {

    private String suiteId;

    private String suiteName;

    private String description;

    private boolean enabled = true;

    private List<TestCaseDto> testCases =
            new ArrayList<>();

    private org.ai.testing.dto.common.AuthDto auth;
    private java.util.Map<String, String> variables =
            new java.util.LinkedHashMap<>();

}