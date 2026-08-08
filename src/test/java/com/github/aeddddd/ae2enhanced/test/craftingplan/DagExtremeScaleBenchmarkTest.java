package com.github.aeddddd.ae2enhanced.test.craftingplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.CraftingJob;
import appeng.util.item.AEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanMarker;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.PlanView;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.ProcessingPatternBuilder;
import com.github.aeddddd.ae2enhanced.test.specialcrafting.SimulationEnv;

/**
 * 极端规模交叉合成图基准（默认跳过,设环境变量 {@code AE2E_BENCH=1} 启用）.
 * <p>图结构:分层交叉图,主体普通合成(~97%),嵌自增殖子环(~2%)与催化 θ 环(~1%);
 * 请求量按 AE 字节标准（镜像 dive 记账:8×Σ样板调用 + Σ网络实取）线性校准到
 * ~100T 字节,对比 DAG 引擎与原生 {@link CraftingJob} 的计划耗时.</p>
 * <p>注意:原生 {@code getByteTotal()} 途经 int 字段,超过 ~2.1G 即溢出回绕,
 * 本基准以独立 long 记账（{@link #impliedBytes}）作为 100T 口径.</p>
 *
 * <p>环境变量:</p>
 * <ul>
 * <li>{@code AE2E_BENCH}=1 启用;</li>
 * <li>{@code AE2E_BENCH_TARGET_BYTES} 目标字节(默认 1e14);</li>
 * <li>{@code AE2E_BENCH_NATIVE_TIMEOUT_MIN} 原生基线超时分钟(默认 10);</li>
 * <li>{@code AE2E_BENCH_LAYERS}/{@code AE2E_BENCH_WIDTH} 图规模(默认 34×60).</li>
 * </ul>
 */
public class DagExtremeScaleBenchmarkTest {

    private static final boolean ENABLED = System.getenv("AE2E_BENCH") != null;
    private static final long TARGET_BYTES = envLong("AE2E_BENCH_TARGET_BYTES", 100_000_000_000_000L);
    private static final long NATIVE_TIMEOUT_MIN = envLong("AE2E_BENCH_NATIVE_TIMEOUT_MIN", 10);
    private static final int LAYERS = (int) envLong("AE2E_BENCH_LAYERS", 34);
    private static final int WIDTH = (int) envLong("AE2E_BENCH_WIDTH", 60);

    private static final int LAYER_NODES = LAYERS * WIDTH;
    private static final int ROOT_ID = LAYER_NODES;
    /** 原料库存:远超任何需求,保证计划可成功(缺料不是本基准的测量目标). */
    private static final long RAW_STOCK = 200_000_000_000_000_000L;
    private static final long PROBE_AMOUNT = 1_000_000L;

    private static long envLong(String name, long fallback) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            return fallback;
        }
        return Long.parseLong(v.trim());
    }

    /** 以木棍 + 损伤值构造任意数量的不同物品 key. */
    private static IAEItemStack key(int id) {
        return AEItemStack.fromItemStack(new ItemStack(Items.STICK, 1, id));
    }

    private static IAEItemStack mult(IAEItemStack template, long multiplier) {
        IAEItemStack copy = template.copy();
        copy.setStackSize(template.getStackSize() * multiplier);
        return copy;
    }

    /** 镜像原生 dive 记账的隐含字节:8×Σ样板调用 + Σ网络实取(long 安全,不走 int 字段). */
    private static long impliedBytes(PlanView plan) {
        return 8L * totalCrafts(plan) + totalUsed(plan);
    }

    private static long totalCrafts(PlanView plan) {
        long sum = 0;
        for (long v : plan.patternTimes().values()) {
            sum += v;
        }
        return sum;
    }

    private static long totalUsed(PlanView plan) {
        long sum = 0;
        for (long v : plan.usedItems().values()) {
            sum += v;
        }
        return sum;
    }

    // ===== 简易采样分析器(定位极端规模下的热点,仅基准用) =====

    private static final Map<String, Integer> LEAF_SAMPLES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> STACK_SAMPLES = new ConcurrentHashMap<>();
    private static volatile boolean sampling = false;

    /** 以 50ms 周期采样 job 线程栈:记录首个应用帧(热点叶)与关键方法命中. */
    private static void startSampler() {
        LEAF_SAMPLES.clear();
        STACK_SAMPLES.clear();
        sampling = true;
        Thread sampler = new Thread(() -> {
            while (sampling) {
                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                    if (!"ae2e-timed-job".equals(entry.getKey().getName())) {
                        continue;
                    }
                    for (StackTraceElement el : entry.getValue()) {
                        String cn = el.getClassName();
                        if (cn.startsWith("com.github.") || cn.startsWith("appeng.")) {
                            LEAF_SAMPLES.merge(cn + "." + el.getMethodName(), 1, Integer::sum);
                            break;
                        }
                    }
                    for (StackTraceElement el : entry.getValue()) {
                        String cn = el.getClassName();
                        if (cn.contains("CycleAnalyzer") || cn.contains("DagCompiler")
                                || cn.contains("CycleBoundarySolver") || cn.contains("DagCraftingJob")
                                || cn.contains("CraftingTreeNode") || cn.contains("CraftingTreeProcess")) {
                            STACK_SAMPLES.merge(cn.substring(cn.lastIndexOf('.') + 1) + "."
                                    + el.getMethodName(), 1, Integer::sum);
                        }
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
        });
        sampler.setDaemon(true);
        sampler.start();
    }

    private static void stopSamplerAndReport(String label) {
        sampling = false;
        System.out.printf("[BENCH] 采样热点(%s):%n", label);
        LEAF_SAMPLES.entrySet().stream()
                .sorted(Map.Entry.<String, Integer> comparingByValue().reversed())
                .limit(8)
                .forEach(e -> System.out.printf("[BENCH]   叶帧 %6d 次  %s%n", e.getValue(), e.getKey()));
        STACK_SAMPLES.entrySet().stream()
                .sorted(Map.Entry.<String, Integer> comparingByValue().reversed())
                .limit(8)
                .forEach(e -> System.out.printf("[BENCH]   栈含 %6d 次  %s%n", e.getValue(), e.getKey()));
    }

    private static void printPlan(String label, long nanos, PlanView plan) {
        long crafts = totalCrafts(plan);
        System.out.printf(
                "[BENCH] %-8s 耗时 %,10.1f ms | 计划成功=%s 特殊标记=%s | 样板调用=%,d 隐含字节=%,d (~%.1fT) getByteTotal=%,d%n",
                label, nanos / 1e6, !plan.simulation(), SpecialPlanMarker.isSpecial(plan.job()), crafts,
                impliedBytes(plan), impliedBytes(plan) / 1e12, plan.bytes());
    }

    // ==================== 混合交叉图基准 ====================

    @Test
    public void mixedCrossGraphBenchmark() {
        Assumptions.assumeTrue(ENABLED, "set AE2E_BENCH=1 to enable");

        SimulationEnv env = buildMixedEnv();
        IAEItemStack root = key(ROOT_ID);
        System.out.printf("[BENCH] 图规模: %d 层 × %d 宽 = %d 节点 + 根, 目标字节 ~%,d (%.0fT)%n", LAYERS, WIDTH,
                LAYER_NODES, TARGET_BYTES, TARGET_BYTES / 1e12);

        // 1) 探测运行:校准字节口径 + JIT 预热 DAG 路径
        long t0 = System.nanoTime();
        CraftingJob probeJob = env.runJobTimed(env.newDagJob(mult(root, PROBE_AMOUNT)), 10, TimeUnit.MINUTES);
        long probeNanos = System.nanoTime() - t0;
        assertThat(probeJob).as("DAG 探测运行超时").isNotNull();
        PlanView probePlan = PlanView.of(probeJob);
        assertThat(probePlan.simulation()).as("DAG 探测计划须成功(循环边界可解)").isFalse();
        long probeBytes = impliedBytes(probePlan);
        assertThat(probeBytes).as("探测计划隐含字节").isGreaterThan(0);
        System.out.printf("[BENCH] 探测: 请求=%,d → 隐含字节=%,d, 耗时 %,.1f ms%n", PROBE_AMOUNT, probeBytes,
                probeNanos / 1e6);

        // 2) 按 AE 字节口径线性外推请求量到 ~TARGET_BYTES
        long finalAmount = Math.max(1L, (long) (PROBE_AMOUNT * (TARGET_BYTES / (double) probeBytes)));
        System.out.printf("[BENCH] 校准后请求量=%,d%n", finalAmount);

        // 3) DAG 正式计时(挂采样器定位热点)
        startSampler();
        t0 = System.nanoTime();
        CraftingJob dagJob = env.runJobTimed(env.newDagJob(mult(root, finalAmount)), 10, TimeUnit.MINUTES);
        long dagNanos = System.nanoTime() - t0;
        stopSamplerAndReport("DAG");
        assertThat(dagJob).as("DAG 正式运行超时").isNotNull();
        PlanView dagPlan = PlanView.of(dagJob);
        assertThat(dagPlan.simulation()).as("DAG 计划须成功(循环边界可解)").isFalse();
        assertThat(SpecialPlanMarker.isSpecial(dagPlan.job())).as("含循环边界须标记特殊(证明未回落原生)").isTrue();
        printPlan("DAG", dagNanos, dagPlan);

        // 4) 原生基线(放最后:超时/失败不影响已得结果;守护线程兜底 JVM 退出)
        startSampler();
        t0 = System.nanoTime();
        CraftingJob nativeJob = null;
        try {
            nativeJob = env.runJobTimed(env.newNativeJob(mult(root, finalAmount)), NATIVE_TIMEOUT_MIN,
                    TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            System.out.printf("[BENCH] 原生基线异常: %s%n", e.getCause() == null ? e : e.getCause());
        }
        long nativeNanos = System.nanoTime() - t0;
        stopSamplerAndReport("原生");
        if (nativeJob == null) {
            System.out.printf("[BENCH] 原生基线: 超过 %d 分钟仍未完成,已中断%n", NATIVE_TIMEOUT_MIN);
        } else {
            printPlan("原生", nativeNanos, PlanView.of(nativeJob));
        }

        System.out.printf("[BENCH] 汇总: DAG=%,.1f ms vs 原生=%s%n", dagNanos / 1e6,
                nativeJob == null ? ">" + NATIVE_TIMEOUT_MIN + " min(未完成)"
                        : String.format("%,.1f ms(倍率 %.0f×)", nativeNanos / 1e6,
                                Math.max(1.0, nativeNanos / (double) Math.max(1, dagNanos))));
    }

    /**
     * 生成分层交叉图:层 0 为原料(足量库存),层 1..L-1 普通合成为主,
     * 指定节点替换为自增殖子环/催化 θ 环,根汇聚末层三个代表节点.
     */
    private static SimulationEnv buildMixedEnv() {
        SimulationEnv env = new SimulationEnv();
        Random rng = new Random(42);

        // 环节点选自中间层(8..L-3),自增殖 ~2%、θ 环 ~1%,互不重叠
        List<Integer> candidates = new ArrayList<>();
        for (int l = 8; l < LAYERS - 2; l++) {
            for (int i = 0; i < WIDTH; i++) {
                candidates.add(l * WIDTH + i);
            }
        }
        Collections.shuffle(candidates, rng);
        int selfDupCount = Math.max(1, LAYER_NODES / 50);
        int thetaCount = Math.max(1, LAYER_NODES / 100);
        Set<Integer> dupIds = new HashSet<>(candidates.subList(0, selfDupCount));
        Set<Integer> thetaIds = new HashSet<>(candidates.subList(selfDupCount, selfDupCount + thetaCount));

        // 层 0:原料
        for (int i = 0; i < WIDTH; i++) {
            env.addStoredItem(mult(key(i), RAW_STOCK));
        }

        int nextExtraId = ROOT_ID + 1;
        int patterns = 0;
        for (int l = 1; l < LAYERS; l++) {
            for (int i = 0; i < WIDTH; i++) {
                int id = l * WIDTH + i;
                IAEItemStack out = key(id);
                if (dupIds.contains(id)) {
                    // 自增殖子环:1X → 2X,种子 1(DAG 循环边界可解;原生不可规划)
                    env.addPattern(new ProcessingPatternBuilder(mult(out, 2)).addPreciseInput(1, out).build());
                    env.addStoredItem(key(id));
                } else if (thetaIds.contains(id)) {
                    // 催化 θ 环:C→X, C→Y, X+Y→4C(与 DagCycleBoundaryTest Z2 同构)
                    IAEItemStack c = key(id);
                    IAEItemStack x = key(nextExtraId++);
                    IAEItemStack y = key(nextExtraId++);
                    env.addPattern(new ProcessingPatternBuilder(x).addPreciseInput(1, c).build());
                    env.addPattern(new ProcessingPatternBuilder(y).addPreciseInput(1, c).build());
                    env.addPattern(new ProcessingPatternBuilder(mult(c, 4))
                            .addPreciseInput(1, x)
                            .addPreciseInput(1, y)
                            .build());
                    env.addStoredItem(mult(c, 8)); // 种子
                    env.addStoredItem(y);
                    patterns += 2;
                } else {
                    // 普通合成:扇入 1~3,含跨层交叉边(共享中间物);20% 批量产出 ×4
                    ProcessingPatternBuilder b = new ProcessingPatternBuilder(
                            rng.nextInt(5) == 0 ? mult(out, 4) : out);
                    int kind = rng.nextInt(100);
                    if (kind < 55) {
                        b.addPreciseInput(1, key((l - 1) * WIDTH + rng.nextInt(WIDTH)));
                    } else if (kind < 85) {
                        b.addPreciseInput(1, key((l - 1) * WIDTH + rng.nextInt(WIDTH)));
                        b.addPreciseInput(1, key((l - 1) * WIDTH + rng.nextInt(WIDTH)));
                    } else if (kind < 95 && l >= 2) {
                        // 跨层交叉边:跳过一层取料,制造共享中间物
                        b.addPreciseInput(1, key((l - 1) * WIDTH + rng.nextInt(WIDTH)));
                        b.addPreciseInput(1, key((l - 2) * WIDTH + rng.nextInt(WIDTH)));
                    } else {
                        b.addPreciseInput(1, key((l - 1) * WIDTH + rng.nextInt(WIDTH)));
                        b.addPreciseInput(1, key((l - 1) * WIDTH + rng.nextInt(WIDTH)));
                        b.addPreciseInput(1, key((l - 1) * WIDTH + rng.nextInt(WIDTH)));
                    }
                    env.addPattern(b.build());
                }
                patterns++;
            }
        }

        // 根:汇聚末层三个代表节点
        env.addPattern(new ProcessingPatternBuilder(key(ROOT_ID))
                .addPreciseInput(1, key((LAYERS - 1) * WIDTH))
                .addPreciseInput(1, key((LAYERS - 1) * WIDTH + WIDTH / 2))
                .addPreciseInput(1, key((LAYERS - 1) * WIDTH + WIDTH - 1))
                .build());
        patterns++;

        System.out.printf("[BENCH] 样板总数=%,d (普通 %,d, 自增殖环 %,d, θ 环 %,d)%n", patterns + thetaCount * 2,
                patterns - selfDupCount - thetaCount * 3, selfDupCount, thetaCount);
        return env;
    }

    /**
     * 极端请求量场景:同一张大图直接请求 {@link Long#MAX_VALUE}——需求沿边饱和传播,
     * 循环边界触及数值不可表示区.修复前:ceilDiv 加法回绕/贷款欠资 → 整单回落原生
     * (>10 min 卡死);修复后:O(1) 缺料记账,秒级出缺料计划.
     */
    @Test
    public void mixedCrossGraphExtremeDemandBenchmark() {
        Assumptions.assumeTrue(ENABLED, "set AE2E_BENCH=1 to enable");

        SimulationEnv env = buildMixedEnv();
        IAEItemStack root = key(ROOT_ID);
        IAEItemStack request = mult(root, Long.MAX_VALUE);

        long t0 = System.nanoTime();
        CraftingJob dagJob = env.runJobTimed(env.newDagJob(request), 10, TimeUnit.MINUTES);
        long dagNanos = System.nanoTime() - t0;
        assertThat(dagJob).as("极端请求下 DAG 须在预算内完成(修复前回落原生>10min)").isNotNull();
        PlanView dagPlan = PlanView.of(dagJob);
        assertThat(dagPlan.simulation()).as("天文数字需求应产出缺料计划").isTrue();
        long craftsSaturated = 0;
        for (long v : dagPlan.patternTimes().values()) {
            craftsSaturated = Long.MAX_VALUE - craftsSaturated < v ? Long.MAX_VALUE : craftsSaturated + v;
        }
        System.out.printf(
                "[BENCH] 极端请求(Long.MAX): DAG 耗时 %,.1f ms | 缺料计划(符合预期)| 样板调用(饱和合计)=%,d 缺料种类=%,d%n",
                dagNanos / 1e6, craftsSaturated, dagPlan.missingItems().size());
    }

    // ==================== 根级催化环:SpecialCraftingJob 路径基准 ====================

    /**
     * 根级催化 θ 环(与 ComplexScenarioTest H13 同构)+ 辅材深链子合成.
     * <p>预期:SpecialCraftingJob 闭式求解成功;DAG 边界 key == 请求 key 不可解,
     * 整单回落原生;原生对环不可规划(失败计划).这正是特殊路由存在的意义.</p>
     */
    @Test
    public void catalyticRootSpecialBenchmark() {
        Assumptions.assumeTrue(ENABLED, "set AE2E_BENCH=1 to enable");

        SimulationEnv env = new SimulationEnv();
        IAEItemStack a = key(0);
        IAEItemStack b = key(1);
        IAEItemStack c = key(2);
        IAEItemStack d = key(3);
        IAEItemStack w = key(4);
        // 四键催化环:A+W→2B, B→C, 2C→3D, 2D→2A(t=[2,4,2,3],净产 4A/轮)
        env.addPattern(new ProcessingPatternBuilder(mult(b, 2)).addPreciseInput(1, a).addPreciseInput(1, w).build());
        env.addPattern(new ProcessingPatternBuilder(c).addPreciseInput(1, b).build());
        env.addPattern(new ProcessingPatternBuilder(mult(d, 3)).addPreciseInput(2, c).build());
        env.addPattern(new ProcessingPatternBuilder(mult(a, 2)).addPreciseInput(2, d).build());
        // 辅材 W 的深链子合成:2W←F0, F0←F1←...←F14(原料)
        env.addPattern(new ProcessingPatternBuilder(mult(w, 2)).addPreciseInput(1, key(10)).build());
        for (int i = 0; i < 14; i++) {
            env.addPattern(new ProcessingPatternBuilder(key(10 + i)).addPreciseInput(1, key(11 + i)).build());
        }
        env.addStoredItem(mult(key(24), RAW_STOCK)); // 链底原料
        env.addStoredItem(mult(a, 2)); // 前缀种子(t0=2)
        env.addStoredItem(w); // W 种子 1

        IAEItemStack request = mult(a, 10_000_000_000_000L);
        System.out.printf("[BENCH] 根级催化环: 请求=%,d × A%n", request.getStackSize());

        long t0 = System.nanoTime();
        CraftingJob specialJob = env.runJobTimed(env.newSpecialJob(request), 10, TimeUnit.MINUTES);
        long specialNanos = System.nanoTime() - t0;
        assertThat(specialJob).as("SpecialCraftingJob 超时").isNotNull();
        PlanView specialPlan = PlanView.of(specialJob);
        assertThat(specialPlan.simulation()).as("特殊路径须成功").isFalse();
        printPlan("特殊", specialNanos, specialPlan);

        t0 = System.nanoTime();
        CraftingJob dagJob = env.runJobTimed(env.newDagJob(request), 10, TimeUnit.MINUTES);
        long dagNanos = System.nanoTime() - t0;
        if (dagJob == null) {
            System.out.println("[BENCH] DAG(根级环): 超时");
        } else {
            printPlan("DAG(根)", dagNanos, PlanView.of(dagJob));
        }

        t0 = System.nanoTime();
        CraftingJob nativeJob = env.runJobTimed(env.newNativeJob(request), NATIVE_TIMEOUT_MIN, TimeUnit.MINUTES);
        long nativeNanos = System.nanoTime() - t0;
        if (nativeJob == null) {
            System.out.printf("[BENCH] 原生(根级环): 超过 %d 分钟仍未完成%n", NATIVE_TIMEOUT_MIN);
        } else {
            printPlan("原生(根)", nativeNanos, PlanView.of(nativeJob));
        }
    }
}
