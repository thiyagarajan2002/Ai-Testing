package org.ai.testing.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiFailureAnalysis {

    private boolean failureDetected;
    private String severity = "INFO";
    private String category = "NONE";
    private String summary;
    private String likelyRootCause;
    private List<String> evidence = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private String strategy = "heuristic-ai-v1";
}
