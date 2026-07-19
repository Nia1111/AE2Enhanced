package com.github.aeddddd.ae2enhanced.mixin.late.fsl;

import appeng.api.AEApi;
import appeng.api.storage.channels.IItemStorageChannel;
import com.github.aeddddd.ae2enhanced.integration.drawer.fsl.FSLAdapter;
import com.xinyihl.functionalstoragelegacy.common.integration.ae2.AE2CapabilityHelper;
import com.xinyihl.functionalstoragelegacy.common.integration.ae2.ControllerMEItemHandler;
import com.xinyihl.functionalstoragelegacy.common.integration.ae2.DrawerMEMonitor;
import com.xinyihl.functionalstoragelegacy.common.integration.ae2.DrawerStorageAccessor;
import com.xinyihl.functionalstoragelegacy.common.inventory.controller.ControllerItemHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 替换 FunctionalStorageLegacy 的 ControllerMEItemHandler 为 Hash 索引优化版本.
 *
 * <p>在 {@link AE2CapabilityHelper#createAccessor} 返回后,通过 accessor 替换
 * DrawerStorageAccessor 中 DrawerMEMonitor 的 handler 字段,
 * 将原生的 ControllerMEItemHandler 替换为 {@link FSLAdapter}.</p>
 */
@Mixin(value = AE2CapabilityHelper.class, remap = false)
public class MixinAE2CapabilityHelper {

    @Inject(method = "createAccessor", at = @At("RETURN"), cancellable = true)
    private static void ae2enhanced$replaceControllerHandler(ControllableDrawerTile tile, CallbackInfoReturnable<Object> cir) {
        Object result = cir.getReturnValue();
        if (!(result instanceof DrawerStorageAccessor)) {
            return;
        }

        try {
            // 获取 itemMonitor
            DrawerMEMonitor itemMonitor = ((IDrawerStorageAccessorAccessor) result).ae2e$getItemMonitor();
            if (itemMonitor == null) {
                return;
            }

            IDrawerMEMonitorAccessor monitorAcc = (IDrawerMEMonitorAccessor) itemMonitor;
            Object oldHandler = monitorAcc.ae2e$getHandler();
            if (oldHandler == null) {
                return;
            }

            // 确认是 ControllerMEItemHandler
            if (!(oldHandler instanceof ControllerMEItemHandler)) {
                return;
            }

            // 获取 ControllerMEItemHandler.handler (ControllerItemHandler)
            ControllerItemHandler controllerHandler = ((IControllerMEItemHandlerAccessor) oldHandler).ae2e$getHandler();
            if (controllerHandler == null) {
                return;
            }

            IItemStorageChannel itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            FSLAdapter adapter = new FSLAdapter(controllerHandler, itemChannel);

            // 替换 handler 字段
            monitorAcc.ae2e$setHandler(adapter);

        } catch (Exception ignored) {
            // 任何失败都保持原有行为
        }
    }
}
