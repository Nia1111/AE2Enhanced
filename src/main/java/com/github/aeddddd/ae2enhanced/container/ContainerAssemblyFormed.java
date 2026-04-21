package com.github.aeddddd.ae2enhanced.container;

import com.github.aeddddd.ae2enhanced.item.ItemUpgradeCard;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerAssemblyFormed extends Container {

    private static final int UPGRADE_X = 16;
    private static final int UPGRADE_Y = 38;
    private static final int PATTERN_X = 90;
    private static final int PATTERN_Y = 38;
    private static final int INV_X = 51;
    private static final int INV_Y = 182;
    private static final int HOTBAR_Y = 240;

    private final TileAssemblyController tile;

    public ContainerAssemblyFormed(IInventory playerInv, TileAssemblyController tile) {
        this.tile = tile;
        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        // 升级槽：2行×3列，槽位 0~5
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 3; ++col) {
                int index = row * 3 + col;
                this.addSlotToContainer(new SlotItemHandler(handler, index,
                    UPGRADE_X + col * 20, UPGRADE_Y + row * 20) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return stack.getItem() instanceof ItemUpgradeCard;
                    }
                });
            }
        }

        // 样板槽：6行×6列，槽位 6~41
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 6; ++col) {
                int index = TileAssemblyController.UPGRADE_SLOTS + row * 6 + col;
                this.addSlotToContainer(new SlotItemHandler(handler, index,
                    PATTERN_X + col * 20, PATTERN_Y + row * 20));
            }
        }

        // 玩家背包 3行×9列
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9,
                    INV_X + col * 18, INV_Y + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInv, col,
                INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile
                && playerIn.getDistanceSq(tile.getPos()) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            int upgradeStart = 0;
            int upgradeEnd = TileAssemblyController.UPGRADE_SLOTS;
            int patternStart = upgradeEnd;
            int patternEnd = patternStart + TileAssemblyController.PATTERN_SLOTS;
            int playerStart = patternEnd;
            int playerEnd = playerStart + 36;

            if (index < patternEnd) {
                // 从机器槽位移到玩家背包
                if (!this.mergeItemStack(itemstack1, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移到机器槽位
                if (itemstack1.getItem() instanceof ItemUpgradeCard) {
                    // 优先放入升级槽
                    if (!this.mergeItemStack(itemstack1, upgradeStart, upgradeEnd, false)) {
                        // 升级槽满了，尝试放入样板槽
                        if (!this.mergeItemStack(itemstack1, patternStart, patternEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    // 非升级卡，放入样板槽
                    if (!this.mergeItemStack(itemstack1, patternStart, patternEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }

    public TileAssemblyController getTile() {
        return tile;
    }
}
