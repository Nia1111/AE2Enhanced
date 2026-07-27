package com.github.aeddddd.ae2enhanced.crafting.smartpattern;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.config.AE2EnhancedConfig;
import com.github.aeddddd.ae2enhanced.item.ItemSmartBlankPattern;
import com.github.aeddddd.ae2enhanced.item.ItemSmartPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 智能样板合成事件处理(仅服务端生效).
 *
 * <ul>
 *   <li>合并：智能样板 + 已编码样板 → 将已编码样板的配方追加到智能样板数据中,
 *       保存存储文件并更新输出样板的 NBT 快照</li>
 *   <li>回收：智能样板 → 空智能样板 时删除对应的存储文件</li>
 * </ul>
 */
public final class SmartPatternMergeHelper {

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        World world = event.player.world;
        if (world.isRemote) return;
        handleMerge(event, world);
        handleClear(event, world);
    }

    /**
     * 合并：输出为智能样板且合成格中存在智能样板 + 已编码样板时,追加配方.
     */
    private static void handleMerge(PlayerEvent.ItemCraftedEvent event, @Nonnull World world) {
        ItemStack result = event.crafting;
        if (!(result.getItem() instanceof ItemSmartPattern)) return;

        ItemStack smartInput = ItemStack.EMPTY;
        List<ItemStack> encodedPatterns = new ArrayList<>();
        for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
            ItemStack stack = event.craftMatrix.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemSmartPattern) {
                smartInput = stack;
            } else if (stack.getItem() instanceof ICraftingPatternItem) {
                encodedPatterns.add(stack);
            }
        }
        if (smartInput.isEmpty() || encodedPatterns.isEmpty()) return;

        UUID dataId = ItemSmartPattern.getPatternDataId(smartInput);
        if (dataId == null) return;
        SmartPatternData data = SmartPatternStorageFile.load(world, dataId);
        if (data == null) return;

        // 以被消耗样板上的禁用掩码为准(展开时使用的也是栈上掩码)
        data.setDisabledMask(ItemSmartPattern.getDisabledMask(smartInput));

        int max = AE2EnhancedConfig.smartPattern.maxRecipes;
        boolean changed = false;
        for (ItemStack pattern : encodedPatterns) {
            if (data.getRecipeCount() >= max) break;
            try {
                ICraftingPatternDetails details =
                        ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(pattern, world);
                if (details == null) continue;
                SmartRecipe recipe = new SmartRecipe(details.getInputs(), details.getOutputs(), false);
                if (data.appendRecipe(recipe)) {
                    changed = true;
                }
            } catch (Exception e) {
                AE2Enhanced.LOGGER.warn("[AE2E] Failed to merge encoded pattern into smart pattern: {}", e.toString());
            }
        }
        if (!changed) return;

        SmartPatternStorageFile.save(world, data);

        // 更新输出样板的 NBT 快照(禁用掩码/配方数量)
        ItemStack updated = ItemSmartPattern.createPattern(
                data.getPatternDataId(), data.getDisabledMask(), data.getRecipeCount(), data.getTargetBlockId());
        result.setTagCompound(updated.getTagCompound());
    }

    /**
     * 回收：智能样板合成为空智能样板时,删除对应的存储文件.
     * 删除后所有引用同一 UUID 的已编码智能样板将失效,这是该操作的设计语义.
     */
    private static void handleClear(PlayerEvent.ItemCraftedEvent event, @Nonnull World world) {
        if (!(event.crafting.getItem() instanceof ItemSmartBlankPattern)) return;
        for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
            ItemStack stack = event.craftMatrix.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemSmartPattern)) continue;
            UUID dataId = ItemSmartPattern.getPatternDataId(stack);
            if (dataId != null) {
                SmartPatternStorageFile.delete(world, dataId);
            }
            return;
        }
    }
}
