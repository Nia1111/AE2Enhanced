package com.github.aeddddd.ae2enhanced.crafting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link BlackHoleRecipeRegistry} 静态注册表测试。
 *
 * <p>注册表为静态状态且无公开 clear 方法，本类注册的配方 id 统一以
 * {@code "bhreg_test:"} 为前缀，并在 {@link AfterEach} 中通过 removeById
 * 清理，同时 drain 延迟移除队列，避免污染其它测试类。</p>
 */
public class BlackHoleRecipeRegistryTest {

    private static final String ID_A = "bhreg_test:a";
    private static final String ID_B = "bhreg_test:b";

    @BeforeAll
    public static void boot() {
        // 构造配方输出 ItemStack 需无头引导
        AE2TestBootstrap.boot();
    }

    @AfterEach
    public void cleanup() {
        // 清理本类注册的条目与可能残留的延迟移除队列
        BlackHoleRecipeRegistry.removeById(ID_A);
        BlackHoleRecipeRegistry.removeById(ID_B);
        BlackHoleRecipeRegistry.applyPendingRemovals();
    }

    /** 构造以 {"bhreg_k":count} 为输入的测试配方。 */
    private static BlackHoleRecipe recipe(String id, String key, int count) {
        Map<String, Integer> inputs = new HashMap<>();
        inputs.put(key, count);
        return new BlackHoleRecipe(id, inputs, new ItemStack(Items.APPLE, 1));
    }

    private static Map<String, Integer> found(String key, int count) {
        Map<String, Integer> found = new HashMap<>();
        found.put(key, count);
        return found;
    }

    // ------------------------------------------------------------------
    // register / findMatching
    // ------------------------------------------------------------------

    /** 注册后 findMatching 可命中，找不到时返回 null。 */
    @Test
    public void testRegisterAndFindMatching() {
        BlackHoleRecipe recipe = recipe(ID_A, "k", 2);
        BlackHoleRecipeRegistry.register(recipe);

        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 2))).isSameAs(recipe);
        // 数量不足 / 无配方匹配时返回 null
        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isNull();
        assertThat(BlackHoleRecipeRegistry.findMatching(found("other", 99))).isNull();
    }

    /** 重复注册（同 id）后者覆盖前者：仅保留一条，findMatching 返回后注册的一条。 */
    @Test
    public void testDuplicateRegisterOverwrites() {
        BlackHoleRecipe first = recipe(ID_A, "k", 1);
        BlackHoleRecipe second = recipe(ID_A, "k", 2);
        BlackHoleRecipeRegistry.register(first);
        BlackHoleRecipeRegistry.register(second);

        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 2))).isSameAs(second);
        // 前者已被覆盖移除：仅满足前者输入的 found 不再命中
        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isNull();
        long count = BlackHoleRecipeRegistry.getRecipes().stream()
                .filter(r -> r.getId().equals(ID_A)).count();
        assertThat(count).isEqualTo(1);
    }

    /** 多个配方时按注册顺序返回第一个匹配者。 */
    @Test
    public void testFindMatchingReturnsFirstInRegistrationOrder() {
        BlackHoleRecipe first = recipe(ID_A, "k", 1);
        BlackHoleRecipe second = recipe(ID_B, "k", 1);
        BlackHoleRecipeRegistry.register(first);
        BlackHoleRecipeRegistry.register(second);

        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isSameAs(first);
    }

    // ------------------------------------------------------------------
    // removeById
    // ------------------------------------------------------------------

    /** removeById 立即移除，移除后 findMatching 不再命中。 */
    @Test
    public void testRemoveByIdRemovesImmediately() {
        BlackHoleRecipeRegistry.register(recipe(ID_A, "k", 1));

        assertThat(BlackHoleRecipeRegistry.removeById(ID_A)).isTrue();
        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isNull();
    }

    /** removeById 移除同 id 配方（覆盖语义下同 id 至多一条）。 */
    @Test
    public void testRemoveByIdRemovesAllDuplicates() {
        BlackHoleRecipeRegistry.register(recipe(ID_A, "k", 1));
        BlackHoleRecipeRegistry.register(recipe(ID_A, "k", 1));

        assertThat(BlackHoleRecipeRegistry.removeById(ID_A)).isTrue();
        assertThat(BlackHoleRecipeRegistry.getRecipes())
                .noneMatch(r -> r.getId().equals(ID_A));
    }

    /** 对不存在的 id 调用 removeById 返回 false，幂等无副作用。 */
    @Test
    public void testRemoveByIdNonExistentIsIdempotent() {
        assertThat(BlackHoleRecipeRegistry.removeById("bhreg_test:nonexistent")).isFalse();
        assertThat(BlackHoleRecipeRegistry.removeById("bhreg_test:nonexistent")).isFalse();
    }

    // ------------------------------------------------------------------
    // queueRemoval / applyPendingRemovals
    // ------------------------------------------------------------------

    /** queueRemoval 后未 apply 前配方仍可命中。 */
    @Test
    public void testQueuedRemovalNotAppliedYet() {
        BlackHoleRecipeRegistry.register(recipe(ID_A, "k", 1));
        BlackHoleRecipeRegistry.queueRemoval(ID_A);

        // 延迟移除：apply 前仍然有效
        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isNotNull();
    }

    /** applyPendingRemovals 执行后配方被移除。 */
    @Test
    public void testApplyPendingRemovalsRemoves() {
        BlackHoleRecipeRegistry.register(recipe(ID_A, "k", 1));
        BlackHoleRecipeRegistry.queueRemoval(ID_A);
        BlackHoleRecipeRegistry.applyPendingRemovals();

        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isNull();
    }

    /** apply 后队列被清空：再次注册同 id 配方不会被误删，重复 apply 无副作用。 */
    @Test
    public void testApplyClearsQueue() {
        BlackHoleRecipeRegistry.register(recipe(ID_A, "k", 1));
        BlackHoleRecipeRegistry.queueRemoval(ID_A);
        BlackHoleRecipeRegistry.applyPendingRemovals();

        // 队列已清空，重复 apply 不影响后续注册的同 id 配方
        BlackHoleRecipeRegistry.register(recipe(ID_A, "k", 1));
        BlackHoleRecipeRegistry.applyPendingRemovals();
        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isNotNull();
    }

    /** 对未注册的 id queueRemoval 后 apply 不抛异常（移除尚未注册的配方场景）。 */
    @Test
    public void testQueueRemovalForUnregisteredId() {
        BlackHoleRecipeRegistry.queueRemoval(ID_B);
        BlackHoleRecipeRegistry.applyPendingRemovals();
        // 无异常即通过；队列已被清空
        BlackHoleRecipeRegistry.register(recipe(ID_B, "k", 1));
        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isNotNull();
    }

    /** getRecipes 返回快照副本，修改返回值不影响注册表。 */
    @Test
    public void testGetRecipesReturnsSnapshot() {
        BlackHoleRecipeRegistry.register(recipe(ID_A, "k", 1));

        BlackHoleRecipeRegistry.getRecipes().clear();

        assertThat(BlackHoleRecipeRegistry.findMatching(found("k", 1))).isNotNull();
    }
}
