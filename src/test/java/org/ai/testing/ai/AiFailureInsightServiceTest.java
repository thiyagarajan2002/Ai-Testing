package org.ai.testing.ai;

import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiFailureInsightServiceTest {

    @Test
    void shouldAnalyzeFailedExecutedTestCase() {
        TestCaseExecutor.TestCaseExecutionResult executionResult =
                new TestCaseExecutor.TestCaseExecutionResult();

        ResponseDto response = new ResponseDto();
        response.setStatusCode(500);
        response.setStatusMessage("Internal Server Error");
        response.setBody("{\"error\":\"database unavailable\"}");
        response.setResponseTimeMs(120);
        response.getHeaders().put("Content-Type", "application/json");
        executionResult.setResponse(response);
        executionResult.setExecuted(true);
        executionResult.setPassed(false);
        executionResult.setMessage("Test case failed");

        AiFailureAnalysis analysis = new AiFailureInsightService().analyze(
                executionResult,
                200,
                2000);

        assertTrue(analysis.isFailureDetected());
        assertEquals("SERVER_ERROR", analysis.getCategory());
        assertEquals("CRITICAL", analysis.getSeverity());
        assertFalse(analysis.getRecommendations().isEmpty());
        assertFalse(analysis.getEvidence().isEmpty());
    }

    @Test
    void shouldReportHealthyExecutedTestCaseAsNoFailure() {
        TestCaseExecutor.TestCaseExecutionResult executionResult =
                new TestCaseExecutor.TestCaseExecutionResult();

        ResponseDto response = new ResponseDto();
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.setBody("{\"id\":1}");
        response.setResponseTimeMs(50);
        response.getHeaders().put("Content-Type", "application/json");
        executionResult.setResponse(response);
        executionResult.setExecuted(true);
        executionResult.setPassed(true);

        AiFailureAnalysis analysis = new AiFailureInsightService().analyze(
                executionResult,
                200,
                2000);

        assertFalse(analysis.isFailureDetected());
        assertEquals("NONE", analysis.getCategory());
        assertEquals("INFO", analysis.getSeverity());
    }

    @Test
    void shouldRejectExecutionResultWithoutResponse() {
        TestCaseExecutor.TestCaseExecutionResult executionResult =
                new TestCaseExecutor.TestCaseExecutionResult();

        assertThrows(IllegalArgumentException.class, () ->
                new AiFailureInsightService().analyze(executionResult, 200, 2000));
    }
}
