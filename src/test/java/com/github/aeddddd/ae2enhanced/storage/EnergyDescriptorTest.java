package com.github.aeddddd.ae2enhanced.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

/**
 * {@link EnergyDescriptor} 单例描述符测试。
 *
 * <p>覆盖单例语义（fromNBT 恒返回 INSTANCE）、toNBT/fromNBT 往返、
 * 基于 instanceof 的 equals（任意两实例相等）与固定 hashCode。</p>
 *
 * <p>不涉及 ItemStack/注册表，无需无头引导。</p>
 */
public class EnergyDescriptorTest {

    /** fromNBT 忽略内容，恒返回单例 INSTANCE。 */
    @Test
    public void testFromNbtAlwaysReturnsSingleton() {
        assertThat(EnergyDescriptor.fromNBT(new NBTTagCompound())).isSameAs(EnergyDescriptor.INSTANCE);

        NBTTagCompound junk = new NBTTagCompound();
        junk.setString("whatever", "ignored");
        assertThat(EnergyDescriptor.fromNBT(junk)).isSameAs(EnergyDescriptor.INSTANCE);
    }

    /** toNBT 产出空 compound；toNBT → fromNBT 往返回到单例。 */
    @Test
    public void testNbtRoundTrip() {
        NBTTagCompound nbt = EnergyDescriptor.INSTANCE.toNBT();

        assertThat(nbt.isEmpty()).isTrue();
        assertThat(EnergyDescriptor.fromNBT(nbt)).isSameAs(EnergyDescriptor.INSTANCE);
    }

    /** equals 基于 instanceof：通过反射创建的第二实例与 INSTANCE 相互相等。 */
    @Test
    public void testEqualsAnyTwoInstances() throws Exception {
        Constructor<EnergyDescriptor> ctor = EnergyDescriptor.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        EnergyDescriptor second = ctor.newInstance();

        assertThat(EnergyDescriptor.INSTANCE).isEqualTo(second);
        assertThat(second).isEqualTo(EnergyDescriptor.INSTANCE);
        assertThat(EnergyDescriptor.INSTANCE).isEqualTo(EnergyDescriptor.INSTANCE);

        // null 与其它类型不相等
        assertThat(EnergyDescriptor.INSTANCE).isNotEqualTo(null);
        assertThat(EnergyDescriptor.INSTANCE).isNotEqualTo("energy");
    }

    /** hashCode 为固定常量，任意实例相同（满足 equals/hashCode 契约）。 */
    @Test
    public void testFixedHashCode() throws Exception {
        Constructor<EnergyDescriptor> ctor = EnergyDescriptor.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        assertThat(EnergyDescriptor.INSTANCE.hashCode()).isEqualTo(0x45E2E2);
        assertThat(ctor.newInstance().hashCode()).isEqualTo(EnergyDescriptor.INSTANCE.hashCode());
    }
}
