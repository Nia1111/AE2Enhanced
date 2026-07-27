package com.github.aeddddd.ae2enhanced.test.specialcrafting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;

import appeng.api.storage.data.IAEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialRecipeDetector;

/**
 * D 组:特殊配方预扫描(detector)命中判定测试(1.12.2 移植版).
 * <p>1.20.1 的 D5(流体 key 自引用)不单独移植:1.12.2 的 ae2fc 流体样板以
 * FluidDrop 假物品形式存在,与物品 key 走同一代码路径,已由物品用例覆盖.</p>
 */
public class SpecialRecipeDetectorTest {

    /** D1:候选样板含净产出自引用 → 命中. */
    @Test
    public void testSelfRefPatternHits() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = CycleAnalyzerTest.block(Blocks.STONE);
        env.addPattern(new ProcessingPatternBuilder(CycleAnalyzerTest.mult(stone, 2)).addPreciseInput(1, stone).build());

        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), stone, null)).isTrue();
    }

    /** D2:仅普通样板 → 未命中. */
    @Test
    public void testNormalPatternMisses() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack cobble = CycleAnalyzerTest.block(Blocks.COBBLESTONE);
        IAEItemStack stone = CycleAnalyzerTest.block(Blocks.STONE);
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());

        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), stone, null)).isFalse();
    }

    /** D3:自引用样板存在但与请求 key 无关 → 未命中. */
    @Test
    public void testUnrelatedSelfRefMisses() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack cobble = CycleAnalyzerTest.block(Blocks.COBBLESTONE);
        IAEItemStack stone = CycleAnalyzerTest.block(Blocks.STONE);
        IAEItemStack dirt = CycleAnalyzerTest.block(Blocks.DIRT);
        // cobble 有自引用样板,但请求的是 stone
        env.addPattern(new ProcessingPatternBuilder(CycleAnalyzerTest.mult(cobble, 2)).addPreciseInput(1, cobble).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, dirt).build());

        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), stone, null)).isFalse();
    }

    /** D4:无任何候选样板 → 未命中. */
    @Test
    public void testNoPatternMisses() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = CycleAnalyzerTest.block(Blocks.STONE);

        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), stone, null)).isFalse();
    }

    /** D6:催化剂型(进出等量)→ 命中(原生逐份展开会在超大订单挂起,必须路由). */
    @Test
    public void testCatalystPatternHits() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = CycleAnalyzerTest.block(Blocks.STONE);
        IAEItemStack stick = CycleAnalyzerTest.item(Items.STICK);
        // A -> A + B:stone 进出等量
        env.addPattern(new ProcessingPatternBuilder(stone, stick).addPreciseInput(1, stone).build());

        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), stone, null)).isTrue();
    }

    /** D7:增殖循环链(A→2B,B→A)→ 命中. */
    @Test
    public void testProductiveCycleHits() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = CycleAnalyzerTest.block(Blocks.STONE);
        IAEItemStack cobble = CycleAnalyzerTest.block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(CycleAnalyzerTest.mult(cobble, 2)).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());

        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), stone, null)).isTrue();
    }

    /** D8:中性循环链(A→B,B→A,净率 1)→ 未命中(不接管,走原生). */
    @Test
    public void testNeutralCycleMisses() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = CycleAnalyzerTest.block(Blocks.STONE);
        IAEItemStack cobble = CycleAnalyzerTest.block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());

        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), stone, null)).isFalse();
    }

    /** D9:θ 形共享结构(A→B、A→C、B+C→4A)→ 命中(单环分析即增殖,并集联立真正可解). */
    @Test
    public void testThetaUnionHits() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = CycleAnalyzerTest.block(Blocks.STONE);
        IAEItemStack cobble = CycleAnalyzerTest.block(Blocks.COBBLESTONE);
        IAEItemStack sand = CycleAnalyzerTest.block(Blocks.SAND);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(sand).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(CycleAnalyzerTest.mult(stone, 4))
                .addPreciseInput(1, cobble)
                .addPreciseInput(1, sand)
                .build());

        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), stone, null)).isTrue();
    }
}
