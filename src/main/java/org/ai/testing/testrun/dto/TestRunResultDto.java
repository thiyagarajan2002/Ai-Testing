package org.ai.testing.testrun.dto;


import lombok.Data;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TestRunResultDto {

    private String runId;

    private String runName;

    private String environment;

    private String executionMode;

    private boolean executed;

    private boolean passed;

    private String message;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private long executionTimeMs;

    private int totalSuites;

    private int passedSuites;

    private int failedSuites;

    private int skippedSuites;

    private int totalTestCases;

    private int passedTestCases;

    private int failedTestCases;

    private int skippedTestCases;

    private List<TestSuiteExecutionResultDto> suiteResults =
            new ArrayList<>();
}