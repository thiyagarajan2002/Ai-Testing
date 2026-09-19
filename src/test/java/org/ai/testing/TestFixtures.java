package org.ai.testing;

import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.validation.dto.ValidationResultDto;

import java.time.LocalDateTime;

/** Builders for result objects, so report tests stay short. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static ResponseDto response(int status, String body) {
        ResponseDto response = new ResponseDto();
        response.setStatusCode(status);
        response.setStatusMessage(org.ai.testing.util.HttpStatus.reasonPhrase(status));
        response.setBody(body);
        response.setBodySizeBytes(body == null ? 0 : body.length());
        response.setResponseTimeMs(42);
        response.getHeaders().put("Content-Type", "application/json");
        response.getHeaders().put("X-Trace", "abc-123");
        return response;
    }

    public static TestCaseResultDto caseResult(String id, String name, TestStatus status) {
        TestCaseResultDto result = new TestCaseResultDto();
        result.setTestCaseId(id);
        result.setTestCaseName(name);
        result.setMethod("GET");
        result.setStatus(status);
        result.setMessage(status.name() + " message");
        result.setStartedAt(LocalDateTime.now());
        result.setExecutionTimeMs(15);
        if (status != TestStatus.SKIPPED) {
            result.setResponse(response(status == TestStatus.PASSED ? 200 : 500, "{\"ok\":true}"));
            ValidationResultDto validation = new ValidationResultDto();
            validation.setPassed(status == TestStatus.PASSED);
            validation.setValidationType("STATUS_CODE");
            validation.setField("statusCode");
            validation.setOperator("EQUALS");
            validation.setExpected("200");
            validation.setActual(status == TestStatus.PASSED ? "200" : "500");
            validation.setMessage("checked");
            result.getValidationSummary().add(validation);
        }
        return result;
    }

    public static TestSuiteExecutionResultDto suiteResult(String name,
                                                          TestCaseResultDto... cases) {
        TestSuiteExecutionResultDto suite = new TestSuiteExecutionResultDto();
        suite.setSuiteId("SUITE-" + name);
        suite.setSuiteName(name);
        suite.setStartedAt(LocalDateTime.now());
        suite.setExecutionTimeMs(80);
        for (TestCaseResultDto result : cases) {
            suite.getTestResults().add(result);
        }
        suite.tally();
        return suite;
    }

    public static TestRunResultDto runResult(String name,
                                             TestSuiteExecutionResultDto... suites) {
        TestRunResultDto run = new TestRunResultDto();
        run.setRunId("RUN-1");
        run.setRunName(name);
        run.setEnvironment("test");
        run.setExecutionMode("SEQUENTIAL");
        run.setStartTime(LocalDateTime.now().minusSeconds(2));
        run.setEndTime(LocalDateTime.now());
        run.setExecutionTimeMs(2000);
        for (TestSuiteExecutionResultDto suite : suites) {
            run.getSuiteResults().add(suite);
        }
        run.tally();
        run.setMessage("fixture run");
        return run;
    }
}
