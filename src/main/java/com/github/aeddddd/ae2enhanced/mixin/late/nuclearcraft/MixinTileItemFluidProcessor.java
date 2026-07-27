package com.github.aeddddd.ae2enhanced.mixin.late.nuclearcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NuclearCraft 2.19a（非重制版）物品+流体处理器产物直注 Mixin。
 *
 * <p>{@code TileItemFluidProcessor} 同时具有物品输出槽与流体输出罐，
 * 两者均为连续区间，分别重定向。</p>
 */
@Mixin(targets = "nc.tile.processor.TileItemFluidProcessor", remap = false)
public class MixinTileItemFluidProcessor {

    @Inject(method = "produceProducts()V", at = @At("TAIL"), remap = false)
    private void ae2enhanced$redirectOutputs(CallbackInfo ci) {
        NCProcessorRedirectHelper.redirectLegacyItemFluidProcessor(this);
    }
}
