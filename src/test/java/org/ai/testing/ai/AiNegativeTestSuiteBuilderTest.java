package org.ai.testing.ai;

import org.ai.testing.ai.model.AiGeneratedNegativeTestSuite;
import org.ai.testing.ai.model.AiNegativeTestData;
import org.ai.testing.ai.model.AiNegativeTestDataResult;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiNegativeTestSuiteBuilderTest {

    @Test
    void shouldBuildDisabledSuiteByDefault() {
        TestCaseDto source = sourceTestCase();
        AiNegativeTestDataResult data = data();

        AiGeneratedNegativeTestSuite suite =
                new AiNegativeTestSuiteBuilder().build(source, data, false);

        assertEquals("TC-010", suite.getSourceTestCaseId());
        assertEquals("heuristic-ai-v1", suite.getStrategy());
        assertFalse(suite.isEnabled());
        assertEquals(1, suite.getTestCaseCount());
        assertFalse(AiNegativeTestSuiteExecutionPolicy.isExecutable(suite));
    }

    @Test
    void shouldAllowExplicitlyEnabledSuite() {
        AiGeneratedNegativeTestSuite suite =
                new AiNegativeTestSuiteBuilder().build(sourceTestCase(), data(), true);

        assertTrue(suite.isEnabled());
        assertTrue(AiNegativeTestSuiteExecutionPolicy.isExecutable(suite));
        assertEquals("TC-010-AI-NEG-1", suite.getTestCases().get(0).getTestCaseId());
    }

    @Test
    void shouldRejectMissingInput() {
        AiNegativeTestSuiteBuilder builder = new AiNegativeTestSuiteBuilder();
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(null, data(), false));
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(sourceTestCase(), null, false));
    }

    private TestCaseDto sourceTestCase() {
        TestCaseDto testCase = new TestCaseDto();
        testCase.setTestCaseId("TC-010");
        testCase.setTestCaseName("Create Pet");
        testCase.setMethod("POST");
        testCase.setEnabled(true);
        return testCase;
    }

    private AiNegativeTestDataResult data() {
        AiNegativeTestData item = new AiNegativeTestData();
        item.setScenario("INVALID_FIELD_TYPE");
        item.setReason("Invalid field type should be rejected");
        item.setExpectedStatusCode(400);

        AiNegativeTestDataResult result = new AiNegativeTestDataResult();
        result.setStrategy("heuristic-ai-v1");
        result.setTestData(List.of(item));
        return result;
    }
}
