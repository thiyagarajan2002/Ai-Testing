package org.ai.testing.ai;

import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.ai.model.AiResponseAnalysis;
import org.ai.testing.ai.model.AiResponseAnalysisRequest;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;

/**
 * Connects executed test responses to response analysis and failure analysis.
 */
public class AiFailureInsightService {

    private final AiResponseAnalyzer responseAnalyzer;
    private final AiFailureAnalyzer failureAnalyzer;

    public AiFailureInsightService() {
        this.responseAnalyzer = new AiResponseAnalyzer();
        this.failureAnalyzer = new AiFailureAnalyzer();
    }

    public AiFailureAnalysis analyze(
            TestCaseExecutor.TestCaseExecutionResult executionResult,
            Integer expectedStatusCode,
            long responseTimeThresholdMs) {

        if (executionResult == null) {
            throw new IllegalArgumentException("execution result is required");
        }
        ResponseDto response = executionResult.getResponse();
        if (response == null) {
            throw new IllegalArgumentException("execution result response is required");
        }

        AiResponseAnalysisRequest request = new AiResponseAnalysisRequest();
        request.setExpectedStatusCode(expectedStatusCode);
        request.setResponse(response);
        request.setResponseTimeThresholdMs(responseTimeThresholdMs);

        AiResponseAnalysis analysis = responseAnalyzer.analyze(request);
        return failureAnalyzer.analyze(response, analysis, expectedStatusCode);
    }
}
