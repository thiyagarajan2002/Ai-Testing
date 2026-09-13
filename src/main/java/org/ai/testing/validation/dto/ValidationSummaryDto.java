package org.ai.testing.validation.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationSummaryDto {

    private boolean passed;

    private int total;

    private int passedCount;

    private int failedCount;

    private List<ValidationResultDto> results =
            new ArrayList<>();
}