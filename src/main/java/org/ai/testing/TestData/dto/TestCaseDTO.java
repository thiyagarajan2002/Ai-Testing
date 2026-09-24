package org.ai.testing.TestData.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class TestCaseDTO {

    private String api;
    private String method;
    private String testCaseId;
    private String category;
    private String priority;
    private String riskRationale;
    private String scenario;

    private JsonNode requestData;

    private int expectedStatusCode;
    private String expectedResponse;

    public String getApi() {
        return api;
    }

    public String getMethod() {
        return method;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public String getCategory() {
        return category;
    }

    public String getPriority() {
        return priority;
    }

    public String getRiskRationale() {
        return riskRationale;
    }

    public String getScenario() {
        return scenario;
    }

    public JsonNode getRequestData() {
        return requestData;
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public String getExpectedResponse() {
        return expectedResponse;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setRiskRationale(String riskRationale) {
        this.riskRationale = riskRationale;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public void setRequestData(JsonNode requestData) {
        this.requestData = requestData;
    }

    public void setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    @Override
    public String toString() {
        return "TestCaseDTO{" +
                "api='" + api + '\'' +
                ", method='" + method + '\'' +
                ", testCaseId='" + testCaseId + '\'' +
                ", category='" + category + '\'' +
                ", priority='" + priority + '\'' +
                ", scenario='" + scenario + '\'' +
                ", expectedStatusCode=" + expectedStatusCode +
                ", expectedResponse='" + expectedResponse + '\'' +
                ", requestData=" + requestData +
                '}';
    }
}