package org.ai.testing.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ai.testing.dto.common.AssertionDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAssertionSuggestion {

    private AssertionDto assertion;
    private String reason;
    private String confidence;
}
