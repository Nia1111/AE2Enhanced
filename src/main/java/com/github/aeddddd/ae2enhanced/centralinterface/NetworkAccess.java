package com.github.aeddddd.ae2enhanced.centralinterface;

import appeng.api.AEApi;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.ticking.ITickManager;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * AE2 网络访问门面.
 *
 * <p>集中封装中枢接口对 appeng.me / appeng.util <b>内部实现层</b>的全部调用
 * （{@link MachineSource}、{@link Platform#poweredInsert}、
 * {@link Platform#spawnDrops}、{@link AENetworkProxy} 包装、网格事件、tick 唤醒）。
 * 核心逻辑（Duality / Dispatcher / Engine）只依赖本门面、
 * {@link IGridConnection} 与 appeng.api。</p>
 */
public final class NetworkAccess {

    private NetworkAccess() {
    }

    /**
     * 将 AE2 网络代理包装为 {@link IGridConnection} seam。
     */
    public static IGridConnection connection(AENetworkProxy proxy) {
        return new ProxyGridConnection(proxy);
    }

    /**
     * 创建以指定宿主为动作源的 {@link MachineSource}。
     */
    public static MachineSource machineSource(IActionHost host) {
        return new MachineSource(host);
    }

    /**
     * 将物品堆注入 AE 物品网络（耗能），返回未能注入的剩余部分。
     */
    public static IAEItemStack poweredInsertItem(IGridConnection grid, IActionHost host, IAEItemStack stack)
            throws GridAccessException {
        IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        return Platform.poweredInsert(
                grid.energy(),
                grid.storage().getInventory(channel),
                stack,
                new MachineSource(host));
    }

    /**
     * 将流体堆注入 AE 流体网络（耗能），返回未能注入的剩余部分。
     */
    public static IAEFluidStack poweredInsertFluid(IGridConnection grid, IActionHost host, IAEFluidStack stack)
            throws GridAccessException {
        IFluidStorageChannel channel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        return Platform.poweredInsert(
                grid.energy(),
                grid.storage().getInventory(channel),
                stack,
                new MachineSource(host));
    }

    /**
     * 在世界中掉落物品列表。
     */
    public static void spawnDrops(World world, BlockPos pos, List<ItemStack> stacks) {
        Platform.spawnDrops(world, pos, stacks);
    }

    /**
     * 将流体列表直接注入 AE 流体网络，不再创建流体假物品。
     *
     * @return 未能注入网络的溢出流体列表；若全部注入成功则返回空列表
     */
    public static List<FluidStack> injectFluidsToNetwork(IGridConnection grid, IActionHost host, List<FluidStack> fluids) {
        List<FluidStack> overflow = new ArrayList<>();
        try {
            IFluidStorageChannel channel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
            for (FluidStack fluid : fluids) {
                if (fluid == null || fluid.amount <= 0) continue;
                IAEFluidStack toInsert = channel.createStack(fluid);
                if (toInsert == null) {
                    overflow.add(fluid.copy());
                    continue;
                }
                IAEFluidStack remaining = poweredInsertFluid(grid, host, toInsert);
                if (remaining != null && remaining.getStackSize() > 0) {
                    FluidStack leftover = remaining.getFluidStack();
                    AE2Enhanced.LOGGER.warn("[AE2E] CentralInterface fluid overflow: {} mb of {}", leftover.amount, leftover.getFluid().getName());
                    overflow.add(leftover.copy());
                }
            }
        } catch (GridAccessException e) {
            AE2Enhanced.LOGGER.warn("[AE2E] CentralInterface failed to inject fluids to network", e);
            for (FluidStack fluid : fluids) {
                if (fluid != null && fluid.amount > 0) {
                    overflow.add(fluid.copy());
                }
            }
        }
        return overflow;
    }

    /**
     * {@link AENetworkProxy} 的 {@link IGridConnection} 实现。
     */
    private static final class ProxyGridConnection implements IGridConnection {

        private final AENetworkProxy proxy;

        ProxyGridConnection(AENetworkProxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public boolean isActive() {
            return this.proxy.isActive();
        }

        @Override
        public boolean isReady() {
            return this.proxy.isReady();
        }

        @Override
        public IStorageGrid storage() throws GridAccessException {
            return this.proxy.getStorage();
        }

        @Override
        public IEnergySource energy() throws GridAccessException {
            return this.proxy.getEnergy();
        }

        @Override
        public void postPatternChange(ICentralInterfaceHost host) {
            try {
                if (this.proxy.isReady()) {
                    this.proxy.getGrid().postEvent(new MENetworkCraftingPatternChange(host, this.proxy.getNode()));
                }
            } catch (GridAccessException e) {
                // ignore
            }
        }

        @Override
        public void wakeTickDevice(ICentralInterfaceHost host) {
            try {
                if (this.proxy.isActive()) {
                    ITickManager tm = this.proxy.getTick();
                    if (tm != null) {
                        tm.wakeDevice(this.proxy.getNode());
                    }
                }
            } catch (GridAccessException e) {
                AE2Enhanced.LOGGER.warn("[AE2E] Failed to wake tick device for CentralInterface", e);
            }
        }
    }
}
