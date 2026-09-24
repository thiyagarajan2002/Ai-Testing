package org.ai.testing.testcase.dto;

import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ExtractDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One request plus the expectations that make it a test. */
public class TestCaseDto {

    private String testCaseId;
    private String testCaseName;
    private String description;
    private String method = "GET";

    private BaseRequestDto request;
    private Integer expectedStatusCode;
    private List<AssertionDto> assertions = new ArrayList<>();

    /** Values captured from the response for later test cases to reference. */
    private List<ExtractDto> extracts = new ArrayList<>();

    /** Variables defined just before this case runs. */
    private Map<String, String> preRequestVariables = new LinkedHashMap<>();

    private AuthDto auth;

    /** Free-form labels used by {@code --tag} filtering. */
    private Set<String> tags = new LinkedHashSet<>();

    /** Per-case overrides; {@code null} means inherit the run settings. */
    private Long timeoutMs;
    private Integer retries;

    private boolean enabled = true;

    public TestCaseDto() {
    }

    public TestCaseDto(String testCaseId, String testCaseName, String method) {
        this.testCaseId = testCaseId;
        this.testCaseName = testCaseName;
        this.method = method;
    }

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

    public BaseRequestDto getRequest() {
        return request;
    }

    public void setRequest(BaseRequestDto request) {
        this.request = request;
    }

    public Integer getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(Integer expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public List<AssertionDto> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<AssertionDto> assertions) {
        this.assertions = assertions == null ? new ArrayList<>() : new ArrayList<>(assertions);
    }

    public List<ExtractDto> getExtracts() {
        return extracts;
    }

    public void setExtracts(List<ExtractDto> extracts) {
        this.extracts = extracts == null ? new ArrayList<>() : new ArrayList<>(extracts);
    }

    public Map<String, String> getPreRequestVariables() {
        return preRequestVariables;
    }

    public void setPreRequestVariables(Map<String, String> preRequestVariables) {
        this.preRequestVariables = preRequestVariables == null
                ? new LinkedHashMap<>() : preRequestVariables;
    }

    public AuthDto getAuth() {
        return auth;
    }

    public void setAuth(AuthDto auth) {
        this.auth = auth;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? new LinkedHashSet<>() : tags;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TestCaseDto assertion(AssertionDto assertion) {
        if (assertion != null) {
            assertions.add(assertion);
        }
        return this;
    }

    public TestCaseDto extract(ExtractDto extract) {
        if (extract != null) {
            extracts.add(extract);
        }
        return this;
    }

    public TestCaseDto tag(String... labels) {
        if (labels != null) {
            for (String label : labels) {
                if (label != null && !label.isBlank()) {
                    tags.add(label.trim());
                }
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return testCaseId + " " + method + " " + testCaseName;
    }
}
