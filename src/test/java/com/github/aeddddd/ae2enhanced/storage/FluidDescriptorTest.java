package com.github.aeddddd.ae2enhanced.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.github.aeddddd.ae2enhanced.test.util.AE2TestBootstrap;

/**
 * {@link FluidDescriptor} 单元测试。
 *
 * <p>覆盖 equals/hashCode 契约（重点：NBT 参与 equals 但不参与 hashCode）、
 * 构造器与 fromRaw 的 NBT 防御性拷贝、toNBT/fromNBT 往返（含/不含 NBT、
 * 未知流体、空 id）、toFluidStack 还原以及 null 流体防御分支。</p>
 *
 * <p>使用 FluidRegistry 默认注册的 water/lava。</p>
 */
public class FluidDescriptorTest {

    @BeforeAll
    public static void boot() {
        // 涉及流体注册表，按约定执行无头引导
        AE2TestBootstrap.boot();
    }

    /** 构造带 NBT 的 FluidStack。 */
    private static FluidStack fluidWithTag(String fluidName, String key, String value) {
        FluidStack stack = new FluidStack(FluidRegistry.getFluid(fluidName), 100);
        stack.tag = new NBTTagCompound();
        stack.tag.setString(key, value);
        return stack;
    }

    /** 相同流体（均无 NBT）的两个 descriptor 相等且 hashCode 相同。 */
    @Test
    public void testEqualsSameFluid() {
        FluidDescriptor a = new FluidDescriptor(new FluidStack(FluidRegistry.WATER, 100));
        FluidDescriptor b = new FluidDescriptor(new FluidStack(FluidRegistry.WATER, 500));

        // 数量不参与身份判定
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("water");
    }

    /** 不同流体不相等。 */
    @Test
    public void testNotEqualsDifferentFluid() {
        FluidDescriptor water = new FluidDescriptor(new FluidStack(FluidRegistry.WATER, 100));
        FluidDescriptor lava = new FluidDescriptor(new FluidStack(FluidRegistry.LAVA, 100));

        assertThat(water).isNotEqualTo(lava);
    }

    /** NBT 参与 equals：内容相同（引用不同）相等，内容不同/有无不一致不相等。 */
    @Test
    public void testNbtParticipatesInEquals() {
        FluidDescriptor a = new FluidDescriptor(fluidWithTag("water", "k", "v"));
        FluidDescriptor b = new FluidDescriptor(fluidWithTag("water", "k", "v"));
        FluidDescriptor c = new FluidDescriptor(fluidWithTag("water", "k", "other"));
        FluidDescriptor noTag = new FluidDescriptor(new FluidStack(FluidRegistry.WATER, 100));

        assertThat(a.getNbt()).isNotSameAs(b.getNbt());
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(noTag);
        assertThat(noTag).isNotEqualTo(a);
    }

    /**
     * NBT 不参与 hashCode：同流体不同 NBT 的 descriptor equals 为 false，
     * 但 hashCode 相同（源码注释声明的契约，允许碰撞，由 equals 进一步区分）。
     */
    @Test
    public void testNbtExcludedFromHashCode() {
        FluidDescriptor a = new FluidDescriptor(fluidWithTag("water", "k", "a"));
        FluidDescriptor b = new FluidDescriptor(fluidWithTag("water", "k", "b"));
        FluidDescriptor noTag = new FluidDescriptor(new FluidStack(FluidRegistry.WATER, 100));

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.hashCode()).isEqualTo(noTag.hashCode());
    }

    /** 构造器对 NBT 做防御性拷贝：构造后修改原 FluidStack.tag 不影响 descriptor。 */
    @Test
    public void testConstructorDefensiveNbtCopy() {
        FluidStack stack = fluidWithTag("water", "k", "original");
        FluidDescriptor descriptor = new FluidDescriptor(stack);

        stack.tag.setString("k", "mutated");
        stack.tag.setString("extra", "x");

        assertThat(descriptor.getNbt().getString("k")).isEqualTo("original");
        assertThat(descriptor.getNbt().hasKey("extra")).isFalse();
    }

    /** getNbt 返回副本：修改返回值不影响内部状态，equals/hashCode 契约不被外部破坏。 */
    @Test
    public void testGetNbtReturnsCopy() {
        FluidDescriptor descriptor = new FluidDescriptor(fluidWithTag("water", "k", "original"));
        FluidDescriptor expected = new FluidDescriptor(fluidWithTag("water", "k", "original"));
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

    /** toNBT/fromNBT 往返（含 NBT 分支）。 */
    @Test
    public void testNbtRoundTripWithTag() {
        FluidDescriptor original = new FluidDescriptor(fluidWithTag("water", "k", "v"));

        NBTTagCompound nbt = original.toNBT();
        assertThat(nbt.getString("id")).isEqualTo("water");
        assertThat(nbt.hasKey("tag", 10)).isTrue();

        FluidDescriptor restored = FluidDescriptor.fromNBT(nbt);
        assertThat(restored).isEqualTo(original);
    }

    /** toNBT/fromNBT 往返（无 NBT 分支）。 */
    @Test
    public void testNbtRoundTripWithoutTag() {
        FluidDescriptor original = new FluidDescriptor(new FluidStack(FluidRegistry.LAVA, 100));

        NBTTagCompound nbt = original.toNBT();
        assertThat(nbt.getString("id")).isEqualTo("lava");
        assertThat(nbt.hasKey("tag", 10)).isFalse();

        FluidDescriptor restored = FluidDescriptor.fromNBT(nbt);
        assertThat(restored).isEqualTo(original);
        assertThat(restored.getNbt()).isNull();
    }

    /** 未知流体 id / 空 id 时 fromNBT 返回 null。 */
    @Test
    public void testFromNbtUnknownOrEmptyIdReturnsNull() {
        NBTTagCompound unknown = new NBTTagCompound();
        unknown.setString("id", "no_such_fluid_zzz");
        assertThat(FluidDescriptor.fromNBT(unknown)).isNull();

        assertThat(FluidDescriptor.fromNBT(new NBTTagCompound())).isNull();
    }

    /** toFluidStack 还原流体与 NBT（数量恒为 1）；还原结果的 tag 是副本。 */
    @Test
    public void testToFluidStackRestores() {
        FluidDescriptor withTag = new FluidDescriptor(fluidWithTag("water", "k", "v"));

        FluidStack restored = withTag.toFluidStack();
        assertThat(restored.getFluid()).isEqualTo(FluidRegistry.WATER);
        assertThat(restored.amount).isEqualTo(1);
        assertThat(restored.tag.getString("k")).isEqualTo("v");

        // 副本语义：修改还原结果的 tag 不影响 descriptor
        restored.tag.setString("k", "mutated");
        assertThat(withTag.getNbt().getString("k")).isEqualTo("v");

        // 无 NBT 分支
        FluidDescriptor noTag = new FluidDescriptor(new FluidStack(FluidRegistry.WATER, 100));
        assertThat(noTag.toFluidStack().tag).isNull();
    }

    /** null 流体防御分支：toFluidStack 返回 null；两个 null 流体 descriptor 彼此相等。 */
    @Test
    public void testNullFluid() {
        FluidDescriptor nullFluidA = FluidDescriptor.fromRaw(null, null);
        FluidDescriptor nullFluidB = FluidDescriptor.fromRaw(null, new NBTTagCompound());

        assertThat(nullFluidA.getFluid()).isNull();
        assertThat(nullFluidA.toFluidStack()).isNull();

        // fluid 引用相等（同为 null），NBT 语义与普通分支一致
        assertThat(nullFluidA).isEqualTo(nullFluidA);
        assertThat(nullFluidA).isNotEqualTo(nullFluidB); // nbt null vs 空 compound
    }

    /** null 流体序列化为空 id，fromNBT 读回 null：null->null 往返保真（不再回退为 water）。 */
    @Test
    public void testNullFluidNbtRoundTrip() {
        FluidDescriptor nullFluid = FluidDescriptor.fromRaw(null, null);

        NBTTagCompound nbt = nullFluid.toNBT();
        assertThat(nbt.getString("id")).isEmpty();

        assertThat(FluidDescriptor.fromNBT(nbt)).isNull();
    }
}
