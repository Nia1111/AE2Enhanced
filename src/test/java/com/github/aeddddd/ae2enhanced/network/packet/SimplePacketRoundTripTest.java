package com.github.aeddddd.ae2enhanced.network.packet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * 简单负载网络包的 toBytes/fromBytes 往返测试。
 *
 * <p>覆盖 {@link PacketOmniToolPlacementSubMode}（单布尔）、
 * {@link PacketSmartPatternToggle}（BlockPos long + int）、
 * {@link PacketPlacementSelectPreset}（单字节 slot）。
 * 仅测序列化层，不涉及 Handler 逻辑；负载均为基本类型，无需 MC 引导。</p>
 */
public class SimplePacketRoundTripTest {

    /** 序列化 → 反序列化，返回解码后的新实例。 */
    @SuppressWarnings("unchecked")
    private static <T extends IMessage> T roundTrip(T packet, Class<? extends IMessage> type) {
        ByteBuf buf = Unpooled.buffer();
        packet.toBytes(buf);
        try {
            T decoded = (T) type.newInstance();
            decoded.fromBytes(buf);
            return decoded;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("无法无参构造 " + type.getSimpleName(), e);
        }
    }

    /** 序列化后返回缓冲区，用于断言原始字节布局。 */
    private static ByteBuf write(IMessage packet) {
        ByteBuf buf = Unpooled.buffer();
        packet.toBytes(buf);
        return buf;
    }

    // ------------------------------------------------------------------
    // PacketOmniToolPlacementSubMode：单布尔
    // ------------------------------------------------------------------

    /** true/false 两个取值均可往返，负载恰好 1 字节。 */
    @Test
    public void testPlacementSubModeRoundTrip() {
        PacketOmniToolPlacementSubMode next = roundTrip(
                new PacketOmniToolPlacementSubMode(true), PacketOmniToolPlacementSubMode.class);
        assertThat(next.isNext()).isTrue();

        PacketOmniToolPlacementSubMode prev = roundTrip(
                new PacketOmniToolPlacementSubMode(false), PacketOmniToolPlacementSubMode.class);
        assertThat(prev.isNext()).isFalse();

        assertThat(write(new PacketOmniToolPlacementSubMode(true)).readableBytes()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // PacketSmartPatternToggle：BlockPos(long) + int
    // ------------------------------------------------------------------

    /** 普通坐标 + 正配方索引往返：getPos 解码回相同坐标，recipeIndex 一致。 */
    @Test
    public void testSmartPatternToggleRoundTrip() {
        BlockPos pos = new BlockPos(123, 64, -456);
        PacketSmartPatternToggle decoded = roundTrip(
                new PacketSmartPatternToggle(pos, 7), PacketSmartPatternToggle.class);

        assertThat(decoded.getPos()).isEqualTo(pos);
        assertThat(decoded.getRecipeIndex()).isEqualTo(7);
    }

    /** 边界值：原点坐标、负配方索引、Integer 极值索引均可往返。 */
    @Test
    public void testSmartPatternToggleBoundaryValues() {
        PacketSmartPatternToggle origin = roundTrip(
                new PacketSmartPatternToggle(BlockPos.ORIGIN, -1), PacketSmartPatternToggle.class);
        assertThat(origin.getPos()).isEqualTo(BlockPos.ORIGIN);
        assertThat(origin.getRecipeIndex()).isEqualTo(-1);

        PacketSmartPatternToggle extremes = roundTrip(
                new PacketSmartPatternToggle(new BlockPos(-30000000, 255, 30000000), Integer.MAX_VALUE),
                PacketSmartPatternToggle.class);
        assertThat(extremes.getPos()).isEqualTo(new BlockPos(-30000000, 255, 30000000));
        assertThat(extremes.getRecipeIndex()).isEqualTo(Integer.MAX_VALUE);
    }

    /** 负载布局：8 字节 long 坐标 + 4 字节 int 索引，共 12 字节。 */
    @Test
    public void testSmartPatternTogglePayloadSize() {
        assertThat(write(new PacketSmartPatternToggle(BlockPos.ORIGIN, 0)).readableBytes())
                .isEqualTo(12);
    }

    // ------------------------------------------------------------------
    // PacketPlacementSelectPreset：单字节 slot
    // ------------------------------------------------------------------

    /** 协议规定的取值（0~8 预设槽、9 准星目标、-2 清空选择）均可往返。 */
    @Test
    public void testPlacementSelectPresetProtocolValues() {
        int[] values = { 0, 1, 8, 9, -2 };
        for (int slot : values) {
            PacketPlacementSelectPreset decoded = roundTrip(
                    new PacketPlacementSelectPreset(slot), PacketPlacementSelectPreset.class);
            assertThat(decoded.getSlot()).isEqualTo(slot);
        }
    }

    /** 负载恰好 1 字节。 */
    @Test
    public void testPlacementSelectPresetPayloadSize() {
        assertThat(write(new PacketPlacementSelectPreset(0)).readableBytes()).isEqualTo(1);
    }

    /**
     * 固化行为：slot 字段为 int 但仅写入单字节，超出 byte 范围 [-128, 127]
     * 的值会被静默截断（如 200 读回为 -56）。协议规定取值仅 -2~9，
     * 正常使用不会触发；此处仅固化截断语义，防止误改字段类型。
     */
    @Test
    public void testPlacementSelectPresetByteTruncation() {
        PacketPlacementSelectPreset decoded = roundTrip(
                new PacketPlacementSelectPreset(200), PacketPlacementSelectPreset.class);
        assertThat(decoded.getSlot()).isEqualTo((byte) 200); // 200 截断为 -56
    }
}
