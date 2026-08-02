package com.github.aeddddd.ae2enhanced.crafting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link SingularityRecipe} 构造语义测试。
 *
 * <p>仅覆盖构造函数的 null 归一化与 lifetimeTicks 默认值逻辑；
 * matches/craft 依赖 World 扫描实体与放置方块，无头环境无法测试。</p>
 */
public class SingularityRecipeTest {

    /** 源码中 lifetimeTicks <= 0 时回落的默认值（TileMicroSingularity.DEFAULT_LIFE_TICKS）。 */
    private static final int DEFAULT_LIFE_TICKS = 6000;

    @BeforeAll
    public static void boot() {
        // 用例中构造 ItemStack，需无头引导
        AE2TestBootstrap.boot();
    }

    // ------------------------------------------------------------------
    // 完整构造函数
    // ------------------------------------------------------------------

    /** null droppedInputs 归一化为空集合。 */
    @Test
    public void testNullDroppedInputsBecomesEmpty() {
        SingularityRecipe recipe = new SingularityRecipe("sr_test:null_inputs", null,
                ItemStack.EMPTY, null, 100);

        assertThat(recipe.getInputs()).isNotNull().isEmpty();
    }

    /** null heldItem 归一化为 ItemStack.EMPTY。 */
    @Test
    public void testNullHeldItemBecomesEmpty() {
        SingularityRecipe recipe = new SingularityRecipe("sr_test:null_held",
                null, null, null, 100);

        assertThat(recipe.getHeldItem()).isNotNull();
        assertThat(recipe.getHeldItem().isEmpty()).isTrue();
    }

    /** lifetimeTicks 为 0 或负数时使用默认值 6000。 */
    @Test
    public void testNonPositiveLifetimeUsesDefault() {
        SingularityRecipe zero = new SingularityRecipe("sr_test:life0", null, null, null, 0);
        SingularityRecipe negative = new SingularityRecipe("sr_test:life_neg", null, null, null, -5);

        assertThat(zero.getLifetimeTicks()).isEqualTo(DEFAULT_LIFE_TICKS);
        assertThat(negative.getLifetimeTicks()).isEqualTo(DEFAULT_LIFE_TICKS);
    }

    /** lifetimeTicks 为正数时原样保留。 */
    @Test
    public void testPositiveLifetimeKept() {
        SingularityRecipe recipe = new SingularityRecipe("sr_test:life_pos", null, null, null, 1234);

        assertThat(recipe.getLifetimeTicks()).isEqualTo(1234);
    }

    /** targetBlock 原样保留（含 null，表示不检查目标方块）。 */
    @Test
    public void testTargetBlockKept() {
        SingularityRecipe recipe = new SingularityRecipe("sr_test:block", null, null, null, 100);

        assertThat(recipe.getTargetBlock()).isNull();
    }

    /** droppedInputs 非 null 时按源码语义直接持有传入列表（不做防御性拷贝）。 */
    @Test
    public void testInputsHoldPassedList() {
        List<ItemStack> inputs = Arrays.asList(new ItemStack(Items.APPLE, 2),
                new ItemStack(Items.COAL, 1, 1));
        SingularityRecipe recipe = new SingularityRecipe("sr_test:inputs", inputs,
                ItemStack.EMPTY, null, 100);

        // 源码语义：getInputs 返回传入列表本身
        assertThat(recipe.getInputs()).isSameAs(inputs);
        assertThat(recipe.getInputs()).hasSize(2);
    }

    // ------------------------------------------------------------------
    // 简化构造函数（向后兼容）
    // ------------------------------------------------------------------

    /** 简化构造函数使用默认值：held 为 EMPTY、targetBlock 为 null、lifetime 为默认值。 */
    @Test
    public void testSimpleConstructorDefaults() {
        List<ItemStack> inputs = Arrays.asList(new ItemStack(Items.APPLE, 1));
        SingularityRecipe recipe = new SingularityRecipe("sr_test:simple", inputs);

        assertThat(recipe.getId()).isEqualTo("sr_test:simple");
        assertThat(recipe.getInputs()).isSameAs(inputs);
        assertThat(recipe.getHeldItem().isEmpty()).isTrue();
        assertThat(recipe.getTargetBlock()).isNull();
        assertThat(recipe.getLifetimeTicks()).isEqualTo(DEFAULT_LIFE_TICKS);
    }
}
