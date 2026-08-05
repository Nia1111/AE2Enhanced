package com.github.aeddddd.ae2enhanced.mixin.late.nuclearcraft;

import nc.tile.processor.IProcessor;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.recycler.NCProcessorRedirectHelper;

/**
 * NuclearCraft Overhauled (2o.x) 能量机器产物直注 Mixin（Tier 2）。
 *
 * <p>{@code IProcessor#produceProducts()} 是接口 default 方法，而 Mixin 0.8.5
 * 不支持在接口 Mixin 中声明注入器，因此本 Mixin 改为以类 Mixin 形式向抽象基类
 * {@code TileEnergyProcessor} 添加 {@code produceProducts()} 的类级重写：
 * 先委托 {@code IProcessor.super} 执行原始 default 逻辑，随后将输出槽产物
 * 重定向到已绑定的 ME 网络回收节点。类级方法优先于接口 default，
 * 所有继承 {@code TileEnergyProcessor} 的机器（TileProcessorImpl$* 全系列）
 * 自动获得该行为。</p>
 *
 * <p>仅在 NC:O 2o.x 下应用，由 {@code NuclearCraftMixinPlugin} 按版本分流；
 * 非重制版 (2.19a) 由 MixinTileItemProcessor 等具体类 Mixin 负责。</p>
 */
@Mixin(targets = "nc.tile.processor.TileEnergyProcessor", remap = false)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class MixinNCOEnergyProcessor implements IProcessor {

    /**
     * 解决 ITile(default) 与 IInventory(abstract) 的签名冲突。
     * 必须为具体实现，且方法体与 {@code ITile} 的 default 完全一致（距离 ≤ 64 检查）；
     * 绝不能声明为 abstract——抽象方法同样会被 Mixin 合并进目标类，
     * 且 reobf 后被改名为 {@code func_70300_a}，成为类级抽象声明并压制
     * {@code ITile} 的 default 实现，导致打开机器 GUI 时 AbstractMethodError。
     */
    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return player.getDistanceSq(getTilePos().getX() + 0.5, getTilePos().getY() + 0.5, getTilePos().getZ() + 0.5) <= 64.0;
    }

    /** 重定向熔断标记：辅助类加载失败等致命错误时置位，保证机器原逻辑不受影响。 */
    private static boolean ae2enhanced$redirectBroken = false;

    /**
     * 类级重写接口 default 方法：执行原逻辑后重定向产物。
     * Mixin 合并时目标类未声明该方法，将作为新方法加入并覆盖接口 default。
     */
    @Override
    public void produceProducts() {
        IProcessor.super.produceProducts();
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
