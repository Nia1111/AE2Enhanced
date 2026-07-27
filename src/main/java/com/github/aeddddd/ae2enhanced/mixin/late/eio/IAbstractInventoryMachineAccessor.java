package com.github.aeddddd.ae2enhanced.mixin.late.eio;

import crazypants.enderio.base.machine.baselegacy.AbstractInventoryMachineEntity;
import crazypants.enderio.base.machine.baselegacy.SlotDefinition;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * AbstractInventoryMachineEntity 的 inventory / slotDefinition 字段访问器.
 *
 * <p>EIO 5.x 中这两个字段声明在 AbstractInventoryMachineEntity
 * （AbstractPoweredTaskEntity 的父类），@Shadow 无法跨父类解析字段，
 * 故用 Accessor Mixin 直接挂在声明类上。</p>
 */
@Mixin(value = AbstractInventoryMachineEntity.class, remap = false)
public interface IAbstractInventoryMachineAccessor {

    @Accessor("inventory")
    ItemStack[] ae2e$getInventory();

    @Accessor("slotDefinition")
    SlotDefinition ae2e$getSlotDefinition();
}
