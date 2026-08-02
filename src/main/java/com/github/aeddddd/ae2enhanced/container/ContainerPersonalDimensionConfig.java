package com.github.aeddddd.ae2enhanced.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import java.util.UUID;

/**
 * 个人维度配置 GUI 的 Container（无物品槽）。
 */
public class ContainerPersonalDimensionConfig extends Container {

    private final UUID playerId;

    public ContainerPersonalDimensionConfig(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        // 所有者本人、OP 以及拥有 MANAGE_RULES 权限的受邀玩家均可编辑
        return com.github.aeddddd.ae2enhanced.dimension.PersonalDimensionManager.canManageRules(player, playerId);
    }
}
