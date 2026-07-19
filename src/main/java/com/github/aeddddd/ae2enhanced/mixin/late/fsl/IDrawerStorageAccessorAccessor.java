package com.github.aeddddd.ae2enhanced.mixin.late.fsl;

import com.xinyihl.functionalstoragelegacy.common.integration.ae2.DrawerMEMonitor;
import com.xinyihl.functionalstoragelegacy.common.integration.ae2.DrawerStorageAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * FSL DrawerStorageAccessor.itemMonitor 访问接口.
 * 仅在 functionalstoragelegacy 存在时随 mixins.ae2enhanced.late.fsl.json 加载.
 */
@Mixin(value = DrawerStorageAccessor.class, remap = false)
public interface IDrawerStorageAccessorAccessor {

    @Accessor("itemMonitor")
    @SuppressWarnings("rawtypes")
    DrawerMEMonitor ae2e$getItemMonitor();
}
