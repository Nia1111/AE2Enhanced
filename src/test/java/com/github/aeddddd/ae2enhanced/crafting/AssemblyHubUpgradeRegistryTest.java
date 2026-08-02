package com.github.aeddddd.ae2enhanced.crafting;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.github.aeddddd.ae2enhanced.item.ItemUpgradeCard;
import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link AssemblyHubUpgradeRegistry} 测试。
 *
 * <p>覆盖 UpgradeDefinition 构造规范化（强制 count=1、数组克隆）、
 * register/findFor 精确匹配语义、getCustomMaxStack、getParallelValue /
 * getSpeedValue 的取值与回退逻辑，以及 isParallelUpgrade / isSpeedUpgrade
 * 的原生卡与注册表两条判定路径。</p>
 *
 * <p>该注册表没有公开的移除/清空 API，{@link AfterEach} 通过反射从
 * DEFINITIONS 中删除本类注册的 key，避免污染其它测试。</p>
 */
public class AssemblyHubUpgradeRegistryTest {

    /** 本类注册占用的 key（item registryName#meta）。 */
    private static final String KEY_REDSTONE = "minecraft:redstone#0";
    private static final String KEY_GLOWSTONE = "minecraft:glowstone_dust#0";

    @BeforeAll
    public static void boot() {
        // 用例中构造 ItemStack，需无头引导
        AE2TestBootstrap.boot();
    }

    @AfterEach
    public void cleanup() throws ReflectiveOperationException {
        // 注册表无公开移除 API，反射清理本类注册的条目
        Field field = AssemblyHubUpgradeRegistry.class.getDeclaredField("DEFINITIONS");
        field.setAccessible(true);
        Map<?, ?> definitions = (Map<?, ?>) field.get(null);
        definitions.remove(KEY_REDSTONE);
        definitions.remove(KEY_GLOWSTONE);
    }

    private static AssemblyHubUpgradeRegistry.UpgradeDefinition parallelDef(ItemStack stack,
            int maxStack, long... values) {
        return new AssemblyHubUpgradeRegistry.UpgradeDefinition(stack,
                AssemblyHubUpgradeRegistry.UpgradeType.PARALLEL, maxStack, values);
    }

    private static AssemblyHubUpgradeRegistry.UpgradeDefinition speedDef(ItemStack stack,
            int maxStack, long... values) {
        return new AssemblyHubUpgradeRegistry.UpgradeDefinition(stack,
                AssemblyHubUpgradeRegistry.UpgradeType.SPEED, maxStack, values);
    }

    // ------------------------------------------------------------------
    // UpgradeDefinition 构造规范化
    // ------------------------------------------------------------------

    /** 构造时拷贝物品并强制 count=1，传入栈不受影响。 */
    @Test
    public void testDefinitionForcesCountOne() {
        ItemStack stack = new ItemStack(Items.REDSTONE, 16);
        AssemblyHubUpgradeRegistry.UpgradeDefinition def = parallelDef(stack, 4, 1L);

        assertThat(def.item.getCount()).isEqualTo(1);
        assertThat(def.item).isNotSameAs(stack);
        assertThat(stack.getCount()).isEqualTo(16);
    }

    /** values 数组被克隆：修改传入数组不影响定义。 */
    @Test
    public void testDefinitionClonesValues() {
        long[] values = { 2L, 4L };
        AssemblyHubUpgradeRegistry.UpgradeDefinition def = parallelDef(
                new ItemStack(Items.REDSTONE), 4, values);

        values[0] = 999L;

        assertThat(def.values).containsExactly(2L, 4L);
    }

    /** null values 归一化为空数组。 */
    @Test
    public void testDefinitionNullValuesBecomesEmpty() {
        AssemblyHubUpgradeRegistry.UpgradeDefinition def = new AssemblyHubUpgradeRegistry.UpgradeDefinition(
                new ItemStack(Items.REDSTONE), AssemblyHubUpgradeRegistry.UpgradeType.PARALLEL, 4, null);

        assertThat(def.values).isNotNull().isEmpty();
    }

    // ------------------------------------------------------------------
    // register / findFor
    // ------------------------------------------------------------------

    /** 注册后 findFor 精确命中（同物品同 meta）；不同 meta / 不同物品 miss。 */
    @Test
    public void testRegisterAndFindFor() {
        AssemblyHubUpgradeRegistry.UpgradeDefinition def = parallelDef(
                new ItemStack(Items.REDSTONE), 4, 2L);
        AssemblyHubUpgradeRegistry.register(def);

        assertThat(AssemblyHubUpgradeRegistry.findFor(new ItemStack(Items.REDSTONE, 64, 0))).isSameAs(def);
        assertThat(AssemblyHubUpgradeRegistry.findFor(new ItemStack(Items.GLOWSTONE_DUST, 1))).isNull();
    }

    /** findFor 对空栈返回 null。 */
    @Test
    public void testFindForEmptyStack() {
        assertThat(AssemblyHubUpgradeRegistry.findFor(ItemStack.EMPTY)).isNull();
    }

    /** 同 key 重复注册时后者覆盖前者（Map.put 语义）。 */
    @Test
    public void testDuplicateRegisterOverwrites() {
        AssemblyHubUpgradeRegistry.UpgradeDefinition first = parallelDef(
                new ItemStack(Items.REDSTONE), 4, 2L);
        AssemblyHubUpgradeRegistry.UpgradeDefinition second = speedDef(
                new ItemStack(Items.REDSTONE), 2, 10L);
        AssemblyHubUpgradeRegistry.register(first);
        AssemblyHubUpgradeRegistry.register(second);

        assertThat(AssemblyHubUpgradeRegistry.findFor(new ItemStack(Items.REDSTONE))).isSameAs(second);
    }

    // ------------------------------------------------------------------
    // getCustomMaxStack
    // ------------------------------------------------------------------

    /** 未注册的物品返回 -1（调用方回退默认逻辑）；已注册返回定义的 maxStack。 */
    @Test
    public void testGetCustomMaxStack() {
        assertThat(AssemblyHubUpgradeRegistry.getCustomMaxStack(new ItemStack(Items.REDSTONE))).isEqualTo(-1);

        AssemblyHubUpgradeRegistry.register(parallelDef(new ItemStack(Items.REDSTONE), 7, 2L));
        assertThat(AssemblyHubUpgradeRegistry.getCustomMaxStack(new ItemStack(Items.REDSTONE))).isEqualTo(7);
    }

    // ------------------------------------------------------------------
    // getParallelValue / getSpeedValue
    // ------------------------------------------------------------------

    /** getParallelValue：未注册或非 PARALLEL 类型返回 -1。 */
    @Test
    public void testGetParallelValueUnregisteredOrWrongType() {
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        assertThat(AssemblyHubUpgradeRegistry.getParallelValue(redstone, 1)).isEqualTo(-1);

        // 注册为 SPEED 类型时，查询并行值仍返回 -1
        AssemblyHubUpgradeRegistry.register(speedDef(redstone, 4, 10L));
        assertThat(AssemblyHubUpgradeRegistry.getParallelValue(redstone, 1)).isEqualTo(-1);
    }

    /** getParallelValue：count<=0 返回默认 64；values 为空数组时同样回落 64。 */
    @Test
    public void testGetParallelValueDefaultFallback() {
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        AssemblyHubUpgradeRegistry.register(parallelDef(redstone, 4, 2L, 4L));

        assertThat(AssemblyHubUpgradeRegistry.getParallelValue(redstone, 0)).isEqualTo(64L);
        assertThat(AssemblyHubUpgradeRegistry.getParallelValue(redstone, -3)).isEqualTo(64L);
    }

    /** getParallelValue：索引 0 对应 1 张卡；count 超出 values 长度时钳制到最后一个值。 */
    @Test
    public void testGetParallelValueIndexing() {
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        AssemblyHubUpgradeRegistry.register(parallelDef(redstone, 4, 2L, 8L, 32L));

        assertThat(AssemblyHubUpgradeRegistry.getParallelValue(redstone, 1)).isEqualTo(2L);
        assertThat(AssemblyHubUpgradeRegistry.getParallelValue(redstone, 3)).isEqualTo(32L);
        // 超出 values 长度：钳制到最后一个元素
        assertThat(AssemblyHubUpgradeRegistry.getParallelValue(redstone, 99)).isEqualTo(32L);
    }

    /** getSpeedValue：未注册或非 SPEED 类型返回 -1。 */
    @Test
    public void testGetSpeedValueUnregisteredOrWrongType() {
        ItemStack glowstone = new ItemStack(Items.GLOWSTONE_DUST);
        assertThat(AssemblyHubUpgradeRegistry.getSpeedValue(glowstone, 1)).isEqualTo(-1);

        AssemblyHubUpgradeRegistry.register(parallelDef(glowstone, 4, 2L));
        assertThat(AssemblyHubUpgradeRegistry.getSpeedValue(glowstone, 1)).isEqualTo(-1);
    }

    /** getSpeedValue：count<=0 返回默认 20。 */
    @Test
    public void testGetSpeedValueDefaultFallback() {
        ItemStack glowstone = new ItemStack(Items.GLOWSTONE_DUST);
        AssemblyHubUpgradeRegistry.register(speedDef(glowstone, 4, 10L));

        assertThat(AssemblyHubUpgradeRegistry.getSpeedValue(glowstone, 0)).isEqualTo(20);
        assertThat(AssemblyHubUpgradeRegistry.getSpeedValue(glowstone, -1)).isEqualTo(20);
    }

    /** getSpeedValue：按 count 索引取值，且结果下限为 1（max(val,1)）。 */
    @Test
    public void testGetSpeedValueIndexingAndClamp() {
        ItemStack glowstone = new ItemStack(Items.GLOWSTONE_DUST);
        AssemblyHubUpgradeRegistry.register(speedDef(glowstone, 4, 10L, 0L));

        assertThat(AssemblyHubUpgradeRegistry.getSpeedValue(glowstone, 1)).isEqualTo(10);
        // values[1] = 0，被钳制为 1
        assertThat(AssemblyHubUpgradeRegistry.getSpeedValue(glowstone, 2)).isEqualTo(1);
        // 超出 values 长度：钳制索引到最后一个元素（同样被钳为 1）
        assertThat(AssemblyHubUpgradeRegistry.getSpeedValue(glowstone, 99)).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // isParallelUpgrade / isSpeedUpgrade
    // ------------------------------------------------------------------

    /** 原生 ItemUpgradeCard：meta=PARALLEL/SPEED 时无需注册即判定为对应升级。 */
    @Test
    public void testNativeUpgradeCard() {
        ItemUpgradeCard card = new ItemUpgradeCard();

        assertThat(AssemblyHubUpgradeRegistry.isParallelUpgrade(
                new ItemStack(card, 1, ItemUpgradeCard.META_PARALLEL))).isTrue();
        assertThat(AssemblyHubUpgradeRegistry.isSpeedUpgrade(
                new ItemStack(card, 1, ItemUpgradeCard.META_SPEED))).isTrue();
        // 其它 meta 的原生卡两种判定均为 false
        assertThat(AssemblyHubUpgradeRegistry.isParallelUpgrade(
                new ItemStack(card, 1, ItemUpgradeCard.META_EFFICIENCY))).isFalse();
        assertThat(AssemblyHubUpgradeRegistry.isSpeedUpgrade(
                new ItemStack(card, 1, ItemUpgradeCard.META_EFFICIENCY))).isFalse();
    }

    /** 注册表路径：非原生卡物品按注册类型判定。 */
    @Test
    public void testRegistryPathTypeChecks() {
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack glowstone = new ItemStack(Items.GLOWSTONE_DUST);
        AssemblyHubUpgradeRegistry.register(parallelDef(redstone, 4, 2L));
        AssemblyHubUpgradeRegistry.register(speedDef(glowstone, 4, 10L));

        assertThat(AssemblyHubUpgradeRegistry.isParallelUpgrade(redstone)).isTrue();
        assertThat(AssemblyHubUpgradeRegistry.isSpeedUpgrade(redstone)).isFalse();
        assertThat(AssemblyHubUpgradeRegistry.isSpeedUpgrade(glowstone)).isTrue();
        assertThat(AssemblyHubUpgradeRegistry.isParallelUpgrade(glowstone)).isFalse();
    }

    /** 未注册且非原生卡的物品两种判定均为 false。 */
    @Test
    public void testUnregisteredItemNotUpgrade() {
        assertThat(AssemblyHubUpgradeRegistry.isParallelUpgrade(new ItemStack(Items.APPLE))).isFalse();
        assertThat(AssemblyHubUpgradeRegistry.isSpeedUpgrade(new ItemStack(Items.APPLE))).isFalse();
    }
}
