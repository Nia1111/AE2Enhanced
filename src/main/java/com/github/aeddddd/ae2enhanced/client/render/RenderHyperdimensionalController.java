package com.github.aeddddd.ae2enhanced.client.render;

import com.github.aeddddd.ae2enhanced.block.BlockHyperdimensionalController;
import com.github.aeddddd.ae2enhanced.tile.TileHyperdimensionalController;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

/**
 * 超维度仓储中枢控制器的 TESR：超立方体全息投影。
 * 绘制一个缓慢旋转的线框超立方体（双立方体 + 连接边），
 * 带有青色发光效果和脉冲呼吸动画。
 *
 * GL 状态恢复策略：所有修改的状态在 finally 中显式恢复。
 */
public class RenderHyperdimensionalController extends TileEntitySpecialRenderer<TileHyperdimensionalController> {

    // 外立方体半对角线长度（从中心到顶点）
    private static final float OUTER_SIZE = 2.4f;
    // 内立方体半对角线长度
    private static final float INNER_SIZE = 1.2f;
    // 主旋转速度
    private static final float ROT_SPEED = 0.8f;
    // 内立方体反向旋转速度
    private static final float INNER_ROT_SPEED = -0.5f;
    // 脉冲速度
    private static final float PULSE_SPEED = 0.06f;

    // 颜色：青色发光
    private static final int COLOR_OUTER = 0x00d4ff;
    private static final int COLOR_INNER = 0x0088cc;
    private static final int COLOR_CONNECT = 0x44aaff;

    @Override
    public void render(TileHyperdimensionalController te, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        if (te == null || !te.isFormed()) return;

        float time = (te.getWorld().getTotalWorldTime() + partialTicks) * ROT_SPEED;
        float innerTime = (te.getWorld().getTotalWorldTime() + partialTicks) * INNER_ROT_SPEED;
        float pulse = 0.5f + 0.5f * (float) Math.sin((te.getWorld().getTotalWorldTime() + partialTicks) * PULSE_SPEED);

        // 结构几何中心（相对于控制器）
        // 结构范围：x∈[-2,2], z∈[0,4]，中心在 (0,0,2)
        EnumFacing facing = EnumFacing.NORTH;
        if (te.getWorld() != null) {
            facing = te.getWorld().getBlockState(te.getPos()).getValue(BlockHyperdimensionalController.FACING);
        }
        double offX = 0, offZ = 2.0;
        switch (facing) {
            case SOUTH: offX = 0; offZ = -2.0; break;
            case EAST:  offX = -2.0; offZ = 0; break;
            case WEST:  offX = 2.0; offZ = 0; break;
            default:    offX = 0; offZ = 2.0; break;
        }

        double cx = x + 0.5 + offX;
        double cy = y + 2.5; // 在结构平面上方 2 格
        double cz = z + 0.5 + offZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, cz);

        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.enableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        try {
            // 外立方体线框
            GlStateManager.pushMatrix();
            GlStateManager.rotate(time, 0, 1, 0);
            GlStateManager.rotate(time * 0.3f, 1, 0, 0);
            drawCubeWireframe(OUTER_SIZE, COLOR_OUTER, 0.55f + 0.25f * pulse, 2.5f);
            GlStateManager.popMatrix();

            // 内立方体线框（反向旋转）
            GlStateManager.pushMatrix();
            GlStateManager.rotate(innerTime, 0, 1, 0);
            GlStateManager.rotate(innerTime * 0.4f, 0, 0, 1);
            drawCubeWireframe(INNER_SIZE, COLOR_INNER, 0.35f + 0.20f * pulse, 1.5f);
            GlStateManager.popMatrix();

            // 连接内外立方体对应顶点的边（超立方体特征）
            drawConnectionLines(time, innerTime, OUTER_SIZE, INNER_SIZE, COLOR_CONNECT, 0.18f + 0.12f * pulse);

            // 中心核心点（小光球）
            drawCenterCore(pulse);

        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            GlStateManager.disableCull();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GlStateManager.popMatrix();
        }
    }

    /**
     * 绘制一个轴对齐立方体的线框。
     * size 是从中心到每个顶点的距离（即半对角线长度 = sqrt(3) * 半边长）。
     * 半边长 = size / sqrt(3)。
     */
    private void drawCubeWireframe(float size, int color, float alpha, float lineWidth) {
        if (alpha <= 0.01f) return;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        float half = size / 1.73205f; // size / sqrt(3)

        // 8 个顶点
        float[][] v = {
            {-half, -half, -half},
            { half, -half, -half},
            { half,  half, -half},
            {-half,  half, -half},
            {-half, -half,  half},
            { half, -half,  half},
            { half,  half,  half},
            {-half,  half,  half},
        };

        // 12 条边（每对顶点索引）
        int[][] edges = {
            {0,1},{1,2},{2,3},{3,0}, // 后面
            {4,5},{5,6},{6,7},{7,4}, // 前面
            {0,4},{1,5},{2,6},{3,7}, // 连接前后
        };

        GlStateManager.glLineWidth(lineWidth);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (int[] e : edges) {
            buf.pos(v[e[0]][0], v[e[0]][1], v[e[0]][2]).color(r, g, b, alpha).endVertex();
            buf.pos(v[e[1]][0], v[e[1]][1], v[e[1]][2]).color(r, g, b, alpha).endVertex();
        }

        tess.draw();
        GlStateManager.glLineWidth(1.0f);
    }

    /**
     * 绘制连接内外立方体对应顶点的 8 条线。
     * 这是超立方体（Tesseract）在 3D 投影中的核心特征。
     */
    private void drawConnectionLines(float outerTime, float innerTime,
                                     float outerSize, float innerSize,
                                     int color, float alpha) {
        if (alpha <= 0.01f) return;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        float outerHalf = outerSize / 1.73205f;
        float innerHalf = innerSize / 1.73205f;

        // 8 个顶点方向（归一化的角方向）
        float[][] dirs = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1,  1}, {1, -1,  1}, {1, 1,  1}, {-1, 1,  1},
        };

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (float[] dir : dirs) {
            // 外顶点（应用外旋转）
            float[] ov = rotatePoint(
                dir[0] * outerHalf, dir[1] * outerHalf, dir[2] * outerHalf,
                outerTime, outerTime * 0.3f, 0
            );
            // 内顶点（应用内旋转）
            float[] iv = rotatePoint(
                dir[0] * innerHalf, dir[1] * innerHalf, dir[2] * innerHalf,
                0, 0, innerTime
            );

            buf.pos(ov[0], ov[1], ov[2]).color(r, g, b, alpha).endVertex();
            buf.pos(iv[0], iv[1], iv[2]).color(r, g, b, alpha).endVertex();
        }

        tess.draw();
    }

    /**
     * 简单旋转：先绕 Y 轴旋转 ry，再绕 X 轴旋转 rx，再绕 Z 轴旋转 rz。
     */
    private float[] rotatePoint(float x, float y, float z, float ry, float rx, float rz) {
        // 绕 Y 轴
        float cosY = (float) Math.cos(Math.toRadians(ry));
        float sinY = (float) Math.sin(Math.toRadians(ry));
        float x1 = x * cosY - z * sinY;
        float z1 = x * sinY + z * cosY;
        float y1 = y;

        // 绕 X 轴
        float cosX = (float) Math.cos(Math.toRadians(rx));
        float sinX = (float) Math.sin(Math.toRadians(rx));
        float y2 = y1 * cosX - z1 * sinX;
        float z2 = y1 * sinX + z1 * cosX;
        float x2 = x1;

        // 绕 Z 轴
        float cosZ = (float) Math.cos(Math.toRadians(rz));
        float sinZ = (float) Math.sin(Math.toRadians(rz));
        float x3 = x2 * cosZ - y2 * sinZ;
        float y3 = x2 * sinZ + y2 * cosZ;
        float z3 = z2;

        return new float[]{x3, y3, z3};
    }

    /**
     * 中心核心：一个微小的旋转八面体，表示奇点。
     */
    private void drawCenterCore(float pulse) {
        float alpha = 0.3f + 0.3f * pulse;
        float size = 0.06f + 0.03f * pulse;

        float r = 0.0f, g = 0.83f, b = 1.0f;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // 上下两个锥体组成八面体
        buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        // 上锥
        buf.pos(-size, 0, -size).color(r, g, b, alpha).endVertex();
        buf.pos( size, 0, -size).color(r, g, b, alpha).endVertex();
        buf.pos( 0,    size,  0).color(r, g, b, alpha).endVertex();

        buf.pos( size, 0, -size).color(r, g, b, alpha).endVertex();
        buf.pos( size, 0,  size).color(r, g, b, alpha).endVertex();
        buf.pos( 0,    size,  0).color(r, g, b, alpha).endVertex();

        buf.pos( size, 0,  size).color(r, g, b, alpha).endVertex();
        buf.pos(-size, 0,  size).color(r, g, b, alpha).endVertex();
        buf.pos( 0,    size,  0).color(r, g, b, alpha).endVertex();

        buf.pos(-size, 0,  size).color(r, g, b, alpha).endVertex();
        buf.pos(-size, 0, -size).color(r, g, b, alpha).endVertex();
        buf.pos( 0,    size,  0).color(r, g, b, alpha).endVertex();

        // 下锥
        buf.pos(-size, 0, -size).color(r, g, b, alpha).endVertex();
        buf.pos( 0,   -size,  0).color(r, g, b, alpha).endVertex();
        buf.pos( size, 0, -size).color(r, g, b, alpha).endVertex();

        buf.pos( size, 0, -size).color(r, g, b, alpha).endVertex();
        buf.pos( 0,   -size,  0).color(r, g, b, alpha).endVertex();
        buf.pos( size, 0,  size).color(r, g, b, alpha).endVertex();

        buf.pos( size, 0,  size).color(r, g, b, alpha).endVertex();
        buf.pos( 0,   -size,  0).color(r, g, b, alpha).endVertex();
        buf.pos(-size, 0,  size).color(r, g, b, alpha).endVertex();

        buf.pos(-size, 0,  size).color(r, g, b, alpha).endVertex();
        buf.pos( 0,   -size,  0).color(r, g, b, alpha).endVertex();
        buf.pos(-size, 0, -size).color(r, g, b, alpha).endVertex();

        tess.draw();
    }
}
