---
navigation:
  title: 能源存储总线
  parent: buses.md
  position: 30
  icon: part_energy_storage_bus
item_ids: [part_energy_storage_bus]
---

# 能源存储总线

<ItemLink id="part_energy_storage_bus" /> **能源存储总线**类似 AE2 存储总线, 但面向 Forge Energy: 它把相邻的 `IEnergyStorage` 容器暴露为 ME 网络的 RF 存储.

## 行为

- 总线自身没有容量; 容量与传输速率完全取决于相邻能量容器.
- 龙之进化能量核心通过专用适配器支持 long 级读数.
- 总线每 tick 轮询容器并向网络上报差值, 第三方管道绕过通知造成的变化也能被捕获.

## 配置

- 63 个过滤槽 (以 RF 假物品作为白名单); 可用槽数 = 18 + 每张容量卡 9.
- 双向过滤卡 (反相卡) 可将白名单反转为黑名单.
- 支持优先级, 访问模式 (读写 / 只读 / 只写) 与存储过滤设置, 与 AE2 存储总线一致.
- 5 个升级槽.
