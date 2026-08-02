package com.github.aeddddd.ae2enhanced.crafting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link SingularityFuelRecipe} 单元测试。
 *
 * <p>覆盖 matches 的匹配语义：物品 + metadata 精确匹配，
 * 忽略 NBT 与数量，空栈不匹配；以及各 getter 行为。</p>
 */
public class SingularityFuelRecipeTest {

    @BeforeAll
    public static void boot() {
        // 用例中构造 ItemStack / NBT，需无头引导
        AE2TestBootstrap.boot();
    }

    /** 燃料为 coal:0 的测试配方。 */
    private static SingularityFuelRecipe coalRecipe() {
        return new SingularityFuelRecipe("sfr_test:coal", new ItemStack(Items.COAL, 1, 0), 100, false);
    }

    // ------------------------------------------------------------------
    // getter
    // ------------------------------------------------------------------

    /** getter 返回构造参数。 */
    @Test
    public void testGetters() {
        ItemStack fuel = new ItemStack(Items.COAL, 1, 1);
        SingularityFuelRecipe recipe = new SingularityFuelRecipe("sfr_test:g", fuel, 250, true);

        assertThat(recipe.getId()).isEqualTo("sfr_test:g");
        assertThat(recipe.getFuelItem()).isSameAs(fuel);
        assertThat(recipe.getTicks()).isEqualTo(250);
        assertThat(recipe.isPermanent()).isTrue();
    }

    // ------------------------------------------------------------------
    // matches
    // ------------------------------------------------------------------

    /** 正确物品 + 正确 meta 匹配。 */
    @Test
    public void testMatchesCorrectItemAndMeta() {
        assertThat(coalRecipe().matches(new ItemStack(Items.COAL, 1, 0))).isTrue();
    }

    /** 同物品但 meta 不同不匹配。 */
    @Test
    public void testMatchesWrongMeta() {
        assertThat(coalRecipe().matches(new ItemStack(Items.COAL, 1, 1))).isFalse();
    }

    /** 不同物品不匹配。 */
    @Test
    public void testMatchesWrongItem() {
        assertThat(coalRecipe().matches(new ItemStack(Items.APPLE, 1, 0))).isFalse();
    }

    /** 空栈不匹配。 */
    @Test
    public void testMatchesEmptyStack() {
        assertThat(coalRecipe().matches(ItemStack.EMPTY)).isFalse();
    }

    /** 匹配忽略 NBT：待匹配栈带 NBT 仍命中。 */
    @Test
    public void testMatchesIgnoresNbt() {
        ItemStack withTag = new ItemStack(Items.COAL, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("k", 1);
        withTag.setTagCompound(tag);

        assertThat(coalRecipe().matches(withTag)).isTrue();
    }

    /** 匹配忽略数量：任意 count 均命中。 */
    @Test
    public void testMatchesIgnoresCount() {
        SingularityFuelRecipe recipe = coalRecipe();

        assertThat(recipe.matches(new ItemStack(Items.COAL, 1, 0))).isTrue();
        assertThat(recipe.matches(new ItemStack(Items.COAL, 64, 0))).isTrue();
    }

    /** 燃料定义本身带 NBT 也不影响匹配（matches 只比较 item 与 meta）。 */
    @Test
    public void testMatchesFuelNbtIrrelevant() {
        ItemStack fuel = new ItemStack(Items.COAL, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("k", 1);
        fuel.setTagCompound(tag);
        SingularityFuelRecipe recipe = new SingularityFuelRecipe("sfr_test:nbtfuel", fuel, 100, false);

        assertThat(recipe.matches(new ItemStack(Items.COAL, 1, 0))).isTrue();
    }
}
