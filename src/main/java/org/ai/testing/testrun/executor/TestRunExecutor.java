package org.ai.testing.testrun.executor;

import org.ai.testing.env.VariableStore;
import org.ai.testing.executor.ExecutorDispatcher;
import org.ai.testing.report.service.ReportService;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.ai.testing.testrun.dto.RunOptions;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testrun.dto.TestRunResultDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.testsuite.executor.TestSuiteExecutor;
import org.ai.testing.util.Redaction;
import org.ai.testing.util.Strings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Drives a whole run: variables, suites, metrics and reports.
 *
 * <p>{@code executionMode} is now honoured. In {@code PARALLEL} mode suites run
 * on a bounded pool while cases inside each suite stay ordered, which keeps
 * response chaining intact. Parallel mode is skipped automatically when the run
 * has only one suite, so a small plan does not pay for a thread pool.</p>
 */
public class TestRunExecutor {

    private final ReportService reportService;
    private final TestSuiteExecutor injectedSuiteExecutor;

    public TestRunExecutor() {
        this(null, new ReportService());
    }

    public TestRunExecutor(ReportService reportService) {
        this(null, reportService);
    }

    public TestRunExecutor(TestSuiteExecutor suiteExecutor, ReportService reportService) {
        if (reportService == null) {
            throw new IllegalArgumentException("Report service cannot be null");
        }
        this.injectedSuiteExecutor = suiteExecutor;
        this.reportService = reportService;
    }

    public TestRunResultDto execute(TestRunDto testRun) {

        if (testRun == null) {
            throw new IllegalArgumentException("Test run cannot be null");
        }

        RunOptions options = testRun.getOptions();

        TestRunResultDto result = new TestRunResultDto();
        result.setRunId(Strings.defaultIfBlank(testRun.getRunId(), "RUN"));
        result.setRunName(Strings.defaultIfBlank(testRun.getRunName(), "API Test Run"));
        result.setEnvironment(Strings.defaultIfBlank(testRun.getEnvironment(), "default"));
        result.setExecutionMode(options.getExecutionMode());
        result.setDescription(testRun.getDescription());
        result.setStartTime(LocalDateTime.now());

        long startNanos = System.nanoTime();

        VariableStore variables = new VariableStore();
        variables.putAllCollection(testRun.getCollectionVariables());
        variables.putAllEnvironment(testRun.getEnvironmentVariables());

        List<TestSuiteDto> suites = testRun.getTestSuites().stream()
                .filter(suite -> suite != null)
                .toList();

        if (suites.isEmpty()) {
            finish(result, startNanos, variables, options);
            result.setMessage("Test run contains no test suites");
            generateReports(result);
            return result;
        }

        TestSuiteExecutor suiteExecutor = injectedSuiteExecutor != null
                ? injectedSuiteExecutor
                : new TestSuiteExecutor(
                        new TestCaseExecutor(
                                new ExecutorDispatcher(options.toExecutionOptions()),
                                options.isRedactSecrets()),
                        options);

        boolean parallel = options.isParallel() && suites.size() > 1;

        if (parallel) {
            runParallel(suites, suiteExecutor, variables, testRun, result, options);
        } else {
            runSequential(suites, suiteExecutor, variables, testRun, result, options);
        }

        finish(result, startNanos, variables, options);
        result.setMessage(summaryMessage(result));
        generateReports(result);
        return result;
    }

    // ------------------------------------------------------------------
    // Execution strategies
    // ------------------------------------------------------------------

    private void runSequential(List<TestSuiteDto> suites,
                               TestSuiteExecutor suiteExecutor,
                               VariableStore variables,
                               TestRunDto testRun,
                               TestRunResultDto result,
                               RunOptions options) {

        boolean halted = false;

        for (TestSuiteDto suite : suites) {
            if (halted) {
                result.getSuiteResults().add(
                        skippedSuite(suite, "Skipped after an earlier failure"));
                continue;
            }
            TestSuiteExecutionResultDto suiteResult =
                    suiteExecutor.execute(suite, variables, testRun.getAuth());
            result.getSuiteResults().add(suiteResult);

            if (options.isFailFast() && suiteResult.getStatus().isFailure()) {
                halted = true;
            }
        }
    }

    private void runParallel(List<TestSuiteDto> suites,
                             TestSuiteExecutor suiteExecutor,
                             VariableStore variables,
                             TestRunDto testRun,
                             TestRunResultDto result,
                             RunOptions options) {

        int threads = Math.min(options.getThreads(), suites.size());
        AtomicInteger threadNumber = new AtomicInteger(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("api-suite-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });

        try {
            List<Future<TestSuiteExecutionResultDto>> futures = new ArrayList<>();
            for (TestSuiteDto suite : suites) {
                futures.add(pool.submit(
                        () -> suiteExecutor.execute(suite, variables, testRun.getAuth())));
            }

            // Results are collected in submission order so the report layout is
            // deterministic regardless of which suite finished first.
            for (int i = 0; i < futures.size(); i++) {
                try {
                    result.getSuiteResults().add(futures.get(i).get());
                } catch (ExecutionException e) {
                    result.getSuiteResults().add(erroredSuite(suites.get(i), e.getCause()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result.getSuiteResults().add(
                            erroredSuite(suites.get(i), e));
                    break;
                }
            }
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------
    // Finishing
    // ------------------------------------------------------------------

    private void finish(TestRunResultDto result, long startNanos,
                        VariableStore variables, RunOptions options) {

        result.setEndTime(LocalDateTime.now());
        result.setExecutionTimeMs((System.nanoTime() - startNanos) / 1_000_000L);
        result.setVariables(Redaction.maskHeaders(variables.snapshot(),
                options.isRedactSecrets()));
        result.tally();
        collectWarnings(result);
    }

    private void collectWarnings(TestRunResultDto result) {
        result.allTestResults().forEach(testResult -> {
            if (Strings.hasText(testResult.getErrorDetail())
                    && testResult.getStatus() != TestStatus.ERROR) {
                result.getWarnings().add(
                        testResult.getTestCaseId() + ": " + testResult.getErrorDetail());
            }
            testResult.getFailedExtracts().forEach(extract ->
                    result.getWarnings().add(
                            testResult.getTestCaseId() + ": extract matched nothing (" + extract + ")"));
        });
    }

    private void generateReports(TestRunResultDto result) {
        try {
            reportService.generateAllReports(result);
        } catch (RuntimeException e) {
            result.getWarnings().add("Report generation failed: " + e.getMessage());
            result.setMessage(Strings.nullToEmpty(result.getMessage())
                    + " | Report generation failed: " + e.getMessage());
        }
    }

    private TestSuiteExecutionResultDto skippedSuite(TestSuiteDto suite, String reason) {
        TestSuiteExecutionResultDto suiteResult = new TestSuiteExecutionResultDto();
        suiteResult.setSuiteId(suite.getSuiteId());
        suiteResult.setSuiteName(suite.getSuiteName());
        suiteResult.setStatus(TestStatus.SKIPPED);
        suiteResult.setMessage(reason);
        return suiteResult;
    }

    private TestSuiteExecutionResultDto erroredSuite(TestSuiteDto suite, Throwable error) {
        TestSuiteExecutionResultDto suiteResult = new TestSuiteExecutionResultDto();
        suiteResult.setSuiteId(suite.getSuiteId());
        suiteResult.setSuiteName(suite.getSuiteName());
        suiteResult.setStatus(TestStatus.ERROR);
        suiteResult.setMessage("Suite execution failed: "
                + (error == null ? "unknown error" : error.getMessage()));
        return suiteResult;
    }

    private String summaryMessage(TestRunResultDto result) {
        StringBuilder message = new StringBuilder();
        message.append(result.getStatus() == TestStatus.PASSED
                ? "Test run passed. " : "Test run finished with failures. ");
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
        message.append(" across ").append(result.getTotalSuites()).append(" suite(s) in ")
                .append(Strings.humanDuration(result.getExecutionTimeMs()));
        return message.toString();
    }
}
