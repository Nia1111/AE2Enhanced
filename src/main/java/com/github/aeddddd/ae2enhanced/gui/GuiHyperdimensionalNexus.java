package com.github.aeddddd.ae2enhanced.gui;

import com.github.aeddddd.ae2enhanced.tile.TileHyperdimensionalController;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 超维度仓储中枢信息面板。
 * 纯展示 GUI，无物品槽位，风格与 GuiHyperdimensionalUnformed 统一。
 */
public class GuiHyperdimensionalNexus extends GuiScreen {

    private static final int PANEL_BG = 0xFF1a1a2e;
    private static final int PANEL_LIGHT = 0xFF16213e;
    private static final int BORDER_DIM = 0xFF0f3460;
    private static final int ACCENT = 0xFF00d4ff;
    private static final int ACCENT_SOFT = 0xFF0f4c75;
    private static final int TEXT_MAIN = 0xFFe0e0e0;
    private static final int TEXT_ERROR = 0xFFff5555;
    private static final int TEXT_SUCCESS = 0xFF55ff88;
    private static final int TEXT_WARN = 0xFFffaa55;

    private final TileHyperdimensionalController tile;
    private int xSize = 240;
    private int ySize = 180;
    private int guiLeft;
    private int guiTop;

    public GuiHyperdimensionalNexus(TileHyperdimensionalController tile) {
        this.tile = tile;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // 主背景
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, PANEL_BG);

        // 顶部高亮条
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 2, ACCENT);

        // 外边框
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 1, BORDER_DIM);
        drawRect(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, BORDER_DIM);
        drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, BORDER_DIM);
        drawRect(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, BORDER_DIM);

        // 角落装饰
        int corner = 10;
        drawRect(guiLeft, guiTop, guiLeft + corner, guiTop + 2, ACCENT);
        drawRect(guiLeft, guiTop, guiLeft + 2, guiTop + corner, ACCENT);
        drawRect(guiLeft + xSize - corner, guiTop, guiLeft + xSize, guiTop + 2, ACCENT);
        drawRect(guiLeft + xSize - 2, guiTop, guiLeft + xSize, guiTop + corner, ACCENT);
        drawRect(guiLeft, guiTop + ySize - 2, guiLeft + corner, guiTop + ySize, ACCENT);
        drawRect(guiLeft, guiTop + ySize - corner, guiLeft + 2, guiTop + ySize, ACCENT);
        drawRect(guiLeft + xSize - corner, guiTop + ySize - 2, guiLeft + xSize, guiTop + ySize, ACCENT);
        drawRect(guiLeft + xSize - 2, guiTop + ySize - corner, guiLeft + xSize, guiTop + ySize, ACCENT);

        // 内面板区域
        drawRect(guiLeft + 10, guiTop + 36, guiLeft + xSize - 10, guiTop + ySize - 10, PANEL_LIGHT);
        drawRect(guiLeft + 10, guiTop + 36, guiLeft + xSize - 10, guiTop + 37, BORDER_DIM);
        drawRect(guiLeft + 10, guiTop + ySize - 11, guiLeft + xSize - 10, guiTop + ySize - 10, BORDER_DIM);

        // 标题
        String title = "§b超维度仓储中枢";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, guiLeft + (xSize - titleWidth) / 2, guiTop + 8, ACCENT);

        // 分隔线
        drawRect(guiLeft + 16, guiTop + 22, guiLeft + xSize - 16, guiTop + 23, ACCENT_SOFT);

        if (tile == null) {
            fontRenderer.drawString("§cTile 不可用", guiLeft + 20, guiTop + 40, TEXT_ERROR);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        int x = guiLeft + 20;
        int y = guiTop + 42;
        int lineHeight = 14;

        // 结构状态
        String formedStr = tile.isFormed() ? "§a已组装" : "§c未组装";
        fontRenderer.drawString("结构状态: " + formedStr, x, y, TEXT_MAIN);
        y += lineHeight;

        // 网络状态
        String networkStr = tile.isNetworkActive() ? "§a在线" : "§c离线";
        fontRenderer.drawString("网络状态: " + networkStr, x, y, TEXT_MAIN);
        y += lineHeight;

        // 能源状态
        String powerStr = tile.isNetworkPowered() ? "§a供能正常" : "§e未供能";
        fontRenderer.drawString("能源状态: " + powerStr, x, y, TEXT_MAIN);
        y += lineHeight;

        // Nexus ID
        if (tile.getNexusId() != null) {
            String id = tile.getNexusId().toString().substring(0, 8);
            fontRenderer.drawString("Nexus ID: §7" + id + "...", x, y, TEXT_MAIN);
            y += lineHeight;
        } else {
            fontRenderer.drawString("Nexus ID: §7未生成", x, y, TEXT_MAIN);
            y += lineHeight;
        }

        // 存储统计
        if (tile.getItemAdapter() != null) {
            int types = tile.getItemAdapter().getStorageMap().size();
            BigInteger totalItems = BigInteger.ZERO;
            for (BigInteger count : tile.getItemAdapter().getStorageMap().values()) {
                totalItems = totalItems.add(count);
            }
            fontRenderer.drawString("存储种类: §e" + types, x, y, TEXT_MAIN);
            y += lineHeight;
            fontRenderer.drawString("总物品数: §e" + formatBigNumber(totalItems), x, y, TEXT_MAIN);
        } else {
            fontRenderer.drawString("存储核心: §e已初始化", x, y, TEXT_SUCCESS);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String formatBigNumber(BigInteger num) {
        if (num.compareTo(BigInteger.valueOf(1_000_000_000_000L)) >= 0) {
            return num.toString();
        } else if (num.compareTo(BigInteger.valueOf(1_000_000_000L)) >= 0) {
            return num.divide(BigInteger.valueOf(1_000_000_000L)) + " B";
        } else if (num.compareTo(BigInteger.valueOf(1_000_000L)) >= 0) {
            return num.divide(BigInteger.valueOf(1_000_000L)) + " M";
        } else if (num.compareTo(BigInteger.valueOf(1_000L)) >= 0) {
            return num.divide(BigInteger.valueOf(1_000L)) + " K";
        }
        return num.toString();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            this.mc.displayGuiScreen(null);
        }
        super.keyTyped(typedChar, keyCode);
    }
}
