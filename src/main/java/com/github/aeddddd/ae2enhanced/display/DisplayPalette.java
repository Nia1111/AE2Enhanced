package com.github.aeddddd.ae2enhanced.display;

/**
 * 监控项曲线的调色板(ARGB).
 * 默认按槽位索引分配,玩家可在 GUI 中逐项切换.
 */
public final class DisplayPalette {

    private DisplayPalette() {}

    public static final int[] COLORS = {
        0xFF00D4FF, // 青
        0xFFFFAA55, // 橙
        0xFF55FF88, // 绿
        0xFFFF66CC, // 品红
        0xFFFFE866, // 黄
        0xFFAA77FF, // 紫
        0xFFFF5555, // 红
        0xFF5599FF, // 蓝
    };

    public static int get(int index) {
        return COLORS[Math.floorMod(index, COLORS.length)];
    }

    public static int size() {
        return COLORS.length;
    }
}
