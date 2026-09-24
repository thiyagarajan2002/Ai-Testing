package org.ai.testing.TestData.dto;

import java.util.List;

public class TestCaseResponseDTO {

    private List<TestCaseDTO> testCases;

    public List<TestCaseDTO> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseDTO> testCases) {
        this.testCases = testCases;
    }

    @Override
    public String toString() {
        return "TestCaseResponseDTO{" +
                "testCases=" + testCases +
                '}';
    }
}