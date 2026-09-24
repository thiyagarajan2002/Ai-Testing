package org.ai.testing.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Command line")
class CliOptionsTest {

    @Test
    @DisplayName("no arguments runs the built-in demo")
    void noArgumentsMeansDemo() {
        assertTrue(CliOptions.parse(new String[0]).isDemo());
    }

    @Test
    @DisplayName("a bare path is taken as the collection")
    void barePathIsCollection() {
        CliOptions options = CliOptions.parse(new String[]{"api.json"});
        assertEquals(Paths.get("api.json"), options.getCollection());
        assertFalse(options.isDemo());
    }

    @Test
    @DisplayName("reads long and short forms of every flag")
    void readsFlags() {
        CliOptions options = CliOptions.parse(new String[]{
                "-c", "api.json", "-e", "env.json", "-o", "build/reports",
                "--parallel", "--threads", "8", "--retries", "2", "--retry-delay", "250",
                "--timeout", "5000", "--connect-timeout", "1000", "--max-body", "4096",
                "--fail-fast", "--no-redact", "--no-follow-redirects", "--quiet",
                "--tag", "smoke,contract", "--exclude-tag", "slow",
                "--var", "token=abc=def"});

        assertEquals(Paths.get("api.json"), options.getCollection());
        assertEquals(Paths.get("env.json"), options.getEnvironment());
        assertEquals(Paths.get("build/reports"), options.getReportDirectory());
        assertTrue(options.getRunOptions().isParallel());
        assertEquals(8, options.getRunOptions().getThreads());
        assertEquals(2, options.getRunOptions().getRetries());
        assertEquals(250, options.getRunOptions().getRetryDelayMs());
        assertEquals(5000, options.getRunOptions().getRequestTimeoutMs());
        assertEquals(1000, options.getRunOptions().getConnectTimeoutMs());
        assertEquals(4096, options.getRunOptions().getMaxBodyChars());
        assertTrue(options.getRunOptions().isFailFast());
        assertFalse(options.getRunOptions().isRedactSecrets());
        assertFalse(options.getRunOptions().isFollowRedirects());
        assertTrue(options.isQuiet());
        assertEquals(2, options.getRunOptions().getIncludeTags().size());
        assertTrue(options.getRunOptions().getExcludeTags().contains("slow"));
        assertEquals("abc=def", options.getVariables().get("token"),
                "only the first equals sign separates name from value");
    }

    @Test
    @DisplayName("rejects an unknown flag, a missing value and a malformed variable")
    void rejectsBadInput() {
        assertThrows(CliOptions.UsageException.class,
                () -> CliOptions.parse(new String[]{"--nope"}));
        assertThrows(CliOptions.UsageException.class,
                () -> CliOptions.parse(new String[]{"-c"}));
        assertThrows(CliOptions.UsageException.class,
                () -> CliOptions.parse(new String[]{"--threads", "many"}));
        assertThrows(CliOptions.UsageException.class,
                () -> CliOptions.parse(new String[]{"--var", "novalue"}));
        assertThrows(CliOptions.UsageException.class,
                () -> CliOptions.parse(new String[]{"a.json", "b.json"}));
    }

    @Test
    @DisplayName("help text names every documented option")
    void helpTextIsComplete() {
        String usage = CliOptions.usage();
        for (String flag : new String[]{"--collection", "--env", "--out", "--demo",
                "--parallel", "--threads", "--timeout", "--retries", "--fail-fast",
                "--tag", "--exclude-tag", "--var", "--no-redact"}) {
            assertTrue(usage.contains(flag), "usage should document " + flag);
        }
    }
}
