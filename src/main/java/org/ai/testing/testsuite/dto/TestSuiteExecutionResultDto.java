package org.ai.testing.testsuite.dto;


import lombok.Data;
import org.ai.testing.testcase.executor.TestCaseExecutor;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestSuiteExecutionResultDto {

    private String suiteId;

    private String suiteName;

    private boolean executed;

    private boolean passed;

    private String message;

    private long executionTimeMs;

    private int totalTestCases;

    private int passedTestCases;

    private int failedTestCases;

    private int skippedTestCases;

    private List<TestCaseExecutor.TestCaseExecutionResult>
            testResults = new ArrayList<>();
}