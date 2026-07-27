package com.github.aeddddd.ae2enhanced.test.specialcrafting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.CycleAnalyzer;

/**
 * F 组:跨样板循环链分析器(CycleAnalyzer)纯单元测试(1.12.2 移植版).
 * <p>API 差异:1.12.2 的 findCyclesThrough 返回候选环列表(按长度降序),
 * 1.20.1 的 findCycle 单环语义对应"列表非空 + 取首个".</p>
 */
public class CycleAnalyzerTest {

    /** F1:无环 → 空列表. */
    @Test
    public void testNoCycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack stone = block(Blocks.STONE);
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());

        assertThat(CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null)).isEmpty();
    }

    /** F2:仅自引用(阶段 1 范围)→ 不识别为跨样板环. */
    @Test
    public void testSelfRefNotACycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        env.addPattern(new ProcessingPatternBuilder(mult(stone, 2)).addPreciseInput(1, stone).build());

        assertThat(CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null)).isEmpty();
    }

    /** F3:两节点增殖环 A→2B,B→A → PRODUCTIVE,数值正确. */
    @Test
    public void testTwoNodeProductiveCycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(mult(cobble, 2)).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());

        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null);
        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0)).hasSize(2);

        CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycles.get(0));
        assertThat(analysis).isNotNull();
        assertThat(analysis.rateClass()).isEqualTo(CycleAnalyzer.RateClass.PRODUCTIVE);
        assertThat(analysis.timesPerRound()).containsExactly(1, 2);
        assertThat(analysis.netGain()).isEqualTo(1);
        assertThat(analysis.seedsPerKey()).containsExactly(1, 0);
    }

    /** F4:三节点增殖环 A→B,B→C,C→2A. */
    @Test
    public void testThreeNodeProductiveCycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack dirt = block(Blocks.DIRT);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(dirt).addPreciseInput(1, cobble).build());
        env.addPattern(new ProcessingPatternBuilder(mult(stone, 2)).addPreciseInput(1, dirt).build());

        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null);
        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0)).hasSize(3);

        CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycles.get(0));
        assertThat(analysis).isNotNull();
        assertThat(analysis.rateClass()).isEqualTo(CycleAnalyzer.RateClass.PRODUCTIVE);
        assertThat(analysis.timesPerRound()).containsExactly(1, 1, 1);
        assertThat(analysis.netGain()).isEqualTo(1);
        assertThat(analysis.seedsPerKey()).containsExactly(1, 0, 0);
    }

    /** F5:存在无关普通样板时仍能发现环. */
    @Test
    public void testCycleFoundWithExtraPatterns() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack dirt = block(Blocks.DIRT);
        env.addPattern(new ProcessingPatternBuilder(mult(cobble, 2)).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, dirt).build());

        assertThat(CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null)).isNotEmpty();
    }

    /** F6:中性环(净率 = 1)→ NEUTRAL. */
    @Test
    public void testNeutralCycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());

        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null);
        assertThat(cycles).isNotEmpty();
        CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycles.get(0));
        assertThat(analysis).isNotNull();
        assertThat(analysis.rateClass()).isEqualTo(CycleAnalyzer.RateClass.NEUTRAL);
    }

    /** F7:耗散环(净率 < 1)→ DISSIPATIVE. */
    @Test
    public void testDissipativeCycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(2, cobble).build());

        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null);
        assertThat(cycles).isNotEmpty();
        CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycles.get(0));
        assertThat(analysis).isNotNull();
        assertThat(analysis.rateClass()).isEqualTo(CycleAnalyzer.RateClass.DISSIPATIVE);
    }

    /**
     * F8:样板消耗多种环内物品但净率为 1(A+B→2B,B→A:每轮 A 净变化为 0)
     * → 泛化引擎可分析,分类为 NEUTRAL,不接管.
     */
    @Test
    public void testMultiCycleKeyInputNeutralCycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        // P0:1A + 1B → 2B(消耗两种环内物品);P1:1B → 1A
        env.addPattern(new ProcessingPatternBuilder(mult(cobble, 2))
                .addPreciseInput(1, stone)
                .addPreciseInput(1, cobble)
                .build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());

        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null);
        assertThat(cycles).isNotEmpty();
        CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycles.get(0));
        assertThat(analysis).isNotNull();
        assertThat(analysis.rateClass()).isEqualTo(CycleAnalyzer.RateClass.NEUTRAL);
    }

    /**
     * F9(用户案例):A→B,16A+16B+1W→64C,64C+1W→64A.
     * 样板同时消耗两种环内物品 → 线性平衡解 t=[16,1,1],每轮净产 32A,种子 32A.
     */
    @Test
    public void testUserCaseMultiInputCycle() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack sand = block(Blocks.SAND);
        IAEItemStack dirt = block(Blocks.DIRT);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(mult(sand, 64))
                .addPreciseInput(16, stone)
                .addPreciseInput(16, cobble)
                .addPreciseInput(1, dirt)
                .build());
        env.addPattern(new ProcessingPatternBuilder(mult(stone, 64))
                .addPreciseInput(64, sand)
                .addPreciseInput(1, dirt)
                .build());

        // 应找到最长的三键环(而非 {A,C} 两键短环)
        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null);
        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0)).hasSize(3);

        CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycles.get(0));
        assertThat(analysis).isNotNull();
        assertThat(analysis.rateClass()).isEqualTo(CycleAnalyzer.RateClass.PRODUCTIVE);
        assertThat(analysis.timesPerRound()).containsExactly(16, 1, 1);
        assertThat(analysis.netGain()).isEqualTo(32);
        assertThat(analysis.seedsPerKey()).containsExactly(32, 0, 0);
        // A 被 P1、P2 两个步骤消耗(多消费者键)→ 全批次保守种子 = 每轮总消耗 32;
        // B、C 单消费者 → 0(前缀种子+贷款在运行时对任意推送顺序安全)
        assertThat(analysis.batchSeedPerKey()).containsExactly(32, 0, 0);
    }

    /**
     * F10(游戏案例,θ 形共享结构):A→B、A→C、B+C→4A——"B+C→4A"被两个两键环共享,
     * 逐环分析互相把对方中间物当环外输入;并集联立:3 键 × 3 样板适定,t=[1,1,1],
     * 净产 2A/轮,A 多消费者(被两个样板消耗)需全批次种子 2/轮.
     */
    @Test
    public void testThetaSharedPatternUnionAnalysis() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack sand = block(Blocks.SAND);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build()); // A→B
        env.addPattern(new ProcessingPatternBuilder(sand).addPreciseInput(1, stone).build()); // A→C
        env.addPattern(new ProcessingPatternBuilder(mult(stone, 4))
                .addPreciseInput(1, cobble)
                .addPreciseInput(1, sand)
                .build()); // B+C→4A

        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer.findCyclesThrough(env.craftingGrid(), stone, null);
        assertThat(cycles).hasSize(2); // 两个两键环,共享 P_back

        CycleAnalyzer.Analysis union = CycleAnalyzer.analyzeUnion(cycles);
        assertThat(union).isNotNull();
        assertThat(union.keys()).containsExactly(
                com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper.canon(stone),
                com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper.canon(cobble),
                com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper.canon(sand));
        assertThat(union.rateClass()).isEqualTo(CycleAnalyzer.RateClass.PRODUCTIVE);
        assertThat(union.timesPerRound()).containsExactly(1, 1, 1);
        assertThat(union.netGain()).isEqualTo(2);
        assertThat(union.seedsPerKey()).containsExactly(1, 0, 1);
        assertThat(union.batchSeedPerKey()).containsExactly(2, 0, 0);
    }

    static IAEItemStack item(Item item) {
        return AEItemStack.fromItemStack(new ItemStack(item));
    }

    static IAEItemStack block(Block block) {
        return AEItemStack.fromItemStack(new ItemStack(block));
    }

    static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(template.getStackSize() * multiplier);
        return copy;
    }
}
