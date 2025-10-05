package com.kevindai.git.helper.mr.model;

public enum Severity {
    BLOCKER(0),
    HIGH(1),
    MEDIUM(2),
    LOW(3),
    INFO(4),
    UNKNOWN(98);

    private final int rank;
    Severity(int rank) { this.rank = rank; }
    public int rank() { return rank; }

    public static Severity from(String s) {
        if (s == null) return UNKNOWN;
        String v = s.trim().toLowerCase();
        return switch (v) {
            case "blocker", "critical" -> BLOCKER;
            case "high", "major" -> HIGH;
            case "medium", "med" -> MEDIUM;
            case "low", "minor" -> LOW;
            case "info", "information", "informational" -> INFO;
            default -> UNKNOWN;
        };
    }
}

