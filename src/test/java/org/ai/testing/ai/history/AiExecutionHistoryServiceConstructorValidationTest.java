package org.ai.testing.ai.history;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiExecutionHistoryServiceConstructorValidationTest {

    @Test
    void shouldRejectNullHistoryStore() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AiExecutionHistoryService(null));

        assertEquals("history store is required", exception.getMessage());
    }
}
