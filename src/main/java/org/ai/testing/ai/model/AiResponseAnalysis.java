package org.ai.testing.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiResponseAnalysis {

    private boolean healthy;

    private int statusCode;

    private String summary;

    private List<String> findings = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();

    private String severity;

    private String strategy = "heuristic-ai-v1";
}
