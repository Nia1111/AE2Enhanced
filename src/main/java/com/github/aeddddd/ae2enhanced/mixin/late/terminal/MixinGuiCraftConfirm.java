package com.github.aeddddd.ae2enhanced.mixin.late.terminal;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiCraftConfirm;
import com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanClientCache;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
            IAEItemStack hovered = ae2enhanced$hoveredStack();
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
                for (String line : com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanTooltip
                        .tooltipLines(hovered, entry)) {
                    sb.append('\n').append(line);
                }
            } else {
                long calls = info.callCountOf(hovered);
                if (calls > 0) {
                    sb.append('\n').append(com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanTooltip
                            .normalDescriptionLine(calls));
                }
            }
            return sb.toString();
        } catch (Throwable t) {
            return message;
        }
    }

    @Unique
    private IAEItemStack ae2enhanced$hoveredStack() {
        if (this.tooltip < 0 || this.visual == null) {
            return null;
        }
        int viewStart = ((MixinAEBaseGuiAccessor) this).ae2enhanced$getMyScrollBar().getCurrentScroll() * 3;
        int idx = viewStart + this.tooltip;
        if (idx < 0 || idx >= this.visual.size()) {
            return null;
        }
        return this.visual.get(idx);
    }

    /**
     * 行内描述（1.1.0 对齐）:每个可见单元格的数量区下方追加一行灰色小字——
     * 自增殖"调用 N 次"/循环链"约 R 轮发配"/普通样板"调用 N 次".
     * 坐标方案与原生数量行一致(0.5 缩放、续接 downY 流).缓存为空时零影响.
     */
    @Inject(method = "drawFG", at = @At("TAIL"), require = 0)
    private void ae2enhanced$drawInlineDescriptions(int offsetX, int offsetY, int mouseX, int mouseY,
            CallbackInfo ci) {
        try {
            if (this.visual == null || this.visual.isEmpty()) {
                return;
            }
            int viewStart = ((MixinAEBaseGuiAccessor) this).ae2enhanced$getMyScrollBar().getCurrentScroll() * 3;
            int viewEnd = viewStart + 15; // 3 列 × 5 行
            net.minecraft.client.gui.FontRenderer fr = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
            int x = 0;
            int y = 0;
            for (int z = viewStart; z < Math.min(viewEnd, this.visual.size()); z++) {
                IAEItemStack refStack = this.visual.get(z);
                if (refStack != null) {
                    String desc = ae2enhanced$inlineDesc(refStack);
                    if (desc != null) {
                        ae2enhanced$drawCellLine(fr, desc, x, y, viewStart, z);
                    }
                }
                if (++x > 2) {
                    ++y;
                    x = 0;
                }
            }
        } catch (Throwable ignored) {
            // 渲染增强失败静默
        }
    }

    @Unique
    private String ae2enhanced$inlineDesc(IAEItemStack refStack) {
        SpecialPlanInfo info = SpecialPlanClientCache.infoFor(refStack.asItemStackRepresentation());
        if (info == null) {
            return null;
        }
        SpecialPlanInfo.Entry entry = info.entryFor(refStack);
        if (entry != null) {
            return com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanTooltip
                    .descriptionLine(entry);
        }
        long calls = info.callCountOf(refStack);
        if (calls > 0) {
            return com.github.aeddddd.ae2enhanced.client.specialcrafting.SpecialPlanTooltip
                    .normalDescriptionLine(calls);
        }
        return null;
    }

    /**
     * 按原生数量行的坐标方案绘制一行(0.5 缩放、居中、续接 downY 流末尾).
     */
    @Unique
    private void ae2enhanced$drawCellLine(net.minecraft.client.gui.FontRenderer fr, String str, int x, int y,
            int viewStart, int z) {
        IAEItemStack refStack = this.visual.get(z);
        int lines = 0;
        IItemList<IAEItemStack> storage = ae2enhanced$storageList();
        IItemList<IAEItemStack> pending = ae2enhanced$pendingList();
        if (storage != null) {
            IAEItemStack stored = storage.findPrecise(refStack);
            if (stored != null && stored.getStackSize() > 0) {
                lines++;
            }
        }
        if (this.missing != null) {
            IAEItemStack missingStack = this.missing.findPrecise(refStack);
            if (missingStack != null && missingStack.getStackSize() > 0) {
                lines++;
            }
        }
        if (pending != null) {
            IAEItemStack pendingStack = pending.findPrecise(refStack);
            if (pendingStack != null && pendingStack.getStackSize() > 0) {
                lines++;
            }
        }
        int negY = (lines - 1) * 5 / 2;
        int downY = lines * 5;
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.scale(0.5, 0.5, 0.5);
        int w = 4 + fr.getStringWidth(str);
        fr.drawString(str, (int) (((double) (x * 68 + 9 + 67 - 19) - (double) w * 0.5) * 2.0),
                (y * 23 + 22 + 6 - negY + downY) * 2, 0x404040);
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    @Shadow
    private IItemList<IAEItemStack> storage;

    @Shadow
    private IItemList<IAEItemStack> pending;

    @Unique
    private IItemList<IAEItemStack> ae2enhanced$storageList() {
        return this.storage;
    }

    @Unique
    private IItemList<IAEItemStack> ae2enhanced$pendingList() {
        return this.pending;
    }
}
