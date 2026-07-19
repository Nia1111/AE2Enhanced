package com.github.aeddddd.ae2enhanced.mixin.late.fsl;

import appeng.api.storage.IMEInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.integration.ae2.DrawerMEMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * FSL DrawerMEMonitor.handler 访问接口.
 * setter 用于把原生 ControllerMEItemHandler 替换为 FSLAdapter.
 * 目标字段为 private final, setter 需要 @Mutable.
 */
@Mixin(value = DrawerMEMonitor.class, remap = false)
public interface IDrawerMEMonitorAccessor {

    @Accessor("handler")
    @SuppressWarnings("rawtypes")
    IMEInventoryHandler ae2e$getHandler();

    @Mutable
    @Accessor("handler")
    @SuppressWarnings("rawtypes")
    void ae2e$setHandler(IMEInventoryHandler handler);
}
