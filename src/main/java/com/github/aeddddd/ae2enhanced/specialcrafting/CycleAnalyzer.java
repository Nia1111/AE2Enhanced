package com.github.aeddddd.ae2enhanced.specialcrafting;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.world.World;

import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;

/**
 * 跨样板循环链分析器（阶段 2,泛化版,移植自 1.20.1）.
 * <p>枚举经过请求物品的<b>简单环</b>,对每个环键集建立"样板×物品"精确系数矩阵,
 * 以广义叉积（Bareiss 精确行列式）求平衡方程组的正整数零空间向量,得到各样板执行
 * 次数比与环净乘积率分类（增殖/中性/耗散）;再以超轮前缀分析求各环内物品的启动种子.</p>
 * <p>秩不足/无正整数解/数值超 long 时返回 null,由调用方回落原生行为.</p>
 */
public final class CycleAnalyzer {

    /**
     * 环净乘积率分类.
     */
    public enum RateClass {
        /** 每轮净产出为正,可增殖. */
        PRODUCTIVE,
        /** 进出相等（中性环）,不接管. */
        NEUTRAL,
        /** 净产出为负（耗散环）,不接管. */
        DISSIPATIVE
    }

    /**
     * 环的一步:{@code pattern} 将 {@code fromKey}（路径视角的主输入）转化为 {@code toKey}.
     */
    public static final class CycleStep {
        private final ICraftingPatternDetails pattern;
        private final IAEItemStack fromKey;
        private final IAEItemStack toKey;

        public CycleStep(ICraftingPatternDetails pattern, IAEItemStack fromKey, IAEItemStack toKey) {
            this.pattern = pattern;
            this.fromKey = fromKey;
            this.toKey = toKey;
        }

        public ICraftingPatternDetails pattern() {
            return pattern;
        }

        public IAEItemStack fromKey() {
            return fromKey;
        }

        public IAEItemStack toKey() {
            return toKey;
        }
    }

    /**
     * 环分析结果.
     */
    public static final class Analysis {
        private final List<IAEItemStack> keys;
        private final List<CycleStep> steps;
        private final RateClass rateClass;
        private final long[] timesPerRound;
        private final long netGain;
        private final long[] seedsPerKey;
        private final long[] batchSeedPerKey;

        Analysis(List<IAEItemStack> keys, List<CycleStep> steps, RateClass rateClass,
                long[] timesPerRound, long netGain, long[] seedsPerKey, long[] batchSeedPerKey) {
            this.keys = keys;
            this.steps = steps;
            this.rateClass = rateClass;
            this.timesPerRound = timesPerRound;
            this.netGain = netGain;
            this.seedsPerKey = seedsPerKey;
            this.batchSeedPerKey = batchSeedPerKey;
        }

        public List<IAEItemStack> keys() {
            return keys;
        }

        public List<CycleStep> steps() {
            return steps;
        }

        public RateClass rateClass() {
            return rateClass;
        }

        public long[] timesPerRound() {
            return timesPerRound;
        }

        public long netGain() {
            return netGain;
        }

        public long[] seedsPerKey() {
            return seedsPerKey;
        }

        public long[] batchSeedPerKey() {
            return batchSeedPerKey;
        }
    }

    /** detector/求解共用的遍历预算,避免超大网络下 DFS 失控. */
    private static final int MAX_VISITED = 512;
    /** 单次请求最多枚举的候选环数量. */
    private static final int MAX_CYCLES = 64;

    private CycleAnalyzer() {
    }

    /**
     * 枚举经过 {@code root} 的所有简单环（长度 ≥ 2;自引用环由阶段 1 处理,此处跳过）,
     * 按环长度降序返回（长环的键集更完整,优先尝试）.
     */
    public static List<List<CycleStep>> findCyclesThrough(ICraftingGrid cc, IAEItemStack root, World world) {
        int[] budget = { MAX_VISITED };
        List<List<CycleStep>> cycles = new ArrayList<>();
        Set<IAEItemStack> onPath = new HashSet<>();
        IAEItemStack rootKey = RecursiveCraftingHelper.canon(root);
        onPath.add(rootKey);
        LinkedHashMap<IAEItemStack, CycleStep> chain = new LinkedHashMap<>();
        dfs(cc, world, rootKey, rootKey, onPath, chain, budget, cycles);
        cycles.sort((a, b) -> Integer.compare(b.size(), a.size()));
        return cycles;
    }

    /**
     * 沿"被产生"边回溯 DFS:current 由某 pattern 产生,其输入 from 即反向边.
     * from == root 时闭合为环并记录（继续搜索其他环）.
     */
    private static void dfs(ICraftingGrid cc, World world, IAEItemStack root, IAEItemStack current,
            Set<IAEItemStack> onPath, LinkedHashMap<IAEItemStack, CycleStep> chain, int[] budget,
            List<List<CycleStep>> cycles) {
        if (budget[0]-- <= 0 || cycles.size() >= MAX_CYCLES) {
            return;
        }
        for (ICraftingPatternDetails pattern : cc.getCraftingFor(current, null, -1, world)) {
            IAEItemStack primaryOut = pattern.getPrimaryOutput();
            if (primaryOut == null || !current.equals(primaryOut) || primaryOut.getStackSize() <= 0) {
                continue;
            }
            for (IAEItemStack input : pattern.getCondensedInputs()) {
                if (input == null || input.getStackSize() <= 0) {
                    continue;
                }
                IAEItemStack from = RecursiveCraftingHelper.canon(input);
                if (from.equals(current)) {
                    continue; // 自引用交给阶段 1
                }
                CycleStep step = new CycleStep(pattern, from, current);
                if (from.equals(root)) {
                    List<CycleStep> steps = new ArrayList<>();
                    steps.add(step);
                    List<CycleStep> chainSteps = new ArrayList<>(chain.values());
                    Collections.reverse(chainSteps);
                    steps.addAll(chainSteps);
                    cycles.add(steps);
                    if (cycles.size() >= MAX_CYCLES) {
                        return;
                    }
                    continue;
                }
                if (onPath.contains(from)) {
                    continue; // 只接受经过 root 的简单环
                }
                onPath.add(from);
                chain.put(from, step);
                dfs(cc, world, root, from, onPath, chain, budget, cycles);
                chain.remove(from);
                onPath.remove(from);
            }
        }
    }

    /**
     * 分析简单环:闭合性校验后委托求解核心.
     *
     * @return 分析结果;闭合性错误/秩不足/无正整数解/数值超 long 时返回 null.
     */
    @Nullable
    public static Analysis analyze(List<CycleStep> cycle) {
        if (cycle == null || cycle.size() < 2) {
            return null;
        }
        int n = cycle.size();
        List<IAEItemStack> keys = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (!cycle.get(i).toKey().equals(cycle.get((i + 1) % n).fromKey())) {
                return null;
            }
            keys.add(cycle.get(i).fromKey());
        }
        return solveSystem(keys, cycle);
    }

    /**
     * 候选环并集分析（θ 形共享结构:多个环共享同一中间样板,逐环分析会互相把
     * 对方的中间物当环外输入而双双失败）.
     * <p>当 样板数 == 键数 时构成适定方程组,与单环同一套零空间求解;否则返回 null.</p>
     */
    @Nullable
    public static Analysis analyzeUnion(List<List<CycleStep>> cycles) {
        if (cycles == null || cycles.size() < 2) {
            return null;
        }
        IAEItemStack root = cycles.get(0).get(0).fromKey();
        List<IAEItemStack> keys = new ArrayList<>();
        keys.add(root);
        Map<ICraftingPatternDetails, CycleStep> stepByPattern = new LinkedHashMap<>();
        for (List<CycleStep> cycle : cycles) {
            for (CycleStep step : cycle) {
                if (!step.fromKey().equals(root) && !keys.contains(step.fromKey())) {
                    keys.add(step.fromKey());
                }
                if (!step.toKey().equals(root) && !keys.contains(step.toKey())) {
                    keys.add(step.toKey());
                }
                if (!stepByPattern.containsKey(step.pattern())) {
                    stepByPattern.put(step.pattern(), step);
                }
            }
        }
        List<CycleStep> steps = new ArrayList<>(stepByPattern.values());
        if (steps.size() != keys.size()) {
            return null; // m ≠ n:欠定或仅有平凡解,回落逐环迭代
        }
        return solveSystem(keys, steps);
    }

    /**
     * 求解核心:对给定的键集与样板集建立系数矩阵,求平衡方程正整数零空间解、
     * 净率分类、前缀种子与多消费者键全批次种子.
     */
    @Nullable
    private static Analysis solveSystem(List<IAEItemStack> keys, List<CycleStep> steps) {
        int n = steps.size();
        if (n < 2 || keys.size() != n) {
            return null;
        }

        // 系数矩阵 coeff[step][key] = 该样板每份对该 key 的净产出(产出-消耗)
        BigInteger[][] coeff = new BigInteger[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                coeff[i][j] = BigInteger.ZERO;
            }
            ICraftingPatternDetails pattern = steps.get(i).pattern();
            for (IAEItemStack output : pattern.getCondensedOutputs()) {
                if (output == null) {
                    continue;
                }
                int keyIdx = keys.indexOf(RecursiveCraftingHelper.canon(output));
                if (keyIdx >= 0) {
                    coeff[i][keyIdx] = coeff[i][keyIdx].add(BigInteger.valueOf(output.getStackSize()));
                }
            }
            for (IAEItemStack input : pattern.getCondensedInputs()) {
                if (input == null) {
                    continue;
                }
                int keyIdx = keys.indexOf(RecursiveCraftingHelper.canon(input));
                if (keyIdx >= 0) {
                    coeff[i][keyIdx] = coeff[i][keyIdx].subtract(BigInteger.valueOf(input.getStackSize()));
                }
            }
        }

        // 平衡方程:对每个非 root 键 Σ coeff[step][key]×t[step] = 0.
        BigInteger[][] balance = new BigInteger[n - 1][n];
        for (int row = 0; row < n - 1; row++) {
            for (int j = 0; j < n; j++) {
                balance[row][j] = coeff[j][row + 1];
            }
        }
        BigInteger[] times = nullSpaceVector(balance, n);
        if (times == null) {
            return null;
        }

        // 净率分类:root 键每超轮净产出 = Σ coeff[step][0]×t[step]
        BigInteger netGain = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            netGain = netGain.add(coeff[i][0].multiply(times[i]));
        }
        int cmp = netGain.compareTo(BigInteger.ZERO);
        RateClass rateClass = cmp > 0 ? RateClass.PRODUCTIVE : cmp == 0 ? RateClass.NEUTRAL : RateClass.DISSIPATIVE;

        // 各键种子:按执行顺序做超轮前缀分析,取各键余额最低点
        BigInteger[] balancePrefix = new BigInteger[n];
        BigInteger[] minPrefix = new BigInteger[n];
        for (int j = 0; j < n; j++) {
            balancePrefix[j] = BigInteger.ZERO;
            minPrefix[j] = BigInteger.ZERO;
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                balancePrefix[j] = balancePrefix[j].add(coeff[i][j].multiply(times[i]));
                if (balancePrefix[j].compareTo(minPrefix[j]) < 0) {
                    minPrefix[j] = balancePrefix[j];
                }
            }
        }

        // 多消费者键检测:某环键被 ≥2 个步骤消耗时,需要按每超轮总消耗记账
        int[] consumers = new int[n];
        BigInteger[] consumption = new BigInteger[n];
        for (int j = 0; j < n; j++) {
            consumption[j] = BigInteger.ZERO;
            for (int i = 0; i < n; i++) {
                if (coeff[i][j].signum() < 0) {
                    consumers[j]++;
                    consumption[j] = consumption[j].add(coeff[i][j].negate().multiply(times[i]));
                }
            }
        }

        try {
            long[] timesLong = new long[n];
            long[] seeds = new long[n];
            long[] batchSeeds = new long[n];
            for (int i = 0; i < n; i++) {
                timesLong[i] = times[i].longValueExact();
                seeds[i] = minPrefix[i].negate().max(BigInteger.ZERO).longValueExact();
                batchSeeds[i] = consumers[i] >= 2 ? consumption[i].longValueExact() : 0;
            }
            return new Analysis(Collections.unmodifiableList(new ArrayList<>(keys)),
                    Collections.unmodifiableList(new ArrayList<>(steps)), rateClass, timesLong,
                    netGain.longValueExact(), seeds, batchSeeds);
        } catch (ArithmeticException e) {
            return null; // 超出 long → 不接管
        }
    }

    /**
     * 求 (n-1)×n 整数矩阵的正整数零空间向量（广义叉积,Bareiss 精确行列式）.
     *
     * @return 已约分的正整数向量;秩不足或不存在全正解时返回 null.
     */
    @Nullable
    private static BigInteger[] nullSpaceVector(BigInteger[][] balance, int n) {
        BigInteger[] v = new BigInteger[n];
        boolean allZero = true;
        for (int j = 0; j < n; j++) {
            BigInteger[][] sub = new BigInteger[n - 1][n - 1];
            for (int r = 0; r < n - 1; r++) {
                int c = 0;
                for (int k = 0; k < n; k++) {
                    if (k != j) {
                        sub[r][c++] = balance[r][k];
                    }
                }
            }
            BigInteger det = determinant(sub, n - 1);
            v[j] = (j % 2 == 0) ? det : det.negate();
            if (!v[j].equals(BigInteger.ZERO)) {
                allZero = false;
            }
        }
        if (allZero) {
            return null; // 秩 < n-1,欠定 → 不接管
        }
        boolean anyPos = false;
        boolean anyNeg = false;
        for (BigInteger x : v) {
            anyPos |= x.signum() > 0;
            anyNeg |= x.signum() < 0;
        }
        if (anyNeg) {
            for (int j = 0; j < n; j++) {
                v[j] = v[j].negate();
            }
        }
        for (BigInteger x : v) {
            if (x.signum() <= 0) {
                return null;
            }
        }
        BigInteger gcd = v[0].abs();
        for (BigInteger x : v) {
            gcd = gcd.gcd(x.abs());
        }
        for (int j = 0; j < n; j++) {
            v[j] = v[j].divide(gcd);
        }
        return v;
    }

    /**
     * Bareiss 无分数高斯消元求精确行列式.
     */
    private static BigInteger determinant(BigInteger[][] matrix, int n) {
        if (n == 0) {
            return BigInteger.ONE;
        }
        BigInteger[][] m = new BigInteger[n][n];
        for (int i = 0; i < n; i++) {
            m[i] = matrix[i].clone();
        }
        BigInteger prevPivot = BigInteger.ONE;
        int sign = 1;
        for (int k = 0; k < n - 1; k++) {
            if (m[k][k].equals(BigInteger.ZERO)) {
                int swap = -1;
                for (int r = k + 1; r < n; r++) {
                    if (!m[r][k].equals(BigInteger.ZERO)) {
                        swap = r;
                        break;
                    }
                }
                if (swap < 0) {
                    return BigInteger.ZERO;
                }
                BigInteger[] tmp = m[k];
                m[k] = m[swap];
                m[swap] = tmp;
                sign = -sign;
            }
            for (int i = k + 1; i < n; i++) {
                for (int j = k + 1; j < n; j++) {
                    m[i][j] = m[i][j].multiply(m[k][k])
                            .subtract(m[i][k].multiply(m[k][j]))
                            .divide(prevPivot);
                }
            }
            prevPivot = m[k][k];
        }
        return sign > 0 ? m[n - 1][n - 1] : m[n - 1][n - 1].negate();
    }
}
