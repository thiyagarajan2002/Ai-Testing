package org.ai.testing.validation.dto;

import java.util.ArrayList;
import java.util.List;

/** All assertion outcomes for one test case, with running counts. */
public class ValidationSummaryDto {

    private boolean passed = true;
    private int total;
    private int passedCount;
    private int failedCount;
    private List<ValidationResultDto> results = new ArrayList<>();

    /** Records one result and keeps the counters and the pass flag consistent. */
    public void add(ValidationResultDto result) {
        if (result == null) {
            return;
        }
        results.add(result);
        total++;
        if (result.isPassed()) {
            passedCount++;
        } else {
            failedCount++;
        }
        passed = failedCount == 0;
    }

    /** Merges another summary into this one. */
    public void addAll(ValidationSummaryDto other) {
        if (other == null) {
            return;
        }
        for (ValidationResultDto result : other.getResults()) {
            add(result);
        }
    }

    /** The first failure, or {@code null} when everything passed. */
    public ValidationResultDto firstFailure() {
        for (ValidationResultDto result : results) {
            if (!result.isPassed()) {
                return result;
            }
        }
        return null;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(int passedCount) {
        this.passedCount = passedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<ValidationResultDto> getResults() {
        return results;
    }

    public void setResults(List<ValidationResultDto> results) {
        this.results = results == null ? new ArrayList<>() : results;
    }

    @Override
    public String toString() {
        return passedCount + "/" + total + " assertions passed";
    }
}
