package org.ai.testing;

import org.ai.testing.cli.CliRunner;

/**
 * Entry point.
 *
 * <p>Running with no arguments executes the built-in demo plan. The process
 * exit code reflects the outcome — {@code 0} when everything passed, {@code 1}
 * when a test failed, {@code 2} for a bad command line — so the tool drops into
 * a CI pipeline without a wrapper script. The previous version threw an
 * exception on failure, which produced a stack trace and exit code 1 with no
 * way to distinguish a failing test from a broken invocation.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CliRunner(System.out).run(args);
        System.exit(exitCode);
    }
}
