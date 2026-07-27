package com.github.aeddddd.ae2enhanced.centralinterface;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * {@link HandlerCapabilities} 工厂方法与 {@link IRemoteHandler#hasCapability} 默认委托语义测试。
 *
 * <p>覆盖 physicalOnly()/virtualOnly()/all() 的集合内容、互斥性、
 * 每次调用返回独立实例（调用方修改不污染后续调用），
 * 以及 hasCapability 默认方法对 getCapabilities() 的委托。</p>
 */
public class HandlerCapabilitiesTest {

    /** physicalOnly 只含 PHYSICAL，不含 VIRTUAL_BATCH。 */
    @Test
    public void testPhysicalOnlyContents() {
        EnumSet<HandlerCapabilities> caps = HandlerCapabilities.physicalOnly();

        assertThat(caps).containsExactly(HandlerCapabilities.PHYSICAL);
        assertThat(caps).doesNotContain(HandlerCapabilities.VIRTUAL_BATCH);
    }

    /** virtualOnly 只含 VIRTUAL_BATCH，不含 PHYSICAL。 */
    @Test
    public void testVirtualOnlyContents() {
        EnumSet<HandlerCapabilities> caps = HandlerCapabilities.virtualOnly();

        assertThat(caps).containsExactly(HandlerCapabilities.VIRTUAL_BATCH);
        assertThat(caps).doesNotContain(HandlerCapabilities.PHYSICAL);
    }

    /** all 同时包含两种能力。 */
    @Test
    public void testAllContents() {
        EnumSet<HandlerCapabilities> caps = HandlerCapabilities.all();

        assertThat(caps).containsExactly(HandlerCapabilities.PHYSICAL, HandlerCapabilities.VIRTUAL_BATCH);
    }

    /** physicalOnly 与 virtualOnly 互斥，all 为两者并集。 */
    @Test
    public void testMutualExclusivityAndUnion() {
        EnumSet<HandlerCapabilities> physical = HandlerCapabilities.physicalOnly();
        EnumSet<HandlerCapabilities> virtual = HandlerCapabilities.virtualOnly();

        EnumSet<HandlerCapabilities> intersection = physical.clone();
        intersection.retainAll(virtual);
        assertThat(intersection).isEmpty();

        EnumSet<HandlerCapabilities> union = physical.clone();
        union.addAll(virtual);
        assertThat(union).isEqualTo(HandlerCapabilities.all());
    }

    /** 工厂每次返回独立实例：修改返回值不影响后续调用结果。 */
    @Test
    public void testFactoriesReturnFreshInstances() {
        EnumSet<HandlerCapabilities> first = HandlerCapabilities.physicalOnly();
        first.add(HandlerCapabilities.VIRTUAL_BATCH);

        assertThat(HandlerCapabilities.physicalOnly())
                .containsExactly(HandlerCapabilities.PHYSICAL);

        EnumSet<HandlerCapabilities> allA = HandlerCapabilities.all();
        EnumSet<HandlerCapabilities> allB = HandlerCapabilities.all();
        assertThat(allA).isNotSameAs(allB);
        allA.clear();
        assertThat(allB).hasSize(2);
    }

    /** hasCapability 默认方法委托 getCapabilities()：physicalOnly 只认 PHYSICAL。 */
    @Test
    public void testHasCapabilityDelegatesToGetCapabilities() {
        IRemoteHandler physical = new StubHandler(HandlerCapabilities.physicalOnly());
        assertThat(physical.hasCapability(HandlerCapabilities.PHYSICAL)).isTrue();
        assertThat(physical.hasCapability(HandlerCapabilities.VIRTUAL_BATCH)).isFalse();

        IRemoteHandler virtual = new StubHandler(HandlerCapabilities.virtualOnly());
        assertThat(virtual.hasCapability(HandlerCapabilities.VIRTUAL_BATCH)).isTrue();
        assertThat(virtual.hasCapability(HandlerCapabilities.PHYSICAL)).isFalse();

        IRemoteHandler all = new StubHandler(HandlerCapabilities.all());
        assertThat(all.hasCapability(HandlerCapabilities.PHYSICAL)).isTrue();
        assertThat(all.hasCapability(HandlerCapabilities.VIRTUAL_BATCH)).isTrue();
    }

    /** 仅实现抽象方法的最小桩，getCapabilities 返回构造时指定的能力集。 */
    private static final class StubHandler implements IRemoteHandler {
        private final EnumSet<HandlerCapabilities> caps;

        private StubHandler(EnumSet<HandlerCapabilities> caps) {
            this.caps = caps;
        }

        @Override
        public EnumSet<HandlerCapabilities> getCapabilities() {
            return caps;
        }

        @Override
        public boolean canHandle(String blockId) {
            return false;
        }

        @Override
        public boolean isValidTarget(World world, BlockPos pos) {
            return false;
        }

        @Override
        public boolean canStart(World world, BlockPos pos, InventoryCrafting ingredients, TargetSession session) {
            return false;
        }

        @Override
        public boolean pushMaterials(World world, BlockPos pos, InventoryCrafting ingredients,
                IActionSource source, TargetSession session) {
            return false;
        }

        @Override
        public boolean startProcess(World world, BlockPos pos, IActionSource source, TargetSession session) {
            return false;
        }

        @Override
        public List<ItemStack> collectProducts(World world, BlockPos pos, IAEItemStack[] expectedOutputs,
                List<ItemStack> inputs, IActionSource source, TargetSession session) {
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean isIdle(World world, BlockPos pos, List<ItemStack> inputs, TargetSession session) {
            return false;
        }
    }
}
