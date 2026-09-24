package org.ai.testing.testsuite;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.env.VariableStore;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testrun.dto.RunOptions;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.testsuite.executor.TestSuiteExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Suite execution")
class TestSuiteExecutorTest {

    /** Stands in for the real executor so no network is needed. */
    private static class StubExecutor extends TestCaseExecutor {

        private final TestStatus statusToReturn;
        int invocations;

        StubExecutor(TestStatus statusToReturn) {
            this.statusToReturn = statusToReturn;
        }

        @Override
        public TestCaseResultDto execute(TestCaseDto testCase, VariableStore store,
                                         AuthScope scope) {
            invocations++;
            TestCaseResultDto result = new TestCaseResultDto();
            result.setTestCaseId(testCase.getTestCaseId());
            result.setTestCaseName(testCase.getTestCaseName());
            result.setMethod(testCase.getMethod());
            result.setStatus(statusToReturn);
            result.setMessage("stubbed");
            return result;
        }
    }

    private TestCaseDto testCase(String id, boolean enabled, String... tags) {
        TestCaseDto testCase = new TestCaseDto(id, "Case " + id, "GET");
        BaseRequestDto request = new BaseRequestDto();
        request.setUrl("https://api.test/" + id);
        testCase.setRequest(request);
        testCase.setEnabled(enabled);
        testCase.tag(tags);
        return testCase;
    }

    @Test
    @DisplayName("a disabled case is skipped and does not fail the suite")
    void disabledCaseDoesNotFailSuite() {
        // Regression: the previous version required skippedTestCases == 0 to
        // mark a suite as passed, so any disabled case failed the whole suite.
        TestSuiteDto suite = new TestSuiteDto("S1", "Mixed");
        suite.add(testCase("a", true));
        suite.add(testCase("b", false));

        TestSuiteExecutionResultDto result = new TestSuiteExecutor(
                new StubExecutor(TestStatus.PASSED), new RunOptions())
                .execute(suite, new VariableStore());

        assertEquals(TestStatus.PASSED, result.getStatus());
        assertEquals(1, result.getPassedTestCases());
        assertEquals(1, result.getSkippedTestCases());
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("a suite of only disabled cases is skipped, not passed")
    void allDisabledSuiteIsSkipped() {
        TestSuiteDto suite = new TestSuiteDto("S2", "All off");
        suite.add(testCase("a", false));

        TestSuiteExecutionResultDto result = new TestSuiteExecutor(
                new StubExecutor(TestStatus.PASSED), new RunOptions())
                .execute(suite, new VariableStore());

        assertEquals(TestStatus.SKIPPED, result.getStatus());
    }

    @Test
    @DisplayName("include tags keep only matching cases")
    void filtersByIncludeTag() {
        TestSuiteDto suite = new TestSuiteDto("S3", "Tagged");
        suite.add(testCase("a", true, "smoke"));
        suite.add(testCase("b", true, "regression"));

        RunOptions options = new RunOptions();
        options.setIncludeTags(Set.of("smoke"));

        StubExecutor stub = new StubExecutor(TestStatus.PASSED);
        TestSuiteExecutionResultDto result =
                new TestSuiteExecutor(stub, options).execute(suite, new VariableStore());

        assertEquals(1, stub.invocations);
        assertEquals(1, result.getPassedTestCases());
        assertEquals(1, result.getSkippedTestCases());
    }

    @Test
    @DisplayName("exclude tags win over include tags")
    void excludeTagWins() {
        TestSuiteDto suite = new TestSuiteDto("S4", "Tagged");
        suite.add(testCase("a", true, "smoke", "slow"));

        RunOptions options = new RunOptions();
        options.setIncludeTags(Set.of("smoke"));
        options.setExcludeTags(Set.of("slow"));

        StubExecutor stub = new StubExecutor(TestStatus.PASSED);
        new TestSuiteExecutor(stub, options).execute(suite, new VariableStore());

        assertEquals(0, stub.invocations);
    }

    @Test
    @DisplayName("stopOnFailure halts the remaining cases")
    void stopsOnFailure() {
        TestSuiteDto suite = new TestSuiteDto("S5", "Halting");
        suite.setStopOnFailure(true);
        suite.add(testCase("a", true));
        suite.add(testCase("b", true));
        suite.add(testCase("c", true));

        StubExecutor stub = new StubExecutor(TestStatus.FAILED);
        TestSuiteExecutionResultDto result =
                new TestSuiteExecutor(stub, new RunOptions()).execute(suite, new VariableStore());

        assertEquals(1, stub.invocations);
        assertEquals(1, result.getFailedTestCases());
        assertEquals(2, result.getSkippedTestCases());
        assertEquals(TestStatus.FAILED, result.getStatus());
    }

    @Test
    @DisplayName("an errored case marks the suite as errored")
    void erroredCasePropagates() {
        TestSuiteDto suite = new TestSuiteDto("S6", "Broken");
        suite.add(testCase("a", true));

        TestSuiteExecutionResultDto result = new TestSuiteExecutor(
                new StubExecutor(TestStatus.ERROR), new RunOptions())
                .execute(suite, new VariableStore());

        assertEquals(TestStatus.ERROR, result.getStatus());
    }

    @Test
    @DisplayName("a disabled suite skips every case without running one")
    void disabledSuiteRunsNothing() {
        TestSuiteDto suite = new TestSuiteDto("S7", "Off");
        suite.setEnabled(false);
        suite.add(testCase("a", true));

        StubExecutor stub = new StubExecutor(TestStatus.PASSED);
        TestSuiteExecutionResultDto result =
                new TestSuiteExecutor(stub, new RunOptions()).execute(suite, new VariableStore());

        assertEquals(0, stub.invocations);
        assertEquals(TestStatus.SKIPPED, result.getStatus());
        assertEquals(1, result.getSkippedTestCases());
    }
}
