package org.ai.testing.report.generator;

import org.ai.testing.report.dto.TestReportDto;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates the standard HTML report and then adds per-test AI insights.
 */
public class AiHtmlReportGenerator implements ReportGenerator {

    private final HtmlReportGenerator delegate;
    private final AiHtmlReportEnhancer enhancer;
    private final Path outputPath;

    public AiHtmlReportGenerator() {
        this(Paths.get("reports", "test-report.html"));
    }

    public AiHtmlReportGenerator(Path outputPath) {
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null");
        }
        this.outputPath = outputPath;
        this.delegate = new HtmlReportGenerator(outputPath);
        this.enhancer = new AiHtmlReportEnhancer();
    }

    @Override
    public void generate(TestReportDto report) {
        delegate.generate(report);
        enhancer.enhance(outputPath, report);
    }
}
