package org.ai.testing.ai.history;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides deterministic filtering, suite comparison, and CSV export for AI execution history.
 */
public class AiHistoryAnalyticsService {

    public List<AiExecutionHistoryEntry> filter(
            List<AiExecutionHistoryEntry> history,
            String suiteId,
            String status,
            LocalDate from,
            LocalDate to) {

        if (history == null) {
            return List.of();
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from date cannot be after to date");
        }

        String normalizedSuite = normalize(suiteId);
        String normalizedStatus = normalize(status);

        return history.stream()
                .filter(Objects::nonNull)
                .filter(entry -> normalizedSuite.isEmpty()
                        || normalizedSuite.equals(normalize(entry.getSourceSuiteId())))
                .filter(entry -> matchesStatus(entry, normalizedStatus))
                .filter(entry -> matchesDate(entry, from, to))
                .sorted(Comparator.comparing(
                        AiExecutionHistoryEntry::getExecutedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public Map<String, SuiteHistorySummary> compareSuites(List<AiExecutionHistoryEntry> history) {
        Map<String, List<AiExecutionHistoryEntry>> grouped = new LinkedHashMap<>();
        if (history != null) {
            for (AiExecutionHistoryEntry entry : history) {
                if (entry == null) continue;
                String suite = entry.getSourceSuiteId() == null ? "" : entry.getSourceSuiteId();
                grouped.computeIfAbsent(suite, ignored -> new ArrayList<>()).add(entry);
            }
        }

        Map<String, SuiteHistorySummary> result = new LinkedHashMap<>();
        grouped.forEach((suite, entries) -> {
            double average = entries.stream()
                    .mapToDouble(AiExecutionHistoryEntry::getPassRate)
                    .average()
                    .orElse(0.0);
            int failed = entries.stream()
                    .mapToInt(AiExecutionHistoryEntry::getFailedTestCases)
                    .sum();
            double latest = entries.stream()
                    .filter(e -> e.getExecutedAt() != null)
                    .max(Comparator.comparing(AiExecutionHistoryEntry::getExecutedAt))
                    .map(AiExecutionHistoryEntry::getPassRate)
                    .orElse(0.0);
            result.put(suite, new SuiteHistorySummary(entries.size(), average, latest, failed));
        });
        return result;
    }

    public String exportCsv(List<AiExecutionHistoryEntry> history) {
        StringBuilder csv = new StringBuilder();
        csv.append("Execution ID,Suite,Source Test Case,Executed At,Pass Rate,Failed,Skipped,Status\n");
        if (history == null) return csv.toString();

        for (AiExecutionHistoryEntry entry : history) {
            if (entry == null) continue;
            csv.append(csv(entry.getExecutionId())).append(',')
                    .append(csv(entry.getSourceSuiteId())).append(',')
                    .append(csv(entry.getSourceTestCaseId())).append(',')
                    .append(csv(String.valueOf(entry.getExecutedAt()))).append(',')
                    .append(String.format(Locale.ROOT, "%.2f", entry.getPassRate())).append(',')
                    .append(entry.getFailedTestCases()).append(',')
                    .append(entry.getSkippedTestCases()).append(',')
                    .append(csv(entry.isPassed() ? "PASSED" : "FAILED"))
                    .append('\n');
        }
        return csv.toString();
    }

    private boolean matchesStatus(AiExecutionHistoryEntry entry, String status) {
        return status.isEmpty()
                || "ALL".equals(status)
                || ("PASSED".equals(status) && entry.isPassed())
                || ("FAILED".equals(status) && !entry.isPassed());
    }

    private boolean matchesDate(AiExecutionHistoryEntry entry, LocalDate from, LocalDate to) {
        if (entry.getExecutedAt() == null) return from == null && to == null;
        LocalDate date = entry.getExecutedAt().toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\\", "\\\\").replace(""", """");
        return """ + escaped + """;
    }

    public record SuiteHistorySummary(
            int executionCount,
            double averagePassRate,
            double latestPassRate,
            int totalFailedTests) {
    }
}
