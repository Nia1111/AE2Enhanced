package com.github.aeddddd.ae2enhanced.specialcrafting;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.MECraftingInventory;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;

/**
 * AE2 合成计算内部成员的反射桥（1.12.2 版,对应 1.20.1 的 Ae2CraftingReflect）.
 * <p>1.12.2 的 {@code CraftingTreeNode.request} / {@code CraftingTreeProcess.request} /
 * {@code dive} 以及 {@code CraftingJob} 的多数成员均为包私有或私有,而本包不在
 * {@code appeng.crafting} 下,统一经本桥访问。所有成员名在初始化时一次性解析并校验,
 * AE2 升级导致签名变化时在首次调用即抛出明确异常（路由层捕获后回落原生行为）.</p>
 */
public final class Ae2CraftingReflect {

    private static final Field JOB_ORIGINAL;
    private static final Field JOB_CC;
    private static final Field JOB_ACTION_SRC;
    private static final Field JOB_AVAILABLE_CHECK;
    private static final Field JOB_SIMULATE;
    private static final Method JOB_SET_TREE;
    private static final Method JOB_HANDLE_PAUSING;
    private static final Method JOB_FINISH;
    private static final Method JOB_GET_WORLD;
    private static final Method NODE_REQUEST;
    private static final Method NODE_DIVE;
    private static final Field NODE_NODES;
    private static final Field NODE_MISSING;
    private static final Method PROCESS_REQUEST;
    private static final Field NODE_USED;
    private static final Field NODE_WHAT;
    private static final Field PROCESS_NODES;
    private static final Field PROCESS_CRAFTS;
    private static final Field PROCESS_DETAILS;
    private static final Field PROCESS_PARENT;

    static {
        try {
            JOB_ORIGINAL = CraftingJob.class.getDeclaredField("original");
            JOB_ORIGINAL.setAccessible(true);
            JOB_CC = CraftingJob.class.getDeclaredField("cc");
            JOB_CC.setAccessible(true);
            JOB_ACTION_SRC = CraftingJob.class.getDeclaredField("actionSrc");
            JOB_ACTION_SRC.setAccessible(true);
            JOB_AVAILABLE_CHECK = CraftingJob.class.getDeclaredField("availableCheck");
            JOB_AVAILABLE_CHECK.setAccessible(true);
            JOB_SIMULATE = CraftingJob.class.getDeclaredField("simulate");
            JOB_SIMULATE.setAccessible(true);
            JOB_SET_TREE = CraftingJob.class.getDeclaredMethod("setTree", CraftingTreeNode.class);
            JOB_SET_TREE.setAccessible(true);
            JOB_HANDLE_PAUSING = CraftingJob.class.getDeclaredMethod("handlePausing");
            JOB_HANDLE_PAUSING.setAccessible(true);
            JOB_FINISH = CraftingJob.class.getDeclaredMethod("finish");
            JOB_FINISH.setAccessible(true);
            JOB_GET_WORLD = CraftingJob.class.getDeclaredMethod("getWorld");
            JOB_GET_WORLD.setAccessible(true);
            NODE_REQUEST = CraftingTreeNode.class.getDeclaredMethod("request",
                    MECraftingInventory.class, long.class, IActionSource.class);
            NODE_REQUEST.setAccessible(true);
            NODE_DIVE = CraftingTreeNode.class.getDeclaredMethod("dive", CraftingJob.class);
            NODE_DIVE.setAccessible(true);
            NODE_NODES = CraftingTreeNode.class.getDeclaredField("nodes");
            NODE_NODES.setAccessible(true);
            NODE_MISSING = CraftingTreeNode.class.getDeclaredField("missing");
            NODE_MISSING.setAccessible(true);
            PROCESS_REQUEST = CraftingTreeProcess.class.getDeclaredMethod("request",
                    MECraftingInventory.class, long.class, IActionSource.class);
            PROCESS_REQUEST.setAccessible(true);
            NODE_USED = CraftingTreeNode.class.getDeclaredField("used");
            NODE_USED.setAccessible(true);
            PROCESS_NODES = CraftingTreeProcess.class.getDeclaredField("nodes");
            PROCESS_NODES.setAccessible(true);
            PROCESS_CRAFTS = CraftingTreeProcess.class.getDeclaredField("crafts");
            PROCESS_CRAFTS.setAccessible(true);
            PROCESS_DETAILS = CraftingTreeProcess.class.getDeclaredField("details");
            PROCESS_DETAILS.setAccessible(true);
            NODE_WHAT = CraftingTreeNode.class.getDeclaredField("what");
            NODE_WHAT.setAccessible(true);
            PROCESS_PARENT = CraftingTreeProcess.class.getDeclaredField("parent");
            PROCESS_PARENT.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Ae2CraftingReflect() {
    }

    static MECraftingInventory getOriginal(CraftingJob job) {
        try {
            return (MECraftingInventory) JOB_ORIGINAL.get(job);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingJob.original 失败", e);
        }
    }

    static ICraftingGrid getCc(CraftingJob job) {
        try {
            return (ICraftingGrid) JOB_CC.get(job);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingJob.cc 失败", e);
        }
    }

    static IActionSource getActionSrc(CraftingJob job) {
        try {
            return (IActionSource) JOB_ACTION_SRC.get(job);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingJob.actionSrc 失败", e);
        }
    }

    static net.minecraft.world.World getWorld(CraftingJob job) {
        try {
            return (net.minecraft.world.World) JOB_GET_WORLD.invoke(job);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用 CraftingJob.getWorld 失败", e);
        }
    }

    static void setAvailableCheck(CraftingJob job, MECraftingInventory inv) {
        try {
            JOB_AVAILABLE_CHECK.set(job, inv);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写入 CraftingJob.availableCheck 失败", e);
        }
    }

    static void setSimulate(CraftingJob job, boolean simulate) {
        try {
            JOB_SIMULATE.setBoolean(job, simulate);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写入 CraftingJob.simulate 失败", e);
        }
    }

    static void setTree(CraftingJob job, CraftingTreeNode tree) {
        try {
            JOB_SET_TREE.invoke(job, tree);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用 CraftingJob.setTree 失败", e);
        }
    }

    static void handlePausing(CraftingJob job) throws InterruptedException {
        try {
            JOB_HANDLE_PAUSING.invoke(job);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            }
            throw new IllegalStateException("调用 CraftingJob.handlePausing 失败", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用 CraftingJob.handlePausing 失败", e);
        }
    }

    static void finish(CraftingJob job) {
        try {
            JOB_FINISH.invoke(job);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用 CraftingJob.finish 失败", e);
        }
    }

    static IAEItemStack nodeRequest(CraftingTreeNode node, MECraftingInventory inv, long amount,
            IActionSource src) throws CraftBranchFailure, InterruptedException {
        try {
            return (IAEItemStack) NODE_REQUEST.invoke(node, inv, amount, src);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof CraftBranchFailure) {
                throw (CraftBranchFailure) e.getCause();
            }
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            }
            throw new IllegalStateException("CraftingTreeNode.request 执行异常", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用 CraftingTreeNode.request 失败", e);
        }
    }

    static void nodeDive(CraftingTreeNode node, CraftingJob job) {
        try {
            NODE_DIVE.invoke(node, job);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用 CraftingTreeNode.dive 失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    static void addProcessToNode(CraftingTreeNode node, CraftingTreeProcess process) {
        try {
            ((ArrayList<CraftingTreeProcess>) NODE_NODES.get(node)).add(process);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写入 CraftingTreeNode.nodes 失败", e);
        }
    }

    static void setNodeMissing(CraftingTreeNode node, long missing) {
        try {
            NODE_MISSING.setLong(node, missing);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写入 CraftingTreeNode.missing 失败", e);
        }
    }

    static void treeProcessRequest(CraftingTreeProcess pro, MECraftingInventory inv, long times,
            IActionSource src) throws CraftBranchFailure, InterruptedException {
        try {
            PROCESS_REQUEST.invoke(pro, inv, times, src);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof CraftBranchFailure) {
                throw (CraftBranchFailure) e.getCause();
            }
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            }
            throw new IllegalStateException("CraftingTreeProcess.request 执行异常", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用 CraftingTreeProcess.request 失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static IItemList<IAEItemStack> getNodeUsed(CraftingTreeNode node) {
        try {
            return (IItemList<IAEItemStack>) NODE_USED.get(node);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingTreeNode.used 失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<CraftingTreeProcess> getNodeProcesses(CraftingTreeNode node) {
        try {
            return (ArrayList<CraftingTreeProcess>) NODE_NODES.get(node);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingTreeNode.nodes 失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Object2LongArrayMap<CraftingTreeNode> getProcessNodes(CraftingTreeProcess pro) {
        try {
            return (Object2LongArrayMap<CraftingTreeNode>) PROCESS_NODES.get(pro);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingTreeProcess.nodes 失败", e);
        }
    }

    public static long getProcessCrafts(CraftingTreeProcess pro) {
        try {
            return PROCESS_CRAFTS.getLong(pro);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingTreeProcess.crafts 失败", e);
        }
    }

    public static ICraftingPatternDetails getProcessDetails(CraftingTreeProcess pro) {
        try {
            return (ICraftingPatternDetails) PROCESS_DETAILS.get(pro);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingTreeProcess.details 失败", e);
        }
    }

    public static IAEItemStack getNodeWhat(CraftingTreeNode node) {
        try {
            return (IAEItemStack) NODE_WHAT.get(node);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("访问 CraftingTreeNode.what 失败", e);
        }
    }

    public static void setProcessParent(CraftingTreeProcess pro, CraftingTreeNode parent) {
        try {
            PROCESS_PARENT.set(pro, parent);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写入 CraftingTreeProcess.parent 失败", e);
        }
    }
}
