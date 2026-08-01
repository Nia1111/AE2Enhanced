package com.github.aeddddd.ae2enhanced.test.specialcrafting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialRecipeDetector;

/**
 * 催化环（中性/增殖环发射环外副产物）测试组,1.12.2 移植.
 * <p>场景:1A → 1X + 1B(X 主产出,B 副产物)、1B → nA,请求 X——
 * 循环经副产物闭合,X 不在环键上;detector/求解/边界/深层 DAG 全链路.</p>
 */
public class CatalyticCycleTest {

    private static IAEItemStack block(net.minecraft.block.Block b) {
        return AEItemStack.fromItemStack(new ItemStack(b));
    }

    private static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(template.getStackSize() * multiplier);
        return copy;
    }

    private static Map<IAEItemStack, Long> primaryMap(PlanView plan) {
        Map<IAEItemStack, Long> out = new HashMap<>();
        for (Map.Entry<appeng.api.networking.crafting.ICraftingPatternDetails, Long> entry : plan
                .patternTimes().entrySet()) {
            IAEItemStack primary = entry.getKey().getPrimaryOutput();
            if (primary != null) {
                out.merge(RecursiveCraftingHelper.canon(primary), entry.getValue(), Long::sum);
            }
        }
        return out;
    }

    /** 催化环环境:1A → 1X + 1B、1B → nA. */
    private SimulationEnv catalyticEnv(int aPerB) {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack b = block(Blocks.COBBLESTONE);
        IAEItemStack x = block(Blocks.SAND);
        env.addPattern(new ProcessingPatternBuilder(x, b).addPreciseInput(1, a).build());
        env.addPattern(new ProcessingPatternBuilder(mult(a, aPerB)).addPreciseInput(1, b).build());
        return env;
    }

    /** T1:中性催化环（1B→1A),种子 1A,请求 X×5 → 两样板各 5 次,种子 1. */
    @Test
    public void testNeutralCatalyticCycle() {
        SimulationEnv env = this.catalyticEnv(1);
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack x = block(Blocks.SAND);
        env.addStoredItem(a);

        PlanView plan = PlanView.of(env.runSpecial(mult(x, 5)));

        assertThat(plan.simulation()).as("计划成功").isFalse();
        Map<IAEItemStack, Long> times = primaryMap(plan);
        assertThat(times.get(RecursiveCraftingHelper.canon(x))).isEqualTo(5);
        assertThat(times.get(RecursiveCraftingHelper.canon(a))).isEqualTo(5);
        assertThat(plan.usedItems().get(RecursiveCraftingHelper.canon(a))).isEqualTo(1); // 种子
    }

    /** T2:增殖催化环（1B→2A,环键还有净产）,同样可发射副产物求解. */
    @Test
    public void testProductiveCatalyticCycle() {
        SimulationEnv env = this.catalyticEnv(2);
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack x = block(Blocks.SAND);
        env.addStoredItem(a);

        PlanView plan = PlanView.of(env.runSpecial(mult(x, 5)));

        assertThat(plan.simulation()).as("计划成功").isFalse();
        Map<IAEItemStack, Long> times = primaryMap(plan);
        assertThat(times.get(RecursiveCraftingHelper.canon(x))).isEqualTo(5);
        assertThat(times.get(RecursiveCraftingHelper.canon(a))).isEqualTo(5);
    }

    /** T3:无种子 → 回落原生,与原生结果一致（缺料）. */
    @Test
    public void testNoSeedFallsBackToNative() {
        SimulationEnv env = this.catalyticEnv(1);
        IAEItemStack x = block(Blocks.SAND);

        PlanView specialPlan = PlanView.of(env.runSpecial(mult(x, 5)));
        PlanView nativePlan = PlanView.of(env.runNative(mult(x, 5)));

        assertThat(specialPlan.simulation()).isEqualTo(nativePlan.simulation());
        assertThat(primaryMap(specialPlan)).isEqualTo(primaryMap(nativePlan));
    }

    /** T4:detector 命中催化环请求（根路由前提）. */
    @Test
    public void testDetectorHitsCatalyticRequest() {
        SimulationEnv env = this.catalyticEnv(1);
        IAEItemStack x = block(Blocks.SAND);
        assertThat(SpecialRecipeDetector.mayInvolveSpecialRecipes(env.craftingGrid(), x, env.world()))
                .isTrue();
    }

    /** T5:深层 DAG——请求 E,E←X,X 由催化环供应;边界应落在 X（而非环键）. */
    @Test
    public void testDeepDagCatalyticBoundary() {
        SimulationEnv env = this.catalyticEnv(1);
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack x = block(Blocks.SAND);
        IAEItemStack e = block(Blocks.GRAVEL);
        env.addPattern(new ProcessingPatternBuilder(e).addPreciseInput(1, x).build());
        env.addStoredItem(a);

        PlanView plan = PlanView.of(env.runDag(mult(e, 5)));

        assertThat(plan.simulation()).as("计划成功").isFalse();
        Map<IAEItemStack, Long> times = primaryMap(plan);
        assertThat(times.get(RecursiveCraftingHelper.canon(e))).isEqualTo(5);
        assertThat(times.get(RecursiveCraftingHelper.canon(x))).isEqualTo(5);
        assertThat(times.get(RecursiveCraftingHelper.canon(a))).isEqualTo(5);
        assertThat(plan.usedItems().get(RecursiveCraftingHelper.canon(a))).isEqualTo(1);
    }
}
