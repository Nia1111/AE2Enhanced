package com.github.aeddddd.ae2enhanced.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * 手绘风格按钮 —— 像素级复刻 3.png 中的按钮条带：
 * 深海军蓝外框 + 内圈亮色斜面 + 中部填充 + 底部双行阴影；
 * 悬停时切换为高亮蓝配色（DAFFFF/9CD3FF/708CBA）。
 */
public class GuiButtonTech extends GuiButton {

    // 手绘调色板（取自 3.png 按钮条带）
    private static final int BORDER       = 0xFF413F54;
    private static final int BEVEL        = 0xFFADB0C4;
    private static final int FILL         = 0xFF9A9FB4;
    private static final int BOTTOM       = 0xFF696D88;
    private static final int BEVEL_HOVER  = 0xFFDAFFFF;
    private static final int FILL_HOVER   = 0xFF9CD3FF;
    private static final int BOTTOM_HOVER = 0xFF708CBA;
    private static final int FILL_OFF     = 0xFF878FA5;
    private static final int TEXT         = 0xFF413F54;
    private static final int TEXT_OFF     = 0xFF3E4256;

    public GuiButtonTech(int buttonId, int x, int y, int width, int height, String text) {
        super(buttonId, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        int bevel, fill, bottom, textColor;
        if (!this.enabled) {
            bevel = BEVEL;
            fill = FILL_OFF;
            bottom = BOTTOM;
            textColor = TEXT_OFF;
        } else if (this.hovered) {
            bevel = BEVEL_HOVER;
            fill = FILL_HOVER;
            bottom = BOTTOM_HOVER;
            textColor = TEXT;
        } else {
            bevel = BEVEL;
            fill = FILL;
            bottom = BOTTOM;
            textColor = TEXT;
        }

        int x0 = this.x, y0 = this.y;
        int x1 = x0 + this.width, y1 = y0 + this.height;

        // 中部填充（含底部阴影区，后面覆盖）
        drawRect(x0, y0, x1, y1, fill);

        // 内圈斜面：上 / 左 / 右 1px
        drawRect(x0 + 1, y0 + 1, x1 - 1, y0 + 2, bevel);
        drawRect(x0 + 1, y0 + 1, x0 + 2, y1 - 4, bevel);
        drawRect(x1 - 2, y0 + 1, x1 - 1, y1 - 4, bevel);

        // 底部：1px 斜面行 + 2px 阴影行
        drawRect(x0 + 1, y1 - 4, x1 - 1, y1 - 3, bevel);
        drawRect(x0 + 1, y1 - 3, x1 - 1, y1 - 1, bottom);

        // 外框 1px
        drawRect(x0, y0, x1, y0 + 1, BORDER);
        drawRect(x0, y1 - 1, x1, y1, BORDER);
        drawRect(x0, y0, x0 + 1, y1, BORDER);
        drawRect(x1 - 1, y0, x1, y1, BORDER);

        // 文字
        mc.fontRenderer.drawString(this.displayString,
            this.x + (this.width - mc.fontRenderer.getStringWidth(this.displayString)) / 2,
            this.y + (this.height - 8) / 2,
            textColor);
    }
}
