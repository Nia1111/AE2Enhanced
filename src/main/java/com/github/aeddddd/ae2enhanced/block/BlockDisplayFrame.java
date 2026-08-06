package com.github.aeddddd.ae2enhanced.block;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

/**
 * 趋势显示幕墙边框方块(纯装饰).
 *
 * <p>深色/浅色两个实例;屏幕成型时统计周边边框颜色,
 * 浅色多于深色则屏幕使用浅色主题,否则深色主题.</p>
 */
public class BlockDisplayFrame extends Block {

    private final boolean light;

    public BlockDisplayFrame(boolean light) {
        super(Material.IRON);
        this.light = light;
        setRegistryName(AE2Enhanced.MOD_ID, light ? "display_frame_light" : "display_frame_dark");
        setTranslationKey(AE2Enhanced.MOD_ID + (light ? ".display_frame_light" : ".display_frame_dark"));
        setHardness(3.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(AE2Enhanced.CREATIVE_TAB);
    }

    public boolean isLight() {
        return light;
    }

    public static boolean isLight(IBlockState state) {
        return state.getBlock() instanceof BlockDisplayFrame
                && ((BlockDisplayFrame) state.getBlock()).isLight();
    }
}
