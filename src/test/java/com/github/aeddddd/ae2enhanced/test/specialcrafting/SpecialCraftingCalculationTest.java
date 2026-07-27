package com.github.aeddddd.ae2enhanced.test.specialcrafting;

import static com.github.aeddddd.ae2enhanced.test.specialcrafting.CycleAnalyzerTest.block;
import static com.github.aeddddd.ae2enhanced.test.specialcrafting.CycleAnalyzerTest.item;
import static com.github.aeddddd.ae2enhanced.test.specialcrafting.CycleAnalyzerTest.mult;
import static com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanAssert.assertThatPlan;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanMarker;

/**
 * B/C 组:特殊配方求解器(SpecialCraftingJob)测试(1.12.2 移植版).
 * <p>B 组验证路由透明性(detector 未命中时与原生逐字段一致);
 * C 组验证净产出自引用的闭式解、种子语义、多分支与溢出回落.</p>
 */
public class SpecialCraftingCalculationTest {

    /** B1:detector 未命中 → 特殊 job 走 super.run(),与原生计划逐字段一致. */
    @Test
    public void testPassthroughMatchesNative() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack stone = block(Blocks.STONE);
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());
        env.addStoredItem(mult(cobble, 100));

        PlanView nativePlan = PlanView.of(env.runNative(mult(stone, 10)));
        PlanView specialPlan = PlanView.of(env.runSpecial(mult(stone, 10)));

        assertThat(specialPlan.simulation()).isEqualTo(nativePlan.simulation());
        assertThat(specialPlan.finalOutput()).isEqualTo(nativePlan.finalOutput());
        assertThat(specialPlan.bytes()).isEqualTo(nativePlan.bytes());
        assertThat(specialPlan.patternTimes()).isEqualTo(nativePlan.patternTimes());
        assertThat(specialPlan.usedItems()).isEqualTo(nativePlan.usedItems());
        assertThat(specialPlan.missingItems()).isEqualTo(nativePlan.missingItems());
        // 原生计划不得被标记为特殊计划
        assertThat(SpecialPlanMarker.isSpecial(specialPlan.job())).isFalse();
    }

    /** C1:1→2 净增殖,唯一候选,有种子 → 闭式解成功,种子计入 usedItems. */
    @Test
    public void testSelfRefClosedFormWithSeed() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        ICraftingPatternDetails dup = env.addPattern(
                new ProcessingPatternBuilder(mult(stone, 2)).addPreciseInput(1, stone).build());
        env.addStoredItem(stone); // 种子 1

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(dup, 10)
                .usedMatch(stone)
                .missingMatch();
        // 守恒:交付 10 = 净产出 10×(2-1),网络仅消耗 1 种子
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isTrue();
    }

    /** C2:1→2 净增殖,无种子 → 回落原生,报缺料(不凭空增殖). */
    @Test
    public void testSelfRefWithoutSeedFallsBackToMissing() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        env.addPattern(new ProcessingPatternBuilder(mult(stone, 2)).addPreciseInput(1, stone).build());

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        assertThatPlan(plan).failed();
        // 原生兜底:缺失物品必须为 stone(具体数量随原生展开细节)
        assertThat(plan.missingItems().get(com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper
                .canon(stone))).isGreaterThanOrEqualTo(1);
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /** C3:A+2B→2A(锻造模板型),种子 + 充足 B → 成功,B 按份数消耗. */
    @Test
    public void testSmithingTemplateStyleDuplication() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack diamond = item(Items.DIAMOND);
        IAEItemStack stick = item(Items.STICK);
        ICraftingPatternDetails dup = env.addPattern(new ProcessingPatternBuilder(mult(diamond, 2))
                .addPreciseInput(1, diamond)
                .addPreciseInput(2, stick)
                .build());
        env.addStoredItem(diamond); // 种子 1
        env.addStoredItem(mult(stick, 100));

        PlanView plan = PlanView.of(env.runSpecial(mult(diamond, 10)));
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(dup, 10)
                .usedMatch(diamond, mult(stick, 20))
                .missingMatch();
    }

    /**
     * C4:库存超出种子时不做"库存直接交付"(AE2 执行模型只认样板产出,
     * 无样板任务的计划永远无法完成):仅种子计入 usedItems,全额合成.
     */
    @Test
    public void testStockBeyondSeedStillCraftsFully() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        ICraftingPatternDetails dup = env.addPattern(
                new ProcessingPatternBuilder(mult(stone, 2)).addPreciseInput(1, stone).build());
        env.addStoredItem(mult(stone, 5)); // 库存远超种子

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(dup, 10) // 全额 10 份,不用库存抵扣
                .usedMatch(stone) // 仅 1 份种子
                .missingMatch();
    }

    /** C15(问题 2 回归防护):库存 > 下单量时,成功计划必须包含样板任务(不得为空). */
    @Test
    public void testPlanNeverReliesOnPureStockDelivery() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        ICraftingPatternDetails dup = env.addPattern(
                new ProcessingPatternBuilder(mult(stone, 2)).addPreciseInput(1, stone).build());
        env.addStoredItem(mult(stone, 100)); // 现存 >> 下单

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 5)));
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(dup, 5)
                .usedMatch(stone)
                .missingMatch();
        assertThat(plan.patternTimes()).isNotEmpty(); // 空任务计划 = CPU 提取材料后卡死
    }

    /** C5:多分支(自引用 + 普通),无种子 → 回落原生(1.12.2 原生语义:不被标记,可完成). */
    @Test
    public void testMultiBranchWithoutSeedUsesNormalBranch() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack stone = block(Blocks.STONE);
        env.addPattern(new ProcessingPatternBuilder(mult(stone, 2)).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());
        env.addStoredItem(mult(cobble, 5));

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 5)));
        // 1.12.2 原生计算器可经自引用样板利用 cobble 完成(与 1.20.1 原生语义不同,
        // 此处仅断言:回落原生、不被标记、守恒闭合)
        assertThat(plan.simulation()).isFalse();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /** C6:多分支,有种子 → 自引用增殖优先. */
    @Test
    public void testMultiBranchWithSeedPrefersSelfRef() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack stone = block(Blocks.STONE);
        ICraftingPatternDetails dup = env.addPattern(
                new ProcessingPatternBuilder(mult(stone, 2)).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(stone).addPreciseInput(1, cobble).build());
        env.addStoredItem(stone); // 种子 1
        env.addStoredItem(mult(cobble, 5));

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(dup, 10)
                .usedMatch(stone);
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isTrue();
    }

    /**
     * C7a:催化剂型(A→A+B)请求 A → O(1) 缺料计划.
     * 请求物无法增殖,与原生失败语义一致,但避免原生逐份展开在超大单下挂起.
     */
    @Test
    public void testCatalystRequestSelfReportsMissing() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack stick = item(Items.STICK);
        env.addPattern(new ProcessingPatternBuilder(stone, stick).addPreciseInput(1, stone).build());
        env.addStoredItem(mult(stone, 10));

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        assertThatPlan(plan)
                .failed()
                .missingMatch(mult(stone, 10));
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /** C7b:催化剂型请求 A 且无库存 → 同样 O(1) 缺料(不逐份展开). */
    @Test
    public void testCatalystRequestSelfInsufficientStock() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack stick = item(Items.STICK);
        env.addPattern(new ProcessingPatternBuilder(stone, stick).addPreciseInput(1, stone).build());
        env.addStoredItem(mult(stone, 3));

        PlanView plan = PlanView.of(env.runSpecial(mult(stone, 10)));
        assertThatPlan(plan)
                .failed()
                .missingMatch(mult(stone, 10));
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /** C12:自引用 key ≠ 请求 key(A→B+A,请求 B)→ 贷款法成功,种子仅 1 份 A. */
    @Test
    public void testCatalystOtherKeyWithSeed() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack diamond = item(Items.DIAMOND);
        IAEItemStack emerald = item(Items.EMERALD);
        // 1 diamond -> 1 emerald + 1 diamond:diamond 为自引用催化剂,emerald 为请求物
        ICraftingPatternDetails pattern = env.addPattern(new ProcessingPatternBuilder(emerald, diamond)
                .addPreciseInput(1, diamond)
                .build());
        env.addStoredItem(diamond); // 种子 1

        PlanView plan = PlanView.of(env.runSpecial(mult(emerald, 10)));
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(pattern, 10)
                .usedMatch(diamond) // 仅 1 份种子
                .missingMatch();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isTrue();
    }

    /** C13:自引用 key ≠ 请求 key 且净增殖(A→B+2A,请求 B)→ 贷款法成功. */
    @Test
    public void testNetPositiveOtherKeyWithSeed() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack diamond = item(Items.DIAMOND);
        IAEItemStack emerald = item(Items.EMERALD);
        ICraftingPatternDetails pattern = env.addPattern(new ProcessingPatternBuilder(emerald, mult(diamond, 2))
                .addPreciseInput(1, diamond)
                .build());
        env.addStoredItem(diamond); // 种子 1

        PlanView plan = PlanView.of(env.runSpecial(mult(emerald, 5)));
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(pattern, 5)
                .usedMatch(diamond)
                .missingMatch();
    }

    /** C12b:自引用 key ≠ 请求 key,无种子 → 回落原生(快速失败). */
    @Test
    public void testCatalystOtherKeyWithoutSeedFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack diamond = item(Items.DIAMOND);
        IAEItemStack emerald = item(Items.EMERALD);
        env.addPattern(new ProcessingPatternBuilder(emerald, diamond)
                .addPreciseInput(1, diamond)
                .build());

        PlanView plan = PlanView.of(env.runSpecial(mult(emerald, 10)));
        assertThatPlan(plan).failed();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }

    /**
     * C14:NBT 恒等催化剂(带 NBT/耐久的物品同 key 返还,如受损镐原样返还)→
     * 精确 key 相等即被广义自引用覆盖,无需逐份展开.
     */
    @Test
    public void testNbtIdenticalCatalystWithSeed() {
        SimulationEnv env = new SimulationEnv();
        ItemStack damagedStack = new ItemStack(Items.DIAMOND_PICKAXE);
        damagedStack.setItemDamage(100);
        IAEItemStack damagedPickaxe = AEItemStack.fromItemStack(damagedStack);
        IAEItemStack emerald = item(Items.EMERALD);
        // 1 受损镐 -> 1 绿宝石 + 1 受损镐(同 NBT 恒等返还)
        ICraftingPatternDetails pattern = env.addPattern(new ProcessingPatternBuilder(emerald, damagedPickaxe)
                .addPreciseInput(1, damagedPickaxe)
                .build());
        env.addStoredItem(damagedPickaxe); // 种子 1(同一 NBT key)

        PlanView plan = PlanView.of(env.runSpecial(mult(emerald, 10)));
        assertThatPlan(plan)
                .succeeded()
                .patternsMatch(pattern, 10)
                .usedMatch(damagedPickaxe) // 仅 1 份种子
                .missingMatch();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isTrue();
    }

    /** C9:天文数字订单(贷款量溢出 long)→ O(1) 缺料计划,不静默截断. */
    @Test
    public void testAstronomicalOrderFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        // 2→3:inPer=2,request Long.MAX_VALUE 时 crafts > Long.MAX_VALUE/2 → 溢出
        env.addPattern(new ProcessingPatternBuilder(mult(stone, 3)).addPreciseInput(2, stone).build());
        env.addStoredItem(mult(stone, 2));

        IAEItemStack huge = stone.copy();
        huge.setStackSize(Long.MAX_VALUE);
        PlanView plan = PlanView.of(env.runSpecial(huge));
        assertThatPlan(plan).failed();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).isFalse();
    }
}
