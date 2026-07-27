package com.github.aeddddd.ae2enhanced.mixin.late.thermal;

import cofh.core.block.TileInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * cofh.core.block.TileInventory 的 inventory 字段访问器.
 *
 * <p>TE 5.x 中 inventory 声明在 TileInventory（TileMachineBase 的曾祖类），
 * @Shadow 无法跨父类解析字段，故用 Accessor Mixin 直接挂在声明类上。</p>
 */
@Mixin(value = TileInventory.class, remap = false)
public interface ITileInventoryAccessor {

    @Accessor("inventory")
    ItemStack[] ae2e$getInventory();
}
