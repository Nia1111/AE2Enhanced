package com.github.aeddddd.ae2enhanced.mixin.late.nuclearcraft;

import nc.tile.processor.IBasicProcessor;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;

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

    /** 解决 ITile(default) 与 IInventory(abstract) 的签名冲突；抽象方法不参与合并。 */
    @Override
    public abstract boolean isUsableByPlayer(EntityPlayer player);

    /**
     * 类级重写接口 default 方法：执行原逻辑后重定向产物。
     */
    @Override
    public void produceProducts() {
        IBasicProcessor.super.produceProducts();
        NCProcessorRedirectHelper.redirectOverhauled(this);
    }
}
