package com.github.aeddddd.ae2enhanced.centralinterface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;
import com.github.aeddddd.ae2enhanced.test.util.FakeItemHandler;

/**
 * {@link HandlerUtils} 工具类测试。
 *
 * <p>覆盖 RecipeCacheKey 的 equals/hashCode 契约（含 null blockId 归一化）、
 * safeInit 的异常吞没语义，以及通用物品 IO 工具方法（pushItemsToTile /
 * pushItemToTile / extractAllItems / isInputMaterial / matchesLoosely）的确定性行为。
 * TileEntity 用 Mockito mock，物品能力统一挂在 DOWN 面。</p>
 */
public class HandlerUtilsTest {

    private static final BlockPos POS = new BlockPos(3, 70, -2);

    @BeforeAll
    public static void boot() {
        // 用例中构造 ItemStack / NBT，需无头引导
        AE2TestBootstrap.boot();
    }

    /** 构造挂了指定物品能力（DOWN 面）的 TileEntity mock。 */
    private static TileEntity tileWith(FakeItemHandler itemHandler) {
        TileEntity tile = mock(TileEntity.class);
        when(tile.getCapability(eq(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY), eq(EnumFacing.DOWN)))
                .thenReturn(itemHandler);
        return tile;
    }

    // ------------------------------------------------------------------
    // RecipeCacheKey
    // ------------------------------------------------------------------

    /** 相同维度/坐标/blockId 的 key 相等且 hashCode 相同，可作为 Map 键命中。 */
    @Test
    public void testRecipeCacheKeyEqualsAndHashCode() {
        HandlerUtils.RecipeCacheKey a = new HandlerUtils.RecipeCacheKey(0, POS, "minecraft:furnace");
        HandlerUtils.RecipeCacheKey b = new HandlerUtils.RecipeCacheKey(0, POS, "minecraft:furnace");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        Map<HandlerUtils.RecipeCacheKey, String> map = new HashMap<>();
        map.put(a, "cached");
        assertThat(map.get(b)).isEqualTo("cached");
    }

    /** 任一分量不同则不相等。 */
    @Test
    public void testRecipeCacheKeyNotEquals() {
        HandlerUtils.RecipeCacheKey base = new HandlerUtils.RecipeCacheKey(0, POS, "minecraft:furnace");

        assertThat(base).isNotEqualTo(new HandlerUtils.RecipeCacheKey(1, POS, "minecraft:furnace"));
        assertThat(base).isNotEqualTo(new HandlerUtils.RecipeCacheKey(0, POS.up(), "minecraft:furnace"));
        assertThat(base).isNotEqualTo(new HandlerUtils.RecipeCacheKey(0, POS, "minecraft:chest"));
    }

    /** null blockId 归一化为空字符串，与显式 "" 相等。 */
    @Test
    public void testRecipeCacheKeyNullBlockIdNormalized() {
        HandlerUtils.RecipeCacheKey nullId = new HandlerUtils.RecipeCacheKey(0, POS, null);
        HandlerUtils.RecipeCacheKey emptyId = new HandlerUtils.RecipeCacheKey(0, POS, "");

        assertThat(nullId.blockId).isEmpty();
        assertThat(nullId).isEqualTo(emptyId);
        assertThat(nullId.hashCode()).isEqualTo(emptyId.hashCode());
    }

    /** equals 边界：自反、null、其它类型。 */
    @Test
    public void testRecipeCacheKeyEqualsEdgeCases() {
        HandlerUtils.RecipeCacheKey key = new HandlerUtils.RecipeCacheKey(0, POS, "a:b");

        assertThat(key).isEqualTo(key);
        assertThat(key).isNotEqualTo(null);
        assertThat(key).isNotEqualTo("a:b");
    }

    // ------------------------------------------------------------------
    // safeInit
    // ------------------------------------------------------------------

    /** 初始化逻辑正常执行时返回 true。 */
    @Test
    public void testSafeInitSuccess() {
        boolean[] ran = { false };

        boolean ok = HandlerUtils.safeInit("TestHandler", () -> ran[0] = true);

        assertThat(ok).isTrue();
        assertThat(ran[0]).isTrue();
    }

    /** 初始化逻辑抛异常时被吞没并返回 false，不向外传播。 */
    @Test
    public void testSafeInitSwallowsException() {
        boolean ok = HandlerUtils.safeInit("BrokenHandler", () -> {
            throw new IllegalStateException("反射目标不存在");
        });

        assertThat(ok).isFalse();
    }

    // ------------------------------------------------------------------
    // pushItemsToTile / pushItemToTile
    // ------------------------------------------------------------------

    /** 目标为 null 或物品列表为 null/空时，返回空列表且不抛异常。 */
    @Test
    public void testPushItemsToTileNullOrEmptyInputs() {
        TileEntity tile = tileWith(new FakeItemHandler(1));
        ItemStack apple = new ItemStack(Items.APPLE, 1);

        assertThat(HandlerUtils.pushItemsToTile(null, Collections.singletonList(apple))).isEmpty();
        assertThat(HandlerUtils.pushItemsToTile(tile, null)).isEmpty();
        assertThat(HandlerUtils.pushItemsToTile(tile, Collections.emptyList())).isEmpty();
    }

    /** 全部推入成功时返回空剩余列表，且物品进入目标。 */
    @Test
    public void testPushItemsToTileSuccess() {
        FakeItemHandler itemHandler = new FakeItemHandler(2);
        TileEntity tile = tileWith(itemHandler);

        List<ItemStack> leftovers = HandlerUtils.pushItemsToTile(tile, Arrays.asList(
                new ItemStack(Items.APPLE, 2), new ItemStack(Items.COAL, 1)));

        assertThat(leftovers).isEmpty();
        assertThat(itemHandler.getStackInSlot(0).getItem()).isEqualTo(Items.APPLE);
        assertThat(itemHandler.getStackInSlot(0).getCount()).isEqualTo(2);
        assertThat(itemHandler.getStackInSlot(1).getItem()).isEqualTo(Items.COAL);
    }

    /** 目标容量不足时返回未能推入的剩余物品。 */
    @Test
    public void testPushItemsToTileLeftover() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        itemHandler.setStack(0, new ItemStack(Items.BRICK, 64)); // 唯一槽已满
        TileEntity tile = tileWith(itemHandler);

        List<ItemStack> leftovers = HandlerUtils.pushItemsToTile(
                tile, Collections.singletonList(new ItemStack(Items.APPLE, 3)));

        assertThat(leftovers).hasSize(1);
        assertThat(leftovers.get(0).getItem()).isEqualTo(Items.APPLE);
        assertThat(leftovers.get(0).getCount()).isEqualTo(3);
    }

    /** 列表中的空物品被跳过，不产生剩余。 */
    @Test
    public void testPushItemsToTileSkipsEmptyStacks() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        TileEntity tile = tileWith(itemHandler);

        List<ItemStack> leftovers = HandlerUtils.pushItemsToTile(
                tile, Arrays.asList(ItemStack.EMPTY, new ItemStack(Items.APPLE, 1)));

        assertThat(leftovers).isEmpty();
        assertThat(itemHandler.getStackInSlot(0).getItem()).isEqualTo(Items.APPLE);
    }

    /** pushItemToTile：目标为 null 时原样返回入参物品。 */
    @Test
    public void testPushItemToTileNullTarget() {
        ItemStack apple = new ItemStack(Items.APPLE, 1);

        assertThat(HandlerUtils.pushItemToTile(null, apple)).isSameAs(apple);
    }

    /** pushItemToTile：空物品直接返回，不触碰目标。 */
    @Test
    public void testPushItemToTileEmptyStack() {
        TileEntity tile = tileWith(new FakeItemHandler(1));

        assertThat(HandlerUtils.pushItemToTile(tile, ItemStack.EMPTY).isEmpty()).isTrue();
    }

    // ------------------------------------------------------------------
    // extractAllItems
    // ------------------------------------------------------------------

    /** 目标为 null 时返回空列表。 */
    @Test
    public void testExtractAllItemsNullTarget() {
        assertThat(HandlerUtils.extractAllItems(null)).isNotNull().isEmpty();
    }

    /** 抽取目标所有槽位的全部物品，抽取后槽位清空。 */
    @Test
    public void testExtractAllItemsDrainsAllSlots() {
        FakeItemHandler itemHandler = new FakeItemHandler(3);
        itemHandler.setStack(0, new ItemStack(Items.APPLE, 2));
        itemHandler.setStack(2, new ItemStack(Items.COAL, 1));
        TileEntity tile = tileWith(itemHandler);

        List<ItemStack> extracted = HandlerUtils.extractAllItems(tile);

        assertThat(extracted).hasSize(2);
        assertThat(extracted).anyMatch(s -> s.getItem() == Items.APPLE && s.getCount() == 2);
        assertThat(extracted).anyMatch(s -> s.getItem() == Items.COAL && s.getCount() == 1);
        assertThat(itemHandler.getStackInSlot(0).isEmpty()).isTrue();
        assertThat(itemHandler.getStackInSlot(2).isEmpty()).isTrue();
    }

    // ------------------------------------------------------------------
    // isInputMaterial / matchesLoosely
    // ------------------------------------------------------------------

    /** isInputMaterial：空物品、null/空输入快照均为 false。 */
    @Test
    public void testIsInputMaterialTrivialFalse() {
        ItemStack apple = new ItemStack(Items.APPLE, 1);

        assertThat(HandlerUtils.isInputMaterial(ItemStack.EMPTY,
                Collections.singletonList(apple))).isFalse();
        assertThat(HandlerUtils.isInputMaterial(apple, null)).isFalse();
        assertThat(HandlerUtils.isInputMaterial(apple, Collections.emptyList())).isFalse();
    }

    /** isInputMaterial：物品 + NBT 均相同才匹配；不同物品/metadata 不匹配。 */
    @Test
    public void testIsInputMaterialMatching() {
        ItemStack apple = new ItemStack(Items.APPLE, 1);

        assertThat(HandlerUtils.isInputMaterial(apple, Collections.singletonList(apple))).isTrue();
        assertThat(HandlerUtils.isInputMaterial(new ItemStack(Items.COAL, 1),
                Collections.singletonList(apple))).isFalse();
        // metadata 不同不匹配
        assertThat(HandlerUtils.isInputMaterial(new ItemStack(Items.APPLE, 1, 1),
                Collections.singletonList(new ItemStack(Items.APPLE, 1, 0)))).isFalse();
    }

    /** isInputMaterial：NBT 必须一致。 */
    @Test
    public void testIsInputMaterialNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("k", 1);
        ItemStack withTag = new ItemStack(Items.APPLE, 1);
        withTag.setTagCompound(tag);

        assertThat(HandlerUtils.isInputMaterial(withTag,
                Collections.singletonList(new ItemStack(Items.APPLE, 1)))).isFalse();

        ItemStack sameTag = new ItemStack(Items.APPLE, 1);
        sameTag.setTagCompound(tag.copy());
        assertThat(HandlerUtils.isInputMaterial(withTag,
                Collections.singletonList(sameTag))).isTrue();
    }

    /** matchesLoosely：不同物品 false；expected 无 NBT 时忽略 actual 的 NBT。 */
    @Test
    public void testMatchesLooselyIgnoresActualNbtWhenExpectedHasNone() {
        ItemStack actual = new ItemStack(Items.APPLE, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("k", 1);
        actual.setTagCompound(tag);

        assertThat(HandlerUtils.matchesLoosely(actual, new ItemStack(Items.APPLE, 1))).isTrue();
        assertThat(HandlerUtils.matchesLoosely(actual, new ItemStack(Items.COAL, 1))).isFalse();
    }

    /** matchesLoosely：expected 带 NBT 时要求 NBT 完全一致。 */
    @Test
    public void testMatchesLooselyRequiresNbtWhenExpectedHasSome() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("k", 1);
        ItemStack expected = new ItemStack(Items.APPLE, 1);
        expected.setTagCompound(tag);

        ItemStack matching = new ItemStack(Items.APPLE, 1);
        matching.setTagCompound(tag.copy());
        ItemStack noTag = new ItemStack(Items.APPLE, 1);

        assertThat(HandlerUtils.matchesLoosely(matching, expected)).isTrue();
        assertThat(HandlerUtils.matchesLoosely(noTag, expected)).isFalse();
    }

    // ------------------------------------------------------------------
    // machineSource
    // ------------------------------------------------------------------

    /** machineSource 包装宿主返回非 null 的 MachineSource。 */
    @Test
    public void testMachineSourceNonNull() {
        ICentralInterfaceHost host = mock(ICentralInterfaceHost.class);

        assertThat(HandlerUtils.machineSource(host)).isNotNull();
    }
}
