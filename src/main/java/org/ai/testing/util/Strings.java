package org.ai.testing.util;

import java.util.Locale;

/** Small text helpers used across the framework. */
public final class Strings {

    private Strings() {
    }

    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean hasText(String value) {
        return !isBlank(value);
    }

    public static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    /** Collapses a value to a single line and caps its length for display. */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength)) + "… (" + value.length() + " chars)";
    }

    /** Turns any label into a filesystem and identifier safe token. */
    public static String slug(String value) {
        if (isBlank(value)) {
            return "item";
        }
        String slug = value.trim()
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isEmpty() ? "item" : slug;
    }

    public static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    public static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /** Formats a byte count for reports. */
    public static String humanBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /** Formats a millisecond duration for reports. */
    public static String humanDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + " ms";
        }
        if (milliseconds < 60_000) {
            return String.format(Locale.ROOT, "%.2f s", milliseconds / 1000.0);
        }
        long minutes = milliseconds / 60_000;
        double seconds = (milliseconds % 60_000) / 1000.0;
        return String.format(Locale.ROOT, "%dm %.1fs", minutes, seconds);
    }
}
