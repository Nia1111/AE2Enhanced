package com.github.aeddddd.ae2enhanced.test.craftingplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanView;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.ProcessingPatternBuilder;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.SimulationEnv;

/**
 * Z 组:DAG 循环边界（深层自引用/循环链识别与委托求解）,1.12.2 移植.
 * <p>根请求本身无环（原生可解）,但中间节点落在自引用/环上样板——
 * 编译期收缩为 CYCLE 叶子,执行期委托 CycleBoundarySolver.</p>
 */
public class DagCycleBoundaryTest {

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

    /** Z1:深层自增殖——请求 D,D 需要 C,C 是自增殖样板（1C→2C). */
    @Test
    public void testDeepSelfDupBoundary() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack c = block(Blocks.COBBLESTONE);
        IAEItemStack d = block(Blocks.DIRT);
        env.addPattern(new ProcessingPatternBuilder(d).addPreciseInput(1, c).build());
        env.addPattern(new ProcessingPatternBuilder(mult(c, 2)).addPreciseInput(1, c).build());
        env.addStoredItem(c); // 种子 1

        PlanView plan = PlanView.of(env.runDag(mult(d, 10)));

        assertThat(plan.simulation()).as("计划成功").isFalse();
        Map<IAEItemStack, Long> times = primaryMap(plan);
        assertThat(times.get(RecursiveCraftingHelper.canon(d))).isEqualTo(10);
        assertThat(times.get(RecursiveCraftingHelper.canon(c))).isEqualTo(10); // dup 净产 1/次 ×10
        assertThat(plan.usedItems().get(RecursiveCraftingHelper.canon(c))).isEqualTo(1); // 种子
    }

    /** Z2:深层 θ 循环——请求 E,E←D←C,C 在 θ 环上（C→X,C→Y,X+Y→4C). */
    @Test
    public void testDeepThetaCycleBoundary() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack c = block(Blocks.STONE);
        IAEItemStack x = block(Blocks.COBBLESTONE);
        IAEItemStack y = block(Blocks.SAND);
        IAEItemStack d = block(Blocks.DIRT);
        IAEItemStack e = block(Blocks.GRAVEL);
        env.addPattern(new ProcessingPatternBuilder(e).addPreciseInput(1, d).build());
        env.addPattern(new ProcessingPatternBuilder(d).addPreciseInput(1, c).build());
        env.addPattern(new ProcessingPatternBuilder(x).addPreciseInput(1, c).build());
        env.addPattern(new ProcessingPatternBuilder(y).addPreciseInput(1, c).build());
        env.addPattern(new ProcessingPatternBuilder(mult(c, 4))
                .addPreciseInput(1, x)
                .addPreciseInput(1, y)
                .build());
        env.addStoredItem(mult(c, 8));
        env.addStoredItem(y);

        PlanView plan = PlanView.of(env.runDag(mult(e, 8)));

        assertThat(plan.simulation()).as("计划成功").isFalse();
        Map<IAEItemStack, Long> times = primaryMap(plan);
        assertThat(times.get(RecursiveCraftingHelper.canon(e))).isEqualTo(8);
        assertThat(times.get(RecursiveCraftingHelper.canon(d))).isEqualTo(8);
        // θ 环:净产 2/轮 ×4 轮,三样板各 4 次
        assertThat(times.get(RecursiveCraftingHelper.canon(c))).isEqualTo(4);
        assertThat(times.get(RecursiveCraftingHelper.canon(x))).isEqualTo(4);
        assertThat(times.get(RecursiveCraftingHelper.canon(y))).isEqualTo(4);
    }

    /** Z3:边界不可解（无种子）→ 整单回落原生,结果与原生一致. */
    @Test
    public void testBoundaryUnsolvableFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack c = block(Blocks.COBBLESTONE);
        IAEItemStack d = block(Blocks.DIRT);
        env.addPattern(new ProcessingPatternBuilder(d).addPreciseInput(1, c).build());
        env.addPattern(new ProcessingPatternBuilder(mult(c, 2)).addPreciseInput(1, c).build());
        // 无 C 库存（无种子）

        PlanView dagPlan = PlanView.of(env.runDag(mult(d, 4)));
        PlanView nativePlan = PlanView.of(env.runNative(mult(d, 4)));

        assertThat(dagPlan.simulation()).isEqualTo(nativePlan.simulation());
        assertThat(primaryMap(dagPlan)).isEqualTo(primaryMap(nativePlan));
    }

    /** Z4:共享边界——E 直接需要环上 key,同时经 D 间接需要（需求沿边累加进同一边界）. */
    @Test
    public void testSharedBoundaryDemandAccumulates() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack c = block(Blocks.COBBLESTONE);
        IAEItemStack d = block(Blocks.DIRT);
        IAEItemStack e = block(Blocks.GRAVEL);
        env.addPattern(new ProcessingPatternBuilder(e)
                .addPreciseInput(1, d)
                .addPreciseInput(1, c)
                .build());
        env.addPattern(new ProcessingPatternBuilder(d).addPreciseInput(1, c).build());
        env.addPattern(new ProcessingPatternBuilder(mult(c, 2)).addPreciseInput(1, c).build());
        env.addStoredItem(c); // 种子 1

        // E×4:D 需 4 个 C,E 直接需 4 个 C → 边界总需求 8
        PlanView plan = PlanView.of(env.runDag(mult(e, 4)));

        assertThat(plan.simulation()).as("计划成功").isFalse();
        Map<IAEItemStack, Long> times = primaryMap(plan);
        assertThat(times.get(RecursiveCraftingHelper.canon(e))).isEqualTo(4);
        assertThat(times.get(RecursiveCraftingHelper.canon(d))).isEqualTo(4);
        assertThat(times.get(RecursiveCraftingHelper.canon(c))).isEqualTo(8); // dup ×8 满足 4+4
        assertThat(plan.usedItems().get(RecursiveCraftingHelper.canon(c))).isEqualTo(1);
    }

    /**
     * Z5:自引用在根——边界 key == 请求 key,ignore(output) 语义下种子不可见,
     * 边界必然不可解 → 整单回落原生（与 1.20.1 P9 一致;游戏内此类请求由
     * 特殊配方路由先行接管,不走 DAG）.
     */
    @Test
    public void testSelfDupAtRootFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack c = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(mult(c, 2)).addPreciseInput(1, c).build());
        env.addStoredItem(mult(c, 5));

        PlanView dagPlan = PlanView.of(env.runDag(mult(c, 4)));
        PlanView nativePlan = PlanView.of(env.runNative(mult(c, 4)));

        assertThat(dagPlan.simulation()).isEqualTo(nativePlan.simulation());
        assertThat(primaryMap(dagPlan)).isEqualTo(primaryMap(nativePlan));
    }
}
