package com.github.aeddddd.ae2enhanced.test.specialcrafting;

import static com.github.aeddddd.ae2enhanced.test.specialcrafting.CycleAnalyzerTest.block;
import static com.github.aeddddd.ae2enhanced.test.specialcrafting.CycleAnalyzerTest.mult;
import static com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanAssert.assertThatPlan;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanMarker;

/**
 * G 组:跨样板循环链求解(CycleSolver 经 SpecialCraftingJob)测试(1.12.2 移植版).
 * <p>守恒不变量:交付量 = 请求量,网络消耗 = 种子 + 环外输入.</p>
 */
public class CycleSolverTest {

    /** G1:两节点增殖环 A→2B,B→A,有种子 → 闭式解成功. */
    @Test
    public void testTwoNodeCycleWithSeed() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        ICraftingPatternDetails p0 = env.addPattern(
                new ProcessingPatternBuilder(mult(cobble, 2)).addPreciseInput(1, stone).build());
        ICraftingPatternDetails p1 = env.addPattern(
                new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());
        env.addStoredItem(stone); // 种子 1

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        Map<ICraftingPatternDetails, Long> expected = new LinkedHashMap<>();
        expected.put(p0, 10L);
        expected.put(p1, 20L);
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(expected)
                .usedMatch(stone) // 仅消耗 1 种子
                .missingMatch();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isTrue();
    }

    /** G2:增殖环无种子 → 回落原生,报缺料(不凭空增殖). */
    @Test
    public void testCycleWithoutSeedFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(mult(cobble, 2)).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        assertThatPlan(plan).failed();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /** G3:中性环 → 不接管,原生行为(缺料失败). */
    @Test
    public void testNeutralCycleFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());
        env.addStoredItem(stone);

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 5)));
        assertThatPlan(plan).failed();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /** G4:耗散环 → 不接管,原生行为(缺料失败). */
    @Test
    public void testDissipativeCycleFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(2, cobble).build());
        env.addStoredItem(stone);

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 5)));
        assertThatPlan(plan).failed();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /** G5:环带环外输入 A+C→2B,B→A → 环外输入按份数消耗. */
    @Test
    public void testCycleWithExternalInput() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack dirt = block(Blocks.DIRT);
        ICraftingPatternDetails p0 = env.addPattern(new ProcessingPatternBuilder(mult(cobble, 2))
                .addPreciseInput(1, stone)
                .addPreciseInput(1, dirt)
                .build());
        ICraftingPatternDetails p1 = env.addPattern(
                new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());
        env.addStoredItem(stone); // 种子 1
        env.addStoredItem(mult(dirt, 100));

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        Map<ICraftingPatternDetails, Long> expected = new LinkedHashMap<>();
        expected.put(p0, 10L);
        expected.put(p1, 20L);
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(expected)
                .usedMatch(stone, mult(dirt, 10))
                .missingMatch();
    }

    /** G6:分数速率超轮缩放 A→1B,2B→3A → times=[2,1],种子 2. */
    @Test
    public void testRationalRateSuperRound() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        ICraftingPatternDetails p0 = env.addPattern(
                new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        ICraftingPatternDetails p1 = env.addPattern(
                new ProcessingPatternBuilder(mult(stone, 3)).addPreciseInput(2, cobble).build());
        env.addStoredItem(mult(stone, 2)); // 种子 2

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        Map<ICraftingPatternDetails, Long> expected = new LinkedHashMap<>();
        expected.put(p0, 20L);
        expected.put(p1, 10L);
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(expected)
                .usedMatch(mult(stone, 2))
                .missingMatch();
    }

    /** G7(问题 2 回归防护):环路径库存超出种子时同样全额环运转,仅种子计入 usedItems. */
    @Test
    public void testCycleBeyondSeedStillCraftsFully() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        ICraftingPatternDetails p0 = env.addPattern(
                new ProcessingPatternBuilder(mult(cobble, 2)).addPreciseInput(1, stone).build());
        ICraftingPatternDetails p1 = env.addPattern(
                new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());
        env.addStoredItem(mult(stone, 50)); // 库存远超种子

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        Map<ICraftingPatternDetails, Long> expected = new LinkedHashMap<>();
        expected.put(p0, 10L);
        expected.put(p1, 20L);
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(expected)
                .usedMatch(stone) // 仅 1 份种子
                .missingMatch();
    }

    /**
     * G8(用户案例):A→B,16A+16B+1W→64C,64C+1W→64A.
     * 平衡解 t=[16,1,1],净产 32A/轮,请求 64A → 2 轮.
     * A 多消费者:库存要求仅为 max(前缀种子, 每轮消耗)=32,
     * 运行时并发消耗由超轮配额调度器闸在每轮以内.
     */
    @Test
    public void testUserCaseMultiInputCycleSolved() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack sand = block(Blocks.SAND);
        IAEItemStack dirt = block(Blocks.DIRT);
        ICraftingPatternDetails p1 = env.addPattern(
                new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        ICraftingPatternDetails p2 = env.addPattern(new ProcessingPatternBuilder(mult(sand, 64))
                .addPreciseInput(16, stone)
                .addPreciseInput(16, cobble)
                .addPreciseInput(1, dirt)
                .build());
        ICraftingPatternDetails p3 = env.addPattern(new ProcessingPatternBuilder(mult(stone, 64))
                .addPreciseInput(64, sand)
                .addPreciseInput(1, dirt)
                .build());
        env.addStoredItem(mult(stone, 32)); // 每轮种子:max(32, 32)
        env.addStoredItem(mult(dirt, 100)); // 辅材 W

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 64)));
        Map<ICraftingPatternDetails, Long> expected = new LinkedHashMap<>();
        expected.put(p1, 32L);
        expected.put(p2, 2L);
        expected.put(p3, 2L);
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(expected) // 2 轮 × [16,1,1]
                .usedMatch(mult(stone, 32), mult(dirt, 4)) // 每轮种子 32A + 2×2W
                .missingMatch();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isTrue();
    }

    /** G9(用户案例变体):库存 16A 低于每轮种子要求(32A)→ 回落原生. */
    @Test
    public void testUserCaseInsufficientSeedFallsBack() {
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
        env.addStoredItem(mult(stone, 16)); // 低于每轮种子 32
        env.addStoredItem(mult(dirt, 100));

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 64)));
        assertThatPlan(plan).failed();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /**
     * G10(游戏案例,θ 形共享结构):A→B、A→C、B+C→4A——并集联立 t=[1,1,1],净产 2A/轮;
     * A 多消费者:库存要求降为 max(前缀种子, 每轮消耗)=2,usedItems 按种子记账.
     */
    @Test
    public void testThetaSharedPatternSolved() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack sand = block(Blocks.SAND);
        ICraftingPatternDetails pCrush = env.addPattern(
                new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        ICraftingPatternDetails pCharge = env.addPattern(
                new ProcessingPatternBuilder(sand).addPreciseInput(1, stone).build());
        ICraftingPatternDetails pBack = env.addPattern(new ProcessingPatternBuilder(mult(stone, 4))
                .addPreciseInput(1, cobble)
                .addPreciseInput(1, sand)
                .build());
        env.addStoredItem(mult(stone, 8)); // 远超每轮要求(2)
        env.addStoredItem(sand); // B+C→4A 中 C(sand)的前缀种子 1

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 8)));
        Map<ICraftingPatternDetails, Long> expected = new LinkedHashMap<>();
        expected.put(pCrush, 4L);
        expected.put(pBack, 4L);
        expected.put(pCharge, 4L);
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(expected)
                .usedMatch(mult(stone, 2), sand) // 每轮种子记账
                .missingMatch();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isTrue();
    }

    /** G11:θ 结构缺少中间物前缀种子(sand=0)→ 并集与逐环均不可解,回落原生. */
    @Test
    public void testThetaSharedPatternMissingSeedFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack sand = block(Blocks.SAND);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(sand).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(mult(stone, 4))
                .addPreciseInput(1, cobble)
                .addPreciseInput(1, sand)
                .build());
        env.addStoredItem(mult(stone, 8)); // 缺 sand 种子

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 8)));
        assertThatPlan(plan).failed();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }
}
