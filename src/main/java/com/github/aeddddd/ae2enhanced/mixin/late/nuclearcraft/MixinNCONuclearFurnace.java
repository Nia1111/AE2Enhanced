package com.github.aeddddd.ae2enhanced.mixin.late.nuclearcraft;

import nc.tile.processor.IBasicProcessor;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.recycler.NCProcessorRedirectHelper;

/**
 * NuclearCraft Overhauled (2o.x) 核燃料炉产物直注 Mixin（Tier 2）。
 *
 * <p>与 {@link MixinNCOEnergyProcessor} 同理：{@code TileNuclearFurnace} 不继承
 * {@code TileEnergyProcessor}，而是直接实现 {@code IBasicProcessor}，因此需要
 * 单独的类 Mixin 添加 {@code produceProducts()} 的类级重写，先委托
 * {@code IBasicProcessor.super}（解析到 {@code IProcessor} 的 default 实现）
 * 执行原逻辑，再重定向产物。</p>
 *
 * <p>仅在 NC:O 2o.x 下应用，由 {@code NuclearCraftMixinPlugin} 按版本分流。</p>
 */
@Mixin(targets = "nc.tile.processor.TileNuclearFurnace", remap = false)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class MixinNCONuclearFurnace implements IBasicProcessor {

    /**
     * 解决 ITile(default) 与 IInventory(abstract) 的签名冲突，原因与注意事项见
     * {@link MixinNCOEnergyProcessor}——必须为具体实现（方法体与 {@code ITile}
     * 的 default 完全一致），声明为 abstract 会被合并并压制 default 实现，
     * 导致 AbstractMethodError。
     */
    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return player.getDistanceSq(getTilePos().getX() + 0.5, getTilePos().getY() + 0.5, getTilePos().getZ() + 0.5) <= 64.0;
    }

    /** 重定向熔断标记：辅助类加载失败等致命错误时置位，保证机器原逻辑不受影响。 */
    private static boolean ae2enhanced$redirectBroken = false;

    /**
     * 类级重写接口 default 方法：执行原逻辑后重定向产物。
     */
    @Override
    public void produceProducts() {
        IBasicProcessor.super.produceProducts();
        if (!ae2enhanced$redirectBroken) {
            try {
                NCProcessorRedirectHelper.redirectOverhauled(this);
            } catch (Throwable t) {
                ae2enhanced$redirectBroken = true;
                AE2Enhanced.LOGGER.warn("[AE2E] NuclearCraft output redirect disabled due to error", t);
            }
        }
    }
}
