---
navigation:
  title: 多方块机器
  parent: index.md
  position: 10
  icon: assembly_controller
---

# 多方块机器

AE2Enhanced 添加了三台大型多方块机器. 在控制器周围摆放精确的结构方块即可成型; 结构会自动验证 (邻居变化, 方块破坏或区块加载后延迟 20 tick 重新验证).

## 机器

- [超因果装配枢纽](machines/assembly-hub.md) —— 大规模并行自动合成阵列, 最多 2880 个样板槽.
- [超维度仓储中枢](machines/storage-nexus.md) —— 基于文件存储, 无硬容量上限.
- [超因果计算核心](machines/computation-core.md) —— 超级合成 CPU, 存储上限 Long.MAX_VALUE, 并行 16384.

## 说明

- 验证时会跳过未加载的区块, 跨区块边界的结构不会被误判解体.
- 装配枢纽 GUI 可以一键补齐缺失的结构方块 (创造模式免费; 生存模式消耗背包中的方块).
