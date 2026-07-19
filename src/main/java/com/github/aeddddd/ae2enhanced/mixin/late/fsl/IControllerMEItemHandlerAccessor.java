package com.github.aeddddd.ae2enhanced.mixin.late.fsl;

import com.xinyihl.functionalstoragelegacy.common.integration.ae2.ControllerMEItemHandler;
import com.xinyihl.functionalstoragelegacy.common.inventory.controller.ControllerItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * FSL ControllerMEItemHandler.handler 访问接口.
 */
@Mixin(value = ControllerMEItemHandler.class, remap = false)
public interface IControllerMEItemHandlerAccessor {

    @Accessor("handler")
    ControllerItemHandler ae2e$getHandler();
}
