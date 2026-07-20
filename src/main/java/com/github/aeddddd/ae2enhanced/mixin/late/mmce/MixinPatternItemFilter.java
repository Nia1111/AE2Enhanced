package com.github.aeddddd.ae2enhanced.mixin.late.mmce;

import com.github.aeddddd.ae2enhanced.item.ItemSmartPattern;
import github.kasuminova.mmce.common.util.PatternItemFilter;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin PatternItemFilter：允许智能样板放入 MMCE 机械样板供应器.
 *
 * <p>MMCE 原版的过滤器只接受 {@code ItemEncodedPattern} 子类,
 * 智能样板({@link ItemSmartPattern})直接继承 {@code Item},会被拒绝.
 * 此 Mixin 在 {@code allowInsert} 的 HEAD 放行带 NBT 的智能样板.</p>
 */
@Mixin(value = PatternItemFilter.class, remap = false)
public class MixinPatternItemFilter {

    @Inject(method = "allowInsert", at = @At("HEAD"), cancellable = true)
    private void ae2e$allowSmartPattern(IItemHandler inv, int slot, ItemStack stack,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty() && stack.hasTagCompound() && stack.getItem() instanceof ItemSmartPattern) {
            cir.setReturnValue(true);
        }
    }
}
