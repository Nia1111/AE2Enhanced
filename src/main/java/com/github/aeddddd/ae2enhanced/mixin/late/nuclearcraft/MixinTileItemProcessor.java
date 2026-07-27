package com.github.aeddddd.ae2enhanced.mixin.late.nuclearcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NuclearCraft 2.19a（非重制版）物品处理器产物直注 Mixin。
 *
 * <p>非重制版中 {@code produceProducts()} 定义在具体基类 {@code TileItemProcessor} 中
 * （机器子类均不覆写），物品输出槽为连续区间。仅流体产物的机器见
 * {@link MixinTileFluidProcessor}。</p>
 */
@Mixin(targets = "nc.tile.processor.TileItemProcessor", remap = false)
public class MixinTileItemProcessor {

    @Inject(method = "produceProducts()V", at = @At("TAIL"), remap = false)
    private void ae2enhanced$redirectOutputs(CallbackInfo ci) {
        NCProcessorRedirectHelper.redirectLegacyItemProcessor(this);
    }
}
