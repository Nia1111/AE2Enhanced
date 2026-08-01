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
 * 容器物合成的 DAG 批量处理（parity 对齐）,1.12.2 移植.
 * <p>原生对容器样板逐次循环并逐次回记容器（CraftingTreeProcess 容器路径）;
 * DAG 批量记账（消耗 N、回记 N-1 容器,首个循环不预贷）,数学等价.
 * 1.12.2 的容器语义直接来自 {@code Item.hasContainerItem/getContainerItem},
 * 测试使用真实物品（水桶/奶桶 → 空桶）;1.20.1 的蜂蜜瓶场景以水/奶桶替代.</p>
 */
public class DagContainerTest {

    private static IAEItemStack item(Item i) {
        return AEItemStack.fromItemStack(new ItemStack(i));
    }

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

    private static void assertParity(SimulationEnv env, IAEItemStack request) {
        CraftingJob nativeJob = env.runNative(request);
        CraftingJob dagJob = env.runDag(request);
        PlanView nativePlan = PlanView.of(nativeJob);
        PlanView dagPlan = PlanView.of(dagJob);
        assertThat(primaryMap(dagPlan)).as("patternTimes").isEqualTo(primaryMap(nativePlan));
        assertThat(dagPlan.usedItems()).as("usedItems").isEqualTo(nativePlan.usedItems());
        assertThat(dagPlan.missingItems()).as("missingItems").isEqualTo(nativePlan.missingItems());
        assertThat(dagPlan.simulation()).as("simulation").isEqualTo(nativePlan.simulation());
    }

    /** 自返还催化剂:容器物即自身(原版无此物品,测试自定义,对应 1.20.1 的
     * ContainerPatternBuilder.withContainer(pattern, catalyst, catalyst)). */
    private static final Item SELF_RETURNING_CATALYST = new Item() {
        @Override
        public boolean hasContainerItem(ItemStack stack) {
            return true;
        }

        @Override
        public ItemStack getContainerItem(ItemStack stack) {
            return stack.copy();
        }
    };

    /** C1:水桶式（4 水桶 → 1 黏液块,返还 4 空桶）大单——批量与原生逐次循环全等. */
    @Test
    public void testWaterBucketBulkParity() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack waterBucket = item(Items.WATER_BUCKET);
        IAEItemStack block = block(Blocks.SLIME_BLOCK);
        env.addPattern(new ProcessingPatternBuilder(block).addPreciseInput(4, waterBucket).build());
        env.addStoredItem(mult(waterBucket, 64));

        assertParity(env, mult(block, 16));
    }

    /** C2:桶式复用——容器（空桶）同时是下游输入,拓扑序先回记后提取;
     * 空桶零网络来源且全靠返还自举,两侧均记 missing=1（原生高水位）,判不可提交. */
    @Test
    public void testContainerReusedDownstreamParity() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack milkBucket = item(Items.MILK_BUCKET);
        IAEItemStack bucket = item(Items.BUCKET);
        IAEItemStack milk = block(Blocks.DIRT); // 以泥土代牛奶(测试物品)
        IAEItemStack cake = item(Items.CAKE);
        // 1 奶桶 → 1 蛋糕,返还 1 空桶;1 空桶 + 1 奶 → 1 奶桶
        env.addPattern(new ProcessingPatternBuilder(cake).addPreciseInput(1, milkBucket).build());
        env.addPattern(new ProcessingPatternBuilder(milkBucket)
                .addPreciseInput(1, bucket)
                .addPreciseInput(1, milk)
                .build());
        env.addStoredItem(mult(milkBucket, 8));
        env.addStoredItem(mult(milk, 64));

        assertParity(env, mult(cake, 10));
    }

    /** C3:深层容器——容器样板嵌在更大订单中间层,不回落、与原生一致. */
    @Test
    public void testDeepContainerInLargerOrderParity() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack waterBucket = item(Items.WATER_BUCKET);
        IAEItemStack block = block(Blocks.SLIME_BLOCK);
        IAEItemStack target = block(Blocks.CHEST);
        env.addPattern(new ProcessingPatternBuilder(target).addPreciseInput(2, block).build());
        env.addPattern(new ProcessingPatternBuilder(block).addPreciseInput(4, waterBucket).build());
        env.addStoredItem(mult(waterBucket, 128));

        assertParity(env, mult(target, 8));
    }

    /** C5:催化剂链(4 低 + 催化 → 1 高,催化自返还)——种子必须计入初始提取.
     * 回归(对应 1.20.1 的 testCatalystChainSeedExtraction):全额预贷会把
     * usedItems(催化)抹成 0,CPU 不提取种子,执行卡死.
     * <p>已知分歧(仅本场景):1.12.2 原生的 missing 分支也回记容器
     * (CraftingTreeNode line 203),形成 ping-pong——1 种子够用的链被原生报成
     * missing=4 不可提交;1.20.1 原生与两侧 DAG 均为 used(种子)=1 可提交.
     * DAG 取正确行为,不与原生逐项比对.</p> */
    @Test
    public void testCatalystChainSeedExtraction() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack low = block(Blocks.STONE);
        IAEItemStack high = block(Blocks.COBBLESTONE);
        IAEItemStack catalyst = item(SELF_RETURNING_CATALYST);
        // 4 低 + 1 催化 → 1 高,催化剂以容器物自返还
        env.addPattern(new ProcessingPatternBuilder(high)
                .addPreciseInput(4, low)
                .addPreciseInput(1, catalyst)
                .build());
        env.addStoredItem(mult(low, 64));
        env.addStoredItem(catalyst); // 种子 1

        PlanView dagPlan = PlanView.of(env.runDag(mult(high, 8)));

        assertThat(dagPlan.simulation()).as("DAG 计划可提交").isFalse();
        assertThat(dagPlan.missingItems()).as("DAG 无缺料").isEmpty();
        // 种子必须出现在初始提取中(1.20.1 原生逐次循环高水位同为 1)
        assertThat(dagPlan.usedItems().get(RecursiveCraftingHelper.canon(catalyst)))
                .as("usedItems 含催化种子").isEqualTo(1L);
        // 记录原生分歧(ping-pong bug):不可提交且缺料非空
        PlanView nativePlan = PlanView.of(env.runNative(mult(high, 8)));
        assertThat(nativePlan.simulation()).as("原生 ping-pong 判为不可提交").isTrue();
        assertThat(nativePlan.missingItems()).as("原生 ping-pong 缺料").isNotEmpty();
    }

    /**
     * C4:库存不足时判为不可提交且有缺料记录.
     * <p>已知分歧（同 1.20.1,仅不可提交计划）:原生在首个失败迭代即停,调用次数与
     * 缺料量只记一轮;DAG 按需求全额上报——不可提交计划的数字无执行语义,
     * 故只断言不可提交判定与缺料非空.</p>
     */
    @Test
    public void testContainerMissingParity() {
        SimulationEnv env = new SimulationEnv();
        IAEItemStack waterBucket = item(Items.WATER_BUCKET);
        IAEItemStack block = block(Blocks.SLIME_BLOCK);
        env.addPattern(new ProcessingPatternBuilder(block).addPreciseInput(4, waterBucket).build());
        env.addStoredItem(mult(waterBucket, 4));

        PlanView nativePlan = PlanView.of(env.runNative(mult(block, 8)));
        PlanView dagPlan = PlanView.of(env.runDag(mult(block, 8)));

        assertThat(dagPlan.simulation()).as("DAG 判为不可提交").isTrue();
        assertThat(nativePlan.simulation()).as("原生判为不可提交").isTrue();
        assertThat(dagPlan.missingItems()).as("DAG 有缺料记录").isNotEmpty();
    }
}
