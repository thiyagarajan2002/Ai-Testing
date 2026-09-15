package org.ai.testing.ai.model;

import lombok.Data;
import org.ai.testing.testcase.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

/** Container for AI generated test cases and generation metadata. */
@Data
public class AiGeneratedTestSuite {
    private String generator = "heuristic-ai-v1";
    private String strategy = "API contract and negative-path analysis";
    private List<TestCaseDto> testCases = new ArrayList<>();

    public int getTestCaseCount() {
        return testCases == null ? 0 : testCases.size();
    }
}
