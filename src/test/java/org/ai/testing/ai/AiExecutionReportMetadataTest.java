package org.ai.testing.ai;

import org.ai.testing.ai.model.AiExecutionReportMetadata;
import org.ai.testing.ai.model.AiGeneratedSuiteExecutionResult;
import org.ai.testing.testcase.executor.TestCaseExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiExecutionReportMetadataTest {
    @Test
    void shouldCreateReportSafeExecutionSnapshot() {
        AiGeneratedSuiteExecutionResult result = new AiGeneratedSuiteExecutionResult();
        result.setSourceSuiteId("SUITE-01");
        result.setSourceTestCaseId("TC-01");
        result.setExecuted(true);
        result.setMessage("completed");

        TestCaseExecutor.TestCaseExecutionResult passed = new TestCaseExecutor.TestCaseExecutionResult();
        passed.setTestCaseId("TC-01-AI-1");
        passed.setExecuted(true);
        passed.setPassed(true);

        TestCaseExecutor.TestCaseExecutionResult failed = new TestCaseExecutor.TestCaseExecutionResult();
        failed.setTestCaseId("TC-01-AI-2");
        failed.setExecuted(true);
        failed.setPassed(false);

        result.addResult(passed);
        result.addResult(failed);

        AiExecutionReportMetadata metadata = AiExecutionReportMetadata.from(result);

        assertNotNull(metadata);
        assertEquals("SUITE-01", metadata.getSourceSuiteId());
        assertEquals("TC-01", metadata.getSourceTestCaseId());
        assertTrue(metadata.isExecuted());
        assertFalse(metadata.isPassed());
        assertEquals(2, metadata.getTotalTestCases());
        assertEquals(1, metadata.getPassedTestCases());
        assertEquals(1, metadata.getFailedTestCases());
        assertEquals(0, metadata.getSkippedTestCases());
        assertEquals(List.of("TC-01-AI-2"), metadata.getFailedTestCaseIds());
    }

    @Test
    void shouldReturnNullForMissingExecutionResult() {
        assertNull(AiExecutionReportMetadata.from(null));
    }
}
