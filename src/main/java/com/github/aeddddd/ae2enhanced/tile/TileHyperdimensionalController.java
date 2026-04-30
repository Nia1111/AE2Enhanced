package com.github.aeddddd.ae2enhanced.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ISaveProvider;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import com.github.aeddddd.ae2enhanced.ModBlocks;
import com.github.aeddddd.ae2enhanced.storage.HyperdimensionalStorageFile;
import com.github.aeddddd.ae2enhanced.storage.ItemStorageAdapter;
import com.github.aeddddd.ae2enhanced.storage.SimpleMEMonitor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 超维度仓储中枢核心控制器。
 * 实现 IGridProxyable + ICellContainer，AE2-UEL 通过 ICellContainer 发现存储。
 */
public class TileHyperdimensionalController extends TileEntity implements IGridProxyable, ICellContainer, ITickable {

    private boolean formed = false;
    private boolean needsReady = false;
    private AENetworkProxy proxy;
    private UUID nexusId;
    private HyperdimensionalStorageFile storageFile;
    private ItemStorageAdapter itemAdapter;
    private SimpleMEMonitor itemMonitor;

    private boolean networkActive = false;
    private boolean networkPowered = false;
    private int tickCounter = 0;

    public boolean isFormed() {
        return formed;
    }

    public UUID getNexusId() {
        return nexusId;
    }

    public ItemStorageAdapter getItemAdapter() {
        return itemAdapter;
    }

    public SimpleMEMonitor getItemMonitor() {
        return itemMonitor;
    }

    // ---- IGridProxyable / IGridHost ----

    private AENetworkProxy createProxy() {
        AENetworkProxy p = new AENetworkProxy(this, "hyperdimensional_controller",
            new net.minecraft.item.ItemStack(ModBlocks.HYPERDIMENSIONAL_CONTROLLER), true);
        p.setValidSides(java.util.EnumSet.allOf(EnumFacing.class));
        return p;
    }

    @Override
    public AENetworkProxy getProxy() {
        if (proxy == null) {
            proxy = createProxy();
        }
        return proxy;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public void gridChanged() {
    }

    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        return getProxy().getNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        // 控制器本身不允许线缆直接连接，AE 接入必须通过 ME 接口
        return AECableType.NONE;
    }

    @Override
    public void securityBreak() {
        disassemble();
    }

    // ---- IActionHost (via ICellContainer) ----

    @Override
    public IGridNode getActionableNode() {
        return getProxy().getNode();
    }

    // ---- ICellProvider (via ICellContainer) ----

    @Override
    public List<appeng.api.storage.IMEInventoryHandler> getCellArray(appeng.api.storage.IStorageChannel<?> channel) {
        if (!formed || itemMonitor == null) return Collections.emptyList();
        if (channel == itemMonitor.getChannel() || channel instanceof appeng.api.storage.channels.IItemStorageChannel) {
            return Collections.singletonList(itemMonitor);
        }
        return Collections.emptyList();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void blinkCell(int slot) {
    }

    // ---- ISaveProvider (via ICellContainer) ----

    @Override
    public void saveChanges(ICellInventory<?> inv) {
        // 我们的存储不是 cell-based，由 ItemStorageAdapter 自行管理持久化
    }

    // ---- Lifecycle ----

    @Override
    public void validate() {
        super.validate();
        needsReady = true;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (proxy != null) {
            proxy.invalidate();
        }
        closeStorage();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (proxy != null) {
            proxy.onChunkUnload();
        }
    }

    public void assemble() {
        if (!formed) {
            formed = true;
            if (nexusId == null) {
                nexusId = UUID.randomUUID();
            }
            initStorage();
            markDirty();
            if (world != null && !world.isRemote) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
            }
            getProxy().onReady();
        }
    }

    public void disassemble() {
        if (formed) {
            formed = false;
            markDirty();
            if (world != null && !world.isRemote) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
            }
            if (proxy != null) {
                proxy.invalidate();
            }
            closeStorage();
        }
    }

    private void initStorage() {
        if (world == null || world.isRemote) return;
        if (storageFile == null) {
            storageFile = new HyperdimensionalStorageFile(world, nexusId);
            itemAdapter = new ItemStorageAdapter(storageFile);
            itemMonitor = new SimpleMEMonitor(itemAdapter);
        }
    }

    private void closeStorage() {
        if (storageFile != null) {
            storageFile.close(itemAdapter != null ? itemAdapter.getStorageMap() : null);
            storageFile = null;
            itemAdapter = null;
            itemMonitor = null;
        }
    }

    @Override
    public void update() {
        if (world == null) return;
        if (world.isRemote) return;

        if (needsReady && formed) {
            needsReady = false;
            getProxy().onReady();
        }

        tickCounter++;
        if (tickCounter % 20 != 0) return;

        boolean newActive = false;
        boolean newPowered = false;
        if (formed) {
            AENetworkProxy p = getProxy();
            if (p != null) {
                newActive = p.isActive();
                newPowered = p.isPowered();
            }
        }

        if (newActive != networkActive || newPowered != networkPowered) {
            networkActive = newActive;
            networkPowered = newPowered;
            markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
        }
    }

    public boolean isNetworkActive() {
        return networkActive;
    }

    public boolean isNetworkPowered() {
        return networkPowered;
    }

    // ---- NBT ----

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        formed = compound.getBoolean("formed");
        if (compound.hasUniqueId("nexusId")) {
            nexusId = compound.getUniqueId("nexusId");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("formed", formed);
        if (nexusId != null) {
            compound.setUniqueId("nexusId", nexusId);
        }
        return compound;
    }

    @Nullable
    @Override
    public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() {
        return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setBoolean("formed", formed);
        tag.setBoolean("networkActive", networkActive);
        tag.setBoolean("networkPowered", networkPowered);
        if (nexusId != null) {
            tag.setUniqueId("nexusId", nexusId);
        }
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        formed = tag.getBoolean("formed");
        networkActive = tag.getBoolean("networkActive");
        networkPowered = tag.getBoolean("networkPowered");
        if (tag.hasUniqueId("nexusId")) {
            nexusId = tag.getUniqueId("nexusId");
        }
    }
}
