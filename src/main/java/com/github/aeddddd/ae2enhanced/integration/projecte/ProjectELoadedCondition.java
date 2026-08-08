package com.github.aeddddd.ae2enhanced.integration.projecte;

import com.github.aeddddd.ae2enhanced.config.AE2EnhancedConfig;
import com.google.gson.JsonObject;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.Loader;

import java.util.function.BooleanSupplier;

/**
 * 配方条件：仅在 ProjectE 已安装且配置启用 EMC 接口时启用配方.
 *
 * <p>与 GameRegistryManager 中方块注册条件保持一致.
 * 不引用任何 ProjectE 类，可安全无条件加载.</p>
 */
public class ProjectELoadedCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(JsonContext context, JsonObject json) {
        return () -> AE2EnhancedConfig.emcInterface.enabled && Loader.isModLoaded("projecte");
    }
}
