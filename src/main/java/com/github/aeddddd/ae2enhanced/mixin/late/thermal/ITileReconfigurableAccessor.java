package com.github.aeddddd.ae2enhanced.mixin.late.thermal;

import cofh.core.block.TileReconfigurable;
import cofh.core.util.core.SlotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * cofh.core.block.TileReconfigurable 的 slotConfig 字段访问器.
 *
 * <p>TE 5.x 中 slotConfig 声明在 TileReconfigurable（TileMachineBase 的祖父类），
 * @Shadow 无法跨父类解析字段，故用 Accessor Mixin 直接挂在声明类上。</p>
 */
@Mixin(value = TileReconfigurable.class, remap = false)
public interface ITileReconfigurableAccessor {

    @Accessor("slotConfig")
    SlotConfig ae2e$getSlotConfig();
}
