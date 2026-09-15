package org.ai.testing.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiNegativeTestDataResult {

    private String strategy = "heuristic-ai-v1";
    private List<AiNegativeTestData> testData = new ArrayList<>();

    public void add(AiNegativeTestData data) {
        testData.add(data);
    }
}
