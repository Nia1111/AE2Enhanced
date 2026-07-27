package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.events.MENetworkCraftingCpuChange;
import appeng.api.networking.security.IActionSource;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.crafting.CraftingLink;
import com.github.aeddddd.ae2enhanced.tile.TileComputationCore;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import net.minecraft.world.World;

import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.storage.data.IAEItemStack;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialCraftingJob;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialCraftingRuntime;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanMarker;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialRecipeDetector;

/**
 * Mixin into {@link CraftingGridCache} to recognise {@link TileComputationCore} virtual CraftingCPUClusters.
 *
 * <p>AE2-UEL stores CPUs in {@code Set<CraftingCPUCluster>} and rebuilds it from physical
 * {@link appeng.tile.crafting.TileCraftingStorageTile} machines. This mixin:</p>
 * <ul>
 *   <li>Tracks {@link TileComputationCore} instances via addNode/removeNode</li>
 *   <li>Re-injects virtual clusters into {@code craftingCPUClusters} after each rebuild</li>
 *   <li>Provides fallback job submission that dynamically spawns new virtual clusters</li>
 * </ul>
 */
@Mixin(value = CraftingGridCache.class, remap = false)
public class MixinCraftingGridCache {

    @Shadow
    @Final
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Shadow
    @Final
    private IGrid grid;

    @Shadow
    public void updateCPUClusters(MENetworkCraftingCpuChange event) {
        // shadow
    }

    @Shadow
    public void addLink(CraftingLink link) {
        // shadow
    }

    @Unique
    private final Set<TileComputationCore> ae2enhanced$computationCores = new HashSet<>();

    @Shadow
    @Final
    private static ExecutorService CRAFTING_POOL;

    // ==================== Special Crafting Routing (Point A: Calculation) ====================

    /**
     * 特殊配方路由（计算请求分流）:detector 命中才提交 {@link SpecialCraftingJob}
     * 并复用原生 CRAFTING_POOL 线程池;未命中/异常时直接放行,原生行为零改动.
     */
    @Inject(method = "beginCraftingJob", at = @At("HEAD"), cancellable = true, require = 0)
    private void ae2enhanced$routeSpecialCalculation(World world, IGrid grid, IActionSource actionSrc,
            IAEItemStack slotItem, ICraftingCallback cb, CallbackInfoReturnable<Future<ICraftingJob>> cir) {
        try {
            if (!SpecialCraftingRuntime.isEnabled() || world == null || grid == null
                    || actionSrc == null || slotItem == null) {
                return;
            }
            ICraftingGrid cc = grid.getCache(ICraftingGrid.class);
            if (!SpecialRecipeDetector.mayInvolveSpecialRecipes(cc, slotItem, world)) {
                return;
            }
            AE2Enhanced.LOGGER.info("[特殊配方] 路由命中,提交专用求解器: {}", slotItem);
            SpecialCraftingJob job = new SpecialCraftingJob(world, grid, actionSrc, slotItem, cb);
            cir.setReturnValue(CRAFTING_POOL.submit(job, job));
        } catch (Throwable t) {
            // 宁可漏判不可误判:路由层任何异常都放行原生
            AE2Enhanced.LOGGER.warn("[特殊配方] 路由判定异常,放行原生计算: {}", t.toString());
        }
    }

    // ==================== Special Crafting Routing (Point B: Submission) ====================

    /**
     * 特殊配方路由（任务提交分流）:特殊计划（{@link SpecialPlanMarker} 标记）独占路由到
     * 超因果计算核心的虚拟 CPU 集群,不回落普通 CPU,防止语义错误的执行;
     * 普通计划直接放行（由原生与下方 fallback 处理）.
     */
    @Inject(method = "submitJob", at = @At("HEAD"), cancellable = true, require = 0)
    private void ae2enhanced$routeSpecialJob(ICraftingJob job, ICraftingRequester requestingMachine,
            ICraftingCPU target, boolean prioritizePower, IActionSource src,
            CallbackInfoReturnable<ICraftingLink> cir) {
        if (!SpecialPlanMarker.isSpecial(job)) {
            return;
        }
        // 与原生相同的先序校验:模拟(缺料)计划一律拒绝
        if (job.isSimulation()) {
            cir.setReturnValue(null);
            return;
        }
        // 手动指定 CPU:只接受计算核心集群,否则拒绝
        if (target != null) {
            if (target instanceof CraftingCPUCluster && ae2enhanced$isCoreCluster((CraftingCPUCluster) target)) {
                CraftingCPUCluster cluster = (CraftingCPUCluster) target;
                if (cluster.isActive() && !cluster.isBusy()) {
                    cir.setReturnValue(cluster.submitJob(this.grid, job, src, requestingMachine));
                    return;
                }
            }
            cir.setReturnValue(null);
            return;
        }
        // 自动分配:仅从计算核心集群中选择
        for (TileComputationCore core : ae2enhanced$computationCores) {
            if (!core.isFormed()) {
                continue;
            }
            ICraftingLink link = core.trySpawnAndSubmitJob(this.grid, job, src, requestingMachine);
            if (link != null) {
                cir.setReturnValue(link);
                return;
            }
        }
        AE2Enhanced.LOGGER.warn("[特殊配方] 特殊计划无可用计算核心,提交失败: {}", job.getOutput());
        cir.setReturnValue(null);
    }

    @Unique
    private boolean ae2enhanced$isCoreCluster(CraftingCPUCluster cluster) {
        for (TileComputationCore core : ae2enhanced$computationCores) {
            List<CraftingCPUCluster> pool = core.getCpuPool();
            if (pool != null && pool.contains(cluster)) {
                return true;
            }
        }
        return false;
    }

    // ==================== Node Lifecycle ====================

    @Inject(method = "addNode", at = @At("HEAD"))
    private void ae2enhanced$onAddNode(IGridNode node, IGridHost host, CallbackInfo ci) {
        if (host instanceof TileComputationCore) {
            TileComputationCore core = (TileComputationCore) host;
            ae2enhanced$computationCores.add(core);
            if (core.isFormed()) {
                updateCPUClusters(new MENetworkCraftingCpuChange(node));
            }
        }
    }

    @Inject(method = "removeNode", at = @At("HEAD"))
    private void ae2enhanced$onRemoveNode(IGridNode node, IGridHost host, CallbackInfo ci) {
        if (host instanceof TileComputationCore) {
            TileComputationCore core = (TileComputationCore) host;
            ae2enhanced$computationCores.remove(core);
            updateCPUClusters(new MENetworkCraftingCpuChange(node));
        }
    }

    // ==================== CPU Cluster Rebuild ====================

    @Inject(method = "updateCPUClusters()V", at = @At("TAIL"))
    private void ae2enhanced$injectComputationCores(CallbackInfo ci) {
        int injected = 0;
        for (TileComputationCore core : ae2enhanced$computationCores) {
            if (core.isFormed()) {
                List<CraftingCPUCluster> pool = core.getCpuPool();
                if (pool != null) {
                    for (CraftingCPUCluster cpu : pool) {
                        this.craftingCPUClusters.add(cpu);
                        injected++;
                        if (cpu.getLastCraftingLink() != null) {
                            this.addLink((CraftingLink) cpu.getLastCraftingLink());
                        }
                    }
                }
            }
        }
        // virtual CPUs injected silently
    }

    // ==================== Job Submission Fallback ====================

    @Inject(method = "submitJob", at = @At("RETURN"), cancellable = true)
    private void ae2enhanced$submitJobFallback(ICraftingJob job, ICraftingRequester requestingMachine,
                                                ICraftingCPU target, boolean prioritizePower, IActionSource src,
                                                CallbackInfoReturnable<ICraftingLink> cir) {
        if (cir.getReturnValue() != null) {
            return; // original already succeeded
        }
        if (job == null || job.isSimulation()) {
            return;
        }
        if (target != null) {
            return; // explicit target was busy or invalid; do not spawn behind user's back
        }
        for (TileComputationCore core : ae2enhanced$computationCores) {
            if (!core.isFormed()) continue;
            ICraftingLink link = core.trySpawnAndSubmitJob(grid, job, src, requestingMachine);
            if (link != null) {
                cir.setReturnValue(link);
                return;
            }
        }
    }

    // ==================== hasCpu ====================

    @Inject(method = "hasCpu", at = @At("HEAD"), cancellable = true)
    private void ae2enhanced$hasCpu(ICraftingCPU cpu, CallbackInfoReturnable<Boolean> cir) {
        if (cpu instanceof CraftingCPUCluster) {
            for (TileComputationCore core : ae2enhanced$computationCores) {
                List<CraftingCPUCluster> pool = core.getCpuPool();
                if (pool != null && pool.contains(cpu)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}
