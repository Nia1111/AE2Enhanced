package com.github.aeddddd.ae2enhanced.craftingplan.dag;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;

/**
 * DAG 计划图:编译产物(纯结构,不含库存).
 * <p>节点按 key 合并(重复子树共享同一节点);{@link #topoOrder} 保证
 * 父节点(需求方)先于子节点(原料方),执行器按此序单趟扫描.</p>
 */
public final class DagGraph {

    public enum Kind {
        /** 可由"干净"样板合成(输入唯一候选). */
        NORMAL,
        /** 网络发射台提供(level emitter),零成本. */
        EMITTER,
        /** 无任何样板且不可发射 → 缺料. */
        TERMINAL,
        /** 循环边界(SCC 收缩点):深层自引用/循环链,执行时委托 CycleBoundarySolver. */
        CYCLE
    }

    /** 一条输入边:每执行一次父样板,消耗子 key {@link #perCraft} 份. */
    public static final class Edge {
        private final DagNode child;
        private final long perCraft;

        public Edge(DagNode child, long perCraft) {
            this.child = child;
            this.perCraft = perCraft;
        }

        public DagNode child() {
            return child;
        }

        public long perCraft() {
            return perCraft;
        }
    }

    public static final class DagNode {
        public final Kind kind;
        public final IAEItemStack key;
        /** 每次执行产出本 key 的数量(NORMAL 有效). */
        public final long outputPerCraft;
        @Nullable
        public final ICraftingPatternDetails pattern;
        public final List<Edge> edges = new ArrayList<>();

        DagNode(Kind kind, IAEItemStack key, long outputPerCraft, @Nullable ICraftingPatternDetails pattern) {
            this.kind = kind;
            this.key = key;
            this.outputPerCraft = outputPerCraft;
            this.pattern = pattern;
        }
    }

    public final List<DagNode> topoOrder = new ArrayList<>();
    public final DagNode root;

    DagGraph(DagNode root) {
        this.root = root;
    }
}
