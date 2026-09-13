package org.ai.testing.report.generator;


import org.ai.testing.report.dto.TestReportDto;

public interface ReportGenerator {

    void generate(TestReportDto report);

}