package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.crafting.CraftingJob;

import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanDisplayHook;

/**
 * 原生合成计算完成钩子:为普通计划补充"样板调用 N 次"显示信息
 * （特殊计划的完整信息由 SpecialCraftingJob 自行发送——其子类 override run()
 * 不会被本注入覆盖）.
 */
@Mixin(value = CraftingJob.class, remap = false)
public class MixinCraftingJob {

    @Inject(method = "run", at = @At("RETURN"), require = 0)
    private void ae2enhanced$sendCallCounts(CallbackInfo ci) {
        SpecialPlanDisplayHook.sendCallCounts((CraftingJob) (Object) this);
    }
}
