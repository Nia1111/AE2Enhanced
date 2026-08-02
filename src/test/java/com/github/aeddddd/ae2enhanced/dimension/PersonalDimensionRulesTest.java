package com.github.aeddddd.ae2enhanced.dimension;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

/**
 * {@link PersonalDimensionRules} 的默认值、NBT 序列化与 copy() 契约测试。
 */
public class PersonalDimensionRulesTest {

    /** 字段默认值固化：日光循环默认开启、时间 6000、移动速度 0.1f，其余关闭/为 0。 */
    @Test
    public void testDefaultValues() {
        PersonalDimensionRules rules = new PersonalDimensionRules();

        assertThat(rules.disableMobSpawning).isFalse();
        assertThat(rules.lockWeather).isFalse();
        assertThat(rules.lockTime).isFalse();
        assertThat(rules.daylightCycle).isTrue();
        assertThat(rules.timeValue).isEqualTo(6000L);
        assertThat(rules.flightEnabled).isFalse();
        assertThat(rules.movementSpeed).isEqualTo(0.1f);
        assertThat(rules.noFlightInertia).isFalse();
    }

    /** 修改全部字段后 NBT 往返，读回的对象逐字段相等。 */
    @Test
    public void testNbtRoundTripAllFieldsModified() {
        PersonalDimensionRules original = new PersonalDimensionRules();
        original.disableMobSpawning = true;
        original.lockWeather = true;
        original.lockTime = true;
        original.daylightCycle = false;
        original.timeValue = 18000L;
        original.flightEnabled = true;
        original.movementSpeed = 0.25f;
        original.noFlightInertia = true;

        PersonalDimensionRules restored = new PersonalDimensionRules();
        restored.readFromNBT(original.writeToNBT());

        assertThat(restored.disableMobSpawning).isTrue();
        assertThat(restored.lockWeather).isTrue();
        assertThat(restored.lockTime).isTrue();
        assertThat(restored.daylightCycle).isFalse();
        assertThat(restored.timeValue).isEqualTo(18000L);
        assertThat(restored.flightEnabled).isTrue();
        assertThat(restored.movementSpeed).isEqualTo(0.25f);
        assertThat(restored.noFlightInertia).isTrue();
    }

    /** 默认值的 NBT 往返保持默认（daylightCycle=true 会被显式写入，不会丢失）。 */
    @Test
    public void testNbtRoundTripDefaults() {
        PersonalDimensionRules restored = new PersonalDimensionRules();
        restored.readFromNBT(new PersonalDimensionRules().writeToNBT());

        assertThat(restored.daylightCycle).isTrue();
        assertThat(restored.timeValue).isEqualTo(6000L);
        assertThat(restored.movementSpeed).isEqualTo(0.1f);
    }

    /**
     * 行为固化：readFromNBT 传入空 NBTTagCompound 时，所有字段变为 NBT 缺省值
     * （boolean=false、long=0、float=0），即 daylightCycle 会从默认 true 变为 false。
     * 注意：这只是当前实现的行为记录。实际读档时传入的都是 writeToNBT 产出的完整 tag，
     * 空 tag 路径在正常流程中不会出现。
     */
    @Test
    public void testReadFromEmptyTagYieldsNbtDefaults() {
        PersonalDimensionRules rules = new PersonalDimensionRules();
        rules.readFromNBT(new NBTTagCompound());

        assertThat(rules.disableMobSpawning).isFalse();
        assertThat(rules.lockWeather).isFalse();
        assertThat(rules.lockTime).isFalse();
        // 关键行为：默认 true 被 NBT 缺省值 false 覆盖
        assertThat(rules.daylightCycle).isFalse();
        assertThat(rules.timeValue).isEqualTo(0L);
        assertThat(rules.flightEnabled).isFalse();
        assertThat(rules.movementSpeed).isEqualTo(0.0f);
        assertThat(rules.noFlightInertia).isFalse();
    }

    /** copy() 产出逐字段相等的副本。 */
    @Test
    public void testCopyHasSameValues() {
        PersonalDimensionRules original = new PersonalDimensionRules();
        original.lockTime = true;
        original.timeValue = 12345L;
        original.flightEnabled = true;
        original.movementSpeed = 0.5f;

        PersonalDimensionRules copy = original.copy();

        assertThat(copy).isNotSameAs(original);
        assertThat(copy.disableMobSpawning).isEqualTo(original.disableMobSpawning);
        assertThat(copy.lockWeather).isEqualTo(original.lockWeather);
        assertThat(copy.lockTime).isEqualTo(original.lockTime);
        assertThat(copy.daylightCycle).isEqualTo(original.daylightCycle);
        assertThat(copy.timeValue).isEqualTo(original.timeValue);
        assertThat(copy.flightEnabled).isEqualTo(original.flightEnabled);
        assertThat(copy.movementSpeed).isEqualTo(original.movementSpeed);
        assertThat(copy.noFlightInertia).isEqualTo(original.noFlightInertia);
    }

    /** copy() 深拷贝独立性：修改副本的每个字段都不影响原对象。 */
    @Test
    public void testCopyIsIndependent() {
        PersonalDimensionRules original = new PersonalDimensionRules();
        PersonalDimensionRules copy = original.copy();

        copy.disableMobSpawning = true;
        copy.lockWeather = true;
        copy.lockTime = true;
        copy.daylightCycle = false;
        copy.timeValue = 999L;
        copy.flightEnabled = true;
        copy.movementSpeed = 0.9f;
        copy.noFlightInertia = true;

        assertThat(original.disableMobSpawning).isFalse();
        assertThat(original.lockWeather).isFalse();
        assertThat(original.lockTime).isFalse();
        assertThat(original.daylightCycle).isTrue();
        assertThat(original.timeValue).isEqualTo(6000L);
        assertThat(original.flightEnabled).isFalse();
        assertThat(original.movementSpeed).isEqualTo(0.1f);
        assertThat(original.noFlightInertia).isFalse();

        // 反向同样成立：修改原对象不影响副本
        original.timeValue = 1L;
        assertThat(copy.timeValue).isEqualTo(999L);
    }
}
