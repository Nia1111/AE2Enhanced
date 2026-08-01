package com.github.aeddddd.ae2enhanced.test.craftingplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingJob;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanView;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.ProcessingPatternBuilder;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.SimulationEnv;

/**
 * DAG 引擎 parity 对齐测试（1.12.2 移植）:同一环境下,DAG 计划与原生计划的关键字段逐项相等.
 * <p>比对字段:patternTimes(按主产出键)、usedItems、missingItems、simulation;
 * bytes 为近似记账(CPU 选择用),不在比对范围;emittedItems 因 v1 对发射台节点
 * 整单回落原生(行为与现状一致)不在比对范围.</p>
 */
public class DagParityTest {

    private static IAEItemStack block(net.minecraft.block.Block b) {
        return AEItemStack.fromItemStack(new ItemStack(b));
    }

    private static IAEItemStack item(Item i) {
        return AEItemStack.fromItemStack(new ItemStack(i));
    }

    private static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(template.getStackSize() * multiplier);
        return copy;
    }

    /** P1:简单两步链 ×N(部分中间库存不足 → 缺料也须一致). */
    @Test
    public void testSimpleChain() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack stick = item(Items.STICK);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addPattern(new ProcessingPatternBuilder(mult(stick, 2)).addPreciseInput(1, cobble).build());
        env.addStoredItem(mult(stone, 3));

        assertParity(env, mult(stick, 8));
    }

    /** P2:共享中间物(D 需 B+C,B、C 各自需 A)——节点合并语义. */
    @Test
    public void testSharedIntermediate() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = block(Blocks.STONE);
        IAEItemStack b = block(Blocks.COBBLESTONE);
        IAEItemStack c = block(Blocks.SAND);
        IAEItemStack d = block(Blocks.DIRT);
        env.addPattern(new ProcessingPatternBuilder(b).addPreciseInput(1, a).build());
        env.addPattern(new ProcessingPatternBuilder(c).addPreciseInput(1, a).build());
        env.addPattern(new ProcessingPatternBuilder(d).addPreciseInput(1, b).addPreciseInput(1, c).build());
        env.addStoredItem(mult(a, 10));

        assertParity(env, mult(d, 4));
    }

    /** P3:中间物库存部分抵扣. */
    @Test
    public void testIntermediateStockCredit() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addStoredItem(mult(cobble, 3));
        env.addStoredItem(mult(stone, 64));

        assertParity(env, mult(cobble, 5));
    }

    /**
     * P4:发射台提供终端输入.
     * <p>1.12.2 v1 对发射台节点整单回落原生(与现状一致,无回归);
     * 回落后 DAG job 即原生计划,parity 按构造成立——本例同时守卫回落路径可用.</p>
     */
    @Test
    public void testEmitterInputFallsBack() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addEmitable(stone);

        assertParity(env, mult(cobble, 7));
    }

    /** P5:缺料报告——无样板终端. */
    @Test
    public void testMissingTerminal() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        IAEItemStack diamond = item(Items.DIAMOND);
        env.addPattern(new ProcessingPatternBuilder(cobble)
                .addPreciseInput(1, stone)
                .addPreciseInput(1, diamond)
                .build());
        env.addStoredItem(mult(stone, 64));

        assertParity(env, mult(cobble, 4));
    }

    /** P6:余量回插(每份产 4,请求 5 → 2 次,余 3). */
    @Test
    public void testSurplusReinsert() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack stick = item(Items.STICK);
        env.addPattern(new ProcessingPatternBuilder(mult(stick, 4)).addPreciseInput(2, stone).build());
        env.addStoredItem(mult(stone, 64));

        assertParity(env, mult(stick, 5));
    }

    /** P7:请求物自身库存不参与扣除(镜像原生 ignore(output)). */
    @Test
    public void testRequestedStockIgnored() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack stone = block(Blocks.STONE);
        IAEItemStack cobble = block(Blocks.COBBLESTONE);
        env.addPattern(new ProcessingPatternBuilder(cobble).addPreciseInput(1, stone).build());
        env.addStoredItem(mult(cobble, 500)); // 请求物库存:不参与计划
        env.addStoredItem(mult(stone, 64));

        assertParity(env, mult(cobble, 10));
    }

    /** P8:深层长链大单(9 层 ×100_000,无递归爆炸). */
    @Test
    public void testDeepChainLargeAmount() {
        SimulationEnv env = new SimulationEnv();
        net.minecraft.block.Block[] chain = { Blocks.STONE, Blocks.COBBLESTONE, Blocks.DIRT, Blocks.SAND,
                Blocks.GRAVEL, Blocks.CLAY, Blocks.BRICK_BLOCK, Blocks.NETHERRACK, Blocks.SOUL_SAND };
        for (int i = 0; i < chain.length - 1; i++) {
            IAEItemStack out = block(chain[i + 1]);
            IAEItemStack in = block(chain[i]);
            env.addPattern(new ProcessingPatternBuilder(out).addPreciseInput(1, in).build());
        }
        env.addStoredItem(mult(block(chain[0]), 100_000));

        assertParity(env, mult(block(chain[chain.length - 1]), 100_000));
    }

    // ===== 比对工具 =====

    private static void assertParity(SimulationEnv env, IAEItemStack request) {
        CraftingJob nativeJob = env.runNative(request);
        CraftingJob dagJob = env.runDag(request);
        PlanView nativePlan = PlanView.of(nativeJob);
        PlanView dagPlan = PlanView.of(dagJob);

        assertThat(dagPlan.simulation()).as("simulation").isEqualTo(nativePlan.simulation());
        assertThat(primaryMap(dagPlan)).as("patternTimes").isEqualTo(primaryMap(nativePlan));
        assertThat(dagPlan.usedItems()).as("usedItems").isEqualTo(nativePlan.usedItems());
        assertThat(dagPlan.missingItems()).as("missingItems").isEqualTo(nativePlan.missingItems());
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
}
