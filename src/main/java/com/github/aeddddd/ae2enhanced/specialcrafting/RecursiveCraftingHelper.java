package com.github.aeddddd.ae2enhanced.specialcrafting;

import javax.annotation.Nullable;

import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;

/**
 * 递归类合成（产物与原料含相同物品且净产出为正,如 A+2B=2A）的判定工具.
 * <p>移植自 1.20.1 分支,AEKey 适配为 IAEItemStack。1.12.2 的 ae2fc 流体样板以
 * FluidDrop 假物品形式存在,因此物品通道天然覆盖流体配方。</p>
 */
public final class RecursiveCraftingHelper {

    private RecursiveCraftingHelper() {
    }

    /**
     * 规范化键:类型相同即相等(数量/可合成标志清零),用于 Map/Set 键与等值比较.
     */
    public static IAEItemStack canon(IAEItemStack stack) {
        IAEItemStack copy = stack.copy();
        copy.reset();
        return copy;
    }

    static boolean sameType(IAEItemStack a, IAEItemStack b) {
        return a.equals(b);
    }

    /**
     * 查找样板的精确自引用 key:某凝聚输入与输出类型完全相等,
     * 且每份产出总量 ≥ 投入总量(净增殖或催化剂型).
     *
     * @return 规范化自引用 key;不存在或净耗型返回 null.
     */
    @Nullable
    public static IAEItemStack findSelfRefKey(ICraftingPatternDetails details) {
        for (IAEItemStack input : details.getCondensedInputs()) {
            if (input == null || input.getStackSize() <= 0) {
                continue;
            }
            long inPer = input.getStackSize();
            long outPer = 0;
            for (IAEItemStack output : details.getCondensedOutputs()) {
                if (output != null && input.equals(output)) {
                    outPer += output.getStackSize();
                }
            }
            if (outPer >= inPer) {
                return canon(input);
            }
        }
        return null;
    }

    /**
     * 每次合成消耗的 {@code what} 数量（凝聚输入合计）.
     */
    public static long selfInputPerCraft(ICraftingPatternDetails details, IAEItemStack what) {
        long in = 0;
        for (IAEItemStack input : details.getCondensedInputs()) {
            if (input != null && what.equals(input)) {
                in += input.getStackSize();
            }
        }
        return in;
    }

    /**
     * 每次合成产出的 {@code what} 数量.
     */
    public static long selfOutputPerCraft(ICraftingPatternDetails details, IAEItemStack what) {
        long out = 0;
        for (IAEItemStack output : details.getCondensedOutputs()) {
            if (output != null && what.equals(output)) {
                out += output.getStackSize();
            }
        }
        return out;
    }

    /**
     * 是否为 {@code what} 的净产出自引用样板:原料与产物均含 what,且每次合成净产出为正.
     */
    public static boolean isNetPositiveSelfRef(ICraftingPatternDetails details, IAEItemStack what) {
        long in = selfInputPerCraft(details, what);
        if (in <= 0) {
            return false;
        }
        return selfOutputPerCraft(details, what) > in;
    }

    /**
     * {@code what} 的候选样板是否唯一且为净产出自引用样板.
     */
    public static boolean isOnlyCandidateSelfRef(ICraftingGrid cc, IAEItemStack what, net.minecraft.world.World world) {
        java.util.Iterator<ICraftingPatternDetails> it = cc.getCraftingFor(what, null, -1, world).iterator();
        if (!it.hasNext()) {
            return false;
        }
        ICraftingPatternDetails only = it.next();
        if (it.hasNext()) {
            return false;
        }
        return isNetPositiveSelfRef(only, what);
    }
}
