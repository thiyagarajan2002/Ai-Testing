package org.ai.testing.ai;

import org.ai.testing.ai.model.AiGeneratedNegativeTestSuite;
import org.ai.testing.ai.model.AiNegativeTestDataResult;
import org.ai.testing.testcase.dto.TestCaseDto;

/**
 * Builds an isolated AI-generated negative test suite from a source test case.
 */
public class AiNegativeTestSuiteBuilder {

    public AiGeneratedNegativeTestSuite build(
            TestCaseDto sourceTestCase,
            AiNegativeTestDataResult generatedData,
            boolean enabled) {

        if (sourceTestCase == null) {
            throw new IllegalArgumentException("source test case is required");
        }
        if (generatedData == null) {
            throw new IllegalArgumentException("generated negative test data is required");
        }

        AiGeneratedNegativeTestSuite suite = new AiGeneratedNegativeTestSuite();
        suite.setSourceTestCaseId(sourceTestCase.getTestCaseId());
        suite.setStrategy(generatedData.getStrategy());
        suite.setEnabled(enabled);

        suite.setTestCases(new AiNegativeTestCaseBuilder().build(sourceTestCase, generatedData));
        return suite;
    }
}
