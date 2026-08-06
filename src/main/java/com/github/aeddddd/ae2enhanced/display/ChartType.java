package com.github.aeddddd.ae2enhanced.display;

/**
 * 趋势显示幕墙的图表类型.
 * 单屏同一时刻只绘制一种图表(全局切换,非逐项配置).
 */
public enum ChartType {
    /** 折线图:多物品多条彩色曲线 */
    LINE,
    /** 面积图:折线下方半透明填充 */
    AREA,
    /** 变化量柱状图:每区间 ±delta,直观看出净流入/流出 */
    DELTA,
    /** 速率读数:大字号 ±x/min + 迷你趋势线 */
    RATE,
    /** 构成占比:堆叠面积图 */
    STACKED;

    public ChartType next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    private static final ChartType[] VALUES = values();

    public static ChartType byOrdinal(int ord) {
        return VALUES[Math.floorMod(ord, VALUES.length)];
    }
}
