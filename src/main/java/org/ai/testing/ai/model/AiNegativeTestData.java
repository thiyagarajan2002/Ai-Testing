package org.ai.testing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiNegativeTestData {

    private String scenario;
    private String reason;
    private String requestBody;
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> queryParams = new LinkedHashMap<>();
    private Map<String, String> pathParams = new LinkedHashMap<>();
    private Integer expectedStatusCode;
    private String strategy = "heuristic-ai-v1";
}
