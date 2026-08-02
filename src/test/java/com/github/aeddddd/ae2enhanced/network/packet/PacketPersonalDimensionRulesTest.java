package com.github.aeddddd.ae2enhanced.network.packet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import com.github.aeddddd.ae2enhanced.dimension.PersonalDimensionRules;

/**
 * {@link PacketPersonalDimensionRules} 位标志打包序列化测试。
 *
 * <p>协议布局：1 字节位标志（6 个布尔，bit0~bit5）+ 8 字节 long timeValue
 * + 4 字节 float movementSpeed，共 13 字节。使用 Unpooled 堆缓冲区做
 * toBytes/fromBytes 往返，无需 MC 引导。</p>
 */
public class PacketPersonalDimensionRulesTest {

    /** 位标志位序（与源码 toBytes/fromBytes 保持一致）： */
    private static final int BIT_DISABLE_MOB_SPAWNING = 1;
    private static final int BIT_LOCK_WEATHER = 2;
    private static final int BIT_LOCK_TIME = 4;
    private static final int BIT_DAYLIGHT_CYCLE = 8;
    private static final int BIT_FLIGHT_ENABLED = 16;
    private static final int BIT_NO_FLIGHT_INERTIA = 32;

    /** 协议总长度：1(flags) + 8(long) + 4(float)。 */
    private static final int PAYLOAD_BYTES = 13;

    /** 构造指定规则字段的包。 */
    private static PacketPersonalDimensionRules packetOf(PersonalDimensionRules rules) {
        return new PacketPersonalDimensionRules(rules);
    }

    /** 序列化后返回缓冲区（读指针在 0）。 */
    private static ByteBuf write(PacketPersonalDimensionRules packet) {
        ByteBuf buf = Unpooled.buffer();
        packet.toBytes(buf);
        return buf;
    }

    /** 从缓冲区反序列化为新实例。 */
    private static PacketPersonalDimensionRules read(ByteBuf buf) {
        PacketPersonalDimensionRules packet = new PacketPersonalDimensionRules();
        packet.fromBytes(buf);
        return packet;
    }

    /** 断言两个包的全部字段一致。 */
    private static void assertSameRules(PacketPersonalDimensionRules expected,
                                        PacketPersonalDimensionRules actual) {
        assertThat(actual.isDisableMobSpawning()).isEqualTo(expected.isDisableMobSpawning());
        assertThat(actual.isLockWeather()).isEqualTo(expected.isLockWeather());
        assertThat(actual.isLockTime()).isEqualTo(expected.isLockTime());
        assertThat(actual.isDaylightCycle()).isEqualTo(expected.isDaylightCycle());
        assertThat(actual.isFlightEnabled()).isEqualTo(expected.isFlightEnabled());
        assertThat(actual.isNoFlightInertia()).isEqualTo(expected.isNoFlightInertia());
        assertThat(actual.getTimeValue()).isEqualTo(expected.getTimeValue());
        assertThat(actual.getMovementSpeed()).isEqualTo(expected.getMovementSpeed());
    }

    // ------------------------------------------------------------------
    // 协议布局
    // ------------------------------------------------------------------

    /** 全 false 时标志字节为 0，且总负载恰好 13 字节。 */
    @Test
    public void testAllFalseFlagsByteAndPayloadSize() {
        PersonalDimensionRules rules = new PersonalDimensionRules();
        rules.daylightCycle = false; // 默认 true，显式置 false

        ByteBuf buf = write(packetOf(rules));

        assertThat(buf.readableBytes()).isEqualTo(PAYLOAD_BYTES);
        assertThat(buf.getByte(0)).isEqualTo((byte) 0);
    }

    /** 全 true 时标志字节为 0x3F（低 6 位全部置位），往返后字段一致。 */
    @Test
    public void testAllTrueRoundTrip() {
        PersonalDimensionRules rules = new PersonalDimensionRules();
        rules.disableMobSpawning = true;
        rules.lockWeather = true;
        rules.lockTime = true;
        rules.daylightCycle = true;
        rules.flightEnabled = true;
        rules.noFlightInertia = true;

        PacketPersonalDimensionRules packet = packetOf(rules);
        ByteBuf buf = write(packet);

        assertThat(buf.getByte(0)).isEqualTo((byte) 0x3F);
        assertSameRules(packet, read(buf));
    }

    /** 默认值（仅 daylightCycle = true）往返：标志字节应只含 bit3。 */
    @Test
    public void testDefaultRulesRoundTrip() {
        PacketPersonalDimensionRules packet = packetOf(new PersonalDimensionRules());
        ByteBuf buf = write(packet);

        assertThat(buf.getByte(0)).isEqualTo((byte) BIT_DAYLIGHT_CYCLE);
        assertSameRules(packet, read(buf));
    }

    // ------------------------------------------------------------------
    // 单位置位：逐位验证位序
    // ------------------------------------------------------------------

    /** 每个布尔单独置位时，标志字节恰为对应的单位掩码，且往返后只有该位为 true。 */
    @Test
    public void testSingleBitPositions() {
        int[] masks = {
                BIT_DISABLE_MOB_SPAWNING, BIT_LOCK_WEATHER, BIT_LOCK_TIME,
                BIT_DAYLIGHT_CYCLE, BIT_FLIGHT_ENABLED, BIT_NO_FLIGHT_INERTIA
        };
        for (int i = 0; i < masks.length; i++) {
            PersonalDimensionRules rules = new PersonalDimensionRules();
            rules.daylightCycle = false; // 先清零，避免默认值干扰
            setFlag(rules, i, true);

            PacketPersonalDimensionRules packet = packetOf(rules);
            ByteBuf buf = write(packet);

            assertThat(buf.getByte(0)).as("第 %d 位的标志字节", i).isEqualTo((byte) masks[i]);

            PacketPersonalDimensionRules decoded = read(buf);
            for (int j = 0; j < masks.length; j++) {
                assertThat(getFlag(decoded, j)).as("置位第 %d 位时读取第 %d 位", i, j)
                        .isEqualTo(i == j);
            }
        }
    }

    /** 按位索引设置规则字段。 */
    private static void setFlag(PersonalDimensionRules rules, int index, boolean value) {
        switch (index) {
            case 0: rules.disableMobSpawning = value; break;
            case 1: rules.lockWeather = value; break;
            case 2: rules.lockTime = value; break;
            case 3: rules.daylightCycle = value; break;
            case 4: rules.flightEnabled = value; break;
            case 5: rules.noFlightInertia = value; break;
            default: throw new IllegalArgumentException("index: " + index);
        }
    }

    /** 按位索引读取包字段。 */
    private static boolean getFlag(PacketPersonalDimensionRules packet, int index) {
        switch (index) {
            case 0: return packet.isDisableMobSpawning();
            case 1: return packet.isLockWeather();
            case 2: return packet.isLockTime();
            case 3: return packet.isDaylightCycle();
            case 4: return packet.isFlightEnabled();
            case 5: return packet.isNoFlightInertia();
            default: throw new IllegalArgumentException("index: " + index);
        }
    }

    // ------------------------------------------------------------------
    // 组合抽查
    // ------------------------------------------------------------------

    /** 间隔置位组合（bit0 + bit2 + bit4 = 0b010101）往返一致。 */
    @Test
    public void testAlternatingBitCombination() {
        PersonalDimensionRules rules = new PersonalDimensionRules();
        rules.disableMobSpawning = true;
        rules.lockTime = true;
        rules.flightEnabled = true;
        rules.daylightCycle = false;

        PacketPersonalDimensionRules packet = packetOf(rules);
        ByteBuf buf = write(packet);

        assertThat(buf.getByte(0)).isEqualTo((byte) 0b010101);
        assertSameRules(packet, read(buf));
    }

    // ------------------------------------------------------------------
    // long / float 负载边界
    // ------------------------------------------------------------------

    /** timeValue 边界值（0、负数、Long.MAX_VALUE、Long.MIN_VALUE）往返一致。 */
    @Test
    public void testTimeValueBoundaries() {
        long[] values = { 0L, 6000L, -1L, Long.MAX_VALUE, Long.MIN_VALUE };
        for (long time : values) {
            PersonalDimensionRules rules = new PersonalDimensionRules();
            rules.timeValue = time;

            PacketPersonalDimensionRules packet = packetOf(rules);
            assertThat(read(write(packet)).getTimeValue()).isEqualTo(time);
        }
    }

    /** movementSpeed 典型值与特殊值（0、负数）往返一致。 */
    @Test
    public void testMovementSpeedValues() {
        float[] values = { 0.1f, 0f, -2.5f, 1.0f, Float.MAX_VALUE };
        for (float speed : values) {
            PersonalDimensionRules rules = new PersonalDimensionRules();
            rules.movementSpeed = speed;

            PacketPersonalDimensionRules packet = packetOf(rules);
            assertThat(read(write(packet)).getMovementSpeed()).isEqualTo(speed);
        }
    }

    /** 全字段混合值整体往返：布尔 + long + float 互不干扰。 */
    @Test
    public void testFullPayloadRoundTrip() {
        PersonalDimensionRules rules = new PersonalDimensionRules();
        rules.disableMobSpawning = true;
        rules.lockWeather = false;
        rules.lockTime = true;
        rules.daylightCycle = false;
        rules.flightEnabled = true;
        rules.noFlightInertia = false;
        rules.timeValue = 18000L;
        rules.movementSpeed = 0.35f;

        PacketPersonalDimensionRules packet = packetOf(rules);
        ByteBuf buf = write(packet);

        assertSameRules(packet, read(buf));
        // 全部字节被消费，无残留
        assertThat(buf.readableBytes()).isEqualTo(0);
    }
}
