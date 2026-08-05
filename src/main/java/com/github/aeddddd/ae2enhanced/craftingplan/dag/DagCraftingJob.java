package com.github.aeddddd.ae2enhanced.craftingplan.dag;

import javax.annotation.Nullable;

import net.minecraft.world.World;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.MECraftingInventory;
import appeng.hooks.TickHandler;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.specialcrafting.Ae2CraftingReflect;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialLog;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanDisplayHook;
import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanMarker;

/**
 * DAG 合成计算器（阶段 4,1.12.2 移植）:以"编译 DAG + 拓扑单趟扫描"取代原生递归树.
 * <p>继承原生 {@link CraftingJob} 复用其时间片调度/暂停/线程池骨架;
 * DAG 路径任何回落信号或异常都退回原生 {@code super.run()}（宁可慢不可错）.</p>
 * <p><b>1.12.2 关键差异</b>:树即计划且是提交载体,DAG 结果经 {@link DagExecutor}
 * 物化为原生树（每节点挂一次、used/缺料回填、边界子树接入）后,提交/显示/执行
 * 全部复用原生路径.含循环边界的计划标记特殊,硬路由到超因果计算核心
 * （无限库存 + 门控/配额调度在场）.</p>
 */
public class DagCraftingJob extends CraftingJob {

    protected final World world;

    /** 本次计划是否含循环边界（物化成功后用于特殊标记）. */
    protected boolean hasCycleBoundary;

    public DagCraftingJob(World w, IGrid grid, IActionSource actionSrc, IAEItemStack what,
            ICraftingCallback callback) {
        super(w, grid, actionSrc, what, callback);
        this.world = w;
    }

    @Override
    public void run() {
        try {
            TickHandler.INSTANCE.registerCraftingSimulation(this.world, this);
            Ae2CraftingReflect.handlePausing(this);

            CraftingTreeNode root = this.computeDagPlan();
            if (root == null) {
                Ae2CraftingReflect.setAvailableCheck(this, null);
                super.run();
                return;
            }
            Ae2CraftingReflect.setTree(this, root);
            Ae2CraftingReflect.nodeDive(root, this);
            // 缺料(模拟)计划不标记:原生 submitJob 本就拒绝模拟计划
            if (this.hasCycleBoundary && !this.isSimulation()) {
                SpecialPlanMarker.mark(this);
            }
            SpecialPlanDisplayHook.sendPlanInfo(this);
            Ae2CraftingReflect.finish(this);
        } catch (InterruptedException e) {
            SpecialLog.info("[DAG] 计算被取消");
            Ae2CraftingReflect.finish(this);
        } catch (Throwable t) {
            AE2Enhanced.LOGGER.warn("DAG 计划异常,回落原生计算: {}", t.toString());
            Ae2CraftingReflect.setAvailableCheck(this, null);
            super.run();
        }
    }

    /**
     * @return 物化完成的根节点;任何不适用情形返回 null（调用方回落原生）.
     */
    @Nullable
    protected CraftingTreeNode computeDagPlan() throws InterruptedException {
        IActionSource src = Ae2CraftingReflect.getActionSrc(this);
        ICraftingGrid cc = Ae2CraftingReflect.getCc(this);
        IAEItemStack output = this.getOutput();

        DagGraph graph;
        try {
            graph = DagCompiler.compile(cc, output, this.world);
        } catch (DagFallback fallback) {
            SpecialLog.info("[DAG] 编译回落({}): {}", fallback.reason, output);
            return null;
        }

        MECraftingInventory inv = new MECraftingInventory(Ae2CraftingReflect.getOriginal(this), true, false, true);
        Ae2CraftingReflect.invIgnore(inv, output); // 镜像原生:请求物自身库存不参与计划扣除
        Ae2CraftingReflect.setAvailableCheck(this,
                new MECraftingInventory(Ae2CraftingReflect.getOriginal(this), false, false, false));
        CraftingTreeNode root = new CraftingTreeNode(cc, this, output.copy(), null, -1, 0);
        DagExecutor.Result result;
        try {
            result = DagExecutor.execute(graph, output.getStackSize(), inv, this, cc, this.world, root, src);
        } catch (DagFallback fallback) {
            SpecialLog.info("[DAG] 执行回落({}): {}", fallback.reason, output);
            return null;
        }
        this.hasCycleBoundary = result.hasCycleBoundary;
        if (!result.missingItems.isEmpty()) {
            // 原生 simulation 标志由失败重试置位;DAG 缺料不抛异常,须显式置位,
            // 否则产出"有缺料却标记可提交"的不一致计划
            Ae2CraftingReflect.setSimulate(this, true);
        }
        SpecialLog.info("[DAG] 计划完成: {}×{},节点 {},循环边界 {}", output, output.getStackSize(),
                graph.topoOrder.size(), this.hasCycleBoundary);
        return root;
    }
}
