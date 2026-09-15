package org.ai.testing.ai.model;

import lombok.Data;
import org.ai.testing.testcase.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiGeneratedNegativeTestSuite {

    private String sourceTestCaseId;
    private String strategy = "heuristic-ai-v1";
    private boolean enabled = false;
    private List<TestCaseDto> testCases = new ArrayList<>();

    public int getTestCaseCount() {
        return testCases == null ? 0 : testCases.size();
    }
}
