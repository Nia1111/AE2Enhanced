package com.github.aeddddd.ae2enhanced.mixin.late.eio;

import com.github.aeddddd.ae2enhanced.recycler.MachineOutputRedirector;
import crazypants.enderio.base.machine.baselegacy.AbstractPoweredTaskEntity;
import crazypants.enderio.base.machine.baselegacy.SlotDefinition;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ender IO 机器产物直注 Mixin。
 *
 * <p>在 {@link AbstractPoweredTaskEntity#taskComplete()} 把产物写入输出槽之后，
 * 立即把输出槽中的产物重定向到已绑定的 ME 网络回收节点。</p>
 */
@Mixin(value = AbstractPoweredTaskEntity.class, remap = false)
public class MixinAbstractPoweredTaskEntity {

    @Inject(method = "taskComplete", at = @At("TAIL"))
    private void ae2enhanced$redirectOutputsAfterTaskComplete(CallbackInfo ci) {
        try {
            TileEntity tile = (TileEntity) (Object) this;
            World world = tile.getWorld();
            if (world == null || world.isRemote) {
                return;
            }
            // slotDefinition / inventory 声明在父类 AbstractInventoryMachineEntity 上，
            // 通过 Accessor Mixin 获取（@Shadow 无法跨父类解析字段）.
            SlotDefinition slotDefinition = ((IAbstractInventoryMachineAccessor) tile).ae2e$getSlotDefinition();
            ItemStack[] inventory = ((IAbstractInventoryMachineAccessor) tile).ae2e$getInventory();
            BlockPos pos = tile.getPos();
            if (slotDefinition == null || inventory == null) {
                return;
            }

            for (int i = slotDefinition.minOutputSlot; i <= slotDefinition.maxOutputSlot; i++) {
                if (i < 0 || i >= inventory.length) {
                    continue;
                }
                ItemStack stack = inventory[i];
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                ItemStack remainder = MachineOutputRedirector.tryRedirect(stack, world, pos);
                inventory[i] = remainder;
            }
        } catch (RuntimeException ignored) {
        }
    }
}
