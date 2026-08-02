package com.github.aeddddd.ae2enhanced.crafting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link SingularityFuelRegistry} 静态注册表测试。
 *
 * <p>注册表为静态状态且无公开 clear 方法，本类注册的配方 id 统一以
 * {@code "sfreg_test:"} 为前缀，并在 {@link AfterEach} 中通过 removeById
 * 清理，同时 drain 延迟移除队列，避免污染其它测试类。</p>
 */
public class SingularityFuelRegistryTest {

    private static final String ID_A = "sfreg_test:a";
    private static final String ID_B = "sfreg_test:b";

    @BeforeAll
    public static void boot() {
        // 构造燃料 ItemStack 需无头引导
        AE2TestBootstrap.boot();
    }

    @AfterEach
    public void cleanup() {
        // 清理本类注册的条目与可能残留的延迟移除队列
        SingularityFuelRegistry.removeById(ID_A);
        SingularityFuelRegistry.removeById(ID_B);
        SingularityFuelRegistry.applyPendingRemovals();
    }

    private static SingularityFuelRecipe coalFuel(String id) {
        return new SingularityFuelRecipe(id, new ItemStack(Items.COAL, 1, 0), 100, false);
    }

    private static boolean contains(String id) {
        return SingularityFuelRegistry.getRecipes().stream().anyMatch(r -> r.getId().equals(id));
    }

    // ------------------------------------------------------------------
    // register / findFor
    // ------------------------------------------------------------------

    /** 注册后 findFor 可按物品命中；不匹配的物品返回 null。 */
    @Test
    public void testRegisterAndFindFor() {
        SingularityFuelRecipe recipe = coalFuel(ID_A);
        SingularityFuelRegistry.register(recipe);

        assertThat(SingularityFuelRegistry.findFor(new ItemStack(Items.COAL, 1, 0))).isSameAs(recipe);
        assertThat(SingularityFuelRegistry.findFor(new ItemStack(Items.APPLE, 1))).isNull();
    }

    /** findFor 对空栈直接返回 null（不遍历注册表）。 */
    @Test
    public void testFindForEmptyStackReturnsNull() {
        SingularityFuelRegistry.register(coalFuel(ID_A));

        assertThat(SingularityFuelRegistry.findFor(ItemStack.EMPTY)).isNull();
    }

    /** 重复注册（同 id）不去重：两条都保留，findFor 返回先注册的一条。 */
    @Test
    public void testDuplicateRegisterKeepsBoth() {
        SingularityFuelRecipe first = coalFuel(ID_A);
        SingularityFuelRecipe second = coalFuel(ID_A);
        SingularityFuelRegistry.register(first);
        SingularityFuelRegistry.register(second);

        assertThat(SingularityFuelRegistry.findFor(new ItemStack(Items.COAL, 1, 0))).isSameAs(first);
        long count = SingularityFuelRegistry.getRecipes().stream()
                .filter(r -> r.getId().equals(ID_A)).count();
        assertThat(count).isEqualTo(2);
    }

    /** 多个配方时按注册顺序返回第一个匹配者。 */
    @Test
    public void testFindForReturnsFirstInRegistrationOrder() {
        SingularityFuelRecipe first = coalFuel(ID_A);
        SingularityFuelRecipe second = coalFuel(ID_B);
        SingularityFuelRegistry.register(first);
        SingularityFuelRegistry.register(second);

        assertThat(SingularityFuelRegistry.findFor(new ItemStack(Items.COAL, 1, 0))).isSameAs(first);
    }

    // ------------------------------------------------------------------
    // removeById
    // ------------------------------------------------------------------

    /** removeById 立即移除，移除后 findFor 不再命中。 */
    @Test
    public void testRemoveByIdRemovesImmediately() {
        SingularityFuelRegistry.register(coalFuel(ID_A));

        assertThat(SingularityFuelRegistry.removeById(ID_A)).isTrue();
        assertThat(SingularityFuelRegistry.findFor(new ItemStack(Items.COAL, 1, 0))).isNull();
    }

    /** 对不存在的 id 调用 removeById 返回 false，幂等无副作用。 */
    @Test
    public void testRemoveByIdNonExistentIsIdempotent() {
        assertThat(SingularityFuelRegistry.removeById("sfreg_test:nonexistent")).isFalse();
        assertThat(SingularityFuelRegistry.removeById("sfreg_test:nonexistent")).isFalse();
    }

    // ------------------------------------------------------------------
    // queueRemoval / applyPendingRemovals
    // ------------------------------------------------------------------

    /** queueRemoval 后未 apply 前配方仍可命中。 */
    @Test
    public void testQueuedRemovalNotAppliedYet() {
        SingularityFuelRegistry.register(coalFuel(ID_A));
        SingularityFuelRegistry.queueRemoval(ID_A);

        assertThat(SingularityFuelRegistry.findFor(new ItemStack(Items.COAL, 1, 0))).isNotNull();
    }

    /** applyPendingRemovals 执行后配方被移除。 */
    @Test
    public void testApplyPendingRemovalsRemoves() {
        SingularityFuelRegistry.register(coalFuel(ID_A));
        SingularityFuelRegistry.queueRemoval(ID_A);
        SingularityFuelRegistry.applyPendingRemovals();

        assertThat(SingularityFuelRegistry.findFor(new ItemStack(Items.COAL, 1, 0))).isNull();
    }

    /** apply 后队列被清空：再次注册同 id 配方不会被误删，重复 apply 无副作用。 */
    @Test
    public void testApplyClearsQueue() {
        SingularityFuelRegistry.register(coalFuel(ID_A));
        SingularityFuelRegistry.queueRemoval(ID_A);
        SingularityFuelRegistry.applyPendingRemovals();

        // 队列已清空，重复 apply 不影响后续注册的同 id 配方
        SingularityFuelRegistry.register(coalFuel(ID_A));
        SingularityFuelRegistry.applyPendingRemovals();
        assertThat(SingularityFuelRegistry.findFor(new ItemStack(Items.COAL, 1, 0))).isNotNull();
    }

    /** 对未注册的 id queueRemoval 后 apply 不抛异常（移除尚未注册的配方场景）。 */
    @Test
    public void testQueueRemovalForUnregisteredId() {
        SingularityFuelRegistry.queueRemoval(ID_B);
        SingularityFuelRegistry.applyPendingRemovals();
        // 无异常即通过；队列已被清空
        SingularityFuelRegistry.register(coalFuel(ID_B));
        assertThat(contains(ID_B)).isTrue();
    }

    /** getRecipes 返回快照副本，修改返回值不影响注册表。 */
    @Test
    public void testGetRecipesReturnsSnapshot() {
        SingularityFuelRegistry.register(coalFuel(ID_A));

        SingularityFuelRegistry.getRecipes().clear();

        assertThat(contains(ID_A)).isTrue();
    }
}
