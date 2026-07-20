package com.github.aeddddd.ae2enhanced.platform.energy;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * 平台能量代理的多模组适配器接口.
 *
 * <p>用于突破标准 Forge {@link IEnergyStorage} 的单次/tick 级能量注入限制.
 * 各模组适配器通过反射获取机器内部能量对象,直接操作以最大化注入效率.</p>
 *
 * <p>安全约定：</p>
 * <ul>
 *   <li>反射异常时必须静默回退到标准 {@link IEnergyStorage}</li>
 *   <li>{@code simulate=true} 时不得修改机器状态</li>
 *   <li>适配器类不得出现在无条件加载路径的常量池中</li>
 * </ul>
 */
public interface IEnergyAdapter {

    /**
     * 按方块注册 ID 字符串匹配此适配器是否支持该类型.
     *
     * @param blockId 方块注册 ID(如 "enderio:block_alloy_smelter")
     * @return true 表示此适配器可以处理该方块类型
     */
    boolean canHandle(String blockId);

    /**
     * 查询目标机器当前可接收的能量总量(模拟模式).
     *
     * @param tile 目标 TileEntity(可能为 null 或已失效)
     * @param cap  标准 Forge IEnergyStorage(可能为 null)
     * @return 当前可接收的能量量；若无法注入则返回 0
     */
    long getReceiveableEnergy(TileEntity tile, IEnergyStorage cap);

    /**
     * 向目标机器注入能量.
     *
     * @param tile     目标 TileEntity(可能为 null 或已失效)
     * @param cap      标准 Forge IEnergyStorage(可能为 null)
     * @param amount   要注入的能量量(已保证 &gt; 0)
     * @param simulate true=仅模拟,false=实际注入
     * @return 实际成功注入(或模拟可注入)的能量量
     */
    long injectEnergy(TileEntity tile, IEnergyStorage cap, long amount, boolean simulate);

    /**
     * 判断目标是否可作为能源存储总线的暴露对象.
     * 默认要求存在 Forge IEnergyStorage capability;各适配器可扩展为原生支持
     * (如龙之研究的 IExtendedRFStorage 不一定暴露 FE capability).
     *
     * @param tile 目标 TileEntity
     * @param cap  标准 Forge IEnergyStorage(可能为 null)
     * @return true 表示该目标可以被能源存储总线挂载
     */
    default boolean canHandleTile(TileEntity tile, IEnergyStorage cap) {
        return cap != null;
    }

    /**
     * 查询目标当前存储的能量(long 级).
     * 默认通过 {@link IEnergyStorage#getEnergyStored()}(int 级).
     */
    default long getStoredEnergy(TileEntity tile, IEnergyStorage cap) {
        return cap != null ? cap.getEnergyStored() : 0;
    }

    /**
     * 查询目标的能量容量(long 级).
     * 默认通过 {@link IEnergyStorage#getMaxEnergyStored()}(int 级).
     */
    default long getCapacityEnergy(TileEntity tile, IEnergyStorage cap) {
        return cap != null ? cap.getMaxEnergyStored() : 0;
    }

    /**
     * 查询目标当前可提取的能量总量(模拟模式).
     * 默认通过 {@link IEnergyStorage#extractEnergy(int, boolean)} 模拟.
     */
    default long getExtractableEnergy(TileEntity tile, IEnergyStorage cap) {
        if (cap == null || !cap.canExtract()) {
            return 0;
        }
        return cap.extractEnergy(Integer.MAX_VALUE, true);
    }

    /**
     * 从目标提取能量.
     * 默认使用标准 FE 多调用策略突破单次 int 上限.
     *
     * @param tile     目标 TileEntity
     * @param cap      标准 Forge IEnergyStorage(可能为 null)
     * @param amount   要提取的能量量(已保证 &gt; 0)
     * @param simulate true=仅模拟,false=实际提取
     * @return 实际成功提取(或模拟可提取)的能量量
     */
    default long extractEnergy(TileEntity tile, IEnergyStorage cap, long amount, boolean simulate) {
        if (cap == null || !cap.canExtract() || amount <= 0) {
            return 0;
        }
        if (simulate) {
            return cap.extractEnergy((int) Math.min(amount, Integer.MAX_VALUE), true);
        }
        long total = 0;
        while (amount > 0) {
            int toExtract = (int) Math.min(amount, Integer.MAX_VALUE);
            int extracted = cap.extractEnergy(toExtract, false);
            if (extracted <= 0) {
                break;
            }
            total += extracted;
            amount -= extracted;
        }
        return total;
    }
}
