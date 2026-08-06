package com.github.aeddddd.ae2enhanced.display;

/**
 * 趋势显示幕墙的 X 轴时间范围.
 */
public enum TimeRange {
    M5(300, "5m"),
    M30(1800, "30m"),
    H2(7200, "2h"),
    H24(86400, "24h");

    private final int seconds;
    private final String label;

    TimeRange(int seconds, String label) {
        this.seconds = seconds;
        this.label = label;
    }

    public int getSeconds() {
        return seconds;
    }

    public String getLabel() {
        return label;
    }

    public TimeRange next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    private static final TimeRange[] VALUES = values();

    public static TimeRange byOrdinal(int ord) {
        return VALUES[Math.floorMod(ord, VALUES.length)];
    }
}
