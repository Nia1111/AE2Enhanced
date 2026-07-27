package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.mixin.bridge.IComputationCoreAccess;
import com.github.aeddddd.ae2enhanced.mixin.bridge.ISpecialCpuAccess;
import com.github.aeddddd.ae2enhanced.mixin.late.accessor.ITaskProgressAccessor;
import com.github.aeddddd.ae2enhanced.specialcrafting.RoundQuotaScheduler;
import com.github.aeddddd.ae2enhanced.specialcrafting.SelfRefOutputGate;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialCraftingRuntime;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanMarker;
import com.github.aeddddd.ae2enhanced.tile.TileComputationCore;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * 特殊合成执行层（自消耗/循环链计划）.
 *
 * <p>实现 {@link ISpecialCpuAccess} 供 SelfRefOutputGate / RoundQuotaScheduler 访问集群内部状态，
 * 并注入 job 提交/完成/取消钩子与超轮配额否决逻辑。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false, priority = 1000)
public abstract class MixinCraftingCPUClusterSpecial implements ISpecialCpuAccess {

    @Shadow
    private Map<ICraftingPatternDetails, Object> tasks;

    @Shadow
    private IItemList<IAEItemStack> waitingFor;

    @Shadow
    private IAEItemStack finalOutput;

    @Shadow
    private appeng.api.networking.crafting.ICraftingLink myLastLink;

    @Shadow
    private void postChange(IAEItemStack diff, appeng.api.networking.security.IActionSource src) {
    }

    @Shadow
    private void postCraftingStatusChange(IAEItemStack diff) {
    }

    @Shadow
    private void updateRemainingItemCount(IAEItemStack is) {
    }

    @Shadow
    private void updateCPU() {
    }

    @Shadow
    private void completeJob() {
    }

    // ==================== ISpecialCpuAccess ====================

    @Override
    public Map<ICraftingPatternDetails, Object> ae2e$tasks() {
        return this.tasks;
    }

    @Override
    public IItemList<IAEItemStack> ae2e$waitingFor() {
        return this.waitingFor;
    }

    @Override
    public IAEItemStack ae2e$finalOutput() {
        return this.finalOutput;
    }

    @Override
    public appeng.api.networking.crafting.ICraftingLink ae2e$myLastLink() {
        return this.myLastLink;
    }

    @Override
    public void ae2e$postChange(IAEItemStack diff, appeng.api.networking.security.IActionSource src) {
        this.postChange(diff, src);
    }

    @Override
    public void ae2e$postCraftingStatusChange(IAEItemStack diff) {
        this.postCraftingStatusChange(diff);
    }

    @Override
    public void ae2e$updateRemainingItemCount(IAEItemStack is) {
        this.updateRemainingItemCount(is);
    }

    @Override
    public void ae2e$markDirtyCluster() {
        TileComputationCore core = ((IComputationCoreAccess) this).ae2enhanced$getComputationCore();
        if (core != null) {
            core.markDirty();
        }
    }

    @Override
    public void ae2e$updateCPU() {
        this.updateCPU();
    }

    @Override
    public void ae2e$completeJob() {
        this.completeJob();
    }

    // ==================== Special Crafting Execution Layer ====================

    /**
     * 特殊计划提交成功:标记集群(启用交付门控)+ 快照 tasks 总次数(供超轮配额推导).
     */
    @Inject(method = "submitJob", at = @At("RETURN"), require = 0)
    private void ae2enhanced$onSpecialJobSubmitted(IGrid g, appeng.api.networking.crafting.ICraftingJob job,
            appeng.api.networking.security.IActionSource src,
            appeng.api.networking.crafting.ICraftingRequester requestingMachine,
            CallbackInfoReturnable<appeng.api.networking.crafting.ICraftingLink> cir) {
        if (cir.getReturnValue() == null || !SpecialPlanMarker.isSpecial(job)) {
            return;
        }
        CraftingCPUCluster self = (CraftingCPUCluster) (Object) this;
        SpecialCraftingRuntime.tagCluster(self);
        Map<ICraftingPatternDetails, Long> totals = new java.util.LinkedHashMap<>();
        for (Map.Entry<ICraftingPatternDetails, Object> entry : this.tasks.entrySet()) {
            totals.put(entry.getKey(), ((ITaskProgressAccessor) entry.getValue()).ae2e$getValue());
        }
        RoundQuotaScheduler.snapshot(self, totals);
        AE2Enhanced.LOGGER.info("[特殊配方] 特殊计划已提交到计算核心集群: {}", job.getOutput());
    }

    /**
     * job 完成/取消:解除特殊标记并清理配额快照.
     */
    @Inject(method = "completeJob", at = @At("HEAD"), require = 0)
    private void ae2enhanced$onCompleteJob(CallbackInfo ci) {
        CraftingCPUCluster self = (CraftingCPUCluster) (Object) this;
        if (SpecialCraftingRuntime.isSpecialCluster(self)) {
            SpecialCraftingRuntime.untagCluster(self);
            RoundQuotaScheduler.clear(self);
        }
    }

    @Inject(method = "cancel", at = @At("HEAD"), require = 0)
    private void ae2enhanced$onCancel(CallbackInfo ci) {
        CraftingCPUCluster self = (CraftingCPUCluster) (Object) this;
        if (SpecialCraftingRuntime.isSpecialCluster(self)) {
            SpecialCraftingRuntime.untagCluster(self);
            RoundQuotaScheduler.clear(self);
        }
    }

    /**
     * 自消耗 job（自引用/循环链计划,最终产出仍是任务输入）的交付门控:
     * 最终产出先入 CPU 库存,全部任务收官后一次性交付,防止边产边交付饿死合成链.
     * 仅对被标记的集群生效;普通 job 判定为不接管时零影响.
     */
    @Inject(method = "injectItems", at = @At("HEAD"), cancellable = true, require = 0)
    private void ae2enhanced$gateSelfConsumingOutput(IAEItemStack input, appeng.api.config.Actionable type,
            appeng.api.networking.security.IActionSource src, CallbackInfoReturnable<IAEItemStack> cir) {
        SelfRefOutputGate.GateResult result = SelfRefOutputGate.handleInsert(
                (CraftingCPUCluster) (Object) this, input, type, src);
        if (result.handled) {
            cir.setReturnValue(result.leftover);
        }
    }

    /**
     * 超轮配额调度:逐次推送否决——超配额的闭包 pattern 令 canCraft 返回 false,
     * 原生视同"输入不足"自然跳过,下一拍配额前进后自动恢复.
     * 仅对被标记集群上的自消耗 job 生效.
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/cluster/implementations/CraftingCPUCluster;canCraft(Lappeng/api/networking/crafting/ICraftingPatternDetails;[Lappeng/api/storage/data/IAEItemStack;)Z"
        ),
        require = 0
    )
    private boolean ae2enhanced$vetoPushOverQuota(CraftingCPUCluster self, ICraftingPatternDetails details,
            IAEItemStack[] condensedInputs, Operation<Boolean> original) {
        if (SpecialCraftingRuntime.isSpecialCluster(self)) {
            Map<ICraftingPatternDetails, Long> remaining = new java.util.LinkedHashMap<>();
            for (Map.Entry<ICraftingPatternDetails, Object> entry : this.tasks.entrySet()) {
                remaining.put(entry.getKey(), ((ITaskProgressAccessor) entry.getValue()).ae2e$getValue());
            }
            if (RoundQuotaScheduler.shouldVetoPush(self, details, remaining, this.finalOutput)) {
                return false; // 超配额:视同输入不足,本拍跳过该 pattern
            }
        }
        return original.call(self, details, condensedInputs);
    }
}
