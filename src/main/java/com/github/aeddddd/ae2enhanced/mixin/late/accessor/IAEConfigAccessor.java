package com.github.aeddddd.ae2enhanced.mixin.late.accessor;

import appeng.core.AEConfig;
import appeng.core.features.AEFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.EnumSet;

/**
 * AEConfig.featureFlags 访问接口.
 * 返回的 EnumSet 为可变引用,调用方直接 add/remove.
 */
@Mixin(value = AEConfig.class, remap = false)
public interface IAEConfigAccessor {

    @Accessor("featureFlags")
    EnumSet<AEFeature> ae2e$getFeatureFlags();
}
