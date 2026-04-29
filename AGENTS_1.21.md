<!-- From: e:\minecraft Games\modi\AE2Enhanced\AGENTS.md -->
# AE2Enhanced —— AI 编码代理项目指南

> 本文档面向 AI 编码代理。阅读本文档前，默认读者对本项目一无所知。
> **注意：当前分支为 `neoforge-1.21.1`，与 `master` 的 1.12.2 版本完全隔离。**

---

## 项目概览

**AE2Enhanced** 是一个针对 **Minecraft 1.21.1 / NeoForge 21.1.x** 的 NeoForge 模组项目。核心内容是重新实现 **"Supercausal Assembly Hub / 超因果装配枢纽"** 大型多方块结构。

- **当前版本**：**2.0.0-neoforge-1.21.1**（规划中，尚未发布）
- **当前阶段**：重构规划完成，等待开始 B1（构建系统骨架）
- **源码目录**：`src/main/java/com/github/aeddddd/ae2enhanced/`（当前为空或待清理）
- **重构规划**：`PLAN_1.21.1.md`
- **历史 1.12.2 版本**：见 `master` 分支

---

## 技术栈与构建环境

| 组件 | 版本 / 说明 |
|---|---|
| Java | **21**（NeoForge 1.21.1 要求） |
| Gradle | **8.8+** |
| 构建插件 | **NeoGradle 7.0+** |
| Mixin | NeoForge 内置支持 |
| 测试框架 | JUnit 5（待定） |
| 目标平台 | Minecraft 1.21.1 + NeoForge 21.1.x + AE2 现代版 |
| Mappings | Official Mojang + Parchment（推荐） |
| 包名 | `com.github.aeddddd.ae2enhanced` |
| Group ID | `com.github.aeddddd` |

---

## 分支隔离规则（极其重要）

- 当前分支 **`neoforge-1.21.1`** 与 `master` **绝不互相合并**
- `master` 是 1.12.2 Forge 版本，继续维护但代码不共享至此分支
- 本分支所有代码从零开始重写（保留设计概念与美术资产）
- 发布 tag 格式：`v2.0.0-neoforge-1.21.1`

---

## 项目结构（目标态，尚未实现）

```
AE2Enhanced/
├── build.gradle                  # NeoGradle 配置
├── settings.gradle               # 根项目名：AE2Enhanced
├── gradle.properties             # Gradle JVM 参数（Java 21）
├── gradle/wrapper/               # Gradle 8.8 wrapper
├── src/main/java/com/github/aeddddd/ae2enhanced/
│   ├── AE2Enhanced.java              # @Mod 主类 + 注册初始化
│   ├── ModBlocks.java                # DeferredRegister<Block>
│   ├── ModItems.java                 # DeferredRegister<Item>
│   ├── ModBlockEntities.java         # DeferredRegister<BlockEntityType>
│   ├── ModMenuTypes.java             # DeferredRegister<MenuType>
│   ├── proxy/
│   │   ├── CommonProxy.java
│   │   └── ClientProxy.java          # 模型注册 + TESR 绑定
│   ├── block/
│   │   ├── BlockAssemblyController.java
│   │   ├── BlockAssemblyMeInterface.java
│   │   ├── BlockAssemblyCasing.java
│   │   ├── BlockAssemblyInnerWall.java
│   │   ├── BlockAssemblyStabilizer.java
│   │   └── BlockMicroSingularity.java
│   ├── blockentity/
│   │   ├── BlockEntityAssemblyController.java
│   │   ├── BlockEntityAssemblyMeInterface.java
│   │   └── BlockEntityMicroSingularity.java
│   ├── item/
│   │   ├── ItemUpgradeCardParallel.java    # 6 种升级卡：独立物品
│   │   ├── ItemUpgradeCardSpeed.java
│   │   ├── ItemUpgradeCardEfficiency.java
│   │   ├── ItemUpgradeCardCapacity.java
│   │   ├── ItemUpgradeCardAutoUpload.java
│   │   ├── ItemUpgradeCardReserved.java
│   │   ├── ItemConformalCharge.java
│   │   ├── ItemDifferentialFormStabilizer.java
│   │   ├── ItemStableSpacetimeManifold.java
│   │   └── ItemBlockMicroSingularity.java
│   ├── mixin/
│   │   └── crafting/               # 现代 AE2 Crafting CPU Mixin（待调研）
│   ├── structure/
│   │   ├── AssemblyStructure.java      # 坐标集合（保留数学逻辑）
│   │   ├── ControllerIndex.java        # SavedData 索引
│   │   └── StructureEventHandler.java  # Forge 事件验证
│   ├── client/gui/
│   │   ├── ScreenAssemblyUnformed.java
│   │   ├── ScreenAssemblyFormed.java
│   │   ├── ScreenAssemblyPattern.java
│   │   └── TechButton.java
│   ├── menu/
│   │   ├── MenuAssemblyUnformed.java
│   │   ├── MenuAssemblyFormed.java
│   │   └── MenuAssemblyPattern.java
│   ├── network/
│   │   └── (CustomPacketPayload 实现)
│   ├── crafting/
│   │   ├── BlackHoleRecipe.java
│   │   ├── BlackHoleRecipeRegistry.java
│   │   ├── BlackHoleCraftingHelper.java
│   │   ├── SingularityRecipe.java
│   │   └── SingularityRecipeRegistry.java
│   ├── event/
│   │   └── SingularityRitualHandler.java
│   ├── client/render/
│   │   ├── BlackHoleRenderer.java
│   │   └── MicroSingularityRenderer.java
│   └── integration/
│       ├── jei/
│       └── crafttweaker/           # 待定，可能改用 KubeJS
└── src/main/resources/
    ├── META-INF/mods.toml          # NeoForge 模组声明
    ├── ae2enhanced.mixins.json     # Mixin 配置
    └── assets/ae2enhanced/
        ├── blockstates/
        ├── models/block/
        ├── models/item/
        ├── textures/
        ├── lang/                 # *.json 格式
        └── recipes/              # 现代数据驱动配方（或代码配方）
```

---

## 构建命令

```bash
./gradlew build      # 编译
./gradlew runClient  # 运行客户端
./gradlew test       # 运行测试
./gradlew clean      # 清理
```

---

## 代码风格约定

1. **语言**：项目设计文档与注释主要使用**中文**。源码注释、GUI 文本、日志提示以中文为主。
2. **包名**：所有新类必须使用 `com.github.aeddddd.ae2enhanced`。
3. **注册方式**：统一使用 `DeferredRegister`，禁止静态初始化时直接 `new Block()` 注册。
4. **方块实体**：统一使用 `BlockEntity` 命名（而非 TileEntity）。
5. **升级卡**：6 种升级卡为独立物品类（或共享基类但独立注册条目），不再使用 meta/damage 区分。
6. **Capability**：NeoForge 1.21.1 的 Capability 系统与 1.12.2 不同，避免直接迁移旧代码。
7. **网络包**：使用 NeoForge `CustomPacketPayload` API，禁止遗留 `SimpleNetworkWrapper`。
8. **GUI**：使用 `MenuType` + `AbstractContainerMenu` + `Screen`，禁止 `IGuiHandler`。

---

## 当前状态与下一步

- [x] 创建 `neoforge-1.21.1` 分支
- [x] 编写 `PLAN_1.21.1.md` 重构规划
- [x] 更新 `AGENTS.md`
- [ ] 清理旧 1.12.2 代码（`src/main/java/` 全部移除，保留 `resources` 参考）
- [ ] 提交骨架
- [ ] **B1**：配置 NeoGradle 构建系统，成功编译空模组

---

*文档版本：v2.0.0-plan | 当前分支：neoforge-1.21.1 | 最后更新：2026-04-27*
