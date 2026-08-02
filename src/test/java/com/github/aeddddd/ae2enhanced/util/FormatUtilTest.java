package com.github.aeddddd.ae2enhanced.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link FormatUtil#formatCount(long)} 数量级缩写格式化测试。
 *
 * <p>纯字符串格式化逻辑，零 MC 依赖。覆盖 K/M/G/T/P/E 各阈值边界、
 * {@code %.1f} 的 HALF_UP 舍入行为，以及负数/零的固化行为。</p>
 */
public class FormatUtilTest {

    // ------------------------------------------------------------------
    // 小于 1000：原样输出
    // ------------------------------------------------------------------

    /** 小于 1000 的值原样输出，含 0 与阈值上界 999。 */
    @Test
    public void testBelowThousandRawOutput() {
        assertThat(FormatUtil.formatCount(0)).isEqualTo("0");
        assertThat(FormatUtil.formatCount(1)).isEqualTo("1");
        assertThat(FormatUtil.formatCount(42)).isEqualTo("42");
        assertThat(FormatUtil.formatCount(999)).isEqualTo("999");
    }

    // ------------------------------------------------------------------
    // K 级（1000 ~ 999999）
    // ------------------------------------------------------------------

    /** K 级阈值边界：1000 起缩写，普通值按 %.1f 输出。 */
    @Test
    public void testKiloRange() {
        assertThat(FormatUtil.formatCount(1000)).isEqualTo("1.0K");
        assertThat(FormatUtil.formatCount(1500)).isEqualTo("1.5K");
        assertThat(FormatUtil.formatCount(12345)).isEqualTo("12.3K");
    }

    /** %.1f 采用 HALF_UP 舍入：1.25 进位为 1.3。 */
    @Test
    public void testHalfUpRounding() {
        assertThat(FormatUtil.formatCount(1250)).isEqualTo("1.3K");
    }

    /**
     * 进位升级：%.1f 舍入后达到 1000.0 时升入下一档，
     * 999999 输出 "1.0M" 而非 "1000.0K"。
     */
    @Test
    public void testKiloUpperBoundaryCarry() {
        assertThat(FormatUtil.formatCount(999999)).isEqualTo("1.0M");
    }

    /** 进位升级边界：999950 起升入 M 档，999949 及以下保持 K 档。 */
    @Test
    public void testCarryUpgradeThreshold() {
        assertThat(FormatUtil.formatCount(999949)).isEqualTo("999.9K");
        assertThat(FormatUtil.formatCount(999499)).isEqualTo("999.5K");
        assertThat(FormatUtil.formatCount(999950)).isEqualTo("1.0M");
    }

    // ------------------------------------------------------------------
    // M 级（1e6 ~ 1e9-1）
    // ------------------------------------------------------------------

    /** M 级阈值边界与普通值。 */
    @Test
    public void testMegaRange() {
        assertThat(FormatUtil.formatCount(1_000_000L)).isEqualTo("1.0M");
        assertThat(FormatUtil.formatCount(2_500_000L)).isEqualTo("2.5M");
        assertThat(FormatUtil.formatCount(999_999_999L)).isEqualTo("1.0G");
    }

    // ------------------------------------------------------------------
    // G 级（1e9 ~ 1e12-1）
    // ------------------------------------------------------------------

    /** G 级阈值边界。 */
    @Test
    public void testGigaRange() {
        assertThat(FormatUtil.formatCount(1_000_000_000L)).isEqualTo("1.0G");
        assertThat(FormatUtil.formatCount(999_999_999_999L)).isEqualTo("1.0T");
    }

    // ------------------------------------------------------------------
    // T / P / E 级
    // ------------------------------------------------------------------

    /** T 级（1e12）阈值边界。 */
    @Test
    public void testTeraRange() {
        assertThat(FormatUtil.formatCount(1_000_000_000_000L)).isEqualTo("1.0T");
        assertThat(FormatUtil.formatCount(999_999_999_999_999L)).isEqualTo("1.0P");
    }

    /** P 级（1e15）阈值边界。 */
    @Test
    public void testPetaRange() {
        assertThat(FormatUtil.formatCount(1_000_000_000_000_000L)).isEqualTo("1.0P");
        assertThat(FormatUtil.formatCount(999_999_999_999_999_999L)).isEqualTo("1.0E");
    }

    /** E 级（1e18）阈值边界，Long.MAX_VALUE 落入 E 级输出 "9.2E"。 */
    @Test
    public void testExaRangeAndLongMax() {
        assertThat(FormatUtil.formatCount(1_000_000_000_000_000_000L)).isEqualTo("1.0E");
        assertThat(FormatUtil.formatCount(Long.MAX_VALUE)).isEqualTo("9.2E");
    }

    // ------------------------------------------------------------------
    // Locale：小数点锁定为 '.'
    // ------------------------------------------------------------------

    /** 小数点为逗号的 Locale 下仍输出 '.'（String.format 锁定 Locale.ROOT）。 */
    @Test
    public void testLocaleIndependentDecimalPoint() {
        java.util.Locale original = java.util.Locale.getDefault();
        java.util.Locale.setDefault(java.util.Locale.GERMANY);
        try {
            assertThat(FormatUtil.formatCount(1500)).isEqualTo("1.5K");
            assertThat(FormatUtil.formatCount(999999)).isEqualTo("1.0M");
        } finally {
            java.util.Locale.setDefault(original);
        }
    }

    // ------------------------------------------------------------------
    // 负数：固化行为
    // ------------------------------------------------------------------

    /**
     * 固化行为：所有阈值判断均为 {@code >=}，负数不满足任何分支，
     * 一律按原值输出（包括 -1000 等绝对值超过阈值的负数）。
     */
    @Test
    public void testNegativeValuesRawOutput() {
        assertThat(FormatUtil.formatCount(-1)).isEqualTo("-1");
        assertThat(FormatUtil.formatCount(-999)).isEqualTo("-999");
        assertThat(FormatUtil.formatCount(-1000)).isEqualTo("-1000");
        assertThat(FormatUtil.formatCount(Long.MIN_VALUE)).isEqualTo("-9223372036854775808");
    }
}
