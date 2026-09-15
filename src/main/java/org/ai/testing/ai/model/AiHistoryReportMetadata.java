package org.ai.testing.ai.model;

import lombok.Data;
import org.ai.testing.ai.history.AiExecutionHistoryEntry;
import org.ai.testing.ai.history.AiExecutionHistoryTrend;

import java.util.ArrayList;
import java.util.List;

/**
 * Report-safe snapshot of AI execution history and its aggregated trend.
 */
@Data
public class AiHistoryReportMetadata {
    private AiExecutionHistoryTrend trend;
    private List<AiExecutionHistoryEntry> history = new ArrayList<>();

    public static AiHistoryReportMetadata from(
            AiExecutionHistoryTrend trend,
            List<AiExecutionHistoryEntry> history) {
        if (trend == null && (history == null || history.isEmpty())) {
            return null;
        }
        AiHistoryReportMetadata metadata = new AiHistoryReportMetadata();
        metadata.setTrend(trend);
        if (history != null) {
            metadata.setHistory(new ArrayList<>(history));
        }
        return metadata;
    }
}
