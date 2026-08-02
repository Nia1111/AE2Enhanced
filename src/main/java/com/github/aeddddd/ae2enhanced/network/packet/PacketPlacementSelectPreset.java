package com.github.aeddddd.ae2enhanced.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketPlacementSelectPreset implements IMessage {

    /**
     * 选中的槽位索引。
     * 0~8：选择对应预设槽。
     * 9（即 PlacementConfig.MAX_PRESETS）：选取当前准星目标（中键）。
     * -2：清空当前选择（径向菜单空选项）。
     */
    private int slot;

    public PacketPlacementSelectPreset() {
    }

    public PacketPlacementSelectPreset(int slot) {
        // toBytes 仅写入单字节，越界会静默截断，构造时直接拒绝
        if (slot < Byte.MIN_VALUE || slot > Byte.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "slot 超出单字节范围 [" + Byte.MIN_VALUE + ", " + Byte.MAX_VALUE + "]: " + slot);
        }
        this.slot = slot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slot = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(slot);
    }

    public int getSlot() {
        return slot;
    }
}
