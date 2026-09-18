package org.ai.testing.dto.common;

import lombok.Data;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

@Data
public class AssertionDto {

    private AssertionType type;

    private String field;

    private AssertionOperator operator;

    private String expectedValue;

}