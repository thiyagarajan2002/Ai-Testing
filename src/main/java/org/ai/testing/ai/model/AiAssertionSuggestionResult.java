package org.ai.testing.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiAssertionSuggestionResult {

    private String analyzerVersion = "heuristic-ai-v1";
    private List<AiAssertionSuggestion> suggestions = new ArrayList<>();

    public void add(AiAssertionSuggestion suggestion) {
        suggestions.add(suggestion);
    }
}
