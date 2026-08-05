---
navigation:
  title: 网络访问节点
  parent: devices.md
  position: 30
  icon: network_access_node
item_ids: [network_access_node]
---

# 网络访问节点

<ItemLink id="network_access_node" /> **网络访问节点**在相邻方块与 ME 网络的存储通道之间桥接能量类资源. 支持三种资源, 各有输入与输出两种模式.

## 用法

- **Shift + 右键**节点循环切换模式, 结果在聊天栏提示.
- RF: 暴露 Forge Energy (`IEnergyStorage`) 能力. 传输上限 `energy.rfAccessNodeMaxTransfer` (默认不限).
- Mana (需要植物魔法): 在网络 Mana 通道与相邻魔力池之间转移 Mana. 上限 `mana.manaAccessNodeMaxTransfer` (默认 10000).
- 星能 (需要星辉魔法): 在网络与相邻祭坛之间转移星能. 输入上限 `starlight.starlightAccessNodeMaxInput` (默认 100), 输出上限 `starlight.starlightAccessNodeMaxOutput` (默认 1000). 输入仅在夜间 (13000-23000) 且祭坛能见到天空时生效.

## 创造 RF 源增益

相邻存在龙之进化创造 RF 源时, 节点会向网络注入能量. 由 `energy.creativeRfSourceBoostEnabled` (默认开) 与 `energy.creativeRfSourceBoostAmount` (默认 1.0E12) 控制.
