package com.github.aeddddd.ae2enhanced.network.packet;

import com.github.aeddddd.ae2enhanced.container.ContainerDisplayWall;
import com.github.aeddddd.ae2enhanced.tile.TileDisplayPanel;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 趋势显示幕墙配置操作包(C→S).
 *
 * <p>GUI 内操作要求玩家当前打开 {@link ContainerDisplayWall};
 * 潜行+右键切换图表类型无需 GUI,仅校验距离与成型状态.</p>
 */
public class PacketDisplayAction implements IMessage {

    public static final int ACTION_CYCLE_CHART = 0;
    public static final int ACTION_CYCLE_RANGE = 1;
    public static final int ACTION_CYCLE_YMODE = 2;
    public static final int ACTION_CYCLE_COLOR = 3;
    public static final int ACTION_TOGGLE_VISIBLE = 4;
    /** 客户端缓冲失步时请求全量重同步 */
    public static final int ACTION_REQUEST_SYNC = 5;
    /** GUI 中直接选定图表类型(arg = ordinal) */
    public static final int ACTION_SET_CHART = 6;
    /** GUI 中直接选定时间范围(arg = ordinal) */
    public static final int ACTION_SET_RANGE = 7;
    /** 色板右键:反向循环颜色(arg = 槽位) */
    public static final int ACTION_CYCLE_COLOR_PREV = 8;

    private BlockPos pos;
    private int action;
    private int arg;

    public PacketDisplayAction() {}

    public PacketDisplayAction(BlockPos pos, int action, int arg) {
        this.pos = pos;
        this.action = action;
        this.arg = arg;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.action = buf.readByte();
        this.arg = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(action);
        buf.writeByte(arg);
    }

    public static class Handler implements IMessageHandler<PacketDisplayAction, IMessage> {
        @Override
        public IMessage onMessage(PacketDisplayAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 距离校验(潜行点击路径没有 openContainer 可校验)
                if (player.getDistanceSq(message.pos) > 64 * 64) return;
                TileEntity te = player.world.getTileEntity(message.pos);
                if (!(te instanceof TileDisplayPanel)) return;
                TileDisplayPanel tile = (TileDisplayPanel) te;
                if (!tile.isMasterRole()) return;

                boolean guiOpen = player.openContainer instanceof ContainerDisplayWall;
                switch (message.action) {
                    case ACTION_CYCLE_CHART:
                        // 允许潜行右键(无 GUI)与 GUI 两种途径
                        tile.cycleChartType();
                        break;
                    case ACTION_CYCLE_RANGE:
                        if (guiOpen) tile.cycleTimeRange();
                        break;
                    case ACTION_CYCLE_YMODE:
                        if (guiOpen) tile.cycleYMode();
                        break;
                    case ACTION_CYCLE_COLOR:
                        if (guiOpen) tile.cycleSlotColor(message.arg);
                        break;
                    case ACTION_CYCLE_COLOR_PREV:
                        if (guiOpen) tile.cycleSlotColorPrev(message.arg);
                        break;
                    case ACTION_TOGGLE_VISIBLE:
                        if (guiOpen) tile.toggleSlotVisible(message.arg);
                        break;
                    case ACTION_REQUEST_SYNC:
                        tile.syncToClient();
                        break;
                    case ACTION_SET_CHART:
                        if (guiOpen) tile.setChartType(message.arg);
                        break;
                    case ACTION_SET_RANGE:
                        if (guiOpen) tile.setTimeRange(message.arg);
                        break;
                    default:
                        break;
                }
            });
            return null;
        }
    }
}
