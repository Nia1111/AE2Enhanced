package com.github.aeddddd.ae2enhanced.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link OmniTerminalStorage} 单终端持久化数据测试。
 *
 * <p>覆盖六个物品栏的尺寸常量、全量 writeToNBT/readFromNBT 往返
 * （含带 NBT 的物品与超过 byte 上限的大数量堆叠）、缺失键时的防御读取，
 * 以及 dirty 标记语义。</p>
 */
public class OmniTerminalStorageTest {

    private OmniTerminalStorage storage;

    @BeforeAll
    public static void boot() {
        // 用例中构造 ItemStack，需无头引导初始化物品注册表
        AE2TestBootstrap.boot();
    }

    @BeforeEach
    public void setUp() {
        this.storage = new OmniTerminalStorage();
    }

    /** 断言两个 ItemStack 的 item/count/meta/NBT 一致。 */
    private static void assertStackEquals(ItemStack actual, ItemStack expected) {
        assertThat(actual.getItem()).isEqualTo(expected.getItem());
        assertThat(actual.getCount()).isEqualTo(expected.getCount());
        assertThat(actual.getMetadata()).isEqualTo(expected.getMetadata());
        assertThat(actual.getTagCompound()).isEqualTo(expected.getTagCompound());
    }

    /** 各物品栏尺寸与 SIZE_* 常量一致。 */
    @Test
    public void testInventorySizes() {
        assertThat(storage.getCraftingInventory().getSlots()).isEqualTo(OmniTerminalStorage.SIZE_CRAFTING);
        assertThat(storage.getPatternInputInventory().getSlots()).isEqualTo(OmniTerminalStorage.SIZE_PATTERN_INPUTS);
        assertThat(storage.getPatternOutputInventory().getSlots()).isEqualTo(OmniTerminalStorage.SIZE_PATTERN_OUTPUTS);
        assertThat(storage.getRightStorageInventory().getSlots()).isEqualTo(OmniTerminalStorage.SIZE_RIGHT_STORAGE);
        assertThat(storage.getUpgradeInventory().getSlots()).isEqualTo(OmniTerminalStorage.SIZE_UPGRADE);
        assertThat(storage.getPatternInventory().getSlots()).isEqualTo(OmniTerminalStorage.SIZE_PATTERN);
    }

    /** 全部六个物品栏的 NBT 往返：含带 NBT 的物品与超过 127 的大数量堆叠。 */
    @Test
    public void testNbtRoundTripAllInventories() {
        ItemStack craftingStack = new ItemStack(Items.APPLE, 5);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("k", "v");
        craftingStack.setTagCompound(tag);
        // 大数量：Count 以 Integer 存储，不受 byte 上限影响
        ItemStack bigStack = new ItemStack(Items.DYE, 300, 4);
        ItemStack patternOut = new ItemStack(Items.STICK, 16);
        ItemStack rightStack = new ItemStack(Items.WHEAT, 64);
        ItemStack upgrade = new ItemStack(Items.APPLE, 1);
        ItemStack pattern = new ItemStack(Items.DIAMOND, 1);

        storage.getCraftingInventory().setStackInSlot(3, craftingStack);
        storage.getPatternInputInventory().setStackInSlot(80, bigStack);
        storage.getPatternOutputInventory().setStackInSlot(10, patternOut);
        storage.getRightStorageInventory().setStackInSlot(35, rightStack);
        storage.getUpgradeInventory().setStackInSlot(0, upgrade);
        storage.getPatternInventory().setStackInSlot(1, pattern);

        NBTTagCompound nbt = storage.writeToNBT(new NBTTagCompound());

        OmniTerminalStorage restored = new OmniTerminalStorage();
        restored.readFromNBT(nbt);

        assertStackEquals(restored.getCraftingInventory().getStackInSlot(3), craftingStack);
        assertStackEquals(restored.getPatternInputInventory().getStackInSlot(80), bigStack);
        assertStackEquals(restored.getPatternOutputInventory().getStackInSlot(10), patternOut);
        assertStackEquals(restored.getRightStorageInventory().getStackInSlot(35), rightStack);
        assertStackEquals(restored.getUpgradeInventory().getStackInSlot(0), upgrade);
        assertStackEquals(restored.getPatternInventory().getStackInSlot(1), pattern);

        // 未写入的槽位保持为空
        assertThat(restored.getCraftingInventory().getStackInSlot(0).isEmpty()).isTrue();
        assertThat(restored.getPatternInputInventory().getStackInSlot(0).isEmpty()).isTrue();
    }

    /** 防御分支：读取缺失全部键的 NBT 时不抛异常，物品栏保持为空。 */
    @Test
    public void testReadFromNbtMissingKeysKeepsEmpty() {
        storage.readFromNBT(new NBTTagCompound());

        for (int i = 0; i < storage.getCraftingInventory().getSlots(); i++) {
            assertThat(storage.getCraftingInventory().getStackInSlot(i).isEmpty()).isTrue();
        }
        assertThat(storage.getPatternInputInventory().getStackInSlot(80).isEmpty()).isTrue();
        assertThat(storage.getRightStorageInventory().getStackInSlot(35).isEmpty()).isTrue();
        assertThat(storage.getUpgradeInventory().getStackInSlot(0).isEmpty()).isTrue();
        assertThat(storage.getPatternInventory().getStackInSlot(1).isEmpty()).isTrue();
    }

    /** dirty 标记：初始为 false，markDirty 置位，clearDirty 复位。 */
    @Test
    public void testDirtyFlag() {
        assertThat(storage.isDirty()).isFalse();

        storage.markDirty();
        assertThat(storage.isDirty()).isTrue();

        storage.clearDirty();
        assertThat(storage.isDirty()).isFalse();
    }
}
