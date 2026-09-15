package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryComparisonValidationTest {

    @Test
    void shouldRejectNullPreviousExecution() {
        AiExecutionHistoryEntry current = new AiExecutionHistoryEntry();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiExecutionHistoryComparison.compare(null, current));

        assertEquals("both executions are required", exception.getMessage());
    }

    @Test
    void shouldRejectNullCurrentExecution() {
        AiExecutionHistoryEntry previous = new AiExecutionHistoryEntry();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiExecutionHistoryComparison.compare(previous, null));

        assertEquals("both executions are required", exception.getMessage());
    }

    @Test
    void shouldRejectBothExecutionsWhenNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiExecutionHistoryComparison.compare(null, null));

        assertEquals("both executions are required", exception.getMessage());
    }
}
