package org.ai.testing.ai;

import org.ai.testing.ai.model.AiGeneratedNegativeTestSuite;

/**
 * Controls whether an AI-generated negative suite is allowed to execute.
 */
public final class AiNegativeTestSuiteExecutionPolicy {

    private AiNegativeTestSuiteExecutionPolicy() {
    }

    public static boolean isExecutable(AiGeneratedNegativeTestSuite suite) {
        return suite != null
                && suite.isEnabled()
                && suite.getTestCases() != null
                && !suite.getTestCases().isEmpty();
    }
}
