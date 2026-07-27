package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.centralinterface.DualityCentralInterface;
import com.github.aeddddd.ae2enhanced.mixin.late.accessor.ITaskProgressAccessor;
import com.github.aeddddd.ae2enhanced.tile.TileCentralMEInterface;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * 中枢 ME 接口虚拟批量修正.
 *
 * <p>包装 CraftingCPUCluster 对 ICraftingMedium.pushPattern 的调用。
 * 当中枢 ME 接口一次处理 N 份产物时，把额外的 (N-1) 份输出计入 CPU 任务进度，
 * 使 CPU 任务计数与实际产出一致。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false, priority = 1000)
public abstract class MixinCraftingCPUClusterVirtualBatch {

    @Shadow
    private Map<ICraftingPatternDetails, Object> tasks;

    @Shadow
    private long remainingItemCount;

    @Shadow
    private IItemList<IAEItemStack> waitingFor;

    @Shadow
    private void postCraftingStatusChange(IAEItemStack diff) {
    }

    /**
     * 包装 CraftingCPUCluster 对 ICraftingMedium.pushPattern 的调用。
     *
     * <p>当中枢 ME 接口的虚拟批量合成一次处理了 N 份产物时，AE2 CPU 仍然只把这次 push
     * 当作 1 个操作。这里在 pushPattern 成功后，把额外的 (N-1) 份输出加入 waitingFor，
     * 并同步减少 taskProgress / remainingItemCount，使 CPU 任务计数与实际产出一致。</p>
     *
     * <p><b>中枢接口虚拟批量契约（本 Mixin ⇄ DualityCentralInterface / VirtualBatchEngine）：</b></p>
     * <ol>
     *   <li><b>调用前</b>：本 Mixin 通过 {@code setNextVirtualBatchLimit(pending)} 传入 CPU 任务剩余数
     *       （Duality 在一次 pushPattern 中消费后自动清零），并通过
     *       {@code setVirtualItemSource(cpu.getInventory())} 传入 CPU 内部物品缓存
     *       （MECraftingInventory，任务提交时已预提整单材料）。</li>
     *   <li><b>调用中</b>：VirtualBatchEngine 的物品成本仅从 virtualItemSource 核算/提取
     *       （网络已被任务预留掏空，从网络核算并行数会恒退化为 1）；
     *       非物品成本（流体/能量/Mana/Starlight/气体/源质）CPU 不预提取，从网络全额提取；
     *       实际并行数被 nextVirtualBatchLimit 钳制，不会超过订单剩余。</li>
     *   <li><b>调用后</b>：Duality 通过 {@code getLastVirtualBatchSize()} 报告实际并行数 N
     *       （物理发配记 1，未处理记 0）；本 Mixin 据此修正 taskProgress / remainingItemCount /
     *       waitingFor，保证 CPU 不多等也不少等产物。</li>
     *   <li><b>清理</b>：finally 中把 virtualItemSource 复位为 null，
     *       避免非 CPU 直推场景误用上一任务的缓存。</li>
     * </ol>
     *
     * <p>修改 DualityCentralInterface / VirtualBatchEngine 的虚拟批量逻辑时，必须同步审视本契约。</p>
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
