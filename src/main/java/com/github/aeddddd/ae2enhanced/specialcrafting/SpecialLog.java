package com.github.aeddddd.ae2enhanced.specialcrafting;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.config.AE2EnhancedConfig;

/**
 * 特殊配方/DAG 计划引擎诊断日志门控:仅在配置 debug.debugMode = true 时输出.
 * 配置未加载(单元测试等环境)时按开启处理(测试可见断言日志).
 */
public final class SpecialLog {

    private SpecialLog() {
    }

    public static boolean isEnabled() {
        try {
            return AE2EnhancedConfig.debug == null || AE2EnhancedConfig.debug.debugMode;
        } catch (Throwable t) {
            return true;
        }
    }

    public static void info(String message, Object... args) {
        if (isEnabled()) {
            AE2Enhanced.LOGGER.info(message, args);
        }
    }

    public static void warn(String message, Object... args) {
        if (isEnabled()) {
            AE2Enhanced.LOGGER.warn(message, args);
        }
    }
}
