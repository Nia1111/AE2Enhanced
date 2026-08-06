package com.github.aeddddd.ae2enhanced.display;

/**
 * Y 轴量程模式.
 */
public enum YAxisMode {
    /** 自动量程:跟随数据,带平滑过渡 */
    AUTO,
    /** 固定量程:切换到该模式时捕获当前自动量程上限 */
    FIXED,
    /** 对数刻度:应对量级跨度大的多物品同屏 */
    LOG;

    public YAxisMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    private static final YAxisMode[] VALUES = values();

    public static YAxisMode byOrdinal(int ord) {
        return VALUES[Math.floorMod(ord, VALUES.length)];
    }
}
