package org.ai.testing.testrun.dto;

import lombok.Data;
import org.ai.testing.testsuite.dto.TestSuiteDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TestRunDto {

    private String runId;

    private String runName;

    private String environment;

    private String executionMode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private long executionTimeMs;

    private boolean passed;
    private org.ai.testing.dto.common.AuthDto auth;

    private java.util.Map<String, String> collectionVariables =
            new java.util.LinkedHashMap<>();

    private java.util.Map<String, String> environmentVariables =
            new java.util.LinkedHashMap<>();

    private List<TestSuiteDto> testSuites =
            new ArrayList<>();

}