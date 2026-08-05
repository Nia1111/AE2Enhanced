---
navigation:
  title: 个人维度
  parent: systems.md
  position: 10
  icon: personal_dimension
item_ids: [personal_dimension, yellow_stripes_block_b]
---

# 个人维度

每名玩家都可以拥有一个私人的平坦单群系维度. <ItemLink id="personal_dimension" /> **个人维度核心**是进入它的钥匙.

## 进入与离开

- **右键方块**: 传送. 不在个人维度时记录返回点并进入 (首次使用时创建维度); 已在个人维度时传送回返回点.
- **Shift + 右键方块**: 绑定进入点 (只能在自己的维度内绑定).
- **右键空气**: 打开规则配置 GUI.
- 个人维度内无法设置重生点; 死亡后回到主世界.

## 规则 (按维度独立)

- 刷怪: 禁止生物自然生成 (默认关).
- 锁定天气: 每 tick 清除下雨与雷暴, 并阻止雨雪冰与闪电.
- 锁定时间 / 昼夜循环: 将世界时间冻结在设定值 (默认 6000).
- 飞行: 允许在维度内飞行 (创造模式恒允许).
- 移动速度: 行走/飞行速度, 限制在 0.05 到 2.0 之间 (默认 0.1).
- 无飞行惯性: 飞行时无移动输入即停止漂移.

## 地板预设

地板生成于 y=64 (配置 `personalDimension.floorY`), 下方固定 2 层基岩. 图案来自 JSON 预设 (`personalDimension.presetPath`, 默认 `ae2enhanced/personal_dimension_floor.json`, 首次启动会复制到 config 目录). 内置预设平铺 96x96 的 <ItemLink id="yellow_stripes_block_b" /> 警示方块与混凝土图案. 未知方块回退为基岩.

## 共享与权限

用 `/ae2e pd invite <player>` 邀请他人. 共有四项权限: ENTER, BUILD, INTERACT, MANAGE_RULES; 邀请默认授予前三项. 用 `/ae2e pd setperm <player> <perm> <true|false>` 单项设置, `kick` 移出, `info` 查看. 维度所有者与权限等级 2 的管理员恒绕过检查. 见[命令与配置](systems/commands-config.md).
