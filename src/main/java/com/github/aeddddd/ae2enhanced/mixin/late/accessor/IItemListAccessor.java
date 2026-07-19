package com.github.aeddddd.ae2enhanced.mixin.late.accessor;

import appeng.util.item.ItemList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ItemList.records 访问接口.
 * 供 MixinContainerMEMonitorable 清理空 bucket 使用.
 */
@Mixin(value = ItemList.class, remap = false)
public interface IItemListAccessor {

    @Accessor("records")
    @SuppressWarnings("rawtypes")
    Reference2ObjectMap ae2e$getRecords();
}
