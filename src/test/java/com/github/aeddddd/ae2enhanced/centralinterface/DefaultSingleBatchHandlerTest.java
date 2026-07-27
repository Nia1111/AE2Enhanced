package com.github.aeddddd.ae2enhanced.centralinterface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;
import com.github.aeddddd.ae2enhanced.test.util.FakeItemHandler;

/**
 * {@link DefaultSingleBatchHandler} 通用兜底处理器测试。
 *
 * <p>World / TileEntity 用 Mockito mock，IItemHandler 能力用 {@link FakeItemHandler}
 * （严格遵守 simulate 语义）或 Mockito mock（用于验证真实模式调用次数）。
 * 物品能力统一挂在 DOWN 面，其余面返回 null（与真实机器一致）。</p>
 */
public class DefaultSingleBatchHandlerTest {

    private static final BlockPos POS = new BlockPos(1, 64, 1);

    private DefaultSingleBatchHandler handler;
    private World world;
    private TileEntity tile;

    @BeforeAll
    public static void boot() {
        // 用例中构造 ItemStack / AEItemStack，需无头引导
        AE2TestBootstrap.boot();
    }

    @BeforeEach
    public void setUp() {
        this.handler = new DefaultSingleBatchHandler();
        this.world = mock(World.class);
        this.tile = mock(TileEntity.class);
        when(world.getTileEntity(POS)).thenReturn(tile);
    }

    /** 把 IItemHandler 能力挂到 DOWN 面，其余面保持 null。 */
    private void wireCapability(IItemHandler itemHandler) {
        when(tile.getCapability(eq(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY), eq(EnumFacing.DOWN)))
                .thenReturn(itemHandler);
    }

    private static InventoryCrafting crafting(ItemStack... stacks) {
        InventoryCrafting inv = new InventoryCrafting(mock(Container.class), 3, 3);
        for (int i = 0; i < stacks.length; i++) {
            inv.setInventorySlotContents(i, stacks[i]);
        }
        return inv;
    }

    // ------------------------------------------------------------------
    // 基本语义
    // ------------------------------------------------------------------

    /** canHandle 恒 false：不主动匹配任何 blockId，仅作 HandlerRegistry 兜底。 */
    @Test
    public void testCanHandleAlwaysFalse() {
        assertThat(handler.canHandle("minecraft:furnace")).isFalse();
        assertThat(handler.canHandle("")).isFalse();
        assertThat(handler.canHandle(null)).isFalse();
    }

    /** getCapabilities 仅 PHYSICAL，不支持虚拟批量。 */
    @Test
    public void testGetCapabilitiesPhysicalOnly() {
        assertThat(handler.getCapabilities()).containsExactly(HandlerCapabilities.PHYSICAL);
        assertThat(handler.hasCapability(HandlerCapabilities.PHYSICAL)).isTrue();
        assertThat(handler.hasCapability(HandlerCapabilities.VIRTUAL_BATCH)).isFalse();
    }

    /** 目标位置无 TileEntity 时 isValidTarget 为 false。 */
    @Test
    public void testIsValidTargetNoTileEntity() {
        World emptyWorld = mock(World.class);
        assertThat(handler.isValidTarget(emptyWorld, POS)).isFalse();
    }

    /** 任一面具备 IItemHandler 能力即为有效目标；全无能力则无效。 */
    @Test
    public void testIsValidTargetRequiresItemHandlerCapability() {
        assertThat(handler.isValidTarget(world, POS)).isFalse();

        when(tile.hasCapability(eq(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY), eq(EnumFacing.UP)))
                .thenReturn(true);
        assertThat(handler.isValidTarget(world, POS)).isTrue();
    }

    /** canStart 语义等同 isValidTarget。 */
    @Test
    public void testCanStartDelegatesToIsValidTarget() {
        assertThat(handler.canStart(world, POS, crafting(), null)).isFalse();

        when(tile.hasCapability(eq(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY), eq(EnumFacing.DOWN)))
                .thenReturn(true);
        assertThat(handler.canStart(world, POS, crafting(), null)).isTrue();
    }

    /** startProcess 恒 true（物品插入后机器自动开始处理）。 */
    @Test
    public void testStartProcessAlwaysTrue() {
        assertThat(handler.startProcess(world, POS, null, null)).isTrue();
    }

    // ------------------------------------------------------------------
    // pushMaterials 两阶段原子性
    // ------------------------------------------------------------------

    /** 目标无 TileEntity 时 pushMaterials 返回 false。 */
    @Test
    public void testPushMaterialsNoTileEntity() {
        World emptyWorld = mock(World.class);
        InventoryCrafting inv = crafting(new ItemStack(Items.APPLE, 1));

        assertThat(handler.pushMaterials(emptyWorld, POS, inv, null, null)).isFalse();
    }

    /** 空配方（无输入）视为推送成功，不触碰任何能力。 */
    @Test
    public void testPushMaterialsEmptyIngredients() {
        IItemHandler itemHandler = mock(IItemHandler.class);
        wireCapability(itemHandler);

        assertThat(handler.pushMaterials(world, POS, crafting(), null, null)).isTrue();
        verify(itemHandler, never()).insertItem(anyInt(), any(ItemStack.class), anyBoolean());
    }

    /**
     * 全部材料可插入时返回 true：模拟与真实阶段各对每个物品调用一次 insertItem，
     * 且 ingredients 内容不被修改（清空输入由调用方负责）。
     */
    @Test
    public void testPushMaterialsSuccessTwoPhase() {
        IItemHandler itemHandler = mock(IItemHandler.class);
        when(itemHandler.getSlots()).thenReturn(1);
        when(itemHandler.insertItem(anyInt(), any(ItemStack.class), anyBoolean()))
                .thenReturn(ItemStack.EMPTY);
        // 所有面都返回同一 handler：插入在第一个面即完成
        when(tile.getCapability(eq(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY), any()))
                .thenReturn(itemHandler);

        InventoryCrafting inv = crafting(new ItemStack(Items.APPLE, 2), new ItemStack(Items.COAL, 1));

        assertThat(handler.pushMaterials(world, POS, inv, null, null)).isTrue();

        // 两个输入物品，模拟阶段与真实阶段各插入一次
        verify(itemHandler, times(2)).insertItem(anyInt(), any(ItemStack.class), eq(true));
        verify(itemHandler, times(2)).insertItem(anyInt(), any(ItemStack.class), eq(false));
        // 生产代码仅拷贝材料，不消费 ingredients（由 DualityCentralInterface 另行扣除）
        assertThat(inv.getStackInSlot(0).getCount()).isEqualTo(2);
        assertThat(inv.getStackInSlot(1).getCount()).isEqualTo(1);
    }

    /** 成功路径下材料确实进入目标物品处理器（FakeItemHandler 状态断言）。 */
    @Test
    public void testPushMaterialsSuccessMutatesTarget() {
        FakeItemHandler itemHandler = new FakeItemHandler(3);
        wireCapability(itemHandler);

        InventoryCrafting inv = crafting(new ItemStack(Items.APPLE, 2), new ItemStack(Items.COAL, 1));

        assertThat(handler.pushMaterials(world, POS, inv, null, null)).isTrue();
        assertThat(itemHandler.getStackInSlot(0).getItem()).isEqualTo(Items.APPLE);
        assertThat(itemHandler.getStackInSlot(0).getCount()).isEqualTo(2);
        assertThat(itemHandler.getStackInSlot(1).getItem()).isEqualTo(Items.COAL);
        assertThat(itemHandler.getStackInSlot(2).isEmpty()).isTrue();
    }

    /** 模拟阶段任一物品无法完整放入 → 返回 false 且绝不发生真实插入（原子性）。 */
    @Test
    public void testPushMaterialsAtomicFailureNoRealInsert() {
        IItemHandler itemHandler = mock(IItemHandler.class);
        when(itemHandler.getSlots()).thenReturn(1);
        // 任何插入都原样退回 → 模拟必然失败
        when(itemHandler.insertItem(anyInt(), any(ItemStack.class), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(tile.getCapability(eq(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY), any()))
                .thenReturn(itemHandler);

        InventoryCrafting inv = crafting(new ItemStack(Items.APPLE, 1));

        assertThat(handler.pushMaterials(world, POS, inv, null, null)).isFalse();
        verify(itemHandler, never()).insertItem(anyInt(), any(ItemStack.class), eq(false));
    }

    /** 目标容量不足时返回 false 且目标内容保持原样（FakeItemHandler 状态断言）。 */
    @Test
    public void testPushMaterialsAtomicFailureStateUnchanged() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        itemHandler.setStack(0, new ItemStack(Items.BRICK, 64)); // 唯一槽已满
        wireCapability(itemHandler);

        InventoryCrafting inv = crafting(new ItemStack(Items.APPLE, 1));

        assertThat(handler.pushMaterials(world, POS, inv, null, null)).isFalse();
        assertThat(itemHandler.getStackInSlot(0).getItem()).isEqualTo(Items.BRICK);
        assertThat(itemHandler.getStackInSlot(0).getCount()).isEqualTo(64);
    }

    // ------------------------------------------------------------------
    // revertMaterials / clearOutputs
    // ------------------------------------------------------------------

    /** revertMaterials 只回退本次推送的输入材料，非输入物品（如升级）保留。 */
    @Test
    public void testRevertMaterialsOnlyInputs() {
        FakeItemHandler itemHandler = new FakeItemHandler(2);
        itemHandler.setStack(0, new ItemStack(Items.APPLE, 2));
        itemHandler.setStack(1, new ItemStack(Items.BRICK, 1));
        wireCapability(itemHandler);

        TargetSession session = mock(TargetSession.class);
        when(session.getInputs()).thenReturn(Collections.singletonList(new ItemStack(Items.APPLE, 1)));

        List<ItemStack> reverted = handler.revertMaterials(world, POS, null, session);

        assertThat(reverted).hasSize(1);
        assertThat(reverted.get(0).getItem()).isEqualTo(Items.APPLE);
        assertThat(reverted.get(0).getCount()).isEqualTo(2);
        // 非输入物品未被回退
        assertThat(itemHandler.getStackInSlot(0).isEmpty()).isTrue();
        assertThat(itemHandler.getStackInSlot(1).getItem()).isEqualTo(Items.BRICK);
    }

    /** session 为 null 时输入快照为空，revertMaterials 不回退任何物品。 */
    @Test
    public void testRevertMaterialsNullSessionRevertsNothing() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        itemHandler.setStack(0, new ItemStack(Items.APPLE, 2));
        wireCapability(itemHandler);

        List<ItemStack> reverted = handler.revertMaterials(world, POS, null, null);

        assertThat(reverted).isEmpty();
        assertThat(itemHandler.getStackInSlot(0).getCount()).isEqualTo(2);
    }

    /** 目标无 TileEntity 时 revertMaterials 返回空列表。 */
    @Test
    public void testRevertMaterialsNoTileEntity() {
        World emptyWorld = mock(World.class);
        assertThat(handler.revertMaterials(emptyWorld, POS, null, null)).isNotNull().isEmpty();
    }

    /** clearOutputs 回收预期产物与残留输入；既非输入又非预期的物品（升级等）保留。 */
    @Test
    public void testClearOutputsCollectsExpectedAndInputs() {
        FakeItemHandler itemHandler = new FakeItemHandler(3);
        itemHandler.setStack(0, new ItemStack(Items.APPLE, 2));  // 预期产物
        itemHandler.setStack(1, new ItemStack(Items.COAL, 1));   // 残留输入
        itemHandler.setStack(2, new ItemStack(Items.BRICK, 1));  // 两者皆非 → 保留
        wireCapability(itemHandler);

        TargetSession session = mock(TargetSession.class);
        when(session.getInputs()).thenReturn(Collections.singletonList(new ItemStack(Items.COAL, 1)));
        when(session.getExpectedOutputs()).thenReturn(new IAEItemStack[] {
                AEItemStack.fromItemStack(new ItemStack(Items.APPLE, 1)) });

        List<ItemStack> cleared = handler.clearOutputs(world, POS, null, session);

        assertThat(cleared).hasSize(2);
        assertThat(cleared).anyMatch(s -> s.getItem() == Items.APPLE && s.getCount() == 2);
        assertThat(cleared).anyMatch(s -> s.getItem() == Items.COAL && s.getCount() == 1);
        assertThat(itemHandler.getStackInSlot(2).getItem()).isEqualTo(Items.BRICK);
    }

    /** 目标无 TileEntity 时 clearOutputs 返回空列表。 */
    @Test
    public void testClearOutputsNoTileEntity() {
        World emptyWorld = mock(World.class);
        assertThat(handler.clearOutputs(emptyWorld, POS, null, null)).isNotNull().isEmpty();
    }

    // ------------------------------------------------------------------
    // collectProducts 两阶段收集
    // ------------------------------------------------------------------

    /** 阶段 1 优先收集预期产物，阶段 2 兜底收集非输入物品；输入材料保留。 */
    @Test
    public void testCollectProductsTwoPhases() {
        FakeItemHandler itemHandler = new FakeItemHandler(3);
        itemHandler.setStack(0, new ItemStack(Items.APPLE, 3)); // 预期产物
        itemHandler.setStack(1, new ItemStack(Items.STICK, 1)); // 副产物/残余
        itemHandler.setStack(2, new ItemStack(Items.COAL, 1));  // 输入材料 → 保留
        wireCapability(itemHandler);

        IAEItemStack[] expected = { AEItemStack.fromItemStack(new ItemStack(Items.APPLE, 3)) };
        List<ItemStack> inputs = Collections.singletonList(new ItemStack(Items.COAL, 1));

        List<ItemStack> collected = handler.collectProducts(world, POS, expected, inputs, null, null);

        assertThat(collected).hasSize(2);
        // 阶段 1：预期产物苹果 3 个排在最前
        assertThat(collected.get(0).getItem()).isEqualTo(Items.APPLE);
        assertThat(collected.get(0).getCount()).isEqualTo(3);
        // 阶段 2：兜底回收木棍
        assertThat(collected.get(1).getItem()).isEqualTo(Items.STICK);
        // 输入材料不被收集
        assertThat(itemHandler.getStackInSlot(2).getItem()).isEqualTo(Items.COAL);
    }

    /** 预期产物部分提取后，槽内剩余数量由阶段 2 兜底继续回收。 */
    @Test
    public void testCollectProductsExpectedPartialThenFallback() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        itemHandler.setStack(0, new ItemStack(Items.APPLE, 5));
        wireCapability(itemHandler);

        IAEItemStack[] expected = { AEItemStack.fromItemStack(new ItemStack(Items.APPLE, 3)) };

        List<ItemStack> collected = handler.collectProducts(
                world, POS, expected, Collections.emptyList(), null, null);

        // 阶段 1 取 3 个，阶段 2 取剩余 2 个，总计 5 个苹果
        assertThat(collected).hasSize(2);
        assertThat(collected.get(0).getCount()).isEqualTo(3);
        assertThat(collected.get(1).getCount()).isEqualTo(2);
        assertThat(collected).allMatch(s -> s.getItem() == Items.APPLE);
        assertThat(itemHandler.getStackInSlot(0).isEmpty()).isTrue();
    }

    /** 目标无 TileEntity 时 collectProducts 返回空列表。 */
    @Test
    public void testCollectProductsNoTileEntity() {
        World emptyWorld = mock(World.class);
        List<ItemStack> collected = handler.collectProducts(
                emptyWorld, POS, null, null, null, null);
        assertThat(collected).isNotNull().isEmpty();
    }

    // ------------------------------------------------------------------
    // isIdle / hasFinished
    // ------------------------------------------------------------------

    /** 存在可抽取的非输入物品（产物）时 isIdle 为 true。 */
    @Test
    public void testIsIdleWithProduct() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        itemHandler.setStack(0, new ItemStack(Items.APPLE, 1));
        wireCapability(itemHandler);

        assertThat(handler.isIdle(world, POS, Collections.emptyList(), null)).isTrue();
        // isIdle 只做模拟抽取，不修改目标内容
        assertThat(itemHandler.getStackInSlot(0).getCount()).isEqualTo(1);
    }

    /** 槽内仅有输入材料或完全为空时 isIdle 为 false。 */
    @Test
    public void testIsIdleOnlyInputsOrEmpty() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        itemHandler.setStack(0, new ItemStack(Items.COAL, 1));
        wireCapability(itemHandler);

        List<ItemStack> inputs = Collections.singletonList(new ItemStack(Items.COAL, 1));
        assertThat(handler.isIdle(world, POS, inputs, null)).isFalse();

        // 清空后仍 false
        itemHandler.setStack(0, ItemStack.EMPTY);
        assertThat(handler.isIdle(world, POS, inputs, null)).isFalse();
    }

    /** 目标无 TileEntity 时 isIdle 为 true（可安全结束）。 */
    @Test
    public void testIsIdleNoTileEntity() {
        World emptyWorld = mock(World.class);
        assertThat(handler.isIdle(emptyWorld, POS, null, null)).isTrue();
    }

    /** 仍有输入材料未消耗时 hasFinished 为 false。 */
    @Test
    public void testHasFinishedWithRemainingInputs() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        itemHandler.setStack(0, new ItemStack(Items.COAL, 1));
        wireCapability(itemHandler);

        List<ItemStack> inputs = Collections.singletonList(new ItemStack(Items.COAL, 1));
        assertThat(handler.hasFinished(world, POS, inputs, null)).isFalse();
    }

    /** 输入耗尽但仍有产物残留时 hasFinished 为 false。 */
    @Test
    public void testHasFinishedWithRemainingProducts() {
        FakeItemHandler itemHandler = new FakeItemHandler(1);
        itemHandler.setStack(0, new ItemStack(Items.APPLE, 1));
        wireCapability(itemHandler);

        assertThat(handler.hasFinished(world, POS, Collections.emptyList(), null)).isFalse();
    }

    /** 输入耗尽且无产物残留时 hasFinished 为 true；无 TileEntity 也为 true。 */
    @Test
    public void testHasFinishedWhenDrained() {
        FakeItemHandler itemHandler = new FakeItemHandler(2);
        wireCapability(itemHandler);

        assertThat(handler.hasFinished(world, POS, Collections.emptyList(), null)).isTrue();

        World emptyWorld = mock(World.class);
        assertThat(handler.hasFinished(emptyWorld, POS, null, null)).isTrue();
    }
}
