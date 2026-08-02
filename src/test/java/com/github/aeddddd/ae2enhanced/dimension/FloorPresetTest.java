package com.github.aeddddd.ae2enhanced.dimension;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link FloorPreset} 地板预设平铺测试。
 *
 * <p>覆盖 getState(x,z) 的基本映射、Math.floorMod 负坐标平铺
 * （正负坐标映射到同一格）、越界 palette 索引返回 null、
 * stateList 长度不足的越界分支，以及 width/depth<=0、数组为 null 的防御分支。</p>
 */
public class FloorPresetTest {

    // 注意：不能在静态字段初始化时取 Blocks 常量（Bootstrap 之前为 null），
    // 必须在 @BeforeAll 引导之后再赋值
    private static IBlockState STONE;
    private static IBlockState DIRT;

    @BeforeAll
    public static void boot() {
        // 用例中访问 Blocks 常量，需无头引导初始化方块注册表
        AE2TestBootstrap.boot();
        STONE = Blocks.STONE.getDefaultState();
        DIRT = Blocks.DIRT.getDefaultState();
    }

    /**
     * 2x2 棋盘预设：
     * <pre>
     * z=0: stone dirt
     * z=1: dirt  stone
     * </pre>
     */
    private static FloorPreset checkerboard() {
        return new FloorPreset(2, 2, new IBlockState[] { STONE, DIRT }, new int[] { 0, 1, 1, 0 });
    }

    /** 基本映射：stateList 按 z*width+x 行主序索引。 */
    @Test
    public void testBasicMapping() {
        FloorPreset preset = checkerboard();

        assertThat(preset.getState(0, 0)).isEqualTo(STONE);
        assertThat(preset.getState(1, 0)).isEqualTo(DIRT);
        assertThat(preset.getState(0, 1)).isEqualTo(DIRT);
        assertThat(preset.getState(1, 1)).isEqualTo(STONE);
    }

    /** 正坐标平铺：坐标按 width/depth 取模重复。 */
    @Test
    public void testPositiveTiling() {
        FloorPreset preset = checkerboard();

        assertThat(preset.getState(2, 0)).isEqualTo(preset.getState(0, 0));
        assertThat(preset.getState(0, 2)).isEqualTo(preset.getState(0, 0));
        assertThat(preset.getState(2, 2)).isEqualTo(preset.getState(0, 0));
        assertThat(preset.getState(3, 5)).isEqualTo(preset.getState(1, 1));
    }

    /** 负坐标平铺：Math.floorMod 保证负坐标与对应正坐标映射到同一格。 */
    @Test
    public void testNegativeCoordinatesFloorMod() {
        FloorPreset preset = checkerboard();

        // -2 mod 2 = 0，与 (0,0) 同格
        assertThat(preset.getState(-2, -2)).isEqualTo(preset.getState(0, 0)).isEqualTo(STONE);
        // floorMod(-1, 2) = 1，与 (1,0) 同格
        assertThat(preset.getState(-1, 0)).isEqualTo(preset.getState(1, 0)).isEqualTo(DIRT);
        // floorMod(-3, 2) = 1、floorMod(-1, 2) = 1，与 (1,1) 同格
        assertThat(preset.getState(-3, -1)).isEqualTo(preset.getState(1, 1)).isEqualTo(STONE);
    }

    /** width/depth <= 0 的防御分支：getState 返回 null。 */
    @Test
    public void testNonPositiveDimensionsReturnNull() {
        IBlockState[] palette = { STONE };
        int[] stateList = { 0 };

        assertThat(new FloorPreset(0, 1, palette, stateList).getState(0, 0)).isNull();
        assertThat(new FloorPreset(1, 0, palette, stateList).getState(0, 0)).isNull();
        assertThat(new FloorPreset(-2, 1, palette, stateList).getState(0, 0)).isNull();
        assertThat(new FloorPreset(1, -2, palette, stateList).getState(0, 0)).isNull();
    }

    /** palette/stateList 为 null 的防御分支：getState 返回 null。 */
    @Test
    public void testNullArraysReturnNull() {
        assertThat(new FloorPreset(1, 1, null, new int[] { 0 }).getState(0, 0)).isNull();
        assertThat(new FloorPreset(1, 1, new IBlockState[] { STONE }, null).getState(0, 0)).isNull();
    }

    /** stateList 长度不足 width*depth：范围内的格正常，越界格返回 null。 */
    @Test
    public void testShortStateListReturnsNullForOutOfRangeCell() {
        // 3x3 预设但 stateList 只有 4 个元素
        FloorPreset preset = new FloorPreset(3, 3, new IBlockState[] { STONE, DIRT }, new int[] { 0, 1, 1, 0 });

        assertThat(preset.getState(0, 0)).isEqualTo(STONE); // idx 0，有效
        assertThat(preset.getState(0, 1)).isEqualTo(STONE); // idx 3，有效（边界）
        assertThat(preset.getState(1, 1)).isNull(); // idx 4 >= 4，越界
        assertThat(preset.getState(2, 2)).isNull(); // idx 8，越界
    }

    /** palette 索引越界（>= length 或负数）返回 null。 */
    @Test
    public void testPaletteIndexOutOfRangeReturnsNull() {
        FloorPreset preset = new FloorPreset(2, 2, new IBlockState[] { STONE, DIRT }, new int[] { 0, 5, -1, 1 });

        assertThat(preset.getState(0, 0)).isEqualTo(STONE); // 索引 0，有效
        assertThat(preset.getState(1, 0)).isNull(); // 索引 5 >= palette.length
        assertThat(preset.getState(0, 1)).isNull(); // 索引 -1
        assertThat(preset.getState(1, 1)).isEqualTo(DIRT); // 索引 1，有效
    }
}
