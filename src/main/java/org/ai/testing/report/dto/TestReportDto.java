package org.ai.testing.report.dto;

import lombok.Data;
import org.ai.testing.testrun.dto.TestRunResultDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TestReportDto {

    private String reportId;

    private String reportName;

    private String reportFormat;

    private LocalDateTime generatedAt;

    private TestRunResultDto testRunResult;

    private String aiSummary;

    private String aiSeverity;

    private List<String> aiFindings = new ArrayList<>();

    private List<String> aiRecommendations = new ArrayList<>();
}
