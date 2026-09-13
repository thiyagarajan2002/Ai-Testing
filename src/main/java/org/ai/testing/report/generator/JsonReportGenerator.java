package org.ai.testing.report.generator;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ai.testing.report.dto.TestReportDto;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonReportGenerator implements ReportGenerator {

    private final ObjectMapper objectMapper;
    private final Path outputPath;

    public JsonReportGenerator() {
        this(Paths.get("reports", "test-report.json"));
    }

    public JsonReportGenerator(Path outputPath) {

        if (outputPath == null) {
            throw new IllegalArgumentException(
                    "Output path cannot be null"
            );
        }

        this.outputPath = outputPath;

        this.objectMapper = new ObjectMapper();

        this.objectMapper
                .findAndRegisterModules();

        this.objectMapper.enable(
                SerializationFeature.INDENT_OUTPUT
        );
    }

    @Override
    public void generate(TestReportDto report) {

        if (report == null) {
            throw new IllegalArgumentException(
                    "Report cannot be null"
            );
        }

        if (report.getTestRunResult() == null) {
            throw new IllegalArgumentException(
                    "Test run result cannot be null"
            );
        }

        try {

            Path parent = outputPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            String json =
                    objectMapper.writeValueAsString(report);

            Files.writeString(
                    outputPath,
                    json,
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to generate JSON report: "
                            + outputPath,
                    e
            );
        }
    }
}