package org.ai.testing.testrun.dto;

import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testsuite.dto.TestSuiteExecutionResultDto;
import org.ai.testing.util.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The complete outcome of a run, plus the metrics every report displays. */
public class TestRunResultDto {

    private String runId;
    private String runName;
    private String environment;
    private String executionMode;
    private String description;

    private TestStatus status = TestStatus.SKIPPED;
    private boolean executed;
    private boolean passed;
    private String message;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long executionTimeMs;

    private int totalSuites;
    private int passedSuites;
    private int failedSuites;
    private int erroredSuites;
    private int skippedSuites;

    private int totalTestCases;
    private int passedTestCases;
    private int failedTestCases;
    private int erroredTestCases;
    private int skippedTestCases;

    private List<TestSuiteExecutionResultDto> suiteResults = new ArrayList<>();

    /** Warnings that do not fail the run, such as an unresolved variable. */
    private List<String> warnings = new ArrayList<>();

    /** Variables in effect, already redacted when redaction is enabled. */
    private Map<String, String> variables = new LinkedHashMap<>();

    private Metrics metrics = new Metrics();

    // ------------------------------------------------------------------
    // Derived metrics
    // ------------------------------------------------------------------

    /** Latency and distribution figures computed once, after execution. */
    public static class Metrics {

        private long minResponseTimeMs;
        private long maxResponseTimeMs;
        private long averageResponseTimeMs;
        private long medianResponseTimeMs;
        private long p90ResponseTimeMs;
        private long p95ResponseTimeMs;
        private long totalBytes;
        private double passRate;
        private double requestsPerSecond;

        /** Count of responses per status family, for example {@code 2xx}. */
        private Map<String, Integer> statusFamilies = new TreeMap<>();

        /** Count of results per method, for example {@code GET}. */
        private Map<String, Integer> methodCounts = new TreeMap<>();

        public long getMinResponseTimeMs() {
            return minResponseTimeMs;
        }

        public void setMinResponseTimeMs(long value) {
            this.minResponseTimeMs = value;
        }

        public long getMaxResponseTimeMs() {
            return maxResponseTimeMs;
        }

        public void setMaxResponseTimeMs(long value) {
            this.maxResponseTimeMs = value;
        }

        public long getAverageResponseTimeMs() {
            return averageResponseTimeMs;
        }

        public void setAverageResponseTimeMs(long value) {
            this.averageResponseTimeMs = value;
        }

        public long getMedianResponseTimeMs() {
            return medianResponseTimeMs;
        }

        public void setMedianResponseTimeMs(long value) {
            this.medianResponseTimeMs = value;
        }

        public long getP90ResponseTimeMs() {
            return p90ResponseTimeMs;
        }

        public void setP90ResponseTimeMs(long value) {
            this.p90ResponseTimeMs = value;
        }

        public long getP95ResponseTimeMs() {
            return p95ResponseTimeMs;
        }

        public void setP95ResponseTimeMs(long value) {
            this.p95ResponseTimeMs = value;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public void setTotalBytes(long value) {
            this.totalBytes = value;
        }

        public double getPassRate() {
            return passRate;
        }

        public void setPassRate(double value) {
            this.passRate = value;
        }

        public double getRequestsPerSecond() {
            return requestsPerSecond;
        }

        public void setRequestsPerSecond(double value) {
            this.requestsPerSecond = value;
        }

        public Map<String, Integer> getStatusFamilies() {
            return statusFamilies;
        }

        public void setStatusFamilies(Map<String, Integer> value) {
            this.statusFamilies = value == null ? new TreeMap<>() : value;
        }

        public Map<String, Integer> getMethodCounts() {
            return methodCounts;
        }

        public void setMethodCounts(Map<String, Integer> value) {
            this.methodCounts = value == null ? new TreeMap<>() : value;
        }
    }

    /** Flattens every case result across all suites, in execution order. */
    public List<TestCaseResultDto> allTestResults() {
        List<TestCaseResultDto> all = new ArrayList<>();
        for (TestSuiteExecutionResultDto suite : suiteResults) {
            if (suite != null) {
                all.addAll(suite.getTestResults());
            }
        }
        return all;
    }

    /** The slowest executed cases, longest first. */
    public List<TestCaseResultDto> slowestTests(int limit) {
        return allTestResults().stream()
                .filter(TestCaseResultDto::isExecuted)
                .sorted(Comparator.comparingLong(TestCaseResultDto::responseTimeMs).reversed())
                .limit(Math.max(0, limit))
                .toList();
    }

    /** Every failed or errored case, for the failures-first report view. */
    public List<TestCaseResultDto> failures() {
        return allTestResults().stream()
                .filter(result -> result.getStatus().isFailure())
                .toList();
    }

    /** Recomputes suite counts, case counts and latency metrics. */
    public void tally() {
        totalSuites = suiteResults.size();
        passedSuites = 0;
        failedSuites = 0;
        erroredSuites = 0;
        skippedSuites = 0;

        for (TestSuiteExecutionResultDto suite : suiteResults) {
            switch (suite.getStatus()) {
                case PASSED -> passedSuites++;
                case FAILED -> failedSuites++;
                case ERROR -> erroredSuites++;
                case SKIPPED -> skippedSuites++;
            }
        }

        List<TestCaseResultDto> all = allTestResults();
        totalTestCases = all.size();
        passedTestCases = 0;
        failedTestCases = 0;
        erroredTestCases = 0;
        skippedTestCases = 0;

        List<Long> latencies = new ArrayList<>();
        Map<String, Integer> families = new TreeMap<>();
        Map<String, Integer> methods = new TreeMap<>();
        long bytes = 0;

        for (TestCaseResultDto result : all) {
            switch (result.getStatus()) {
                case PASSED -> passedTestCases++;
                case FAILED -> failedTestCases++;
                case ERROR -> erroredTestCases++;
                case SKIPPED -> skippedTestCases++;
            }
            if (result.getMethod() != null) {
                methods.merge(result.getMethod(), 1, Integer::sum);
            }
            if (result.isExecuted() && result.getResponse() != null) {
                latencies.add(result.getResponse().getResponseTimeMs());
                bytes += result.getResponse().getBodySizeBytes();
                families.merge(HttpStatus.family(result.getResponse().getStatusCode()),
                        1, Integer::sum);
            } else if (result.getStatus() == TestStatus.ERROR) {
                families.merge("error", 1, Integer::sum);
            }
        }

        metrics.setStatusFamilies(families);
        metrics.setMethodCounts(methods);
        metrics.setTotalBytes(bytes);

        int executedCount = passedTestCases + failedTestCases + erroredTestCases;
        metrics.setPassRate(executedCount == 0
                ? 0.0
                : (passedTestCases * 100.0) / executedCount);
        metrics.setRequestsPerSecond(executionTimeMs <= 0
                ? 0.0
                : (executedCount * 1000.0) / executionTimeMs);

        if (!latencies.isEmpty()) {
            latencies.sort(Long::compareTo);
            metrics.setMinResponseTimeMs(latencies.get(0));
            metrics.setMaxResponseTimeMs(latencies.get(latencies.size() - 1));
            metrics.setAverageResponseTimeMs(
                    (long) latencies.stream().mapToLong(Long::longValue).average().orElse(0));
            metrics.setMedianResponseTimeMs(percentile(latencies, 50));
            metrics.setP90ResponseTimeMs(percentile(latencies, 90));
            metrics.setP95ResponseTimeMs(percentile(latencies, 95));
        }

        if (totalSuites == 0) {
            setStatus(TestStatus.SKIPPED);
        } else if (erroredTestCases > 0) {
            setStatus(TestStatus.ERROR);
        } else if (failedTestCases > 0) {
            setStatus(TestStatus.FAILED);
        } else if (passedTestCases == 0) {
            setStatus(TestStatus.SKIPPED);
        } else {
            setStatus(TestStatus.PASSED);
        }
    }

    private long percentile(List<Long> sorted, int percentile) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.min(Math.max(index, 0), sorted.size() - 1));
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status == null ? TestStatus.SKIPPED : status;
        this.passed = this.status == TestStatus.PASSED;
        this.executed = this.status != TestStatus.SKIPPED;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public int getTotalSuites() {
        return totalSuites;
    }

    public void setTotalSuites(int value) {
        this.totalSuites = value;
    }

    public int getPassedSuites() {
        return passedSuites;
    }

    public void setPassedSuites(int value) {
        this.passedSuites = value;
    }

    public int getFailedSuites() {
        return failedSuites;
    }

    public void setFailedSuites(int value) {
        this.failedSuites = value;
    }

    public int getErroredSuites() {
        return erroredSuites;
    }

    public void setErroredSuites(int value) {
        this.erroredSuites = value;
    }

    public int getSkippedSuites() {
        return skippedSuites;
    }

    public void setSkippedSuites(int value) {
        this.skippedSuites = value;
    }

    public int getTotalTestCases() {
        return totalTestCases;
    }

    public void setTotalTestCases(int value) {
        this.totalTestCases = value;
    }

    public int getPassedTestCases() {
        return passedTestCases;
    }

    public void setPassedTestCases(int value) {
        this.passedTestCases = value;
    }

    public int getFailedTestCases() {
        return failedTestCases;
    }

    public void setFailedTestCases(int value) {
        this.failedTestCases = value;
    }

    public int getErroredTestCases() {
        return erroredTestCases;
    }

    public void setErroredTestCases(int value) {
        this.erroredTestCases = value;
    }

    public int getSkippedTestCases() {
        return skippedTestCases;
    }

    public void setSkippedTestCases(int value) {
        this.skippedTestCases = value;
    }

    public List<TestSuiteExecutionResultDto> getSuiteResults() {
        return suiteResults;
    }

    public void setSuiteResults(List<TestSuiteExecutionResultDto> suiteResults) {
        this.suiteResults = suiteResults == null ? new ArrayList<>() : suiteResults;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables == null ? new LinkedHashMap<>() : variables;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics == null ? new Metrics() : metrics;
    }

    @Override
    public String toString() {
        return status + " " + runName + " (" + totalTestCases + " cases)";
    }
}
