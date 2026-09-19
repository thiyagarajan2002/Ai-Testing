package org.ai.testing.testrun.dto;

import org.ai.testing.executor.common.ExecutionOptions;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/** Everything that changes how a run behaves, in one place. */
public class RunOptions {

    /** SEQUENTIAL or PARALLEL. */
    private String executionMode = "SEQUENTIAL";

    /** Worker threads used when the mode is PARALLEL. */
    private int threads = 4;

    private long requestTimeoutMs = 60_000;
    private long connectTimeoutMs = 15_000;
    private int retries;
    private long retryDelayMs = 500;
    private boolean followRedirects = true;
    private int maxBodyChars = ExecutionOptions.DEFAULT_MAX_BODY_CHARS;

    /** Stop the whole run at the first failing case. */
    private boolean failFast;

    /** Only run cases carrying at least one of these tags; empty means all. */
    private Set<String> includeTags = new LinkedHashSet<>();

    /** Never run cases carrying any of these tags. */
    private Set<String> excludeTags = new LinkedHashSet<>();

    /** Mask credentials in generated reports. */
    private boolean redactSecrets = true;

    public boolean isParallel() {
        return "PARALLEL".equalsIgnoreCase(executionMode);
    }

    public ExecutionOptions toExecutionOptions() {
        return new ExecutionOptions(
                Duration.ofMillis(connectTimeoutMs),
                Duration.ofMillis(requestTimeoutMs),
                retries,
                Duration.ofMillis(retryDelayMs),
                followRedirects,
                maxBodyChars);
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode == null ? "SEQUENTIAL" : executionMode;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = Math.max(1, threads);
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = Math.max(0, retries);
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = Math.max(0, retryDelayMs);
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public int getMaxBodyChars() {
        return maxBodyChars;
    }

    public void setMaxBodyChars(int maxBodyChars) {
        this.maxBodyChars = maxBodyChars;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public Set<String> getIncludeTags() {
        return includeTags;
    }

    public void setIncludeTags(Set<String> includeTags) {
        this.includeTags = includeTags == null ? new LinkedHashSet<>() : includeTags;
    }

    public Set<String> getExcludeTags() {
        return excludeTags;
    }

    public void setExcludeTags(Set<String> excludeTags) {
        this.excludeTags = excludeTags == null ? new LinkedHashSet<>() : excludeTags;
    }

    public boolean isRedactSecrets() {
        return redactSecrets;
    }

    public void setRedactSecrets(boolean redactSecrets) {
        this.redactSecrets = redactSecrets;
    }
}
