package org.ai.testing.report.generator;

import org.ai.testing.report.dto.TestReportDto;

import java.nio.file.Path;

/** Writes one report file. */
public interface ReportGenerator {

    void generate(TestReportDto report);

    /** Where the generated file will be written. */
    Path outputPath();

    /** A short label used in console output, for example {@code HTML}. */
    String format();
}
