package com.github.aeddddd.ae2enhanced.container;

import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerAssemblyPattern extends Container {

    private static final int PATTERN_X = 10;
    private static final int PATTERN_Y = 24;
    private static final int INV_X = 89;
    private static final int INV_Y = 152;
    private static final int HOTBAR_Y = 210;

    private final TileAssemblyController tile;
    private final int page;
    private final int patternSlotCount;

    public ContainerAssemblyPattern(IInventory playerInv, TileAssemblyController tile, int page, int patternPages) {
        this.tile = tile;
        this.page = page;
        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        int startSlot = TileAssemblyController.UPGRADE_SLOTS
            + page * TileAssemblyController.PATTERN_SLOTS_PER_PAGE;
        // 使用服务端同步的 patternPages 计算边界，避免客户端 handler.getSlots() 未同步导致槽位映射错误
        int expectedTotalSlots = TileAssemblyController.UPGRADE_SLOTS
            + patternPages * TileAssemblyController.PATTERN_SLOTS_PER_PAGE;
        int endSlot = Math.min(startSlot + TileAssemblyController.PATTERN_SLOTS_PER_PAGE,
            expectedTotalSlots);

        this.patternSlotCount = endSlot - startSlot;

        // 样板槽：当前页 16×6=96 槽（实际受 handler.getSlots() 限制）
        for (int i = startSlot; i < endSlot; i++) {
            int localIndex = i - startSlot;
            int row = localIndex / 16;
            int col = localIndex % 16;
            final int handlerIndex = i;
            final IItemHandler handlerRef = handler;
            this.addSlotToContainer(new SlotItemHandler(handler, i,
                PATTERN_X + col * 20, PATTERN_Y + row * 20) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    return 1;
                }

                @Override
                public int getSlotStackLimit() {
                    return 1;
                }

                /**
                 * 覆盖 putStack，使用 setStackInSlot 直接替换槽位内容。
                 * 默认 SlotItemHandler.putStack 使用 insertItem，其语义是"合并"而非"替换"，
                 * 会导致 Container.setAll (SPacketWindowItems) 每次同步时都将新 stack grow
                 * 到已有 stack 上，造成样板数量异常累加（如切页后返回时数量+2）。
                 *
                 * 使用 IItemHandlerModifiable.setStackInSlot 可直接设置槽位内容，避免合并语义。
                 * PatternItemHandler 已对 setStackInSlot 添加越界保护，防止客户端容量未同步时崩溃。
                 */
                @Override
                public void putStack(@Nonnull ItemStack stack) {
                    if (handlerRef instanceof net.minecraftforge.items.IItemHandlerModifiable) {
                        ((net.minecraftforge.items.IItemHandlerModifiable) handlerRef)
                                .setStackInSlot(handlerIndex, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                    } else {
                        handlerRef.extractItem(handlerIndex, Integer.MAX_VALUE, false);
                        if (!stack.isEmpty()) {
                            handlerRef.insertItem(handlerIndex, stack.copy(), false);
                        }
                    }
                    stack.setCount(0);
                    this.onSlotChanged();
                }
            });
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

            int patternEnd = this.patternSlotCount;
            int playerStart = patternEnd;
            int playerEnd = playerStart + 36;

            if (index < patternEnd) {
                // 从样板槽移到玩家背包
                if (!this.mergeItemStack(itemstack1, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移到样板槽
                if (!this.mergeItemStack(itemstack1, 0, patternEnd, false)) {
                    return ItemStack.EMPTY;
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

    public int getPage() {
        return page;
    }
}
