package org.ai.testing.ai;

import com.sun.net.httpserver.HttpServer;
import org.ai.testing.ai.model.AiFailureAnalysis;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AiPerTestInsightIntegrationTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/failure", exchange -> {
            byte[] body = "{\"error\":\"database unavailable\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldAttachAiFailureInsightToExecutedTestCase() {
        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("TC-AI-001");
        testCase.setTestCaseName("AI failure insight integration");
        testCase.setMethod("GET");
        testCase.setEnabled(true);

        BaseRequestDto request = new BaseRequestDto();
        request.setUrl(baseUrl + "/failure");
        testCase.setRequest(request);
        testCase.setExpectedStatusCode(200);

        TestCaseExecutor.TestCaseExecutionResult result = new TestCaseExecutor().execute(testCase);

        assertTrue(result.isExecuted());
        assertFalse(result.isPassed());
        assertNotNull(result.getResponse());

        AiFailureAnalysis insight = result.getAiFailureAnalysis();
        assertNotNull(insight);
        assertTrue(insight.isFailureDetected());
        assertEquals("SERVER_ERROR", insight.getCategory());
        assertEquals("CRITICAL", insight.getSeverity());
        assertFalse(insight.getEvidence().isEmpty());
        assertFalse(insight.getRecommendations().isEmpty());
    }
}
