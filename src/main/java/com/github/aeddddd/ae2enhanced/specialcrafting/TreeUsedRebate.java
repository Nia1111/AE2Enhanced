package com.github.aeddddd.ae2enhanced.specialcrafting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;

/**
 * used 返利工具（从 SpecialCraftingJob 提取,边界求解器共用）.
 * <p>贷款法模拟中,计划键的 gross 提取经 availableCheck 被网络库存量钳制,
 * 但计划语义要求 used = 计划种子量(与 1.20.1 一致)——多余库存不应被 CPU 提取.
 * 全树遍历收集计划键的 used 条目,清零后把首个条目回填为计划种子量.</p>
 * <p>安全性:种子校验已保证库存 ≥ 计划种子,且 gross 提取总量 ≥ 计划种子
 * （种子 ≤ 每轮消耗,gross = 轮次 × 消耗）,回填不会突破实际可用量.</p>
 */
public final class TreeUsedRebate {

    private TreeUsedRebate() {
    }

    public static void rebate(CraftingTreeNode root, Map<IAEItemStack, Long> plannedSeeds) {
        Map<IAEItemStack, List<IAEItemStack>> found = new LinkedHashMap<>();
        collectUsedEntries(root, plannedSeeds, found);
        for (Map.Entry<IAEItemStack, Long> plan : plannedSeeds.entrySet()) {
            List<IAEItemStack> entries = found.get(plan.getKey());
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setStackSize(i == 0 ? plan.getValue() : 0);
            }
        }
    }

    private static void collectUsedEntries(CraftingTreeNode node, Map<IAEItemStack, Long> plannedSeeds,
            Map<IAEItemStack, List<IAEItemStack>> found) {
        IItemList<IAEItemStack> used = Ae2CraftingReflect.getNodeUsed(node);
        for (IAEItemStack key : plannedSeeds.keySet()) {
            IAEItemStack entry = used.findPrecise(key);
            if (entry != null && entry.getStackSize() > 0) {
                found.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
            }
        }
        for (CraftingTreeProcess pro : Ae2CraftingReflect.getNodeProcesses(node)) {
            for (CraftingTreeNode child : Ae2CraftingReflect.getProcessNodes(pro).keySet()) {
                collectUsedEntries(child, plannedSeeds, found);
            }
        }
    }
}
