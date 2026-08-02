package com.github.aeddddd.ae2enhanced.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link ItemDescriptor} 单元测试。
 *
 * <p>覆盖 equals/hashCode 契约（item+meta+NBT 内容语义）、构造器与工厂方法的
 * NBT 防御性拷贝、toNBT/fromNBT 往返（含/不含 NBT、未知物品 id、空 id）、
 * toItemStack 还原以及 getModId 的正常与回退分支。</p>
 */
public class ItemDescriptorTest {

    @BeforeAll
    public static void boot() {
        // 用例中构造 ItemStack，需无头引导初始化物品注册表
        AE2TestBootstrap.boot();
    }

    /** 构造带单个字符串 NBT 标签的 ItemStack。 */
    private static ItemStack stackWithTag(Item item, int meta, String key, String value) {
        ItemStack stack = new ItemStack(item, 1, meta);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(key, value);
        stack.setTagCompound(tag);
        return stack;
    }

    /** 相同 item+meta（均无 NBT）的两个 descriptor 相等且 hashCode 相同。 */
    @Test
    public void testEqualsSameItemAndMeta() {
        ItemDescriptor a = new ItemDescriptor(new ItemStack(Items.APPLE, 1, 0));
        ItemDescriptor b = new ItemDescriptor(new ItemStack(Items.APPLE, 1, 0));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isEqualTo(a); // 自反
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a descriptor");
    }

    /** NBT 参与 equals：内容相同（引用不同）的 NBT 仍判定相等，且 hashCode 一致。 */
    @Test
    public void testEqualsWithEqualNbtContent() {
        ItemDescriptor a = new ItemDescriptor(stackWithTag(Items.APPLE, 0, "k", "v"));
        ItemDescriptor b = new ItemDescriptor(stackWithTag(Items.APPLE, 0, "k", "v"));

        assertThat(a.getNbt()).isNotSameAs(b.getNbt());
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    /** meta 不同则不相等。 */
    @Test
    public void testNotEqualsDifferentMeta() {
        ItemDescriptor lapis = new ItemDescriptor(new ItemStack(Items.DYE, 1, 4));
        ItemDescriptor roseRed = new ItemDescriptor(new ItemStack(Items.DYE, 1, 1));

        assertThat(lapis).isNotEqualTo(roseRed);
    }

    /** NBT 内容不同则不相等；有 NBT 与无 NBT 也不相等。 */
    @Test
    public void testNotEqualsDifferentNbt() {
        ItemDescriptor withTagA = new ItemDescriptor(stackWithTag(Items.APPLE, 0, "k", "a"));
        ItemDescriptor withTagB = new ItemDescriptor(stackWithTag(Items.APPLE, 0, "k", "b"));
        ItemDescriptor noTag = new ItemDescriptor(new ItemStack(Items.APPLE, 1, 0));

        assertThat(withTagA).isNotEqualTo(withTagB);
        assertThat(withTagA).isNotEqualTo(noTag);
        assertThat(noTag).isNotEqualTo(withTagA);
    }

    /** 构造器对 NBT 做防御性拷贝：构造后修改原 ItemStack 的 NBT 不影响 descriptor。 */
    @Test
    public void testConstructorDefensiveNbtCopy() {
        ItemStack stack = stackWithTag(Items.APPLE, 0, "k", "original");
        ItemDescriptor descriptor = new ItemDescriptor(stack);

        // 修改原 stack 的 NBT，descriptor 应保持构造时的快照
        stack.getTagCompound().setString("k", "mutated");
        stack.getTagCompound().setString("extra", "x");

        ItemDescriptor expected = new ItemDescriptor(stackWithTag(Items.APPLE, 0, "k", "original"));
        assertThat(descriptor).isEqualTo(expected);
        assertThat(descriptor.getNbt().getString("k")).isEqualTo("original");
        assertThat(descriptor.getNbt().hasKey("extra")).isFalse();
    }

    /** getNbt 返回副本：修改返回值不影响内部状态，equals/hashCode 契约不被外部破坏。 */
    @Test
    public void testGetNbtReturnsCopy() {
        ItemDescriptor descriptor = new ItemDescriptor(stackWithTag(Items.APPLE, 0, "k", "original"));
        ItemDescriptor expected = new ItemDescriptor(stackWithTag(Items.APPLE, 0, "k", "original"));
        int hashBefore = descriptor.hashCode();

        // 每次调用返回不同实例
        assertThat(descriptor.getNbt()).isNotSameAs(descriptor.getNbt());

        // 修改返回的副本，descriptor 内部状态与哈希不变
        NBTTagCompound leaked = descriptor.getNbt();
        leaked.setString("k", "mutated");
        leaked.setString("extra", "x");

        assertThat(descriptor.getNbt().getString("k")).isEqualTo("original");
        assertThat(descriptor.getNbt().hasKey("extra")).isFalse();
        assertThat(descriptor).isEqualTo(expected);
        assertThat(descriptor.hashCode()).isEqualTo(hashBefore);
    }

    /** toItemStack 还原 item/meta/NBT；还原出的 NBT 是副本，修改不影响 descriptor。 */
    @Test
    public void testToItemStackRestores() {
        ItemDescriptor withTag = new ItemDescriptor(stackWithTag(Items.DYE, 4, "k", "v"));

        ItemStack restored = withTag.toItemStack();
        assertThat(restored.getItem()).isEqualTo(Items.DYE);
        assertThat(restored.getMetadata()).isEqualTo(4);
        assertThat(restored.getCount()).isEqualTo(1);
        assertThat(restored.getTagCompound().getString("k")).isEqualTo("v");

        // 副本语义：修改还原结果的 NBT 不影响 descriptor
        restored.getTagCompound().setString("k", "mutated");
        assertThat(withTag.getNbt().getString("k")).isEqualTo("v");

        // 无 NBT 分支：还原结果不带 tag
        ItemDescriptor noTag = new ItemDescriptor(new ItemStack(Items.APPLE, 1, 2));
        ItemStack restoredNoTag = noTag.toItemStack();
        assertThat(restoredNoTag.getMetadata()).isEqualTo(2);
        assertThat(restoredNoTag.hasTagCompound()).isFalse();
    }

    /** toNBT/fromNBT 往返（含 NBT 分支）。 */
    @Test
    public void testNbtRoundTripWithTag() {
        ItemDescriptor original = new ItemDescriptor(stackWithTag(Items.DYE, 4, "k", "v"));

        NBTTagCompound nbt = original.toNBT();
        assertThat(nbt.getString("id")).isEqualTo("minecraft:dye");
        assertThat(nbt.getShort("Damage")).isEqualTo((short) 4);
        assertThat(nbt.hasKey("tag", 10)).isTrue();

        ItemDescriptor restored = ItemDescriptor.fromNBT(nbt);
        assertThat(restored).isEqualTo(original);
        assertThat(restored.hashCode()).isEqualTo(original.hashCode());
    }

    /** toNBT/fromNBT 往返（无 NBT 分支，不写 "tag" 键）。 */
    @Test
    public void testNbtRoundTripWithoutTag() {
        ItemDescriptor original = new ItemDescriptor(new ItemStack(Items.APPLE, 1, 0));

        NBTTagCompound nbt = original.toNBT();
        assertThat(nbt.hasKey("tag", 10)).isFalse();

        ItemDescriptor restored = ItemDescriptor.fromNBT(nbt);
        assertThat(restored).isEqualTo(original);
        assertThat(restored.getNbt()).isNull();
    }

    /**
     * 未知物品 id 时 fromNBT 返回 null。
     * 已验证 Forge 1.12.2 中 Item.REGISTRY 为普通 NamespacedWrapper（非 Defaulted），
     * getObject 对未注册 id 委托 ForgeRegistry.getValue 返回 null，故该分支可达。
     */
    @Test
    public void testFromNbtUnknownIdReturnsNull() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("id", "minecraft:no_such_item_zzz");
        nbt.setShort("Damage", (short) 0);

        assertThat(ItemDescriptor.fromNBT(nbt)).isNull();
    }

    /** id 缺失（空字符串）时 fromNBT 返回 null。 */
    @Test
    public void testFromNbtEmptyIdReturnsNull() {
        assertThat(ItemDescriptor.fromNBT(new NBTTagCompound())).isNull();
    }

    /** getModId 取 registryName 的命名空间；未注册物品回退为 "unknown"。 */
    @Test
    public void testGetModId() {
        ItemDescriptor apple = new ItemDescriptor(new ItemStack(Items.APPLE, 1, 0));
        assertThat(apple.getModId()).isEqualTo("minecraft");

        // 未注册的 Item 没有 registryName，回退 "unknown"；重复调用走缓存路径
        ItemDescriptor unregistered = new ItemDescriptor(new ItemStack(new Item(), 1, 0));
        assertThat(unregistered.getModId()).isEqualTo("unknown");
        assertThat(unregistered.getModId()).isEqualTo("unknown");
    }

    /** fromRaw 工厂方法：对传入 NBT 做防御性拷贝。 */
    @Test
    public void testFromRawCopiesNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("k", "v");
        ItemDescriptor descriptor = ItemDescriptor.fromRaw(Items.APPLE, 1, tag);

        tag.setString("k", "mutated");
        assertThat(descriptor.getNbt().getString("k")).isEqualTo("v");
        assertThat(descriptor.getMeta()).isEqualTo(1);

        // null NBT 分支
        ItemDescriptor noTag = ItemDescriptor.fromRaw(Items.APPLE, 0, null);
        assertThat(noTag.getNbt()).isNull();
    }
}
