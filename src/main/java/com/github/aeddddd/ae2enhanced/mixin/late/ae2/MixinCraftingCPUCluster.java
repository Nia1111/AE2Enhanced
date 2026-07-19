package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.cache.CraftingGridCache;
import appeng.me.helpers.MachineSource;
import appeng.tile.crafting.TileCraftingMonitorTile;
import appeng.tile.crafting.TileCraftingTile;
import appeng.container.ContainerNull;
import net.minecraft.inventory.Container;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.centralinterface.DualityCentralInterface;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyMeInterface;
import com.github.aeddddd.ae2enhanced.tile.TileCentralMEInterface;
import com.github.aeddddd.ae2enhanced.tile.TileComputationCore;
import com.github.aeddddd.ae2enhanced.mixin.bridge.IComputationCoreAccess;
import com.github.aeddddd.ae2enhanced.mixin.late.accessor.ITaskProgressAccessor;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = CraftingCPUCluster.class, remap = false, priority = 1000)
public class MixinCraftingCPUCluster implements IComputationCoreAccess {

    private static final boolean CRAZYAE_LOADED =
        net.minecraftforge.fml.common.Loader.isModLoaded("crazyae");

    // ==================== Computation Core Support ====================

    @Unique
    private TileComputationCore ae2enhanced$computationCore;

    @Override
    public void ae2enhanced$setComputationCore(TileComputationCore core) {
        this.ae2enhanced$computationCore = core;
    }

    @Override
    public TileComputationCore ae2enhanced$getComputationCore() {
        return this.ae2enhanced$computationCore;
    }

    @Shadow
    private MachineSource machineSrc;

    @Shadow
    private String myName;

    @Shadow
    private boolean isDestroyed;

    @Shadow
    private List<TileCraftingTile> tiles;

    @Shadow
    private List<TileCraftingMonitorTile> status;

    @Shadow
    private void updateCPU() {
    }

    @Shadow
    private Map<ICraftingPatternDetails, Object> tasks;

    @Shadow
    private int remainingOperations;

    @Shadow
    private long remainingItemCount;

    @Shadow
    private boolean isComplete;

    @Shadow
    private IItemList<IAEItemStack> waitingFor;

    @Shadow
    private IAEItemStack finalOutput;

    @Shadow
    private void postChange(IAEItemStack diff, appeng.api.networking.security.IActionSource src) {
    }

    @Shadow
    private void postCraftingStatusChange(IAEItemStack diff) {
    }

    @Shadow
    private void completeJob() {
    }

    // ==================== Overwrites for Virtual Clusters ====================

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void onIsActive(CallbackInfoReturnable<Boolean> cir) {
        if (ae2enhanced$computationCore != null) {
            IGridNode node = ae2enhanced$computationCore.getActionableNode();
            cir.setReturnValue(node != null && node.isActive());
        }
    }

    @Inject(method = "markDirty", at = @At("HEAD"), cancellable = true)
    private void onMarkDirty(CallbackInfo ci) {
        if (ae2enhanced$computationCore != null) {
            ae2enhanced$computationCore.markDirty();
            ci.cancel();
        }
    }

    @Inject(method = "getGrid", at = @At("HEAD"), cancellable = true)
    private void onGetGrid(CallbackInfoReturnable<IGrid> cir) {
        if (ae2enhanced$computationCore != null) {
            IGridNode node = ae2enhanced$computationCore.getActionableNode();
            cir.setReturnValue(node != null ? node.getGrid() : null);
        }
    }

    @Inject(method = "getWorld", at = @At("HEAD"), cancellable = true)
    private void onGetWorld(CallbackInfoReturnable<World> cir) {
        if (ae2enhanced$computationCore != null) {
            cir.setReturnValue(ae2enhanced$computationCore.getWorld());
        }
    }

    @Inject(method = "getCore", at = @At("HEAD"), cancellable = true)
    private void onGetCore(CallbackInfoReturnable<TileCraftingTile> cir) {
        if (ae2enhanced$computationCore != null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "updateName", at = @At("HEAD"), cancellable = true)
    public void onUpdateName(CallbackInfo ci) {
        if (ae2enhanced$computationCore != null) {
            this.myName = net.minecraft.util.text.translation.I18n.translateToLocal("tile.ae2enhanced.computation_core.name");
            ci.cancel();
        }
    }

    @Inject(method = "breakCluster", at = @At("HEAD"), cancellable = true)
    public void onBreakCluster(CallbackInfo ci) {
        if (ae2enhanced$computationCore != null) {
            ci.cancel();
        }
    }

    @Inject(method = "done", at = @At("HEAD"), cancellable = true)
    void onDone(CallbackInfo ci) {
        if (ae2enhanced$computationCore != null) {
            ci.cancel();
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    public void onDestroy(CallbackInfo ci) {
        if (ae2enhanced$computationCore != null) {
            ci.cancel();
        }
    }

    @Inject(method = "updateStatus", at = @At("HEAD"), cancellable = true)
    public void onUpdateStatus(boolean updateGrid, CallbackInfo ci) {
        if (ae2enhanced$computationCore != null) {
            ci.cancel();
        }
    }

    // ==================== Processing Pattern Table Resize ====================

    /**
     * AE2 CPU 对处理样板固定构造 4×4 的 InventoryCrafting。
     * 当处理样板输入数超过 16 时，executeCrafting 在写入第 17 个槽位时会越界。
     * 这里在处理样板的 table 构造完成后，按实际输入数扩容到 5×5~10×10，
     * 避免物品被丢弃或崩溃。
     */
    @ModifyVariable(
        method = "executeCrafting",
        at = @At(value = "INVOKE_ASSIGN",
                 target = "Lnet/minecraft/inventory/InventoryCrafting;<init>(Lnet/minecraft/inventory/Container;II)V"),
        ordinal = 0
    )
    private InventoryCrafting ae2enhanced$resizeInventoryCrafting(
            InventoryCrafting ic,
            @Local ICraftingPatternDetails details) {
        if (details == null || details.isCraftable()) {
            return ic;
        }
        IAEItemStack[] inputs = details.getInputs();
        if (inputs == null || inputs.length <= ic.getSizeInventory()) {
            return ic;
        }
        int size = Math.max(4, (int) Math.ceil(Math.sqrt(inputs.length)));
        if (size > 10) {
            size = 10;
        }
        InventoryCrafting larger = new InventoryCrafting(new ContainerNull(), size, size);
        for (int i = 0; i < ic.getSizeInventory(); i++) {
            ItemStack stack = ic.getStackInSlot(i);
            if (!stack.isEmpty()) {
                larger.setInventorySlotContents(i, stack.copy());
            }
        }
        return larger;
    }

    // ==================== Batch Crafting (Assembly Hub) — retained ====================

    private static int batchCallCount = 0;
    private static int batchSuccessCount = 0;
    private static int batchFailCount = 0;

    private static appeng.api.storage.data.IAEItemStack fetchFromNetwork(
            CraftingCPUCluster cpu,
            appeng.api.storage.data.IAEItemStack request,
            appeng.api.networking.security.IActionSource source) {
        try {
            appeng.api.networking.security.IActionSource src = cpu.getActionSource();
            if (src instanceof appeng.me.helpers.MachineSource) {
                java.util.Optional<appeng.api.networking.security.IActionHost> hostOpt =
                    ((appeng.me.helpers.MachineSource) src).machine();
                if (hostOpt.isPresent()) {
                    appeng.api.networking.IGridNode node = hostOpt.get().getActionableNode();
                    if (node != null) {
                        appeng.api.networking.IGrid grid = node.getGrid();
                        if (grid != null) {
                            appeng.api.networking.storage.IStorageGrid sg =
                                grid.getCache(appeng.api.networking.storage.IStorageGrid.class);
                            appeng.api.storage.channels.IItemStorageChannel channel =
                                appeng.api.AEApi.instance().storage().getStorageChannel(
                                    appeng.api.storage.channels.IItemStorageChannel.class);
                            appeng.api.storage.IMEMonitor<appeng.api.storage.data.IAEItemStack> storage =
                                sg.getInventory(channel);
                            return storage.extractItems(request, appeng.api.config.Actionable.MODULATE, source);
                        }
                    }
                }
            }
        } catch (Exception e) {
            AE2Enhanced.LOGGER.error("[AE2E] fetchFromNetwork failed: {}", e.toString());
        }
        return null;
    }

    @Redirect(
        method = "updateCraftingLogic",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/tile/crafting/TileCraftingTile;isActive()Z"
        )
    )
    private boolean redirectIsActive(TileCraftingTile instance) {
        if (ae2enhanced$computationCore != null) {
            // CrazyAE 兼容：保留默认行为,避免干扰其修改后的 isActive 逻辑.
            if (!CRAZYAE_LOADED) {
                IGridNode node = ae2enhanced$computationCore.getActionableNode();
                return node != null && node.isActive();
            }
        }
        // 防御：某些情况下 getCore() 可能返回 null(如集群尚未完全初始化),
        // 此时应视为 inactive,避免 NPE.
        if (instance == null) {
            return false;
        }
        return instance.isActive();
    }

    @Inject(method = "updateCraftingLogic", at = @At("HEAD"))
    private void onUpdateCraftingLogicHead(IGrid grid, IEnergyGrid eg, CraftingGridCache cache, CallbackInfo ci) {
        // CrazyAE 通过 ASM 大幅修改了 CraftingCPUCluster,虚拟集群的字段初始化
        // 与其状态机不兼容；跳过我们的 HEAD 注入以避免干扰 CrazyAE 逻辑.
        if (ae2enhanced$computationCore != null && CRAZYAE_LOADED) return;
        try {
            if (!this.isComplete && this.tasks.isEmpty()) {
                IItemList<IAEItemStack> waitingFor = this.waitingFor;
                boolean waitingForEmpty = true;
                if (waitingFor != null) {
                    for (IAEItemStack is : waitingFor) {
                        if (is != null && is.getStackSize() > 0) {
                            waitingForEmpty = false;
                            break;
                        }
                    }
                }
                if (waitingForEmpty) {
                    this.completeJob();
                    // 修复：completeJob() 不重置 finalOutput 也不调用 updateCPU(),
                    // 导致 Crafting Monitor 在任务完成后不清空
                    this.finalOutput = null;
                    updateCPU();
                }
            }
        } catch (Exception e) {
            AE2Enhanced.LOGGER.error("[AE2E] onUpdateCraftingLogicHead unexpected error: {}", e.toString());
        }
    }

    @Inject(method = "executeCrafting", at = @At("HEAD"))
    private void batchProcessVirtualTasks(IEnergyGrid energy, CraftingGridCache cache, CallbackInfo ci) {
        // CrazyAE 兼容：跳过批量合成注入,避免与其修改后的 executeCrafting 冲突.
        if (ae2enhanced$computationCore != null && CRAZYAE_LOADED) return;

        CraftingCPUCluster cpu;
        boolean anyOurTask = false;
        int virtualTasksFound = 0;
        int virtualTasksExecuted = 0;

        try {
            cpu = (CraftingCPUCluster) (Object) this;

            Map<ICraftingPatternDetails, Object> tasks = this.tasks;
            if (tasks.isEmpty()) return;

            IItemList<IAEItemStack> waitingFor = this.waitingFor;

            boolean changed;
            int doWhileIterations = 0;
            do {
                changed = false;
                for (Map.Entry<ICraftingPatternDetails, Object> entry : new ArrayList<>(tasks.entrySet())) {
                    ICraftingPatternDetails details = entry.getKey();
                    Object progress = entry.getValue();

                    long remaining = ((ITaskProgressAccessor) progress).ae2e$getValue();
                    if (remaining <= 0) continue;

                    List<ICraftingMedium> mediums = cache.getMediums(details);
                    if (mediums == null || mediums.isEmpty()) continue;

                    for (ICraftingMedium medium : mediums) {
                        if (!(medium instanceof TileAssemblyMeInterface)) continue;
                        anyOurTask = true;

                        TileAssemblyController controller = ((TileAssemblyMeInterface) medium).getController();
                        if (controller == null) continue;

                        if (!controller.isVirtualPattern(details)) {
                            if (!controller.canBatch()) break;

                            long cap = controller.getParallelCap();
                            long batchSize = (cap >= Long.MAX_VALUE / 2) ? remaining : Math.min(remaining, cap);
                            long actualBatchSize = batchSize;

                            appeng.api.networking.security.IActionSource source = cpu.getActionSource();
                            controller.setCurrentActionSource(source);
                            try {
                                appeng.crafting.MECraftingInventory meInv = (appeng.crafting.MECraftingInventory) cpu.getInventory();
                                appeng.api.config.Actionable SIMULATE = appeng.api.config.Actionable.SIMULATE;
                                appeng.api.config.Actionable MODULATE = appeng.api.config.Actionable.MODULATE;

                                TileAssemblyController.PatternBatchInfo info = controller.getPatternBatchInfo(details, meInv, source);
                                if (info == null || info.recipe == null || info.slotTemplates == null || info.catalystSlots == null) break;

                                if (info.transformSlots != null && info.transformSlots.cardinality() > 0) {
                                    actualBatchSize = 1;
                                }

                                int estimatedStacks = 1;
                                for (int i = 0; i < info.slotTemplates.length; i++) {
                                    if (info.slotTemplates[i] != null && !info.catalystSlots.get(i)) {
                                        estimatedStacks++;
                                    }
                                }
                                if (!controller.canAcceptRealBatch(estimatedStacks)) break;

                                boolean canExtract = true;
                                for (int i = 0; i < info.slotTemplates.length; i++) {
                                    if (info.slotTemplates[i] == null) continue;
                                    long needCount;
                                    if (info.catalystSlots.get(i) || info.transformSlots.get(i)) {
                                        needCount = 1;
                                    } else {
                                        needCount = actualBatchSize;
                                    }
                                    IAEItemStack need = info.slotTemplates[i].copy();
                                    need.setStackSize(needCount);
                                    IAEItemStack simResult = meInv.extractItems(need, SIMULATE, source);
                                    if (simResult == null || simResult.getStackSize() < needCount) {
                                        if (info.catalystSlots.get(i) || info.transformSlots.get(i)) {
                                            IAEItemStack toFetch = info.slotTemplates[i].copy();
                                            toFetch.setStackSize(1);
                                            IAEItemStack fetched = fetchFromNetwork(cpu, toFetch, source);
                                            if (fetched != null && fetched.getStackSize() > 0) {
                                                meInv.injectItems(fetched, MODULATE, source);
                                                simResult = meInv.extractItems(need, SIMULATE, source);
                                                if (simResult == null || simResult.getStackSize() < needCount) {
                                                    canExtract = false;
                                                }
                                            } else {
                                                canExtract = false;
                                            }
                                        } else {
                                            long missing = needCount - (simResult != null ? simResult.getStackSize() : 0);
                                            IAEItemStack toFetch = info.slotTemplates[i].copy();
                                            toFetch.setStackSize(missing);
                                            IAEItemStack fetched = fetchFromNetwork(cpu, toFetch, source);
                                            if (fetched != null && fetched.getStackSize() > 0) {
                                                meInv.injectItems(fetched, MODULATE, source);
                                                long nowAvailable = (simResult != null ? simResult.getStackSize() : 0)
                                                    + fetched.getStackSize();
                                                actualBatchSize = Math.min(actualBatchSize, nowAvailable);
                                            } else {
                                                actualBatchSize = Math.min(actualBatchSize,
                                                    simResult != null ? simResult.getStackSize() : 0);
                                            }
                                        }
                                    }
                                }
                                if (!canExtract || actualBatchSize <= 0) break;

                                for (int i = 0; i < info.slotTemplates.length; i++) {
                                    if (info.slotTemplates[i] == null) continue;
                                    long needCount;
                                    if (info.catalystSlots.get(i) || info.transformSlots.get(i)) {
                                        needCount = 1;
                                    } else {
                                        needCount = actualBatchSize;
                                    }
                                    IAEItemStack need = info.slotTemplates[i].copy();
                                    need.setStackSize(needCount);
                                    IAEItemStack extracted = meInv.extractItems(need, MODULATE, source);
                                    if (extracted != null && extracted.getStackSize() > 0) {
                                        IAEItemStack diff = extracted.copy();
                                        diff.setStackSize(-diff.getStackSize());
                                        this.postChange(diff, source);
                                        this.postCraftingStatusChange(diff);
                                    }
                                }

                                InventoryCrafting ic = new InventoryCrafting(new net.minecraft.inventory.Container() {
                                    @Override
                                    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
                                        return false;
                                    }
                                }, 3, 3);
                                for (int i = 0; i < info.slotTemplates.length; i++) {
                                    if (info.slotTemplates[i] == null) continue;
                                    ItemStack stack = info.slotTemplates[i].createItemStack();
                                    stack.setCount(1);
                                    ic.setInventorySlotContents(i, stack);
                                }

                                ItemStack output = info.recipe.getCraftingResult(ic);
                                NonNullList<ItemStack> recipeRemaining = info.recipe.getRemainingItems(ic);

                                if (!output.isEmpty()) {
                                    ItemStack batchOutput = output.copy();
                                    batchOutput.setCount(output.getCount() * (int) actualBatchSize);
                                    controller.addPendingOutput(batchOutput);
                                }

                                for (int i = 0; i < recipeRemaining.size(); i++) {
                                    ItemStack rem = recipeRemaining.get(i);
                                    if (rem.isEmpty()) continue;
                                    if (info.catalystSlots.get(i)) {
                                        IAEItemStack catalystReturn = info.slotTemplates[i].copy();
                                        catalystReturn.setStackSize(1);
                                        meInv.injectItems(catalystReturn, MODULATE, source);
                                    } else {
                                        ItemStack batchRem = rem.copy();
                                        batchRem.setCount(rem.getCount() * (int) actualBatchSize);
                                        controller.addPendingOutput(batchRem);
                                    }
                                }

                                long newRemaining = remaining - actualBatchSize;
                                ((ITaskProgressAccessor) progress).ae2e$setValue(newRemaining);

                                this.remainingOperations = (int) (this.remainingOperations - actualBatchSize);
                                long oldRemItemCount = this.remainingItemCount;
                                long totalOutputCount = 0;
                                for (IAEItemStack out : details.getCondensedOutputs()) {
                                    if (out != null) totalOutputCount += out.getStackSize() * actualBatchSize;
                                }
                                this.remainingItemCount = oldRemItemCount - totalOutputCount;

                                controller.setBatchBusy(true);
                                changed = true;
                                controller.resetBatchCooldown();


                            } catch (Exception e) {
                                AE2Enhanced.LOGGER.error("[AE2E] Real batch error: {}", e.toString());
                            } finally {
                                controller.setCurrentActionSource(null);
                            }
                            break;
                        }

                        if (!controller.canBatch()) continue;
                        virtualTasksFound++;

                        long cap = controller.getParallelCap();
                        long batchSize = (cap >= Long.MAX_VALUE / 2) ? remaining : Math.min(remaining, cap);

                        appeng.api.networking.security.IActionSource source = cpu.getActionSource();
                        controller.setCurrentActionSource(source);
                        try {
                            appeng.crafting.MECraftingInventory meInv = (appeng.crafting.MECraftingInventory) cpu.getInventory();
                            IItemList<IAEItemStack> itemList = meInv.getItemList();
                            appeng.api.config.Actionable SIMULATE = appeng.api.config.Actionable.SIMULATE;
                            appeng.api.config.Actionable MODULATE = appeng.api.config.Actionable.MODULATE;

                            boolean canExtract = true;
                            for (int retry = 0; retry < 5; retry++) {
                                canExtract = true;
                                for (IAEItemStack inputTemplate : details.getCondensedInputs()) {
                                    if (inputTemplate == null || inputTemplate.getStackSize() <= 0) continue;
                                    long totalNeed = inputTemplate.getStackSize() * batchSize;
                                    if (totalNeed <= 0) { canExtract = false; batchSize = 0; break; }
                                    IAEItemStack need = inputTemplate.copy();
                                    need.setStackSize(totalNeed);
                                    IAEItemStack simResult = meInv.extractItems(need, SIMULATE, source);
                                    if (simResult == null || simResult.getStackSize() < totalNeed) {
                                        long available = simResult != null ? simResult.getStackSize() : 0;
                                        long missing = totalNeed - available;
                                        if (missing > 0) {
                                            IAEItemStack toFetch = inputTemplate.copy();
                                            toFetch.setStackSize(missing);
                                            IAEItemStack fetched = fetchFromNetwork(cpu, toFetch, source);
                                            if (fetched != null && fetched.getStackSize() > 0) {
                                                meInv.injectItems(fetched, MODULATE, source);
                                                simResult = meInv.extractItems(need, SIMULATE, source);
                                                if (simResult != null && simResult.getStackSize() >= totalNeed) {
                                                    continue;
                                                }
                                                available = simResult != null ? simResult.getStackSize() : 0;
                                            }
                                        }
                                        long maxBatch = available / inputTemplate.getStackSize();
                                        if (maxBatch > 0) {
                                            batchSize = Math.min(batchSize, maxBatch);
                                            canExtract = false; // 需要重试
                                        } else {
                                            canExtract = false;
                                            batchSize = 0;
                                            break;
                                        }
                                    }
                                }
                                if (canExtract) break;
                            }
                            if (!canExtract || batchSize <= 0) {
                                continue;
                            }

                            for (IAEItemStack inputTemplate : details.getCondensedInputs()) {
                                if (inputTemplate == null || inputTemplate.getStackSize() <= 0) continue;
                                long totalNeed = inputTemplate.getStackSize() * batchSize;
                                if (totalNeed <= 0) continue;
                                IAEItemStack need = inputTemplate.copy();
                                need.setStackSize(totalNeed);
                                IAEItemStack extracted = meInv.extractItems(need, MODULATE, source);
                                if (extracted != null && extracted.getStackSize() > 0) {
                                    IAEItemStack diff = extracted.copy();
                                    diff.setStackSize(-diff.getStackSize());
                                    this.postChange(diff, source);
                                    this.postCraftingStatusChange(diff);
                                }
                            }

                            long totalOutputItems = 0;
                            for (IAEItemStack outputTemplate : details.getCondensedOutputs()) {
                                if (outputTemplate == null || outputTemplate.getStackSize() <= 0) continue;
                                long totalCount = outputTemplate.getStackSize() * batchSize;
                                if (totalCount <= 0) continue;
                                totalOutputItems += totalCount;

                                IAEItemStack product = outputTemplate.copy();
                                product.setStackSize(totalCount);
                                itemList.add(product);
                                this.postChange(product.copy(), source);
                                this.postCraftingStatusChange(product.copy());

                                if (waitingFor != null) {
                                    IAEItemStack waiting = waitingFor.findPrecise(outputTemplate);
                                    if (waiting != null) {
                                        waiting.decStackSize(totalCount);
                                        if (waiting.getStackSize() <= 0) {
                                            waiting.setStackSize(0);
                                        }
                                    }
                                }
                            }

                            long newRemaining = remaining - batchSize;
                            ((ITaskProgressAccessor) progress).ae2e$setValue(newRemaining);

                            controller.setBatchBusy(true);

                            changed = true;
                            virtualTasksExecuted++;
                            controller.resetBatchCooldown();


                        } finally {
                            controller.setCurrentActionSource(null);
                        }
                        break;
                    }
                }
                doWhileIterations++;
            } while (changed && doWhileIterations < 100000);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.error("[AE2E] batchProcessVirtualTasks unexpected error: {}", e.toString());
        } finally {
            batchCallCount++;
            if (virtualTasksExecuted > 0) {
                batchSuccessCount += virtualTasksExecuted;

            } else if (anyOurTask && batchCallCount % 20 == 1) {
                batchFailCount++;

            }
        }
    }

    // ==================== Central Interface Virtual Batch Correction ====================

    /**
     * 包装 CraftingCPUCluster 对 ICraftingMedium.pushPattern 的调用。
     *
     * <p>当中枢 ME 接口的虚拟批量合成一次处理了 N 份产物时，AE2 CPU 仍然只把这次 push
     * 当作 1 个操作。这里在 pushPattern 成功后，把额外的 (N-1) 份输出加入 waitingFor，
     * 并同步减少 taskProgress / remainingItemCount，使 CPU 任务计数与实际产出一致。</p>
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingMedium;pushPattern(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;)Z"
        )
    )
    private boolean ae2enhanced$wrapPushPatternForVirtualBatch(
            ICraftingMedium medium,
            ICraftingPatternDetails details,
            InventoryCrafting table,
            Operation<Boolean> original) {
        // 在调用 pushPattern 前把 CPU 任务剩余数传给中枢接口，防止实际并行超过订单需求
        if (medium instanceof TileCentralMEInterface) {
            DualityCentralInterface duality = ((TileCentralMEInterface) medium).getInterfaceDuality();
            long pending = -1;
            try {
                Map<ICraftingPatternDetails, Object> tasks = this.tasks;
                Object progress = tasks == null ? null : tasks.get(details);
                if (progress != null) {
                    pending = ((ITaskProgressAccessor) progress).ae2e$getValue();
                }
            } catch (Exception ignored) {
            }
            if (pending > 0) {
                duality.setNextVirtualBatchLimit(pending);
            }
            // 把 CPU 内部缓存（任务提交时已预提整单材料）设为虚拟批量的物品来源，
            // 虚拟并行的额外物品从该缓存核算/提取，而非从已被预留掏空的网络提取。
            duality.setVirtualItemSource(((CraftingCPUCluster) (Object) this).getInventory());
        }

        boolean result;
        try {
            result = original.call(medium, details, table);
        } finally {
            if (medium instanceof TileCentralMEInterface) {
                ((TileCentralMEInterface) medium).getInterfaceDuality().setVirtualItemSource(null);
            }
        }
        if (!result || !(medium instanceof TileCentralMEInterface)) {
            return result;
        }

        DualityCentralInterface duality = ((TileCentralMEInterface) medium).getInterfaceDuality();
        long batchSize = duality.getLastVirtualBatchSize();
        if (batchSize <= 1) {
            return true;
        }

        try {
            Map<ICraftingPatternDetails, Object> tasks = this.tasks;
            Object progress = tasks == null ? null : tasks.get(details);
            if (progress == null) {
                return true;
            }

            long remaining = ((ITaskProgressAccessor) progress).ae2e$getValue();
            long extra = Math.min(batchSize - 1, remaining - 1);
            if (extra <= 0) {
                return true;
            }

            // 减少任务剩余数（原方法还会再减 1，因此这里只减 extra）
            ((ITaskProgressAccessor) progress).ae2e$setValue(remaining - extra);

            // 同步减少 remainingItemCount
            long oldRemItemCount = this.remainingItemCount;
            long totalOutputCount = 0;
            for (IAEItemStack out : details.getCondensedOutputs()) {
                if (out != null) {
                    totalOutputCount += out.getStackSize() * extra;
                }
            }
            this.remainingItemCount = oldRemItemCount - totalOutputCount;

            // 把额外的预期产出加入 waitingFor，使 CPU 认为这些产物也已经被发出
            IItemList<IAEItemStack> waitingFor = this.waitingFor;
            if (waitingFor != null) {
                for (IAEItemStack out : details.getCondensedOutputs()) {
                    if (out == null || out.getStackSize() <= 0) {
                        continue;
                    }
                    IAEItemStack extraOut = out.copy();
                    extraOut.setStackSize(out.getStackSize() * extra);
                    waitingFor.add(extraOut);
                    this.postCraftingStatusChange(extraOut.copy());
                }
            }

        } catch (Exception e) {
            AE2Enhanced.LOGGER.error("[AE2E] Virtual batch CPU correction failed: {}", e.toString(), e);
        }
        return true;
    }
}
