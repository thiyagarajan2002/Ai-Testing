package org.ai.testing.testsuite.executor;

import org.ai.testing.env.VariableStore;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testrun.dto.RunOptions;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.util.Strings;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Executes the cases of one suite in declaration order.
 *
 * <p>Cases always run sequentially inside a suite, because response chaining
 * makes their order meaningful. Parallelism is applied across suites by
 * {@code TestRunExecutor}.</p>
 */
public class TestSuiteExecutor {

    private final TestCaseExecutor testCaseExecutor;
    private final RunOptions options;

    public TestSuiteExecutor() {
        this(new TestCaseExecutor(), new RunOptions());
    }

    public TestSuiteExecutor(TestCaseExecutor testCaseExecutor, RunOptions options) {
        this.testCaseExecutor = testCaseExecutor == null
                ? new TestCaseExecutor() : testCaseExecutor;
        this.options = options == null ? new RunOptions() : options;
    }

    public TestSuiteExecutionResultDto execute(TestSuiteDto suite, VariableStore store) {
        return execute(suite, store, null);
    }

    public TestSuiteExecutionResultDto execute(TestSuiteDto suite,
                                               VariableStore store,
                                               org.ai.testing.dto.common.AuthDto runAuth) {

        if (suite == null) {
            throw new IllegalArgumentException("Test suite cannot be null");
        }

        VariableStore variables = store == null ? new VariableStore() : store;

        TestSuiteExecutionResultDto result = new TestSuiteExecutionResultDto();
        result.setSuiteId(suite.getSuiteId());
        result.setSuiteName(suite.getSuiteName());
        result.setDescription(suite.getDescription());
        result.setStartedAt(LocalDateTime.now());

        if (!suite.isEnabled()) {
            for (TestCaseDto testCase : suite.getTestCases()) {
                result.getTestResults().add(skipped(testCase, "Parent suite is disabled"));
            }
            result.tally();
            result.setStatus(TestStatus.SKIPPED);
            result.setMessage("Test suite is disabled");
            return result;
        }

        long started = System.nanoTime();

        if (suite.getTestCases().isEmpty()) {
            result.setExecutionTimeMs(0);
            result.setStatus(TestStatus.SKIPPED);
            result.setMessage("Test suite contains no test cases");
            return result;
        }

        variables.putAllRuntime(suite.getVariables());

        TestCaseExecutor.AuthScope scope =
                new TestCaseExecutor.AuthScope(suite.getAuth(), runAuth);

        boolean halted = false;

        for (TestCaseDto testCase : suite.getTestCases()) {
            if (testCase == null) {
                continue;
            }

            if (halted) {
                result.getTestResults().add(
                        skipped(testCase, "Skipped after an earlier failure in this suite"));
                continue;
            }

            String exclusion = excludedBecause(testCase);
            if (exclusion != null) {
                result.getTestResults().add(skipped(testCase, exclusion));
                continue;
            }

            TestCaseResultDto caseResult =
                    testCaseExecutor.execute(testCase, variables, scope);
            result.getTestResults().add(caseResult);

            if (caseResult.getStatus().isFailure()
                    && (suite.isStopOnFailure() || options.isFailFast())) {
                halted = true;
            }
        }

        result.setExecutionTimeMs((System.nanoTime() - started) / 1_000_000L);
        result.tally();
        result.setMessage(summaryMessage(result));
        return result;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Why a case will not run, or {@code null} when it should. */
    private String excludedBecause(TestCaseDto testCase) {
        if (!testCase.isEnabled()) {
            return "Test case is disabled";
        }

        Set<String> include = options.getIncludeTags();
        Set<String> exclude = options.getExcludeTags();

        if (!exclude.isEmpty()) {
            for (String tag : testCase.getTags()) {
                if (exclude.contains(tag)) {
                    return "Excluded by tag '" + tag + "'";
                }
            }
        }

        if (!include.isEmpty()) {
            boolean matched = testCase.getTags().stream().anyMatch(include::contains);
            if (!matched) {
                return "Does not carry any of the requested tags: "
                        + String.join(", ", include);
            }
        }

        return null;
    }

    private TestCaseResultDto skipped(TestCaseDto testCase, String reason) {
        TestCaseResultDto result = new TestCaseResultDto();
        if (testCase != null) {
            result.setTestCaseId(testCase.getTestCaseId());
            result.setTestCaseName(testCase.getTestCaseName());
            result.setDescription(testCase.getDescription());
            result.setMethod(Strings.upper(testCase.getMethod()));
            result.setTags(testCase.getTags());
        }
        result.setStatus(TestStatus.SKIPPED);
        result.setMessage(reason);
        return result;
    }

    private String summaryMessage(TestSuiteExecutionResultDto result) {
        StringBuilder message = new StringBuilder();
        message.append(result.getPassedTestCases()).append(" passed");
        if (result.getFailedTestCases() > 0) {
            message.append(", ").append(result.getFailedTestCases()).append(" failed");
        }
        if (result.getErroredTestCases() > 0) {
            message.append(", ").append(result.getErroredTestCases()).append(" errored");
        }
        if (result.getSkippedTestCases() > 0) {
            message.append(", ").append(result.getSkippedTestCases()).append(" skipped");
        }
        message.append(" of ").append(result.getTotalTestCases()).append(" test case(s)");
        return message.toString();
    }
}
