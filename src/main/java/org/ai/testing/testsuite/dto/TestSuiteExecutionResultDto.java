package org.ai.testing.testsuite.dto;

import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Aggregated outcome of one suite. */
public class TestSuiteExecutionResultDto {

    private String suiteId;
    private String suiteName;
    private String description;

    private TestStatus status = TestStatus.SKIPPED;
    private boolean executed;
    private boolean passed;
    private String message;

    private long executionTimeMs;
    private LocalDateTime startedAt;

    private int totalTestCases;
    private int passedTestCases;
    private int failedTestCases;
    private int erroredTestCases;
    private int skippedTestCases;

    private List<TestCaseResultDto> testResults = new ArrayList<>();

    public String getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(String suiteId) {
        this.suiteId = suiteId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status == null ? TestStatus.SKIPPED : status;
        this.passed = this.status == TestStatus.PASSED;
        this.executed = this.status != TestStatus.SKIPPED;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public int getTotalTestCases() {
        return totalTestCases;
    }

    public void setTotalTestCases(int totalTestCases) {
        this.totalTestCases = totalTestCases;
    }

    public int getPassedTestCases() {
        return passedTestCases;
    }

    public void setPassedTestCases(int passedTestCases) {
        this.passedTestCases = passedTestCases;
    }

    public int getFailedTestCases() {
        return failedTestCases;
    }

    public void setFailedTestCases(int failedTestCases) {
        this.failedTestCases = failedTestCases;
    }

    public int getErroredTestCases() {
        return erroredTestCases;
    }

    public void setErroredTestCases(int erroredTestCases) {
        this.erroredTestCases = erroredTestCases;
    }

    public int getSkippedTestCases() {
        return skippedTestCases;
    }

    public void setSkippedTestCases(int skippedTestCases) {
        this.skippedTestCases = skippedTestCases;
    }

    public List<TestCaseResultDto> getTestResults() {
        return testResults;
    }

    public void setTestResults(List<TestCaseResultDto> testResults) {
        this.testResults = testResults == null ? new ArrayList<>() : testResults;
    }

    /** Recomputes counters and status from the collected case results. */
    public void tally() {
        totalTestCases = testResults.size();
        passedTestCases = 0;
        failedTestCases = 0;
        erroredTestCases = 0;
        skippedTestCases = 0;

        for (TestCaseResultDto result : testResults) {
            switch (result.getStatus()) {
                case PASSED -> passedTestCases++;
                case FAILED -> failedTestCases++;
                case ERROR -> erroredTestCases++;
                case SKIPPED -> skippedTestCases++;
            }
        }

        // A suite with only skipped cases is itself skipped; skipped cases
        // alongside passing ones do not make the suite fail.
        if (totalTestCases == 0) {
            setStatus(TestStatus.SKIPPED);
        } else if (erroredTestCases > 0) {
            setStatus(TestStatus.ERROR);
        } else if (failedTestCases > 0) {
            setStatus(TestStatus.FAILED);
        } else if (passedTestCases == 0) {
            setStatus(TestStatus.SKIPPED);
        } else {
            setStatus(TestStatus.PASSED);
        }
    }

    @Override
    public String toString() {
        return status + " " + suiteName;
    }
}
