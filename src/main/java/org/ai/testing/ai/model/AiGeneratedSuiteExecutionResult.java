package org.ai.testing.ai.model;

import lombok.Data;
import org.ai.testing.testcase.executor.TestCaseExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Execution result for an approved AI-generated suite.
 * The result is kept separate from normal regression execution totals.
 */
@Data
public class AiGeneratedSuiteExecutionResult {

    private String sourceSuiteId;
    private String sourceTestCaseId;
    private boolean executed;
    private boolean passed;
    private String message;
    private int totalTestCases;
    private int passedTestCases;
    private int failedTestCases;
    private int skippedTestCases;
    private List<TestCaseExecutor.TestCaseExecutionResult> testCaseResults = new ArrayList<>();

    public void addResult(TestCaseExecutor.TestCaseExecutionResult result) {
        if (result == null) {
            return;
        }
        testCaseResults.add(result);
        totalTestCases++;
        if (!result.isExecuted()) {
            skippedTestCases++;
        } else if (result.isPassed()) {
            passedTestCases++;
        } else {
            failedTestCases++;
        }
        passed = failedTestCases == 0 && skippedTestCases == 0;
    }
}
