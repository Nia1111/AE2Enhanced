package com.github.aeddddd.ae2enhanced.craftingplan.dag;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.MECraftingInventory;
import appeng.util.item.AEItemStack;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;

import com.github.aeddddd.ae2enhanced.specialcrafting.Ae2CraftingReflect;
import com.github.aeddddd.ae2enhanced.specialcrafting.CycleBoundarySolver;
import com.github.aeddddd.ae2enhanced.specialcrafting.RecursiveCraftingHelper;

/**
 * DAG 拓扑执行器（1.12.2 移植）:两阶段——先物化树结构,再单趟扫描记账.
 * <p><b>1.12.2 关键差异</b>:本版没有独立计划对象,树即计划且是提交载体
 * ({@code submitJob → tree.setJob}).因此 DAG 结果必须物化为原生树:</p>
 * <ul>
 * <li>每个 DAG 节点只挂载一次（首个父节点槽位）,其余父槽位保持空叶子——
 * 共享生产在执行层经 CPU 库存池自然衔接,dive/setJob 不会重复计数;</li>
 * <li>层级由构造保证合法（dive/getAmountCrafted 要求父节点 what 是样板输出之一:
 * 子树节点 what 恒等于子 key,而子 key 就是该 DAG 节点样板的产出）;</li>
 * <li>used 记账在根节点 used 列表（初始提取,网络优先）;容器物回记模拟库存
 * （自返还按 times-1,首个循环不预贷,与 1.20.1 的 4aaa50b3 修复一致）;
 * 零网络来源的自举容器补记 missing=1（原生高水位,防零种子执行死锁）;</li>
 * <li>循环边界委托 {@link CycleBoundarySolver} 以真实 request 模拟记账,
 * 子树根替换进首个父槽位.</li>
 * </ul>
 */
public final class DagExecutor {

    /**
     * 执行结果.
     */
    public static final class Result {
        /** 缺料（规范化键 → 数量）,非空时调用方须置 simulation 标志. */
        public final Map<IAEItemStack, Long> missingItems;
        public final boolean hasCycleBoundary;

        Result(Map<IAEItemStack, Long> missingItems, boolean hasCycleBoundary) {
            this.missingItems = missingItems;
            this.hasCycleBoundary = hasCycleBoundary;
        }
    }

    /** CYCLE 节点的首个父槽位（执行阶段替换为边界子树根）. */
    private static final class ParentSlot {
        final CraftingTreeProcess parentPro;
        final CraftingTreeNode childNode;

        ParentSlot(CraftingTreeProcess parentPro, CraftingTreeNode childNode) {
            this.parentPro = parentPro;
            this.childNode = childNode;
        }
    }

    private DagExecutor() {
    }

    /**
     * @param graph 编译产物
     * @param target 根请求量
     * @param inv 模拟库存（调用方已 ignore 请求物）
     * @param job 缺料/used 记账宿主（availableCheck 已就位,供边界求解 checkUse）
     * @param rootNode 物化根节点（what = 请求物）
     */
    public static Result execute(DagGraph graph, long target, MECraftingInventory inv,
            CraftingJob job, ICraftingGrid cc, World world, CraftingTreeNode rootNode, IActionSource src)
            throws DagFallback, InterruptedException {
        Map<DagGraph.DagNode, CraftingTreeProcess> proByNode = new IdentityHashMap<>();
        Map<DagGraph.DagNode, CraftingTreeNode> terminalSlotByNode = new IdentityHashMap<>();
        Map<DagGraph.DagNode, ParentSlot> cycleSlotByNode = new IdentityHashMap<>();

        // ==================== 阶段 1:结构物化(BFS,每节点挂一次) ====================
        if (graph.root.kind == DagGraph.Kind.EMITTER) {
            throw new DagFallback("emitter_root");
        }
        Set<DagGraph.DagNode> attached = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<DagGraph.DagNode> queue = new ArrayDeque<>();
        ArrayDeque<CraftingTreeNode> parentQueue = new ArrayDeque<>();
        if (graph.root.kind == DagGraph.Kind.NORMAL) {
            queue.add(graph.root);
            parentQueue.add(rootNode);
        }
        while (!queue.isEmpty()) {
            DagGraph.DagNode node = queue.poll();
            CraftingTreeNode parentTreeNode = parentQueue.poll();
            if (!attached.add(node)) {
                continue; // 已挂载:本父槽位保持空叶子(共享生产)
            }
            CraftingTreeProcess pro = new CraftingTreeProcess(cc, job, node.pattern, parentTreeNode, 1);
            // 构造函数不建输入子节点(惰性),物化结构需要立即展开
            Ae2CraftingReflect.processAddProcess(pro);
            Ae2CraftingReflect.addProcessToNode(parentTreeNode, pro);
            proByNode.put(node, pro);
            for (DagGraph.Edge edge : node.edges) {
                CraftingTreeNode childTreeNode = findChildNode(pro, edge.child().key);
                if (childTreeNode == null) {
                    throw new DagFallback("child_node_missing:" + edge.child().key);
                }
                switch (edge.child().kind) {
                    case NORMAL:
                        queue.add(edge.child());
                        parentQueue.add(childTreeNode);
                        break;
                    case CYCLE:
                        cycleSlotByNode.putIfAbsent(edge.child(), new ParentSlot(pro, childTreeNode));
                        break;
                    case TERMINAL:
                        terminalSlotByNode.putIfAbsent(edge.child(), childTreeNode);
                        break;
                    case EMITTER:
                        throw new DagFallback("emitter_node:" + edge.child().key);
                    default:
                        throw new DagFallback("unexpected_node_kind");
                }
            }
        }

        // ==================== 阶段 2:拓扑单趟记账 ====================
        Map<DagGraph.DagNode, Long> requests = new IdentityHashMap<>();
        requests.put(graph.root, target);
        Map<DagGraph.DagNode, Long> missingByNode = new IdentityHashMap<>();
        Map<IAEItemStack, Long> synthetic = new LinkedHashMap<>(); // 合成侧余额(产出+容器返还)
        Map<IAEItemStack, Long> fundedByCredit = new LinkedHashMap<>(); // 合成侧抵扣量(按 key)
        Map<IAEItemStack, Long> networkSourced = new LinkedHashMap<>(); // 网络实取量(按 key)
        Set<IAEItemStack> containerKeys = new LinkedHashSet<>(); // 收到容器返还的 key
        boolean hasCycleBoundary = false;
        long totalExtracted = 0;

        for (DagGraph.DagNode node : graph.topoOrder) {
            long need = requests.getOrDefault(node, 0L);
            if (need <= 0) {
                continue;
            }
            // 循环边界:库存/种子语义由边界求解器全权处理,不做预提取
            if (node.kind == DagGraph.Kind.CYCLE) {
                hasCycleBoundary = true;
                CraftingTreeNode subtreeRoot = node == graph.root ? rootNode
                        : new CraftingTreeNode(cc, job, node.key.copy(), null, -1, 0);
                if (!CycleBoundarySolver.solveInto(cc, job, node.key, need, inv, subtreeRoot, src, world)) {
                    throw new DagFallback("cycle_boundary_unsolvable:" + node.key);
                }
                if (node != graph.root) {
                    ParentSlot slot = cycleSlotByNode.get(node);
                    if (slot != null) {
                        swapChild(slot.parentPro, slot.childNode, subtreeRoot);
                    }
                }
                continue;
            }

            // 提取:网络优先——物理实取记 used(执行期 CPU 仅吃 used 预取,
            // 返还物随合成渐进可用);实取不足的部分才由合成侧余额抵扣
            // (与原生语义一致:原生计划层逐次建模容器返还,种子高水位 = 0~1)
            long credited = synthetic.getOrDefault(node.key, 0L);
            long realAvailable = Math.max(0L, invAmount(inv, node.key) - credited);
            long extracted = extract(inv, node.key, need, src);
            if (extracted > 0) {
                long fromNetwork = Math.min(extracted, realAvailable);
                long funded = extracted - fromNetwork;
                if (funded > 0) {
                    synthetic.merge(node.key, -funded, Long::sum);
                    fundedByCredit.merge(node.key, funded, Long::sum);
                }
                if (fromNetwork > 0) {
                    networkSourced.merge(node.key, fromNetwork, Long::sum);
                    totalExtracted = SaturatedMath.add(totalExtracted, fromNetwork);
                    IAEItemStack usedStack = node.key.copy();
                    usedStack.setStackSize(fromNetwork);
                    Ae2CraftingReflect.getNodeUsed(rootNode).add(usedStack);
                }
            }
            long remaining = need - extracted;
            if (remaining <= 0) {
                continue;
            }

            switch (node.kind) {
                case TERMINAL:
                    missingByNode.merge(node, remaining, Long::sum);
                    break;
                case NORMAL: {
                    long times = SaturatedMath.ceilDiv(remaining, node.outputPerCraft);
                    for (DagGraph.Edge edge : node.edges) {
                        long childRequest = SaturatedMath.multiply(edge.perCraft(), times);
                        requests.merge(edge.child(), childRequest, SaturatedMath::add);
                    }
                    // 容器物返还:消耗 N 份输入回记容器——自返还容器(容器本身是本样板
                    // 的输入,催化剂型)按 times-1 计(最后一份无法自供,保住种子提取);
                    // 跨样板复用的容器按全额 times 计(下游拓扑序后提取);零网络来源
                    // 的自举容器由扫描后的高水位修正补记 missing=1(对齐原生)
                    for (IAEItemStack input : node.pattern.getCondensedInputs()) {
                        if (input == null) {
                            continue;
                        }
                        Item item = input.getItem();
                        ItemStack def = input.getDefinition();
                        if (item == null || !item.hasContainerItem(def)) {
                            continue;
                        }
                        ItemStack containerStack = item.getContainerItem(def);
                        if (containerStack.isEmpty()) {
                            continue;
                        }
                        IAEItemStack containerAe = AEItemStack.fromItemStack(containerStack);
                        if (containerAe == null) {
                            continue;
                        }
                        boolean selfReturn = containsInput(node.pattern, containerAe);
                        long creditTimes = selfReturn ? times - 1 : times;
                        if (creditTimes <= 0) {
                            continue;
                        }
                        containerAe = containerAe.copy();
                        long credit = SaturatedMath.multiply(input.getStackSize(), creditTimes);
                        containerAe.setStackSize(credit);
                        inv.injectItems(containerAe, Actionable.MODULATE, src);
                        IAEItemStack containerKey = RecursiveCraftingHelper.canon(containerAe);
                        synthetic.merge(containerKey, credit, Long::sum);
                        containerKeys.add(containerKey);
                    }
                    for (IAEItemStack output : node.pattern.getCondensedOutputs()) {
                        if (output == null) {
                            continue;
                        }
                        IAEItemStack out = output.copy();
                        out.setStackSize(SaturatedMath.multiply(output.getStackSize(), times));
                        inv.injectItems(out, Actionable.MODULATE, src);
                        synthetic.merge(RecursiveCraftingHelper.canon(out), out.getStackSize(), Long::sum);
                    }
                    CraftingTreeProcess pro = proByNode.get(node);
                    if (pro == null) {
                        throw new DagFallback("process_not_materialized:" + node.key);
                    }
                    Ae2CraftingReflect.setProcessCrafts(pro, times);
                    break;
                }
                default:
                    throw new DagFallback("unexpected_node_kind");
            }
        }

        // 种子高水位修正(对齐原生逐次循环):某容器类型的消耗全部由返还抵扣
        // (网络零实取)且无样板可合成时,首份无法自供——原生在首个失败迭代记
        // missing=1 使计划不可提交,避免零种子 CPU 执行死锁
        // (1.12.2 CPU 仅从 CPU 库存取料且不按依赖序执行)
        for (IAEItemStack containerKey : containerKeys) {
            if (fundedByCredit.getOrDefault(containerKey, 0L) <= 0
                    || networkSourced.getOrDefault(containerKey, 0L) > 0) {
                continue;
            }
            for (DagGraph.DagNode node : graph.topoOrder) {
                if (node.kind == DagGraph.Kind.TERMINAL && containerKey.equals(node.key)) {
                    missingByNode.merge(node, 1L, Long::sum);
                    break;
                }
            }
        }

        // bytes 近似:初始提取总量(合成次数的 8 倍由 dive 经 crafts×8 另行记账)
        Ae2CraftingReflect.setNodeBytes(rootNode, totalExtracted);

        // 缺料回填到对应树节点(原生 getPlan 逐节点输出 missing 条目)
        Map<IAEItemStack, Long> missingItems = new LinkedHashMap<>();
        for (Map.Entry<DagGraph.DagNode, Long> entry : missingByNode.entrySet()) {
            DagGraph.DagNode node = entry.getKey();
            long amount = entry.getValue();
            missingItems.merge(node.key, amount, Long::sum);
            CraftingTreeNode slot = node == graph.root ? rootNode : terminalSlotByNode.get(node);
            if (slot != null) {
                Ae2CraftingReflect.setNodeMissing(slot, amount);
            }
        }
        return new Result(missingItems, hasCycleBoundary);
    }

    /** 在 pro 的输入子节点中找到 what 等于 key 的节点(挂载点). */
    private static CraftingTreeNode findChildNode(CraftingTreeProcess pro, IAEItemStack key) {
        for (CraftingTreeNode child : Ae2CraftingReflect.getProcessNodes(pro).keySet()) {
            IAEItemStack what = Ae2CraftingReflect.getNodeWhat(child);
            if (what != null && key.equals(what)) {
                return child;
            }
        }
        return null;
    }

    /** 把父 process 的子槽位从占位节点替换为边界子树根（保持 perCraft 计数值）. */
    private static void swapChild(CraftingTreeProcess parentPro, CraftingTreeNode oldChild,
            CraftingTreeNode newChild) {
        Object2LongArrayMap<CraftingTreeNode> nodes = Ae2CraftingReflect.getProcessNodes(parentPro);
        long value = nodes.getLong(oldChild);
        nodes.removeLong(oldChild);
        nodes.put(newChild, value);
    }

    /** 模拟库存中某 key 的当前总量（含网络余量与已注入的合成侧余额）. */
    private static long invAmount(MECraftingInventory inv, IAEItemStack key) {
        IAEItemStack entry = inv.getItemList().findPrecise(key);
        return entry == null ? 0L : entry.getStackSize();
    }

    /** 从模拟库存提取（精确匹配）,返回实际提取量. */
    private static long extract(MECraftingInventory inv, IAEItemStack key, long amount, IActionSource src) {
        IAEItemStack request = key.copy();
        request.setStackSize(amount);
        IAEItemStack result = inv.extractItems(request, Actionable.MODULATE, src);
        return result == null ? 0 : result.getStackSize();
    }

    /** 容器键是否同时是本样板的输入（自返还/催化剂型判定）. */
    private static boolean containsInput(ICraftingPatternDetails pattern, IAEItemStack containerKey) {
        for (IAEItemStack input : pattern.getCondensedInputs()) {
            if (input != null && containerKey.equals(input)) {
                return true;
            }
        }
        return false;
    }
}
