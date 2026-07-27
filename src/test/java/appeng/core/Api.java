package appeng.core;

import appeng.api.IAppEngApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.features.IRegistryContainer;
import appeng.api.networking.IGridHelper;
import appeng.api.parts.IPartHelper;
import appeng.api.storage.IStorageHelper;
import appeng.core.api.ApiStorage;

/**
 * 测试桩:遮蔽真实的 appeng.core.Api(测试 classpath 优先于依赖 jar).
 * <p>AEApi 静态初始化经反射读取本类的 INSTANCE 字段.真实 Api 的构造链会拉起
 * ApiDefinitions(客户端渲染/注册表泥潭),无头测试环境不可用;合成计算层
 * (CraftingJob/MECraftingInventory/CraftingTreeNode)只依赖
 * {@link AEApi#instance()}.storage() 的物品列表创建,故桩仅实现 storage().</p>
 */
public class Api implements IAppEngApi {

    public static final IAppEngApi INSTANCE = new Api();

    private final ApiStorage storageHelper = new ApiStorage();

    private Api() {
    }

    @Override
    public IStorageHelper storage() {
        return this.storageHelper;
    }

    @Override
    public IRegistryContainer registries() {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public IGridHelper grid() {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public IPartHelper partHelper() {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public IDefinitions definitions() {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public appeng.api.util.IClientHelper client() {
        throw new UnsupportedOperationException("test stub");
    }
}
