---
navigation:
  title: 总线部件
  parent: index.md
  position: 30
  icon: part_stocking_bus
---

# 总线部件

在 ME 网络与相邻容器之间搬运资源的线缆部件, 一个部件即可处理多种资源类型.

## 总线

- [库存维持总线](buses/stocking-bus.md) —— 将相邻容器中的资源维持在精确的目标数量.
- [通用总线](buses/universal-buses.md) —— 导入或导出物品, 流体, 气体与源质.
- [能源存储总线](buses/energy-storage-bus.md) —— 把相邻能量容器暴露为网络的 RF 存储.

## 通用行为

- 通用输入/输出总线有 63 个配置槽; 可用槽数 = min(18 + 每张容量卡 9, 63).
- 通用总线有 5 个升级槽, 另有无线频道配置提供的 2 个额外槽位.
- 槽位遍历模式可在 GUI 中切换: 顺序, 轮询或随机.
