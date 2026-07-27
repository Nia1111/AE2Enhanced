package com.github.aeddddd.ae2enhanced.specialcrafting;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import appeng.api.storage.data.IAEItemStack;

/**
 * 特殊计划显示信息（服务端计算 → 客户端渲染,移植自 1.20.1）.
 * <p>{@link #entries} 记录自增殖/循环链键的轮次与每轮产耗;{@link #callCounts}
 * 统计各主产出键的样板调用次数,客户端在合成确认界面显示"调用 N 次(约 R 轮发配)".</p>
 */
public final class SpecialPlanInfo {

    public static final int KIND_SELF_DUP = 1;
    public static final int KIND_CYCLE = 2;

    public static final SpecialPlanInfo EMPTY = new SpecialPlanInfo(new LinkedHashMap<>(), new LinkedHashMap<>());

    /**
     * @param kind 1=自增殖样板;2=循环链成员
     * @param rounds 循环链总轮次(自增殖恒 1)
     * @param perRoundProduce 每轮(自增殖:每次)产出量
     * @param perRoundConsume 每轮(自增殖:每次)消耗量
     * @param totalCrafts 自增殖总调用次数(循环链恒 0)
     * @param initialExtract 初始提取(种子)
     */
    public static final class Entry {
        public final int kind;
        public final long rounds;
        public final long perRoundProduce;
        public final long perRoundConsume;
        public final long totalCrafts;
        public final long initialExtract;

        public Entry(int kind, long rounds, long perRoundProduce, long perRoundConsume,
                long totalCrafts, long initialExtract) {
            this.kind = kind;
            this.rounds = rounds;
            this.perRoundProduce = perRoundProduce;
            this.perRoundConsume = perRoundConsume;
            this.totalCrafts = totalCrafts;
            this.initialExtract = initialExtract;
        }
    }

    public final Map<IAEItemStack, Entry> entries;
    public final Map<IAEItemStack, Long> callCounts;

    public SpecialPlanInfo(Map<IAEItemStack, Entry> entries, Map<IAEItemStack, Long> callCounts) {
        this.entries = entries;
        this.callCounts = callCounts;
    }

    public boolean isEmpty() {
        return entries.isEmpty() && callCounts.isEmpty();
    }

    @Nullable
    public Entry entryFor(IAEItemStack key) {
        for (Map.Entry<IAEItemStack, Entry> e : entries.entrySet()) {
            if (e.getKey().equals(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    /** 该键作为主产出的样板调用总次数(无则 0). */
    public long callCountOf(IAEItemStack key) {
        for (Map.Entry<IAEItemStack, Long> e : callCounts.entrySet()) {
            if (e.getKey().equals(key)) {
                return e.getValue();
            }
        }
        return 0;
    }
}
