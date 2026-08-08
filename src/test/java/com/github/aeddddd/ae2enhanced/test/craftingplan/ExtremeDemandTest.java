package com.github.aeddddd.ae2enhanced.test.craftingplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanMarker;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanView;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.ProcessingPatternBuilder;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.SimulationEnv;

/**
 * E 组:天文数字需求（接近 {@link Long#MAX_VALUE}）回归.
 * <p>历史病灶:各求解器的 ceilDiv 写作 (a + b - 1) / b,需求近 Long.MAX 时加法回绕成
 * 负数,被误判为"循环边界不可解"而<b>整单回落原生递归树</b>——在大网络上即用户观测到
 * 的"高请求计算速度很慢".修复后:数值不可表示的边界需求 O(1) 就地记缺料（对齐根路径
 * missingRoot 语义）,其余分支照常规划.</p>
 * <p>区分"O(1) 缺料"与"回落原生"的判据:原生模拟路径中环样板会记录 crafts &gt; 0
 * （子请求发出后才失败）,而 O(1) 缺料路径环样板零调用.</p>
 */
public class ExtremeDemandTest {

    private static IAEItemStack block(net.minecraft.block.Block b) {
        return AEItemStack.fromItemStack(new ItemStack(b));
    }

    private static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(multiplier);
        return copy;
    }

    /** E1:θ 边界需求 Long.MAX → 贷款水位不可表示 → O(1) 缺料,环样板零调用,其余分支照常. */
    @Test
    public void testThetaBoundaryAstronomicalDemandMissing() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack c = block(Blocks.STONE);
        IAEItemStack x = block(Blocks.COBBLESTONE);
        IAEItemStack y = block(Blocks.SAND);
        IAEItemStack d = block(Blocks.DIRT);
        IAEItemStack e = block(Blocks.GRAVEL);
        ICraftingPatternDetails pE = env.addPattern(
                new ProcessingPatternBuilder(e).addPreciseInput(1, d).build());
        ICraftingPatternDetails pD = env.addPattern(
                new ProcessingPatternBuilder(d).addPreciseInput(1, c).build());
        ICraftingPatternDetails pX = env.addPattern(
                new ProcessingPatternBuilder(x).addPreciseInput(1, c).build());
        ICraftingPatternDetails pY = env.addPattern(
                new ProcessingPatternBuilder(y).addPreciseInput(1, c).build());
        ICraftingPatternDetails pC = env.addPattern(new ProcessingPatternBuilder(mult(c, 4))
                .addPreciseInput(1, x)
                .addPreciseInput(1, y)
                .build());
        env.addStoredItem(mult(c, 8)); // 种子
        env.addStoredItem(y);

        PlanView plan = PlanView.of(env.runDag(mult(e, Long.MAX_VALUE)));

        assertThat(plan.simulation()).as("缺料计划").isTrue();
        assertThat(plan.missingItems().get(RecursiveCraftingHelper.canon(c)))
                .as("边界需求全额记缺").isEqualTo(Long.MAX_VALUE);
        Map<ICraftingPatternDetails, Long> times = plan.patternTimes();
        // 非原生回落的证据:环样板零调用(原生模拟路径会记录 crafts > 0)
        assertThat(times.getOrDefault(pX, 0L)).isEqualTo(0L);
        assertThat(times.getOrDefault(pY, 0L)).isEqualTo(0L);
        assertThat(times.getOrDefault(pC, 0L)).isEqualTo(0L);
        // 环外分支照常规划
        assertThat(times.get(pE)).isEqualTo(Long.MAX_VALUE);
        assertThat(times.get(pD)).isEqualTo(Long.MAX_VALUE);
    }

    /** E2:自增殖边界 2X→3X 需求 Long.MAX → 贷款 crafts > Long.MAX/inPer → O(1) 缺料. */
    @Test
    public void testSelfDupBoundaryAstronomicalDemandMissing() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack x = block(Blocks.COBBLESTONE);
        IAEItemStack d = block(Blocks.DIRT);
        ICraftingPatternDetails pD = env.addPattern(
                new ProcessingPatternBuilder(d).addPreciseInput(1, x).build());
        ICraftingPatternDetails pDup = env.addPattern(
                new ProcessingPatternBuilder(mult(x, 3)).addPreciseInput(2, x).build());
        env.addStoredItem(mult(x, 2)); // 种子 = inPer

        PlanView plan = PlanView.of(env.runDag(mult(d, Long.MAX_VALUE)));

        assertThat(plan.simulation()).as("缺料计划").isTrue();
        assertThat(plan.missingItems().get(RecursiveCraftingHelper.canon(x)))
                .isEqualTo(Long.MAX_VALUE);
        Map<ICraftingPatternDetails, Long> times = plan.patternTimes();
        assertThat(times.getOrDefault(pDup, 0L)).as("dup 样板零调用").isEqualTo(0L);
        assertThat(times.get(pD)).isEqualTo(Long.MAX_VALUE);
    }

    /**
     * E3:普通路径批量产出样的 ceilDiv 饱和——请求 Long.MAX、每次产 4:
     * 旧实现 (a+b-1)/b 回绕成负数导致子需求被钳为 0(错误地"无缺料"),
     * 修复后 times = ceil(Long.MAX/4),缺料如实上报.
     */
    @Test
    public void testNormalPathCeilDivSaturation() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack b = block(Blocks.COBBLESTONE);
        IAEItemStack c = block(Blocks.DIRT);
        env.addPattern(new ProcessingPatternBuilder(c).addPreciseInput(1, b).build());
        ICraftingPatternDetails pB = env.addPattern(
                new ProcessingPatternBuilder(mult(b, 4)).addPreciseInput(1, a).build());
        env.addStoredItem(mult(a, 1000));

        PlanView plan = PlanView.of(env.runDag(mult(c, Long.MAX_VALUE)));

        long expectedTimes = Long.MAX_VALUE / 4 + 1; // 2305843009213693952
        assertThat(plan.patternTimes().get(pB)).isEqualTo(expectedTimes);
        assertThat(plan.simulation()).as("原料不足须报缺料").isTrue();
        assertThat(plan.missingItems().get(RecursiveCraftingHelper.canon(a)))
                .isEqualTo(expectedTimes - 1000);
    }

    /**
     * E5:自增殖边界 1X→2X(inPer=1)需求 exact Long.MAX → 产出 2×crafts 超 long 不可表示
     * → O(1) 缺料.(旧的贷款守卫用 inPer 判定,inPer=1 时恰好漏过 exact Long.MAX:
     * 产出回绕成负数 → 结算失败 → 整单回落原生)
     */
    @Test
    public void testSelfDupUnitInputAstronomicalDemandMissing() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack x = block(Blocks.COBBLESTONE);
        IAEItemStack d = block(Blocks.DIRT);
        ICraftingPatternDetails pD = env.addPattern(
                new ProcessingPatternBuilder(d).addPreciseInput(1, x).build());
        ICraftingPatternDetails pDup = env.addPattern(
                new ProcessingPatternBuilder(mult(x, 2)).addPreciseInput(1, x).build());
        env.addStoredItem(x); // 种子 1

        PlanView plan = PlanView.of(env.runDag(mult(d, Long.MAX_VALUE)));

        assertThat(plan.simulation()).as("缺料计划").isTrue();
        assertThat(plan.missingItems().get(RecursiveCraftingHelper.canon(x)))
                .isEqualTo(Long.MAX_VALUE);
        Map<ICraftingPatternDetails, Long> times = plan.patternTimes();
        assertThat(times.getOrDefault(pDup, 0L)).as("dup 样板零调用").isEqualTo(0L);
        assertThat(times.get(pD)).isEqualTo(Long.MAX_VALUE);
    }

    /** E4:根级 θ 环请求 exact Long.MAX → 特殊路径 O(1) 缺料(不回落原生,环样板零调用). */
    @Test
    public void testRootCycleAstronomicalDemandMissing() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack c = block(Blocks.STONE);
        IAEItemStack x = block(Blocks.COBBLESTONE);
        IAEItemStack y = block(Blocks.SAND);
        ICraftingPatternDetails pX = env.addPattern(
                new ProcessingPatternBuilder(x).addPreciseInput(1, c).build());
        ICraftingPatternDetails pY = env.addPattern(
                new ProcessingPatternBuilder(y).addPreciseInput(1, c).build());
        ICraftingPatternDetails pC = env.addPattern(new ProcessingPatternBuilder(mult(c, 4))
                .addPreciseInput(1, x)
                .addPreciseInput(1, y)
                .build());
        env.addStoredItem(mult(c, 8));
        env.addStoredItem(y);

        PlanView plan = PlanView.of(env.runSpecial(mult(c, Long.MAX_VALUE)));

        assertThat(plan.simulation()).as("缺料计划").isTrue();
        assertThat(SpecialPlanMarker.isSpecial(plan.job())).as("缺料计划不标记特殊").isFalse();
        assertThat(plan.missingItems().get(RecursiveCraftingHelper.canon(c)))
                .isEqualTo(Long.MAX_VALUE);
        Map<ICraftingPatternDetails, Long> times = plan.patternTimes();
        assertThat(times.getOrDefault(pX, 0L)).isEqualTo(0L);
        assertThat(times.getOrDefault(pY, 0L)).isEqualTo(0L);
        assertThat(times.getOrDefault(pC, 0L)).isEqualTo(0L);
    }
}
