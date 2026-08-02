package com.github.aeddddd.ae2enhanced.util.compat;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;

import java.lang.reflect.Method;

/**
 * ae2fc 流体替换合成样板（FluidCraftingPatternDetails）反射隔离层.
 *
 * <p>ae2fc 的流体替换合成样板 {@code isCraftable()} 恒为 false，装配枢纽的三处闸门
 * （provideCrafting / pushPattern / prefillVirtualCache）需要通过本类识别并放行。</p>
 *
 * <p>所有 ae2fc 类型均以字符串类名 + 反射访问，本类常量池不含任何 ae2fc 的
 * {@code CONSTANT_Class} 引用，可被无条件加载的类（如 TileAssemblyController）安全调用。</p>
 */
public final class Ae2fcFluidPatternHelper {

    private static final String FLUID_CRAFT_PATTERN_CLASS =
        "com.glodblock.github.util.FluidCraftingPatternDetails";
    private static final String AE2FC_ITEM_FLUID_DROP =
        "com.glodblock.github.common.item.ItemFluidDrop";

    /** getOriginInputs() 方法缓存（public 方法，首次调用时解析） */
    private static Method getOriginInputsMethod;
    private static boolean methodResolved = false;

    private Ae2fcFluidPatternHelper() {
    }

    /**
     * 判断样板是否为 ae2fc 的流体替换合成样板（FluidCraftingPatternDetails）.
     * 仅按类名字符串识别，ae2fc 未安装时恒返回 false.
     */
    public static boolean isFluidCraftPattern(ICraftingPatternDetails details) {
        if (details == null || !Ae2fcCompat.AE2FC_LOADED) return false;
        return FLUID_CRAFT_PATTERN_CLASS.equals(details.getClass().getName());
    }

    /**
     * 判断 AE 物品堆叠是否为 ae2fc 的 FLUID_DROP（流体假物品，stackSize 单位为 mB）.
     */
    public static boolean isAe2fcFluidDrop(IAEItemStack stack) {
        if (stack == null || !Ae2fcCompat.AE2FC_LOADED) return false;
        return AE2FC_ITEM_FLUID_DROP.equals(stack.getItem().getClass().getName());
    }

    /**
     * 反射调用 ae2fc FluidCraftingPatternDetails#getOriginInputs()，
     * 获取容器形态的 9 槽原始输入（用于重建原版配方）.
     *
     * @return 容器形态输入数组；反射失败时返回 null（调用方应安全降级）
     */
    public static IAEItemStack[] getOriginInputs(ICraftingPatternDetails details) {
        if (!isFluidCraftPattern(details)) return null;
        try {
            if (!methodResolved) {
                getOriginInputsMethod = details.getClass().getMethod("getOriginInputs");
                methodResolved = true;
            }
            if (getOriginInputsMethod == null) return null;
            return (IAEItemStack[]) getOriginInputsMethod.invoke(details);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] Failed to invoke ae2fc getOriginInputs, fluid pattern support degraded", e);
            return null;
        }
    }
}
