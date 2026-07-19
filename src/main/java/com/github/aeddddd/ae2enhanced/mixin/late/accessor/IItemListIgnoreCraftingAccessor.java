package com.github.aeddddd.ae2enhanced.mixin.late.accessor;

import appeng.api.storage.data.IItemList;
import appeng.util.inv.ItemListIgnoreCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ItemListIgnoreCrafting.target 访问接口.
 * 供 MixinContainerMEMonitorable 解包被装饰的 ItemList.
 */
@Mixin(value = ItemListIgnoreCrafting.class, remap = false)
public interface IItemListIgnoreCraftingAccessor {

    @Accessor("target")
    @SuppressWarnings("rawtypes")
    IItemList ae2e$getTarget();
}
