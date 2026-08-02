package com.github.aeddddd.ae2enhanced.dimension;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 个人维度建造/交互权限的强制执行器。
 *
 * <p>维度所有者与服务端 OP（权限等级 &ge; 2）始终绕过检查；
 * 其余玩家按 {@link PlayerDimEntry} 中的权限表判定：</p>
 * <ul>
 *   <li>{@link PersonalDimPermission#BUILD}：破坏方块、放置方块/实体、桶装取流体</li>
 *   <li>{@link PersonalDimPermission#INTERACT}：右键方块（打开 GUI、按钮、拉杆等）、
 *   右键实体、攻击实体</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = AE2Enhanced.MOD_ID)
public final class PersonalDimensionProtection {

    private PersonalDimensionProtection() {}

    /**
     * 拒绝提示的发送冷却，防止连续点击时聊天栏刷屏。
     */
    private static final Map<UUID, Long> DENY_MESSAGE_COOLDOWN = new HashMap<>();
    private static final long DENY_MESSAGE_INTERVAL_TICKS = 20L;

    /**
     * 检查玩家在所处个人维度内是否拥有某项权限。
     * 非个人维度、客户端侧、维度所有者与 OP 一律放行。
     */
    public static boolean canAct(EntityPlayer player, PersonalDimPermission permission) {
        if (player.world.isRemote) return true;
        int dimId = player.dimension;
        if (!PersonalDimensionManager.isPersonalDimension(dimId)) return true;
        PlayerDimEntry entry = PersonalDimensionManager.getEntryByDimension(dimId);
        if (entry == null) return true;
        if (entry.playerId.equals(player.getUniqueID())) return true;
        if (player.canUseCommand(2, "")) return true;
        return entry.hasPermission(player.getUniqueID(), permission);
    }

    private static void sendDenyMessage(EntityPlayer player, String langKey) {
        long now = player.world.getTotalWorldTime();
        Long last = DENY_MESSAGE_COOLDOWN.get(player.getUniqueID());
        if (last != null && now - last < DENY_MESSAGE_INTERVAL_TICKS) return;
        DENY_MESSAGE_COOLDOWN.put(player.getUniqueID(), now);
        player.sendMessage(new TextComponentTranslation(langKey));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DENY_MESSAGE_COOLDOWN.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!canAct(event.getPlayer(), PersonalDimPermission.BUILD)) {
            event.setCanceled(true);
            sendDenyMessage(event.getPlayer(), "chat.ae2enhanced.personal_dimension.no_permission_build");
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (!canAct(event.getPlayer(), PersonalDimPermission.BUILD)) {
            event.setCanceled(true);
            sendDenyMessage(event.getPlayer(), "chat.ae2enhanced.personal_dimension.no_permission_build");
        }
    }

    @SubscribeEvent
    public static void onMultiPlace(BlockEvent.MultiPlaceEvent event) {
        if (!canAct(event.getPlayer(), PersonalDimPermission.BUILD)) {
            event.setCanceled(true);
            sendDenyMessage(event.getPlayer(), "chat.ae2enhanced.personal_dimension.no_permission_build");
        }
    }

    /**
     * 桶的装取都会改变世界的流体状态，归入 BUILD。
     */
    @SubscribeEvent
    public static void onFillBucket(FillBucketEvent event) {
        if (!canAct(event.getEntityPlayer(), PersonalDimPermission.BUILD)) {
            event.setCanceled(true);
            sendDenyMessage(event.getEntityPlayer(), "chat.ae2enhanced.personal_dimension.no_permission_build");
        }
    }

    /**
     * 只拒绝方块激活（打开 GUI、按钮、拉杆等），不拦截物品使用本身，
     * 放置行为由 {@link #onBlockPlace} 等事件按 BUILD 单独判定。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!canAct(event.getEntityPlayer(), PersonalDimPermission.INTERACT)) {
            event.setUseBlock(Event.Result.DENY);
            sendDenyMessage(event.getEntityPlayer(), "chat.ae2enhanced.personal_dimension.no_permission_interact");
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!canAct(event.getEntityPlayer(), PersonalDimPermission.INTERACT)) {
            event.setCanceled(true);
            sendDenyMessage(event.getEntityPlayer(), "chat.ae2enhanced.personal_dimension.no_permission_interact");
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!canAct(event.getEntityPlayer(), PersonalDimPermission.INTERACT)) {
            event.setCanceled(true);
            sendDenyMessage(event.getEntityPlayer(), "chat.ae2enhanced.personal_dimension.no_permission_interact");
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!canAct(event.getEntityPlayer(), PersonalDimPermission.INTERACT)) {
            event.setCanceled(true);
            sendDenyMessage(event.getEntityPlayer(), "chat.ae2enhanced.personal_dimension.no_permission_interact");
        }
    }
}
