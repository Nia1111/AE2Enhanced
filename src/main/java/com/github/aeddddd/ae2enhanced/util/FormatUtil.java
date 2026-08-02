package com.github.aeddddd.ae2enhanced.util;

import java.util.Locale;

/**
 * 通用格式化工具类.
 */
public final class FormatUtil {

    private static final long[] THRESHOLDS = {
            1_000L,
            1_000_000L,
            1_000_000_000L,
            1_000_000_000_000L,
            1_000_000_000_000_000L,
            1_000_000_000_000_000_000L
    };

    private static final char[] SUFFIXES = { 'K', 'M', 'G', 'T', 'P', 'E' };

    private FormatUtil() {}

    /**
     * 将大数值缩写为 K / M / G / T / P / E 格式.
     *
     * @param count 原始数值
     * @return 缩写字符串,如 "1.2M"、"3.5K",小于 1000 返回原值
     */
    public static String formatCount(long count) {
        if (count < 1000L) {
            return String.valueOf(count);
        }
        int tier = 0;
        while (tier < THRESHOLDS.length - 1 && count >= THRESHOLDS[tier + 1]) {
            tier++;
        }
        double value = count / (double) THRESHOLDS[tier];
        // %.1f 舍入会进位到 1000.0 时升入下一档（如 999999 → "1.0M" 而非 "1000.0K"）
        if (value >= 999.95 && tier < SUFFIXES.length - 1) {
            tier++;
            value = count / (double) THRESHOLDS[tier];
        }
        // 锁定 Locale.ROOT，避免小数点为逗号的 Locale 下输出 "1,5K"
        return String.format(Locale.ROOT, "%.1f%c", value, SUFFIXES[tier]);
    }
}
