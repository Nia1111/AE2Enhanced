package com.github.aeddddd.ae2enhanced.mixin.late.terminal;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiCraftingCPU;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

/**
 * 修改 Crafting CPU 状态界面中物品列表的排序：
 * 按 正在合成 + 计划合成 总数降序排列（类似高版本行为）。
 * 纯库存项（无合成活动）排在末尾，同级按物品显示名排序，避免合成进行中条目频繁跳动。
 */
@Mixin(value = GuiCraftingCPU.class, remap = false)
public class MixinGuiCraftingCPU {

    @Shadow
    private List<IAEItemStack> visual;

    @Shadow
    private IItemList<IAEItemStack> active;

    @Shadow
    private IItemList<IAEItemStack> pending;

    @Inject(
        method = "postUpdate",
        at = @At(value = "INVOKE", target = "Lappeng/client/gui/implementations/GuiCraftingCPU;setScrollBar()V")
    )
    private void ae2enhanced$sortVisualByCraftingStatus(List<IAEItemStack> list, byte ref, CallbackInfo ci) {
        if (this.visual == null || this.visual.size() <= 1) {
            return;
        }

        this.visual.sort(Comparator.comparingLong(this::getCraftingTotal).reversed()
                .thenComparing(this::getItemDisplayName));
    }

    private long getCraftingTotal(IAEItemStack stack) {
        long total = 0;
        IAEItemStack activeStack = this.active.findPrecise(stack);
        if (activeStack != null) {
            total += activeStack.getStackSize();
        }
        IAEItemStack pendingStack = this.pending.findPrecise(stack);
        if (pendingStack != null) {
            total += pendingStack.getStackSize();
        }
        return total;
    }

    private String getItemDisplayName(IAEItemStack stack) {
        ItemStack itemStack = stack.createItemStack();
        return itemStack.getDisplayName();
    }
}
