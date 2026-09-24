package org.ai.testing.executor.common;

import java.time.Duration;

/**
 * Transport settings for one run.
 *
 * <p>These were previously hard-coded inside the abstract executor, so a slow
 * or flaky endpoint could not be accommodated without editing the source.</p>
 *
 * @param connectTimeout  TCP/TLS connect budget
 * @param requestTimeout  end-to-end budget for a single attempt
 * @param retries         extra attempts after the first one fails
 * @param retryDelay      pause between attempts
 * @param followRedirects whether 3xx responses are followed automatically
 * @param maxBodyChars    response body characters kept in memory and in reports
 */
public record ExecutionOptions(Duration connectTimeout,
                               Duration requestTimeout,
                               int retries,
                               Duration retryDelay,
                               boolean followRedirects,
                               int maxBodyChars) {

    public static final int DEFAULT_MAX_BODY_CHARS = 250_000;

    public ExecutionOptions {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            connectTimeout = Duration.ofSeconds(15);
        }
        if (requestTimeout == null || requestTimeout.isNegative() || requestTimeout.isZero()) {
            requestTimeout = Duration.ofSeconds(60);
        }
        if (retries < 0) {
            retries = 0;
        }
        if (retryDelay == null || retryDelay.isNegative()) {
            retryDelay = Duration.ofMillis(500);
        }
        if (maxBodyChars <= 0) {
            maxBodyChars = DEFAULT_MAX_BODY_CHARS;
        }
    }

    public static ExecutionOptions defaults() {
        return new ExecutionOptions(Duration.ofSeconds(15), Duration.ofSeconds(60),
                0, Duration.ofMillis(500), true, DEFAULT_MAX_BODY_CHARS);
    }

    public ExecutionOptions withRequestTimeout(Duration timeout) {
        return new ExecutionOptions(connectTimeout, timeout, retries, retryDelay,
                followRedirects, maxBodyChars);
    }

    public ExecutionOptions withRetries(int count, Duration delay) {
        return new ExecutionOptions(connectTimeout, requestTimeout, count, delay,
                followRedirects, maxBodyChars);
    }

    public ExecutionOptions withFollowRedirects(boolean follow) {
        return new ExecutionOptions(connectTimeout, requestTimeout, retries, retryDelay,
                follow, maxBodyChars);
    }

    public int totalAttempts() {
        return retries + 1;
    }
}
