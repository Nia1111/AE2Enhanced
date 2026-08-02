package com.github.aeddddd.ae2enhanced.crafting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link BlackHoleRecipe} 单元测试。
 *
 * <p>覆盖 matches(Map) 的数量/缺 key 判定语义、keyOf 的 key 拼接格式
 * （registryName:meta + NBT 追加、空栈/无注册名返回 ""），以及构造与
 * getter 的防御性拷贝行为。</p>
 */
public class BlackHoleRecipeTest {

    @BeforeAll
    public static void boot() {
        // 用例中构造 ItemStack / NBT，需无头引导
        AE2TestBootstrap.boot();
    }

    /** 构造双输入配方 {"a":2, "b":1} 的便捷方法。 */
    private static BlackHoleRecipe twoInputRecipe() {
        Map<String, Integer> inputs = new HashMap<>();
        inputs.put("a", 2);
        inputs.put("b", 1);
        return new BlackHoleRecipe("bhr_test:two", inputs, new ItemStack(Items.APPLE, 1));
    }

    // ------------------------------------------------------------------
    // matches(Map)
    // ------------------------------------------------------------------

    /** 某一输入数量不足时不匹配。 */
    @Test
    public void testMatchesInsufficientCount() {
        BlackHoleRecipe recipe = twoInputRecipe();
        Map<String, Integer> found = new HashMap<>();
        found.put("a", 1); // 需要 2
        found.put("b", 1);

        assertThat(recipe.matches(found)).isFalse();
    }

    /** 缺少某个输入 key（视为 0）时不匹配。 */
    @Test
    public void testMatchesMissingKey() {
        BlackHoleRecipe recipe = twoInputRecipe();
        Map<String, Integer> found = new HashMap<>();
        found.put("a", 2);

        assertThat(recipe.matches(found)).isFalse();
    }

    /** 数量恰好满足时匹配。 */
    @Test
    public void testMatchesExact() {
        BlackHoleRecipe recipe = twoInputRecipe();
        Map<String, Integer> found = new HashMap<>();
        found.put("a", 2);
        found.put("b", 1);

        assertThat(recipe.matches(found)).isTrue();
    }

    /** 数量超量且含无关 key 时仍匹配。 */
    @Test
    public void testMatchesOverSupplied() {
        BlackHoleRecipe recipe = twoInputRecipe();
        Map<String, Integer> found = new HashMap<>();
        found.put("a", 99);
        found.put("b", 3);
        found.put("c", 7); // 无关 key 不影响判定

        assertThat(recipe.matches(found)).isTrue();
    }

    /** 无输入的配方匹配任何 found（含空 Map）。 */
    @Test
    public void testMatchesEmptyInputs() {
        BlackHoleRecipe recipe = new BlackHoleRecipe("bhr_test:empty",
                new HashMap<>(), new ItemStack(Items.APPLE, 1));

        assertThat(recipe.matches(new HashMap<>())).isTrue();
        Map<String, Integer> found = new HashMap<>();
        found.put("anything", 1);
        assertThat(recipe.matches(found)).isTrue();
    }

    // ------------------------------------------------------------------
    // keyOf(ItemStack)
    // ------------------------------------------------------------------

    /** key 格式为 "registryName:meta"。 */
    @Test
    public void testKeyOfFormat() {
        assertThat(BlackHoleRecipe.keyOf(new ItemStack(Items.APPLE, 1)))
                .isEqualTo("minecraft:apple:0");
        // 同一 Item 不同 metadata 生成不同 key
        assertThat(BlackHoleRecipe.keyOf(new ItemStack(Items.COAL, 1, 0)))
                .isEqualTo("minecraft:coal:0");
        assertThat(BlackHoleRecipe.keyOf(new ItemStack(Items.COAL, 1, 1)))
                .isEqualTo("minecraft:coal:1");
    }

    /** 含 NBT 时在 key 末尾追加 NBT 字符串，同 meta 不同 NBT 可区分。 */
    @Test
    public void testKeyOfWithNbt() {
        ItemStack withTag = new ItemStack(Items.APPLE, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("k", 1);
        withTag.setTagCompound(tag);

        String key = BlackHoleRecipe.keyOf(withTag);
        assertThat(key).startsWith("minecraft:apple:0");
        assertThat(key).contains("k:1");
        // 与无 NBT 的同物品 key 不同
        assertThat(key).isNotEqualTo(BlackHoleRecipe.keyOf(new ItemStack(Items.APPLE, 1)));
    }

    /** 空栈 key 为 ""。 */
    @Test
    public void testKeyOfEmptyStack() {
        assertThat(BlackHoleRecipe.keyOf(ItemStack.EMPTY)).isEmpty();
    }

    /** 物品无注册名（未注册进 GameRegistry）时 key 为 ""。 */
    @Test
    public void testKeyOfNoRegistryName() {
        ItemStack unregistered = new ItemStack(new Item(), 1);
        assertThat(unregistered.getItem().getRegistryName()).isNull();
        assertThat(BlackHoleRecipe.keyOf(unregistered)).isEmpty();
    }

    // ------------------------------------------------------------------
    // 防御性拷贝
    // ------------------------------------------------------------------

    /** 构造后修改传入的 inputs Map 不影响配方内部状态。 */
    @Test
    public void testConstructorCopiesInputs() {
        Map<String, Integer> inputs = new HashMap<>();
        inputs.put("a", 2);
        BlackHoleRecipe recipe = new BlackHoleRecipe("bhr_test:copy", inputs,
                new ItemStack(Items.APPLE, 1));

        // 修改原 Map：移除 key、修改数量、新增 key
        inputs.put("a", 999);
        inputs.put("b", 1);

        Map<String, Integer> found = new HashMap<>();
        found.put("a", 2);
        assertThat(recipe.matches(found)).isTrue();
        assertThat(recipe.getInputs()).hasSize(1).containsEntry("a", 2);
    }

    /** getInputs 返回副本，修改返回值不影响内部状态。 */
    @Test
    public void testGetInputsReturnsCopy() {
        BlackHoleRecipe recipe = twoInputRecipe();

        Map<String, Integer> exposed = recipe.getInputs();
        exposed.clear();

        Map<String, Integer> found = new HashMap<>();
        found.put("a", 2);
        found.put("b", 1);
        assertThat(recipe.matches(found)).isTrue();
    }

    /** 构造时拷贝输出物品：修改原 ItemStack 不影响配方输出。 */
    @Test
    public void testConstructorCopiesOutput() {
        ItemStack output = new ItemStack(Items.APPLE, 3);
        BlackHoleRecipe recipe = new BlackHoleRecipe("bhr_test:out", new HashMap<>(), output);

        output.setCount(64);

        assertThat(recipe.getOutput().getCount()).isEqualTo(3);
    }

    /** getOutput 每次返回副本，修改返回值不影响后续获取。 */
    @Test
    public void testGetOutputReturnsCopy() {
        BlackHoleRecipe recipe = new BlackHoleRecipe("bhr_test:out2", new HashMap<>(),
                new ItemStack(Items.APPLE, 3));

        ItemStack first = recipe.getOutput();
        first.setCount(1);

        assertThat(recipe.getOutput().getCount()).isEqualTo(3);
        assertThat(recipe.getOutput()).isNotSameAs(first);
    }

    // ------------------------------------------------------------------
    // null 归一化
    // ------------------------------------------------------------------

    /** 构造传 null inputs 归一化为空 Map：不抛 NPE，匹配任何 found。 */
    @Test
    public void testNullInputsNormalizedToEmptyMap() {
        BlackHoleRecipe recipe = new BlackHoleRecipe("bhr_test:null_inputs", null,
                new ItemStack(Items.APPLE, 1));

        assertThat(recipe.getInputs()).isNotNull().isEmpty();
        assertThat(recipe.matches(new HashMap<>())).isTrue();
    }

    /** 构造传 null output 归一化为 ItemStack.EMPTY：不抛 NPE。 */
    @Test
    public void testNullOutputNormalizedToEmpty() {
        BlackHoleRecipe recipe = new BlackHoleRecipe("bhr_test:null_output",
                new HashMap<>(), null);

        assertThat(recipe.getOutput()).isNotNull();
        assertThat(recipe.getOutput().isEmpty()).isTrue();
    }
}
