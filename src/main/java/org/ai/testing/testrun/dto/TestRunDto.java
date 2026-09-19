package org.ai.testing.testrun.dto;

import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A complete execution plan: suites, variables, credentials and settings. */
public class TestRunDto {

    private String runId;
    private String runName;
    private String environment = "default";
    private String description;

    private AuthDto auth;

    private Map<String, String> collectionVariables = new LinkedHashMap<>();
    private Map<String, String> environmentVariables = new LinkedHashMap<>();

    private List<TestSuiteDto> testSuites = new ArrayList<>();

    private RunOptions options = new RunOptions();

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AuthDto getAuth() {
        return auth;
    }

    public void setAuth(AuthDto auth) {
        this.auth = auth;
    }

    public Map<String, String> getCollectionVariables() {
        return collectionVariables;
    }

    public void setCollectionVariables(Map<String, String> collectionVariables) {
        this.collectionVariables = collectionVariables == null
                ? new LinkedHashMap<>() : collectionVariables;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables == null
                ? new LinkedHashMap<>() : environmentVariables;
    }

    public List<TestSuiteDto> getTestSuites() {
        return testSuites;
    }

    public void setTestSuites(List<TestSuiteDto> testSuites) {
        this.testSuites = testSuites == null ? new ArrayList<>() : new ArrayList<>(testSuites);
    }

    public RunOptions getOptions() {
        return options;
    }

    public void setOptions(RunOptions options) {
        this.options = options == null ? new RunOptions() : options;
    }

    /** Kept for compatibility with the previous API. */
    public String getExecutionMode() {
        return options.getExecutionMode();
    }

    public void setExecutionMode(String executionMode) {
        options.setExecutionMode(executionMode);
    }

    public TestRunDto add(TestSuiteDto suite) {
        if (suite != null) {
            testSuites.add(suite);
        }
        return this;
    }

    public int totalTestCases() {
        return testSuites.stream()
                .filter(suite -> suite != null)
                .mapToInt(suite -> suite.getTestCases().size())
                .sum();
    }

    @Override
    public String toString() {
        return runId + " " + runName;
    }
}
