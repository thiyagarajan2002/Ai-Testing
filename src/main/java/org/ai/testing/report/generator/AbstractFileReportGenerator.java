package org.ai.testing.report.generator;

import org.ai.testing.report.dto.TestReportDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shared validation and file writing for the text-based generators. */
public abstract class AbstractFileReportGenerator implements ReportGenerator {

    private final Path outputPath;
    private final String format;

    protected AbstractFileReportGenerator(Path outputPath, String format) {
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null");
        }
        this.outputPath = outputPath;
        this.format = format;
    }

    @Override
    public Path outputPath() {
        return outputPath;
    }

    @Override
    public String format() {
        return format;
    }

    @Override
    public void generate(TestReportDto report) {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }
        if (report.getTestRunResult() == null) {
            throw new IllegalArgumentException("Test run result cannot be null");
        }
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, render(report), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReportGenerationException(
                    "Failed to generate " + format + " report: " + outputPath, e);
        }
    }

    /** Produces the complete file contents. */
    protected abstract String render(TestReportDto report);
}
