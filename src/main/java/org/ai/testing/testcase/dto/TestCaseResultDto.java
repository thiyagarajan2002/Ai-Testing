package org.ai.testing.testcase.dto;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Everything the reports need about one executed test case.
 *
 * <p>This was previously a class nested inside {@code TestCaseExecutor}, which
 * forced every report generator and every caller to import the executor just to
 * name a result type. It is now a first-class DTO alongside the other models.</p>
 */
public class TestCaseResultDto {

    private String testCaseId;
    private String testCaseName;
    private String description;
    private String method;

    private TestStatus status = TestStatus.SKIPPED;
    private boolean executed;
    private boolean passed;
    private String message;

    /** Populated when the request itself failed rather than an assertion. */
    private String errorType;
    private String errorDetail;

    private BaseRequestDto request;
    private ResponseDto response;
    private ValidationSummaryDto validationSummary = new ValidationSummaryDto();

    private long executionTimeMs;
    private LocalDateTime startedAt;

    /** Which credentials were applied, for example {@code bearer}. */
    private String authApplied;

    /** Values this case captured into the variable store. */
    private Map<String, String> capturedVariables = new LinkedHashMap<>();

    /** Extracts that matched nothing, surfaced so a broken chain is obvious. */
    private Set<String> failedExtracts = new LinkedHashSet<>();

    private Set<String> tags = new LinkedHashSet<>();

    /** A reproduction command shown in the HTML report. */
    private String curlCommand;

    public String getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
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

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public BaseRequestDto getRequest() {
        return request;
    }

    public void setRequest(BaseRequestDto request) {
        this.request = request;
    }

    public ResponseDto getResponse() {
        return response;
    }

    public void setResponse(ResponseDto response) {
        this.response = response;
    }

    public ValidationSummaryDto getValidationSummary() {
        return validationSummary;
    }

    public void setValidationSummary(ValidationSummaryDto validationSummary) {
        this.validationSummary = validationSummary == null
                ? new ValidationSummaryDto() : validationSummary;
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

    public String getAuthApplied() {
        return authApplied;
    }

    public void setAuthApplied(String authApplied) {
        this.authApplied = authApplied;
    }

    public Map<String, String> getCapturedVariables() {
        return capturedVariables;
    }

    public void setCapturedVariables(Map<String, String> capturedVariables) {
        this.capturedVariables = capturedVariables == null
                ? new LinkedHashMap<>() : capturedVariables;
    }

    public Set<String> getFailedExtracts() {
        return failedExtracts;
    }

    public void setFailedExtracts(Set<String> failedExtracts) {
        this.failedExtracts = failedExtracts == null ? new LinkedHashSet<>() : failedExtracts;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? new LinkedHashSet<>() : tags;
    }

    public String getCurlCommand() {
        return curlCommand;
    }

    public void setCurlCommand(String curlCommand) {
        this.curlCommand = curlCommand;
    }

    /** Convenience for reports; {@code 0} when no response was captured. */
    public int statusCode() {
        return response == null ? 0 : response.getStatusCode();
    }

    public long responseTimeMs() {
        return response == null ? executionTimeMs : response.getResponseTimeMs();
    }

    @Override
    public String toString() {
        return status + " " + testCaseId + " " + testCaseName;
    }
}
