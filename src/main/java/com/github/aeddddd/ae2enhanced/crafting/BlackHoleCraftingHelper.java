package com.github.aeddddd.ae2enhanced.crafting;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 黑洞合成辅助类。
 * 扫描黑洞中心周围 3×3×3 区域内的物品实体，累加匹配配方后消耗/产出。
 */
public class BlackHoleCraftingHelper {

    /**
     * 尝试执行一次黑洞合成。
     * 产物生成在扫描范围外（y+2），默认行为：配方不匹配时销毁所有物品。
     */
    public static void tryCraft(World world, BlockPos pos) {
        tryCraft(world, pos, pos.add(0, 2, 0), true);
    }

    /**
     * 尝试执行一次黑洞合成。
     *
     * @param world 世界
     * @param pos 扫描中心坐标
     * @param outputPos 产物掉落坐标
     * @param destroyOnMismatch 配方不匹配时是否销毁区域内的所有物品。
     *                          正式黑洞自动吸入时应为 true；
     *                          微型奇点玩家主动触发时应为 false，避免误销毁未配齐的材料。
     */
    public static void tryCraft(World world, BlockPos pos, BlockPos outputPos, boolean destroyOnMismatch) {
        AxisAlignedBB area = new AxisAlignedBB(
                pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2
        );
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, area);
        if (items.isEmpty()) return;

        // 累加物品数量
        Map<Item, Integer> found = new HashMap<>();
        for (EntityItem entityItem : items) {
            ItemStack stack = entityItem.getItem();
            if (stack.isEmpty()) continue;
            found.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        // 匹配配方
        BlackHoleRecipe recipe = BlackHoleRecipeRegistry.findMatching(found);
        if (recipe != null) {
            // 消耗材料
            Map<Item, Integer> remaining = new HashMap<>(recipe.getInputs());
            for (EntityItem entityItem : items) {
                ItemStack stack = entityItem.getItem();
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                int needed = remaining.getOrDefault(item, 0);
                if (needed > 0) {
                    int consume = Math.min(needed, stack.getCount());
                    stack.shrink(consume);
                    remaining.put(item, needed - consume);
                    if (stack.isEmpty()) {
                        entityItem.setDead();
                    }
                }
            }
            // 生成产物（从指定位置喷出）
            EntityItem result = new EntityItem(world,
                    outputPos.getX() + 0.5, outputPos.getY() + 0.5, outputPos.getZ() + 0.5,
                    recipe.getOutput().copy());
            result.setNoPickupDelay();
            world.spawnEntity(result);
        } else if (destroyOnMismatch) {
            // 不匹配任何配方：黑洞销毁所有物品
            for (EntityItem entityItem : items) {
                entityItem.setDead();
            }
        }
        // 若 destroyOnMismatch == false 且配方不匹配，保留所有物品，什么都不做
    }
}
