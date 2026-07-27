package com.github.aeddddd.ae2enhanced.util.compat;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.core.AELog;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.container.ContainerOmniTerm;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * NAE2 (Neeve's AE2: Extended Life Additions) 缺失物品自动合成兼容层.
 *
 * <p>复刻 NAE2 的 Ctrl+JEI转移 自动下单流程,作用于 Omni 终端：
 * JEI 转移配方填充合成台后,收集仍缺失且网络内有可合成样板的原料,
 * 通过 NAE2 的虚拟样板(VirtualPatternDetails)机制直接提交合成任务并打开确认界面.</p>
 *
 * <p>所有 NAE2 类均通过反射访问(Class.forName + Method.invoke),本类不持有任何 NAE2 符号引用;
 * NAE2 未安装时所有调用静默跳过.</p>
 */
public final class NAE2CraftHelper {

    private static boolean initialized = false;
    private static boolean available = false;
    private static Constructor<?> virtualPatternDetailsCtor;
    private static Method beginCraftingJobFromDetails;

    private NAE2CraftHelper() {}

    /**
     * NAE2 及其虚拟样板机制是否可用.
     */
    public static synchronized boolean isAvailable() {
        if (!initialized) {
            initialized = true;
            try {
                if (!Loader.isModLoaded("nae2")) {
                    return false;
                }
                Class<?> vpd = Class.forName("co.neeve.nae2.common.helpers.VirtualPatternDetails");
                virtualPatternDetailsCtor = vpd.getConstructor(Iterable.class, Iterable.class);
                Class<?> extendedCache = Class.forName("co.neeve.nae2.common.interfaces.IExtendedCraftingGridCache");
                beginCraftingJobFromDetails = extendedCache.getMethod("beginCraftingJobFromDetails",
                        World.class, IGrid.class, IActionSource.class, IAEItemStack.class,
                        ICraftingPatternDetails.class, ICraftingCallback.class);
                available = true;
            } catch (Throwable t) {
                AE2Enhanced.LOGGER.warn("[AE2E] NAE2 detected but virtual pattern API unavailable: {}", t.toString());
                available = false;
            }
        }
        return available;
    }

    /**
     * JEI 转移后尝试自动合成缺失原料.
     *
     * @param autoStart 是否在确认界面自动开始合成
     */
    public static void tryCraftMissing(@Nonnull EntityPlayerMP player, @Nonnull ContainerOmniTerm container,
                                       @Nonnull Map<Integer, ItemStack> inputs, @Nonnull Map<Integer, ItemStack> outputs,
                                       boolean autoStart) {
        if (!isAvailable()) return;

        Future<ICraftingJob> futureJob = null;
        try {
            IGridNode node = container.getNetworkNode();
            if (node == null) return;
            IGrid grid = node.getGrid();
            if (grid == null) return;
            ICraftingGrid cg = grid.getCache(ICraftingGrid.class);

            IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IItemHandler craftMatrix = container.getInventoryByName("crafting");
            if (craftMatrix == null || craftMatrix.getSlots() != 9) return;

            // 收集填充后仍缺失、且网络内有可合成样板的原料
            List<IAEItemStack> missing = new ArrayList<>();
            for (Map.Entry<Integer, ItemStack> entry : inputs.entrySet()) {
                int slot = entry.getKey();
                if (slot < 0 || slot >= 9) continue;
                if (!craftMatrix.getStackInSlot(slot).isEmpty()) continue;
                ItemStack required = entry.getValue();
                if (required == null || required.isEmpty()) continue;
                IAEItemStack ae = channel.createStack(required);
                if (ae == null) continue;
                if (cg.getCraftingFor(ae, null, 0, player.world).isEmpty()) continue; // 无可合成样板,跳过
                missing.add(ae);
            }
            if (missing.isEmpty()) return;

            // 仅支持单输出配方
            if (outputs.size() != 1) return;
            ItemStack outputStack = outputs.values().iterator().next();
            if (outputStack == null || outputStack.isEmpty()) return;
            IAEItemStack outputAE = channel.createStack(outputStack);
            if (outputAE == null) return;

            // 构造 NAE2 虚拟样板并通过扩展的 CraftingGridCache 提交任务
            Object details = virtualPatternDetailsCtor.newInstance(missing, Collections.singletonList(outputAE));
            IActionSource src = container.getActionSource();
            futureJob = (Future<ICraftingJob>) beginCraftingJobFromDetails.invoke(
                    cg, player.world, grid, src, outputAE.copy(), details, null);

            // 打开合成确认界面
            ContainerOpenContext context = container.getOpenContext();
            if (context == null) {
                futureJob.cancel(true);
                return;
            }
            TileEntity te = context.getTile();
            if (te != null) {
                Platform.openGUI(player, te, context.getSide(), GuiBridge.GUI_CRAFTING_CONFIRM);
            } else {
                IInventorySlotAware slotAware = container;
                Platform.openGUI(player, slotAware.getInventorySlot(), GuiBridge.GUI_CRAFTING_CONFIRM, slotAware.isBaubleSlot());
            }
            if (player.openContainer instanceof ContainerCraftConfirm) {
                ContainerCraftConfirm ccc = (ContainerCraftConfirm) player.openContainer;
                ccc.setAutoStart(autoStart);
                ccc.setJob(futureJob);
            } else {
                futureJob.cancel(true);
            }
        } catch (Throwable t) {
            if (futureJob != null) {
                futureJob.cancel(true);
            }
            AE2Enhanced.LOGGER.warn("[AE2E] NAE2 craft-missing failed: {}", t.toString());
            AELog.debug(t);
        }
    }
}
