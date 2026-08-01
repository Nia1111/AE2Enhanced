package com.github.aeddddd.ae2enhanced.mixin.bridge;

import java.util.Set;

import appeng.api.storage.data.IAEItemStack;

/**
 * CraftingGridCache 样板索引访问接口（循环分析副产物边用）.
 * <p>1.12.2 的 {@code getCraftingFor} 只按主产出索引样板;发现"经副产物闭合的环"
 * （催化环,如 1A→1X+1B、1B→1A）需要全样板键集做生产者扫描.</p>
 */
public interface ICraftingGridCacheAccess {

    /** 网络当前所有可合成键（craftableItems 索引键,只读快照）. */
    Set<IAEItemStack> ae2enhanced$craftableKeys();
}
