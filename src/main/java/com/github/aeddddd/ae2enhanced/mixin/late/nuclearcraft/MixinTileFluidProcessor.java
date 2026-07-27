package com.github.aeddddd.ae2enhanced.mixin.late.nuclearcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NuclearCraft 2.19a（非重制版）流体处理器产物直注 Mixin。
 *
 * <p>{@code TileFluidProcessor} 仅有流体产物，输出罐为连续区间
 * [fluidInputSize, fluidInputSize+fluidOutputSize)。</p>
 */
@Mixin(targets = "nc.tile.processor.TileFluidProcessor", remap = false)
public class MixinTileFluidProcessor {

    @Inject(method = "produceProducts()V", at = @At("TAIL"), remap = false)
    private void ae2enhanced$redirectOutputs(CallbackInfo ci) {
        NCProcessorRedirectHelper.redirectLegacyFluidProcessor(this);
    }
}
