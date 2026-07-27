package com.github.aeddddd.ae2enhanced.centralinterface;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.me.GridAccessException;

/**
 * 网格连接 seam：中枢接口核心逻辑对 AE2 网格的最小依赖面。
 *
 * <p>实现由 {@link NetworkAccess#connection} 提供（包装
 * {@code appeng.me.helpers.AENetworkProxy}）。核心逻辑
 * （Duality / Dispatcher / Engine）只面向本接口编程，
 * 不再直接持有 AENetworkProxy，从而在签名层面与 appeng.me 内部实现解耦。</p>
 */
public interface IGridConnection {

    /**
     * 网格是否已激活（有频道且在线）。
     */
    boolean isActive();

    /**
     * 网格是否就绪（可接收事件）。
     */
    boolean isReady();

    /**
     * 获取网络存储。
     */
    IStorageGrid storage() throws GridAccessException;

    /**
     * 获取网络能量。
     */
    IEnergySource energy() throws GridAccessException;

    /**
     * 网络就绪时向网格发送样板变更事件；未就绪或网络断开时静默忽略。
     */
    void postPatternChange(ICentralInterfaceHost host);

    /**
     * 唤醒宿主的 tick 设备；网络断开时记录警告。
     */
    void wakeTickDevice(ICentralInterfaceHost host);
}
