package com.github.aeddddd.ae2enhanced.mixin.late.terminal;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiCraftConfirm;
import com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanClientCache;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * E2c: 合成计划缺少物品置顶.
 * AE2-UEL 原生的 GuiCraftConfirm 中,visual 列表按服务端推送顺序排列,
 * 缺少的物品虽然标红,但可能夹在中间不易发现.
 * 此 Mixin 在 postUpdate 末尾对 visual 列表重新排序,将 missing > 0 的项置顶.
 */
@Mixin(value = GuiCraftConfirm.class, remap = false)
public class MixinGuiCraftConfirm {

    @Shadow
    private List<IAEItemStack> visual;

    @Shadow
    private IItemList<IAEItemStack> missing;

    @Inject(method = "postUpdate", at = @At("TAIL"))
    private void ae2enhanced$onPostUpdate(List<IAEItemStack> list, byte ref, CallbackInfo ci) {
        if (this.visual == null || this.visual.isEmpty()) {
            return;
        }
        this.visual.sort((a, b) -> {
            boolean aMissing = ae2enhanced$isMissing(a);
            boolean bMissing = ae2enhanced$isMissing(b);
            if (aMissing && !bMissing) {
                return -1;
            }
            if (!aMissing && bMissing) {
                return 1;
            }
            return 0;
        });
    }

    @Unique
    private boolean ae2enhanced$isMissing(IAEItemStack stack) {
        if (this.missing == null || stack == null) {
            return false;
        }
        IAEItemStack m = this.missing.findPrecise(stack);
        return m != null && m.getStackSize() > 0;
    }

    // ==================== Special Plan Tooltip ====================

    @Shadow
    private int tooltip;

    /**
     * 特殊计划显示:在合成确认界面的悬停 tooltip 末尾追加
     * 自增殖/循环链结构信息与样板调用次数(显示层始终启用,不受功能开关影响).
     */
    @WrapOperation(
        method = "drawFG",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftConfirm;drawTooltip(IILjava/lang/String;)V"
        ),
        require = 0
    )
    private void ae2enhanced$wrapPlanTooltip(GuiCraftConfirm self, int x, int y, String message,
            Operation<Void> original) {
        original.call(self, x, y, ae2enhanced$appendSpecialPlanLines(message));
    }

    @Unique
    private String ae2enhanced$appendSpecialPlanLines(String message) {
        try {
            if (this.tooltip < 0 || this.visual == null) {
                return message;
            }
            int viewStart = ((MixinAEBaseGuiAccessor) this).ae2enhanced$getMyScrollBar().getCurrentScroll() * 3;
            int idx = viewStart + this.tooltip;
            if (idx < 0 || idx >= this.visual.size()) {
                return message;
            }
            IAEItemStack hovered = this.visual.get(idx);
            if (hovered == null) {
                return message;
            }
            SpecialPlanInfo info = SpecialPlanClientCache.infoFor(hovered.asItemStackRepresentation());
            if (info == null) {
                return message;
            }
            StringBuilder sb = new StringBuilder(message);
            SpecialPlanInfo.Entry entry = info.entryFor(hovered);
            if (entry != null) {
                if (entry.kind == SpecialPlanInfo.KIND_SELF_DUP) {
                    long net = entry.perRoundProduce - entry.perRoundConsume;
                    sb.append('\n').append(I18n.format("gui.ae2enhanced.special_plan.dup",
                            entry.perRoundConsume, entry.perRoundProduce, net,
                            entry.totalCrafts, entry.initialExtract));
                } else {
                    sb.append('\n').append(I18n.format("gui.ae2enhanced.special_plan.cycle",
                            entry.rounds, entry.perRoundConsume, entry.perRoundProduce,
                            entry.initialExtract));
                }
            }
            long calls = info.callCountOf(hovered);
            if (calls > 0) {
                sb.append('\n').append(I18n.format("gui.ae2enhanced.special_plan.calls", calls));
            }
            return sb.toString();
        } catch (Throwable t) {
            return message;
        }
    }
}
