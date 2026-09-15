package org.ai.testing.ai.history;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed persistent store for isolated AI execution history.
 */
public class AiExecutionHistoryStore implements AutoCloseable {
    private final Connection connection;
    private boolean closed;

    public AiExecutionHistoryStore(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("database URL is required");
        }
        try {
            connection = DriverManager.getConnection(databaseUrl);
            closed = false;
            initializeSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize AI execution history store", e);
        }
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ai_execution_history (" +
                    "execution_id TEXT PRIMARY KEY, executed_at TEXT NOT NULL, source_suite_id TEXT, " +
                    "source_test_case_id TEXT, executed INTEGER NOT NULL, passed INTEGER NOT NULL, " +
                    "message TEXT, total_test_cases INTEGER NOT NULL, passed_test_cases INTEGER NOT NULL, " +
                    "failed_test_cases INTEGER NOT NULL, skipped_test_cases INTEGER NOT NULL, " +
                    "pass_rate REAL NOT NULL, failed_test_case_ids TEXT)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ai_history_suite " +
                    "ON ai_execution_history(source_suite_id, executed_at)");
        }
    }

    public synchronized void save(AiExecutionHistoryEntry entry) {
        ensureOpen();
        if (entry == null || entry.getExecutionId() == null || entry.getExecutionId().isBlank()) {
            throw new IllegalArgumentException("valid history entry is required");
        }
        String sql = "INSERT OR REPLACE INTO ai_execution_history " +
                "(execution_id, executed_at, source_suite_id, source_test_case_id, executed, passed, message, " +
                "total_test_cases, passed_test_cases, failed_test_cases, skipped_test_cases, pass_rate, failed_test_case_ids) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entry.getExecutionId());
            ps.setString(2, entry.getExecutedAt().toString());
            ps.setString(3, entry.getSourceSuiteId());
            ps.setString(4, entry.getSourceTestCaseId());
            ps.setInt(5, entry.isExecuted() ? 1 : 0);
            ps.setInt(6, entry.isPassed() ? 1 : 0);
            ps.setString(7, entry.getMessage());
            ps.setInt(8, entry.getTotalTestCases());
            ps.setInt(9, entry.getPassedTestCases());
            ps.setInt(10, entry.getFailedTestCases());
            ps.setInt(11, entry.getSkippedTestCases());
            ps.setDouble(12, entry.getPassRate());
            ps.setString(13, String.join("\n", entry.getFailedTestCaseIds()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save AI execution history", e);
        }
    }

    public synchronized List<AiExecutionHistoryEntry> findBySourceSuite(String sourceSuiteId) {
        ensureOpen();
        String sql = "SELECT * FROM ai_execution_history WHERE source_suite_id = ? ORDER BY executed_at ASC";
        return query(sql, sourceSuiteId);
    }

    public synchronized List<AiExecutionHistoryEntry> findAll() {
        ensureOpen();
        return query("SELECT * FROM ai_execution_history ORDER BY executed_at ASC");
    }

    public synchronized AiExecutionHistoryEntry findLatest(String sourceSuiteId) {
        ensureOpen();
        String sql = "SELECT * FROM ai_execution_history WHERE source_suite_id = ? ORDER BY executed_at DESC LIMIT 1";
        List<AiExecutionHistoryEntry> entries = query(sql, sourceSuiteId);
        return entries.isEmpty() ? null : entries.get(0);
    }

    private List<AiExecutionHistoryEntry> query(String sql, Object... parameters) {
        ensureOpen();
        List<AiExecutionHistoryEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                ps.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AiExecutionHistoryEntry entry = new AiExecutionHistoryEntry();
                    entry.setExecutionId(rs.getString("execution_id"));
                    entry.setExecutedAt(LocalDateTime.parse(rs.getString("executed_at")));
                    entry.setSourceSuiteId(rs.getString("source_suite_id"));
                    entry.setSourceTestCaseId(rs.getString("source_test_case_id"));
                    entry.setExecuted(rs.getInt("executed") == 1);
                    entry.setPassed(rs.getInt("passed") == 1);
                    entry.setMessage(rs.getString("message"));
                    entry.setTotalTestCases(rs.getInt("total_test_cases"));
                    entry.setPassedTestCases(rs.getInt("passed_test_cases"));
                    entry.setFailedTestCases(rs.getInt("failed_test_cases"));
                    entry.setSkippedTestCases(rs.getInt("skipped_test_cases"));
                    entry.setPassRate(rs.getDouble("pass_rate"));
                    String failedIds = rs.getString("failed_test_case_ids");
                    entry.setFailedTestCaseIds(failedIds == null || failedIds.isBlank()
                            ? new ArrayList<>() : new ArrayList<>(List.of(failedIds.split("\\n", -1))));
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read AI execution history", e);
        }
        return entries;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("AI execution history store is already closed");
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            throw new IllegalStateException("AI execution history store is already closed");
        }
        try {
            connection.close();
            closed = true;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to close AI execution history store", e);
        }
    }
}
