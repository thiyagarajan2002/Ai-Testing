package org.ai.testing.report.dto;

import lombok.Data;
import org.ai.testing.testrun.dto.TestRunResultDto;

import java.time.LocalDateTime;

@Data
public class TestReportDto {

    private String reportId;

    private String reportName;

    private String reportFormat;

    private LocalDateTime generatedAt;

    private TestRunResultDto testRunResult;
}