package com.github.aeddddd.ae2enhanced.centralinterface;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * {@link TargetBinding} 的 NBT 序列化与 equals/hashCode 契约测试。
 */
public class TargetBindingTest {

    private static TargetBinding sample() {
        return new TargetBinding(new BlockPos(10, 64, -20), 3, "minecraft:furnace");
    }

    /** NBT 往返一致：写出的 tag 读回后与原始 binding 相等，三要素逐一相同。 */
    @Test
    public void testNbtRoundTrip() {
        TargetBinding original = sample();
        NBTTagCompound tag = original.writeToNBT();

        TargetBinding restored = TargetBinding.readFromNBT(tag);

        assertThat(restored).isEqualTo(original);
        assertThat(restored).isNotSameAs(original);
        assertThat(restored.pos).isEqualTo(original.pos);
        assertThat(restored.dimension).isEqualTo(original.dimension);
        assertThat(restored.blockId).isEqualTo(original.blockId);
    }

    /** 负坐标与负维度（如下界 -1）也能正确往返。 */
    @Test
    public void testNbtRoundTripNegativeValues() {
        TargetBinding original = new TargetBinding(new BlockPos(-12345, 5, 6789), -1, "modid:machine");
        TargetBinding restored = TargetBinding.readFromNBT(original.writeToNBT());
        assertThat(restored).isEqualTo(original);
        assertThat(restored.pos).isEqualTo(original.pos);
    }

    /** 三要素全部相同（含 blockId 均为 null）时相等，hashCode 一致。 */
    @Test
    public void testEqualsSameValues() {
        TargetBinding a = sample();
        TargetBinding b = sample();
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        TargetBinding c = new TargetBinding(new BlockPos(1, 2, 3), 0, null);
        TargetBinding d = new TargetBinding(new BlockPos(1, 2, 3), 0, null);
        assertThat(c).isEqualTo(d);
        assertThat(c.hashCode()).isEqualTo(d.hashCode());
    }

    /** pos 不同则不等。 */
    @Test
    public void testNotEqualsDifferentPos() {
        TargetBinding a = sample();
        TargetBinding b = new TargetBinding(new BlockPos(11, 64, -20), 3, "minecraft:furnace");
        assertThat(a).isNotEqualTo(b);
    }

    /** dimension 不同则不等。 */
    @Test
    public void testNotEqualsDifferentDimension() {
        TargetBinding a = sample();
        TargetBinding b = new TargetBinding(new BlockPos(10, 64, -20), 4, "minecraft:furnace");
        assertThat(a).isNotEqualTo(b);
    }

    /** blockId 不同则不等；null 与非 null 也不等。 */
    @Test
    public void testNotEqualsDifferentBlockId() {
        TargetBinding a = sample();
        TargetBinding b = new TargetBinding(new BlockPos(10, 64, -20), 3, "minecraft:chest");
        assertThat(a).isNotEqualTo(b);

        TargetBinding nullId = new TargetBinding(new BlockPos(10, 64, -20), 3, null);
        assertThat(a).isNotEqualTo(nullId);
        assertThat(nullId).isNotEqualTo(a);
    }

    /** 自反性与异类型比较。 */
    @Test
    public void testEqualsReflexiveAndOtherTypes() {
        TargetBinding a = sample();
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("minecraft:furnace");
        assertThat(a).isNotEqualTo(new BlockPos(10, 64, -20));
    }
}
