package com.github.aeddddd.ae2enhanced.tile;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.integration.projecte.ProjectEEventHandler;
import com.github.aeddddd.ae2enhanced.config.AE2EnhancedConfig;
import com.github.aeddddd.ae2enhanced.integration.projecte.ProjectEHelper;
import com.github.aeddddd.ae2enhanced.registry.content.BlockRegistry;
import com.github.aeddddd.ae2enhanced.storage.EMCInventoryHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.github.aeddddd.ae2enhanced.storage.ItemDescriptor;

/**
 * EMC 接口 TileEntity.
 *
 * <p>将绑定玩家的 ProjectE EMC 余额作为 AE 网络物品源.单向输出,不接收物品.</p>
 */
public class TileEMCInterface extends TileAENetworkBase implements ICellContainer, ITickable, IAEAppEngInventory {

    public static final int WHITELIST_PAGES = 20;
    public static final int WHITELIST_SLOTS_PER_PAGE = 102; // 17×6，与 3.png 顶部网格一致
    public static final int WHITELIST_SIZE = WHITELIST_PAGES * WHITELIST_SLOTS_PER_PAGE; // 2040


    private final EMCInventoryHandler handler = new EMCInventoryHandler(this);
    private final AppEngInternalAEInventory config;
    private final ItemStack[] whitelist = new ItemStack[WHITELIST_SIZE];
    private final Set<ItemDescriptor> whitelistSet = new HashSet<>();

    @Nullable
    private UUID ownerUUID;
    private String ownerName = "";

    private boolean registeredEvents = false;

    public void invalidateHandlerCache() {
        handler.invalidateAvailableCache();
        // 知识/EMC remap 变化会改变可用物品集合,同步刷新网络存储视图
        notifyCellArrayUpdate();
    }

    public void invalidateEmcCache() {
        handler.invalidateEmcCache();
    }


    public TileEMCInterface() {
        this.config = new AppEngInternalAEInventory(this, WHITELIST_SIZE);
        for (int i = 0; i < WHITELIST_SIZE; i++) {
            whitelist[i] = ItemStack.EMPTY;
        }
    }

    @Override
    protected String getProxyName() {
        return "emc_interface";
    }

    @Override
    protected ItemStack getProxyRepresentation() {
        return new ItemStack(BlockRegistry.EMC_INTERFACE);
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        // 安全破坏时不掉落,仅解绑
        setOwner(null);
    }

    // ---- 玩家绑定 ----

    public boolean isBound() {
        return ownerUUID != null && ProjectEHelper.isAvailable();
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(@Nullable EntityPlayer player) {
        if (player == null) {
            this.ownerUUID = null;
            this.ownerName = "";
        } else {
            this.ownerUUID = player.getUniqueID();
            this.ownerName = player.getName();
        }
        handler.invalidateAvailableCache();
        markDirty();
        notifyCellArrayUpdate();
        syncToClient();
    }

    /**
     * 玩家是否有权管理(重新绑定/打开 GUI/编辑白名单)此接口.
     * 未绑定时任何人可认领;已绑定时仅所有者或 OP(权限等级 2).
     */
    public boolean canManage(@Nonnull EntityPlayer player) {
        if (ownerUUID == null) return true;
        if (player.getUniqueID().equals(ownerUUID)) return true;
        return player.canUseCommand(2, "");
    }

    /**
     * 绑定玩家当前是否在线.
     * 离线时 ProjectE 返回的 TransmutationOffline 包装 provider 为只读快照,
     * 无法扣减 EMC,因此离线期间必须禁止提取.
     */
    public boolean isOwnerOnline() {
        if (ownerUUID == null) return false;
        net.minecraft.server.MinecraftServer server =
                net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        return server != null && server.getPlayerList().getPlayerByUUID(ownerUUID) != null;
    }

    /**
     * 规范化白名单物品: count=1, 且对齐 ProjectE 学习知识时的 NBT 修剪规则
     * (非 NBTWhitelist 物品剥离 NBT),保证与已学知识列表可匹配.
     */
    @Nonnull
    private static ItemStack normalizeWhitelistStack(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        if (copy.hasTagCompound() && !ProjectEHelper.shouldDupeWithNBT(copy)) {
            copy.setTagCompound(null);
        }
        return copy;
    }

    private void syncToClient() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    @Nullable
    public Object getKnowledgeProvider() {
        if (ownerUUID == null) return null;
        return ProjectEHelper.getKnowledgeProvider(ownerUUID);
    }

    // ---- 白名单 ----

    public AppEngInternalAEInventory getConfig() {
        return config;
    }

    public ItemStack getWhitelistSlot(int index) {
        return whitelist[index].copy();
    }

    public void setWhitelistSlot(int index, @Nonnull ItemStack stack) {
        whitelist[index] = normalizeWhitelistStack(stack);
        config.setStackInSlot(index, whitelist[index]);
        rebuildWhitelistSet();
        handler.invalidateAvailableCache();
        markDirty();
        notifyCellArrayUpdate();
    }

    public ItemStack[] getWhitelist() {
        return whitelist;
    }

    public boolean isWhitelisted(@Nonnull ItemStack stack) {
        if (whitelistSet.isEmpty()) return false; // 空白名单 = 不暴露任何物品
        return whitelistSet.contains(new ItemDescriptor(stack));
    }

    public boolean isWhitelistActive() {
        return !whitelistSet.isEmpty();
    }

    private void rebuildWhitelistSet() {
        whitelistSet.clear();
        for (ItemStack stack : whitelist) {
            if (!stack.isEmpty()) {
                whitelistSet.add(new ItemDescriptor(stack));
            }
        }
    }

    // ---- ICellContainer ----

    @Override
    public IGridNode getActionableNode() {
        return getProxy().getNode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IMEInventoryHandler> getCellArray(IStorageChannel<?> channel) {
        if (!isBound()) return Collections.emptyList();
        if (channel instanceof IItemStorageChannel) {
            return Collections.singletonList((IMEInventoryHandler) handler);
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

    @Override
    public void saveChanges(ICellInventory<?> inv) {
    }

    // ---- 生命周期 ----

    @Override
    public void validate() {
        super.validate();
        if (world != null && !world.isRemote) {
            registerProjectEEvents();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        unregisterProjectEEvents();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        unregisterProjectEEvents();
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        if (needsReady()) {
            clearNeedsReady();
            getProxy().setIdlePowerUsage(AE2EnhancedConfig.emcInterface.idlePower);
            getProxy().onReady();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasUniqueId("OwnerUUID")) {
            ownerUUID = compound.getUniqueId("OwnerUUID");
        } else {
            ownerUUID = null;
        }
        ownerName = compound.getString("OwnerName");

        NBTTagList list = compound.getTagList("Whitelist", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < WHITELIST_SIZE; i++) {
            whitelist[i] = ItemStack.EMPTY;
        }
        for (int i = 0; i < list.tagCount() && i < WHITELIST_SIZE; i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getShort("Slot") & 0xFFFF;
            if (slot < WHITELIST_SIZE) {
                whitelist[slot] = new ItemStack(tag);
            }
        }
        rebuildWhitelistSet();
        for (int i = 0; i < WHITELIST_SIZE; i++) {
            config.setStackInSlot(i, whitelist[i]);
        }
        handler.invalidateAvailableCache();
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (ownerUUID != null) {
            compound.setUniqueId("OwnerUUID", ownerUUID);
        }
        compound.setString("OwnerName", ownerName);

        NBTTagList list = new NBTTagList();
        for (int i = 0; i < WHITELIST_SIZE; i++) {
            if (!whitelist[i].isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setShort("Slot", (short) i);
                whitelist[i].writeToNBT(tag);
                list.appendTag(tag);
            }
        }
        compound.setTag("Whitelist", list);
        return compound;
    }

    // ---- 客户端同步 ----

    @Override
    @Nonnull
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        if (ownerUUID != null) {
            tag.setUniqueId("OwnerUUID", ownerUUID);
        }
        tag.setString("OwnerName", ownerName);
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        NBTTagCompound tag = pkt.getNbtCompound();
        if (tag.hasUniqueId("OwnerUUID")) {
            ownerUUID = tag.getUniqueId("OwnerUUID");
        } else {
            ownerUUID = null;
        }
        ownerName = tag.getString("OwnerName");
    }

    // ---- 内部辅助 ----

    private void notifyCellArrayUpdate() {
        try {
            IGrid grid = getProxy().getGrid();
            if (grid != null) {
                grid.postEvent(new appeng.api.networking.events.MENetworkCellArrayUpdate());
            }
        } catch (GridAccessException e) {
            // grid 尚未就绪
        }
    }

    private void registerProjectEEvents() {
        if (registeredEvents || !ProjectEHelper.isAvailable()) return;
        registeredEvents = true;
        try {
            Class<?> clazz = Class.forName("com.github.aeddddd.ae2enhanced.integration.projecte.ProjectEEventHandler");
            clazz.getMethod("registerTile", TileEMCInterface.class).invoke(null, this);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] Failed to register ProjectE event listeners", e);
        }
    }

    private void unregisterProjectEEvents() {
        if (!registeredEvents) return;
        registeredEvents = false;
        try {
            Class<?> clazz = Class.forName("com.github.aeddddd.ae2enhanced.integration.projecte.ProjectEEventHandler");
            clazz.getMethod("unregisterTile", TileEMCInterface.class).invoke(null, this);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] Failed to unregister ProjectE event tile", e);
        }
    }

    @Override
    public void disassemble() {
        // EMC 接口无结构,解绑即可
        ownerUUID = null;
        ownerName = "";
        handler.invalidateAvailableCache();
        markDirty();
    }

    // ---- IAEAppEngInventory ----

    @Override
    public void saveChanges() {
        markDirty();
    }

    @Override
    public void onChangeInventory(net.minecraftforge.items.IItemHandler inv, int slot, InvOperation mc, ItemStack removed, ItemStack added) {
        if (inv == config && slot >= 0 && slot < WHITELIST_SIZE) {
            ItemStack normalized = normalizeWhitelistStack(added);
            if (!ItemStack.areItemStacksEqual(added, normalized)) {
                // 回写规范化后的物品; 再次触发本回调时 added 已规范化,不再进入此分支
                config.setStackInSlot(slot, normalized);
                return;
            }
            whitelist[slot] = normalized;
            rebuildWhitelistSet();
            handler.invalidateAvailableCache();
            markDirty();
            notifyCellArrayUpdate();
        }
    }

}
