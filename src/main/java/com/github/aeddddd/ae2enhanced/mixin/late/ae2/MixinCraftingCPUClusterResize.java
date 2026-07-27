package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerNull;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 处理样板合成表扩容.
 *
 * <p>AE2 CPU 对处理样板固定构造 4×4 的 InventoryCrafting。
 * 当处理样板输入数超过 16 时，executeCrafting 在写入第 17 个槽位时会越界。
 * 这里在处理样板的 table 构造完成后，按实际输入数扩容到 5×5~10×10，
 * 避免物品被丢弃或崩溃。</p>
 *
 * <p><b>ae2fc 兼容</b>：ae2fc 通过 WrapOperation 包装同一 NEW 指令，把合成表替换为
 * FluidConvertingInventoryCrafting（写入时把流体/气体假物品转换为数据包）。
 * 本 Mixin 使用相同的 NEW 包装点并设置更高优先级，使本包装成为最外层：
 * original.call 先得到 ae2fc 的转换表（或其缺省的原版表），再按运行时类反射构造
 * 同类型的大尺寸实例，保证流体转换行为不丢失。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false, priority = 1100)
public class MixinCraftingCPUClusterResize {

    @WrapOperation(
        method = "executeCrafting",
        at = @At(value = "NEW", target = "net/minecraft/inventory/InventoryCrafting", remap = true)
    )
    private InventoryCrafting ae2enhanced$resizeCraftingBuffer(
            Container container, int width, int height,
            Operation<InventoryCrafting> original,
            @Local ICraftingPatternDetails details) {
        InventoryCrafting ic = original.call(container, width, height);
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
        InventoryCrafting larger = ae2enhanced$newSameClassTable(ic, container, size);
        for (int i = 0; i < ic.getSizeInventory(); i++) {
            ItemStack stack = ic.getStackInSlot(i);
            if (!stack.isEmpty()) {
                larger.setInventorySlotContents(i, stack.copy());
            }
        }
        return larger;
    }

    /**
     * 按原表运行时类构造同类型的大尺寸合成表，保留子类（如 ae2fc 流体转换表）的写入行为；
     * 反射失败时回退为普通 InventoryCrafting。
     */
    private static InventoryCrafting ae2enhanced$newSameClassTable(InventoryCrafting ic, Container container, int size) {
        try {
            return ic.getClass()
                .getConstructor(Container.class, int.class, int.class)
                .newInstance(container, size, size);
        } catch (Throwable ignored) {
            return new InventoryCrafting(new ContainerNull(), size, size);
        }
    }
}
