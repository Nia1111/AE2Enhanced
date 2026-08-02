package com.github.aeddddd.ae2enhanced.crafting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

/**
 * {@link BatchManager} 批次拆分逻辑测试。
 *
 * <p>纯 BigInteger 数学逻辑，零 MC 依赖，无需无头引导。
 * 覆盖 splitToLongBatches / splitToParallelBatches 的防御分支、
 * 整除/余数路径、总和守恒性质以及批次数上限异常。</p>
 */
public class BatchManagerTest {

    private static final BigInteger BIG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    /** 以 BigInteger 累加批次，避免 long 溢出。 */
    private static BigInteger sum(long[] batches) {
        BigInteger total = BigInteger.ZERO;
        for (long b : batches) {
            total = total.add(BigInteger.valueOf(b));
        }
        return total;
    }

    // ------------------------------------------------------------------
    // splitToLongBatches：防御分支
    // ------------------------------------------------------------------

    /** null / 零 / 负数输入均返回空数组。 */
    @Test
    public void testSplitToLongBatchesNullOrNonPositive() {
        assertThat(BatchManager.splitToLongBatches(null)).isEmpty();
        assertThat(BatchManager.splitToLongBatches(BigInteger.ZERO)).isEmpty();
        assertThat(BatchManager.splitToLongBatches(BigInteger.valueOf(-1))).isEmpty();
        assertThat(BatchManager.splitToLongBatches(BigInteger.valueOf(-100))).isEmpty();
    }

    // ------------------------------------------------------------------
    // splitToLongBatches：单批路径（≤ Long.MAX_VALUE）
    // ------------------------------------------------------------------

    /** 最小正数 BigInteger.ONE 返回单批 [1]。 */
    @Test
    public void testSplitToLongBatchesOne() {
        assertThat(BatchManager.splitToLongBatches(BigInteger.ONE))
                .containsExactly(1L);
    }

    /** 恰好等于 Long.MAX_VALUE 时仍走单批路径。 */
    @Test
    public void testSplitToLongBatchesExactlyLongMax() {
        assertThat(BatchManager.splitToLongBatches(BIG_MAX))
                .containsExactly(Long.MAX_VALUE);
    }

    /** Long.MAX_VALUE 之下的普通值单批返回。 */
    @Test
    public void testSplitToLongBatchesBelowLongMax() {
        assertThat(BatchManager.splitToLongBatches(BigInteger.valueOf(12345L)))
                .containsExactly(12345L);
    }

    // ------------------------------------------------------------------
    // splitToLongBatches：多批拆分
    // ------------------------------------------------------------------

    /** Long.MAX_VALUE + 1 拆为 [MAX, 1]：满批 + 余数尾批。 */
    @Test
    public void testSplitToLongBatchesLongMaxPlusOne() {
        assertThat(BatchManager.splitToLongBatches(BIG_MAX.add(BigInteger.ONE)))
                .containsExactly(Long.MAX_VALUE, 1L);
    }

    /** 2 倍 Long.MAX_VALUE 整除无余数，不产生尾批。 */
    @Test
    public void testSplitToLongBatchesExactMultipleNoRemainder() {
        assertThat(BatchManager.splitToLongBatches(BIG_MAX.multiply(BigInteger.valueOf(2))))
                .containsExactly(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /** 2 倍 Long.MAX_VALUE + 5 拆为两个满批加余数尾批 5。 */
    @Test
    public void testSplitToLongBatchesMultipleWithRemainder() {
        BigInteger amount = BIG_MAX.multiply(BigInteger.valueOf(2)).add(BigInteger.valueOf(5));
        assertThat(BatchManager.splitToLongBatches(amount))
                .containsExactly(Long.MAX_VALUE, Long.MAX_VALUE, 5L);
    }

    /** 性质断言：任意大输入拆分后总和必须等于原值，且除尾批外均为满批。 */
    @Test
    public void testSplitToLongBatchesSumInvariant() {
        BigInteger[] samples = {
                BIG_MAX.multiply(BigInteger.valueOf(1000)).add(BigInteger.valueOf(12345)),
                BIG_MAX.multiply(BigInteger.valueOf(7)),
                BIG_MAX.multiply(BigInteger.valueOf(100000)).add(BigInteger.ONE) // 10 万个满批 + 余数 1
        };
        for (BigInteger amount : samples) {
            long[] batches = BatchManager.splitToLongBatches(amount);

            // 总和守恒
            assertThat(sum(batches)).isEqualTo(amount);
            // 除最后一个批次外必须全部为 Long.MAX_VALUE
            for (int i = 0; i < batches.length - 1; i++) {
                assertThat(batches[i]).isEqualTo(Long.MAX_VALUE);
            }
            // 尾批不超过 Long.MAX_VALUE
            assertThat(batches[batches.length - 1]).isLessThanOrEqualTo(Long.MAX_VALUE);
        }
    }

    // ------------------------------------------------------------------
    // splitToLongBatches：批次数上限异常
    // ------------------------------------------------------------------

    /** 满批数恰为 Integer.MAX_VALUE 且有余数时，超出数组上限，抛 IllegalArgumentException。 */
    @Test
    public void testSplitToLongBatchesThrowsWhenBatchCountExceedsLimitWithRemainder() {
        BigInteger amount = BIG_MAX.multiply(BigInteger.valueOf(Integer.MAX_VALUE))
                .add(BigInteger.ONE); // 满批 2^31-1 个 + 余数 1

        assertThatThrownBy(() -> BatchManager.splitToLongBatches(amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch count exceeds maximum array size");
    }

    /** 满批数超过 Integer.MAX_VALUE（无余数）时同样抛 IllegalArgumentException。 */
    @Test
    public void testSplitToLongBatchesThrowsWhenFullBatchesExceedIntMax() {
        BigInteger amount = BIG_MAX.multiply(BigInteger.ONE.shiftLeft(31)); // 满批 2^31 个，余数 0

        assertThatThrownBy(() -> BatchManager.splitToLongBatches(amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch count exceeds maximum array size");
    }

    /**
     * 满批数恰为 Integer.MAX_VALUE 且无余数时也抛 IllegalArgumentException。
     * JVM 数组实际最大长度为 Integer.MAX_VALUE - 2(部分 VM 为 -8)，
     * 此场景下尝试分配会 OOM，必须被上限校验拦截。
     */
    @Test
    public void testSplitToLongBatchesThrowsWhenFullBatchesAtIntMaxNoRemainder() {
        BigInteger amount = BIG_MAX.multiply(BigInteger.valueOf(Integer.MAX_VALUE)); // 满批 2^31-1 个，余数 0

        assertThatThrownBy(() -> BatchManager.splitToLongBatches(amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch count exceeds maximum array size");
    }

    /** 批次总数超过 Integer.MAX_VALUE - 8（满批 2^31-8 个 + 余数 1）时抛 IllegalArgumentException。 */
    @Test
    public void testSplitToLongBatchesThrowsWhenTotalBatchesAboveIntMaxMinus8() {
        BigInteger amount = BIG_MAX.multiply(BigInteger.valueOf(Integer.MAX_VALUE - 8L))
                .add(BigInteger.ONE); // 满批 2^31-8 个 + 余数尾批，总数 2^31-7

        assertThatThrownBy(() -> BatchManager.splitToLongBatches(amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch count exceeds maximum array size");
    }

    // ------------------------------------------------------------------
    // splitToParallelBatches：防御分支
    // ------------------------------------------------------------------

    /** null / 零 / 负数输入，或 parallelLimit <= 0 均返回空数组。 */
    @Test
    public void testSplitToParallelBatchesDefensiveBranches() {
        assertThat(BatchManager.splitToParallelBatches(null, 10)).isEmpty();
        assertThat(BatchManager.splitToParallelBatches(BigInteger.ZERO, 10)).isEmpty();
        assertThat(BatchManager.splitToParallelBatches(BigInteger.valueOf(-5), 10)).isEmpty();
        assertThat(BatchManager.splitToParallelBatches(BigInteger.TEN, 0)).isEmpty();
        assertThat(BatchManager.splitToParallelBatches(BigInteger.TEN, -1)).isEmpty();
    }

    // ------------------------------------------------------------------
    // splitToParallelBatches：单批路径（amount ≤ parallelLimit）
    // ------------------------------------------------------------------

    /** amount 小于 parallelLimit 时单批返回原值。 */
    @Test
    public void testSplitToParallelBatchesBelowLimit() {
        assertThat(BatchManager.splitToParallelBatches(BigInteger.valueOf(5), 10))
                .containsExactly(5L);
    }

    /** amount 恰好等于 parallelLimit 时仍走单批路径。 */
    @Test
    public void testSplitToParallelBatchesExactlyLimit() {
        assertThat(BatchManager.splitToParallelBatches(BigInteger.TEN, 10))
                .containsExactly(10L);
    }

    // ------------------------------------------------------------------
    // splitToParallelBatches：多批拆分
    // ------------------------------------------------------------------

    /** 10 / 3：三个满批 3 加余数尾批 1。 */
    @Test
    public void testSplitToParallelBatchesWithRemainder() {
        assertThat(BatchManager.splitToParallelBatches(BigInteger.TEN, 3))
                .containsExactly(3L, 3L, 3L, 1L);
    }

    /** 9 / 3：整除无余数，不产生尾批。 */
    @Test
    public void testSplitToParallelBatchesExactDivision() {
        assertThat(BatchManager.splitToParallelBatches(BigInteger.valueOf(9), 3))
                .containsExactly(3L, 3L, 3L);
    }

    /** parallelLimit = 1 时每个批次都是 1。 */
    @Test
    public void testSplitToParallelBatchesLimitOne() {
        assertThat(BatchManager.splitToParallelBatches(BigInteger.valueOf(5), 1))
                .containsExactly(1L, 1L, 1L, 1L, 1L);
    }

    /** 性质断言：BigInteger 大输入拆分后总和必须等于原值，且除尾批外均为 parallelLimit。 */
    @Test
    public void testSplitToParallelBatchesSumInvariant() {
        int limit = Integer.MAX_VALUE;
        // 1000 个满批 + 余数 777（注意批次数不得超过 Integer.MAX_VALUE，否则触发上限异常）
        BigInteger amount = BigInteger.valueOf(limit)
                .multiply(BigInteger.valueOf(1000))
                .add(BigInteger.valueOf(777));

        long[] batches = BatchManager.splitToParallelBatches(amount, limit);

        assertThat(sum(batches)).isEqualTo(amount);
        for (int i = 0; i < batches.length - 1; i++) {
            assertThat(batches[i]).isEqualTo(limit);
        }
        assertThat(batches[batches.length - 1]).isLessThanOrEqualTo(limit);
    }

    // ------------------------------------------------------------------
    // splitToParallelBatches：批次数上限异常
    // ------------------------------------------------------------------

    /** parallelLimit = 1 且 amount = 2^31 时满批数超出 Integer.MAX_VALUE，抛 IllegalArgumentException。 */
    @Test
    public void testSplitToParallelBatchesThrowsWhenBatchCountExceedsLimit() {
        BigInteger amount = BigInteger.ONE.shiftLeft(31); // 2^31 个批次，余数 0

        assertThatThrownBy(() -> BatchManager.splitToParallelBatches(amount, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch count exceeds maximum array size");
    }

    /** 满批数恰为 Integer.MAX_VALUE 且有余数时，同样触发上限异常。 */
    @Test
    public void testSplitToParallelBatchesThrowsWhenFullBatchesAtIntMaxWithRemainder() {
        // 满批 2^31-1 个 + 余数 1：amount = 2 * (2^31-1) + 1
        BigInteger amount = BigInteger.valueOf(2)
                .multiply(BigInteger.valueOf(Integer.MAX_VALUE))
                .add(BigInteger.ONE);

        assertThatThrownBy(() -> BatchManager.splitToParallelBatches(amount, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch count exceeds maximum array size");
    }

    /** 满批数恰为 Integer.MAX_VALUE 且无余数时，同样触发上限异常（漏网路径回归）。 */
    @Test
    public void testSplitToParallelBatchesThrowsWhenFullBatchesAtIntMaxNoRemainder() {
        // 满批 2^31-1 个，余数 0：amount = 2 * (2^31-1)
        BigInteger amount = BigInteger.valueOf(2)
                .multiply(BigInteger.valueOf(Integer.MAX_VALUE));

        assertThatThrownBy(() -> BatchManager.splitToParallelBatches(amount, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch count exceeds maximum array size");
    }

    /** 批次总数超过 Integer.MAX_VALUE - 8（满批 2^31-8 个 + 余数 1）时抛 IllegalArgumentException。 */
    @Test
    public void testSplitToParallelBatchesThrowsWhenTotalBatchesAboveIntMaxMinus8() {
        // 满批 2^31-8 个 + 余数尾批：amount = 2 * (2^31-8) + 1
        BigInteger amount = BigInteger.valueOf(2)
                .multiply(BigInteger.valueOf(Integer.MAX_VALUE - 8L))
                .add(BigInteger.ONE);

        assertThatThrownBy(() -> BatchManager.splitToParallelBatches(amount, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch count exceeds maximum array size");
    }
}
