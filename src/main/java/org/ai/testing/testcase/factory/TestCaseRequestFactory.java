package org.ai.testing.testcase.factory;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.executor.ExecutorDispatcher;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.util.Strings;

/**
 * Produces the request that will actually be sent for a test case.
 *
 * <p>The returned object is always a deep copy, so a run never mutates the test
 * definition. That matters once variables are substituted: without the copy, a
 * second run of the same plan would resolve placeholders that are no longer
 * there.</p>
 */
public class TestCaseRequestFactory {

    public BaseRequestDto createRequest(TestCaseDto testCase) {

        if (testCase == null) {
            throw new IllegalArgumentException("Test case cannot be null");
        }
        if (Strings.isBlank(testCase.getMethod())) {
            throw new IllegalArgumentException(
                    "HTTP method cannot be null or empty for test case: "
                            + testCase.getTestCaseId());
        }
        if (!ExecutorDispatcher.supports(testCase.getMethod())) {
            throw new IllegalArgumentException(
                    "Unsupported HTTP method: " + testCase.getMethod()
                            + ". Supported methods: "
                            + String.join(", ", ExecutorDispatcher.supportedMethods()));
        }
        if (testCase.getRequest() == null) {
            throw new IllegalArgumentException(
                    "Request cannot be null for test case: " + testCase.getTestCaseId());
        }
        if (Strings.isBlank(testCase.getRequest().getUrl())) {
            throw new IllegalArgumentException(
                    "Request URL cannot be null or empty for test case: "
                            + testCase.getTestCaseId());
        }

        BaseRequestDto request = testCase.getRequest().copy();
        if (request.getAuth() == null && testCase.getAuth() != null) {
            request.setAuth(testCase.getAuth().copy());
        }
        return request;
    }
}
