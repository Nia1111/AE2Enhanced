package com.github.aeddddd.ae2enhanced.crafting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link SingularityRecipeRegistry} 静态注册表测试。
 *
 * <p>注册表为静态状态且无公开 clear 方法，本类注册的配方 id 统一以
 * {@code "sreg_test:"} 为前缀，并在 {@link AfterEach} 中通过 removeById
 * 清理，同时 drain 延迟移除队列，避免污染其它测试类。</p>
 *
 * <p>注意：findMatching(World, BlockPos, ItemStack) 依赖 World 扫描实体，
 * 无头环境无法构造 World，故命中/未命中语义以 getRecipes 成员关系间接验证。</p>
 */
public class SingularityRecipeRegistryTest {

    private static final String ID_A = "sreg_test:a";
    private static final String ID_B = "sreg_test:b";

    @AfterEach
    public void cleanup() {
        // 清理本类注册的条目与可能残留的延迟移除队列
        SingularityRecipeRegistry.removeById(ID_A);
        SingularityRecipeRegistry.removeById(ID_B);
        SingularityRecipeRegistry.applyPendingRemovals();
    }

    /** 构造无输入的测试配方（构造不依赖 World/ItemStack 引导）。 */
    private static SingularityRecipe recipe(String id) {
        return new SingularityRecipe(id, Collections.emptyList(), null, null, 6000);
    }

    private static boolean contains(String id) {
        return SingularityRecipeRegistry.getRecipes().stream().anyMatch(r -> r.getId().equals(id));
    }

    private static long countOf(String id) {
        return SingularityRecipeRegistry.getRecipes().stream().filter(r -> r.getId().equals(id)).count();
    }

    // ------------------------------------------------------------------
    // register / getRecipes
    // ------------------------------------------------------------------

    /** 注册后 getRecipes 可查到该配方。 */
    @Test
    public void testRegisterAddsRecipe() {
        SingularityRecipe recipe = recipe(ID_A);
        SingularityRecipeRegistry.register(recipe);

        assertThat(contains(ID_A)).isTrue();
        assertThat(SingularityRecipeRegistry.getRecipes()).contains(recipe);
    }

    /** 重复注册（同 id）不去重：两条都保留。 */
    @Test
    public void testDuplicateRegisterKeepsBoth() {
        SingularityRecipeRegistry.register(recipe(ID_A));
        SingularityRecipeRegistry.register(recipe(ID_A));

        assertThat(countOf(ID_A)).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // removeById
    // ------------------------------------------------------------------

    /** removeById 立即移除，getRecipes 不再包含。 */
    @Test
    public void testRemoveByIdRemovesImmediately() {
        SingularityRecipeRegistry.register(recipe(ID_A));

        assertThat(SingularityRecipeRegistry.removeById(ID_A)).isTrue();
        assertThat(contains(ID_A)).isFalse();
    }

    /** removeById 会移除所有同 id 的配方（removeIf 语义）。 */
    @Test
    public void testRemoveByIdRemovesAllDuplicates() {
        SingularityRecipeRegistry.register(recipe(ID_A));
        SingularityRecipeRegistry.register(recipe(ID_A));

        assertThat(SingularityRecipeRegistry.removeById(ID_A)).isTrue();
        assertThat(countOf(ID_A)).isZero();
    }

    /** 对不存在的 id 调用 removeById 返回 false，幂等无副作用。 */
    @Test
    public void testRemoveByIdNonExistentIsIdempotent() {
        assertThat(SingularityRecipeRegistry.removeById("sreg_test:nonexistent")).isFalse();
        assertThat(SingularityRecipeRegistry.removeById("sreg_test:nonexistent")).isFalse();
    }

    // ------------------------------------------------------------------
    // queueRemoval / applyPendingRemovals
    // ------------------------------------------------------------------

    /** queueRemoval 后未 apply 前配方仍在注册表中。 */
    @Test
    public void testQueuedRemovalNotAppliedYet() {
        SingularityRecipeRegistry.register(recipe(ID_A));
        SingularityRecipeRegistry.queueRemoval(ID_A);

        assertThat(contains(ID_A)).isTrue();
    }

    /** applyPendingRemovals 执行后配方被移除。 */
    @Test
    public void testApplyPendingRemovalsRemoves() {
        SingularityRecipeRegistry.register(recipe(ID_A));
        SingularityRecipeRegistry.queueRemoval(ID_A);
        SingularityRecipeRegistry.applyPendingRemovals();

        assertThat(contains(ID_A)).isFalse();
    }

    /** apply 后队列被清空：再次注册同 id 配方不会被误删，重复 apply 无副作用。 */
    @Test
    public void testApplyClearsQueue() {
        SingularityRecipeRegistry.register(recipe(ID_A));
        SingularityRecipeRegistry.queueRemoval(ID_A);
        SingularityRecipeRegistry.applyPendingRemovals();

        // 队列已清空，重复 apply 不影响后续注册的同 id 配方
        SingularityRecipeRegistry.register(recipe(ID_A));
        SingularityRecipeRegistry.applyPendingRemovals();
        assertThat(contains(ID_A)).isTrue();
    }

    /** 对未注册的 id queueRemoval 后 apply 不抛异常（移除尚未注册的配方场景）。 */
    @Test
    public void testQueueRemovalForUnregisteredId() {
        SingularityRecipeRegistry.queueRemoval(ID_B);
        SingularityRecipeRegistry.applyPendingRemovals();
        // 无异常即通过；队列已被清空
        SingularityRecipeRegistry.register(recipe(ID_B));
        assertThat(contains(ID_B)).isTrue();
    }

    /** getRecipes 返回快照副本，修改返回值不影响注册表。 */
    @Test
    public void testGetRecipesReturnsSnapshot() {
        SingularityRecipeRegistry.register(recipe(ID_A));

        SingularityRecipeRegistry.getRecipes().clear();

        assertThat(contains(ID_A)).isTrue();
    }
}
