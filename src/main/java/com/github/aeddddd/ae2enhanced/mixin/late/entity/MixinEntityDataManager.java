package com.github.aeddddd.ae2enhanced.mixin.late.entity;

import com.github.aeddddd.ae2enhanced.mixin.late.accessor.IEntityLivingBaseAccessor;
import com.github.aeddddd.ae2enhanced.omnitool.OmniToolUpgrades;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 EntityDataManager.set() 层面拦截对 HEALTH 参数的治疗性修改。
 * 某些实体（如 dechaosislandlegacy 的 DraconicGuardianEntity）会绕过 setHealth()
 * 直接通过 dataManager.set(HEALTH, maxHealth) 来复活，此 mixin 专门堵这个口子。
 */
@Mixin(value = EntityDataManager.class, remap = true)
public class MixinEntityDataManager {

    @Shadow
    private Entity entity;

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void ae2e$onSet(Object key, Object value, CallbackInfo ci) {
        DataParameter<Float> healthParam = IEntityLivingBaseAccessor.ae2e$getHEALTH();
        if (key != healthParam) return;

        if (!(this.entity instanceof EntityLivingBase)) return;
        EntityLivingBase living = (EntityLivingBase) this.entity;
        if (!OmniToolUpgrades.hasAntiHeal(living)) return;

        float newHealth = value instanceof Float ? (Float) value : 0.0f;
        Object currentValue = ((EntityDataManager) (Object) this).get(healthParam);
        float currentHealth = currentValue instanceof Float ? (Float) currentValue : 0.0f;

        if (newHealth > currentHealth) {
            ci.cancel();
        }
    }
}
