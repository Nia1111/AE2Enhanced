package com.github.aeddddd.ae2enhanced.mixin.late.nuclearcraft;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.recycler.NCProcessorRedirectHelper;
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

    /** 重定向熔断标记：辅助类加载失败等致命错误时置位，保证机器原逻辑不受影响。 */
    private static boolean ae2enhanced$redirectBroken = false;

    @Inject(method = "produceProducts()V", at = @At("TAIL"), remap = false)
    private void ae2enhanced$redirectOutputs(CallbackInfo ci) {
        if (ae2enhanced$redirectBroken) {
            return;
        }
        try {
            NCProcessorRedirectHelper.redirectLegacyItemFluidProcessor(this);
        } catch (Throwable t) {
            ae2enhanced$redirectBroken = true;
            AE2Enhanced.LOGGER.warn("[AE2E] NuclearCraft output redirect disabled due to error", t);
        }
    }
}
