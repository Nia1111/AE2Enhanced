package com.github.aeddddd.ae2enhanced.specialcrafting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.MECraftingInventory;

/**
 * 深层循环边界求解器（DAG 引擎 4.3,1.12.2 移植）:把含环子图当黑盒,
 * 以**当前模拟库存状态**求解 what×target 子需求并就地记账
 * （种子贷款、环外输入经原生树、产出回插、crafts 记账）.
 * <p>支持:① 净增殖自引用(selfKey == 边界 key,贷款法闭式);
 * ② 跨样板增殖环（并集联立优先,逐环迭代兜底,复用 {@link CycleSolver});
 * ③ 催化环（边界 key 是某中性/增殖环发射的环外副产物）.</p>
 * <p>结算语义与根请求求解一致:交付量（= 边界需求量）从库存取走,
 * 种子保留——防止同一批产出被 DAG 其他节点重复取用.</p>
 */
public final class CycleBoundarySolver {

    private CycleBoundarySolver() {
    }

    /**
     * @return true = 求解成功并已记账;false = 不适用（调用方应整单回落原生）.
     */
    public static boolean solveInto(ICraftingGrid cc, CraftingJob job, IAEItemStack what, long target,
            MECraftingInventory inv, CraftingTreeNode rootNode, IActionSource src, World world)
            throws InterruptedException {
        // ① 净增殖自引用（单节点自环）
        for (ICraftingPatternDetails pattern : cc.getCraftingFor(what, null, -1, world)) {
            if (RecursiveCraftingHelper.isNetPositiveSelfRef(pattern, what)) {
                return solveDup(cc, job, pattern, what, target, inv, rootNode, src);
            }
        }
        // ② 跨样板环:并集优先(θ 形共享结构),再逐环迭代
        List<List<CycleAnalyzer.CycleStep>> cycles = CycleAnalyzer.findCyclesThrough(cc, what, world);
        CycleAnalyzer.Analysis union = CycleAnalyzer.analyzeUnion(cycles);
        if (union != null && union.rateClass() == CycleAnalyzer.RateClass.PRODUCTIVE
                && CycleSolver.trySolve(cc, job, union, inv, what, target, rootNode,
                        src) == CycleSolver.SolveResult.SUCCESS) {
            return true;
        }
        for (List<CycleAnalyzer.CycleStep> cycle : cycles) {
            CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycle);
            if (analysis == null || analysis.rateClass() != CycleAnalyzer.RateClass.PRODUCTIVE) {
                continue;
            }
            if (CycleSolver.trySolve(cc, job, analysis, inv, what, target, rootNode,
                    src) == CycleSolver.SolveResult.SUCCESS) {
                return true;
            }
        }
        // ③ 催化环:边界 key 是某中性/增殖环发射的环外副产物(深层 A→X+B、B→A 中的 X)
        for (List<CycleAnalyzer.CycleStep> cycle : CycleAnalyzer.findCatalyticCycles(cc, what, world)) {
            CycleAnalyzer.Analysis analysis = CycleAnalyzer.analyze(cycle);
            if (analysis == null || analysis.rateClass() == CycleAnalyzer.RateClass.DISSIPATIVE) {
                continue;
            }
            long xPerRound = CycleAnalyzer.byproductPerRound(analysis, what);
            if (xPerRound <= 0) {
                continue;
            }
            if (CycleSolver.trySolveCatalytic(cc, job, analysis, xPerRound, inv, what, target, rootNode,
                    src) == CycleSolver.SolveResult.SUCCESS) {
                return true;
            }
        }
        SpecialLog.info("[DAG] 循环边界不可解: {}×{}", what, target);
        return false;
    }

    /**
     * 净增殖自引用闭式解（贷款法）,与 SpecialCraftingJob 根路径同语义.
     */
    private static boolean solveDup(ICraftingGrid cc, CraftingJob job, ICraftingPatternDetails selfRef,
            IAEItemStack what, long target, MECraftingInventory inv, CraftingTreeNode rootNode,
            IActionSource src) throws InterruptedException {
        long inPer = RecursiveCraftingHelper.selfInputPerCraft(selfRef, what);
        long outPer = RecursiveCraftingHelper.selfOutputPerCraft(selfRef, what);
        long gain = outPer - inPer;
        if (gain <= 0 || inPer <= 0) {
            return false;
        }
        // 种子校验(不 ignore,边界 key 的库存可见)
        long stock = CycleSolver.invAmount(inv, what);
        if (stock < inPer) {
            return false;
        }
        long crafts = (target + gain - 1) / gain;
        if (crafts <= 0 || crafts > Long.MAX_VALUE / inPer) {
            return false; // 天文数字子需求:回落(整单走原生/缺料语义)
        }

        CraftingTreeProcess pro = new CraftingTreeProcess(cc, job, selfRef, rootNode, 1);
        Ae2CraftingReflect.addProcessToNode(rootNode, pro);

        long loan = inPer * (crafts - 1);
        if (loan > 0) {
            IAEItemStack loanStack = RecursiveCraftingHelper.canon(what);
            loanStack.setStackSize(loan);
            inv.injectItems(loanStack, Actionable.MODULATE, src);
        }
        try {
            Ae2CraftingReflect.treeProcessRequest(pro, inv, crafts, src);
        } catch (CraftBranchFailure failure) {
            return false; // 非自输入不足 → 整单回落(缺料报告)
        } finally {
            if (loan > 0) {
                IAEItemStack payback = RecursiveCraftingHelper.canon(what);
                payback.setStackSize(loan);
                inv.extractItems(payback, Actionable.MODULATE, src);
            }
        }

        // used 返利:种子语义 = inPer(与根请求路径一致)
        Map<IAEItemStack, Long> seeds = new LinkedHashMap<>();
        seeds.put(RecursiveCraftingHelper.canon(what), inPer);
        TreeUsedRebate.rebate(rootNode, seeds);

        // 结算:取走交付量(边界需求),种子保留
        long avail = CycleSolver.invAmount(inv, what);
        long keep = avail > target ? inPer : 0;
        long drainable = Math.min(target, Math.max(0, avail - keep));
        if (drainable > 0) {
            IAEItemStack drainStack = RecursiveCraftingHelper.canon(what);
            drainStack.setStackSize(drainable);
            inv.extractItems(drainStack, Actionable.MODULATE, src);
        }
        return drainable == target;
    }
}
