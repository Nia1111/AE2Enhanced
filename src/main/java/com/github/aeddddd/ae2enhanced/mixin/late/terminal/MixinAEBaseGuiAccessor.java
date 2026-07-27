package com.github.aeddddd.ae2enhanced.mixin.late.terminal;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * AEBaseGui 滚动条字段访问器.
 * <p>存在原因:子类 mixin(如 MixinGuiCraftConfirm)无法 @Shadow 父类声明的
 * {@code myScrollBar}/{@code getScrollBar()},强行 shadow 会导致整个 mixin 应用失败.</p>
 */
@Mixin(value = AEBaseGui.class, remap = false)
public interface MixinAEBaseGuiAccessor {

    @Accessor("myScrollBar")
    GuiScrollbar ae2enhanced$getMyScrollBar();
}
