package org.ai.testing.testsuite.dto;

import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.testcase.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A folder of related test cases. */
public class TestSuiteDto {

    private String suiteId;
    private String suiteName;
    private String description;
    private boolean enabled = true;

    private List<TestCaseDto> testCases = new ArrayList<>();

    private AuthDto auth;
    private Map<String, String> variables = new LinkedHashMap<>();
    private Set<String> tags = new LinkedHashSet<>();

    /** When true, the remaining cases are skipped after the first failure. */
    private boolean stopOnFailure;

    public TestSuiteDto() {
    }

    public TestSuiteDto(String suiteId, String suiteName) {
        this.suiteId = suiteId;
        this.suiteName = suiteName;
    }

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<TestCaseDto> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseDto> testCases) {
        this.testCases = testCases == null ? new ArrayList<>() : new ArrayList<>(testCases);
    }

    public AuthDto getAuth() {
        return auth;
    }

    public void setAuth(AuthDto auth) {
        this.auth = auth;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables == null ? new LinkedHashMap<>() : variables;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? new LinkedHashSet<>() : tags;
    }

    public boolean isStopOnFailure() {
        return stopOnFailure;
    }

    public void setStopOnFailure(boolean stopOnFailure) {
        this.stopOnFailure = stopOnFailure;
    }

    public TestSuiteDto add(TestCaseDto testCase) {
        if (testCase != null) {
            testCases.add(testCase);
        }
        return this;
    }

    @Override
    public String toString() {
        return suiteId + " " + suiteName + " (" + testCases.size() + " cases)";
    }
}
