# AE2Enhanced

AE2Enhanced 是 **AE2 Unofficial Extended Life (AE2-UEL)** 的终局扩展模组,引入了 **超因果装配枢纽** —— 一个类似LazyAE的大型分子装配室的巨型多方块结构,允许以极高的并行和速度来完成合成样板.

---
## 介绍
这个多方块结构允许以基础**64并行,1s每次**的速度运行合成样板的配方,这相当于`LazyAE`的大型分子装配室在不修改配置文件的最强表现.

**需要强调,装配枢纽并行不依赖CPU并行!**

并且,装配枢纽提供了安装模块升级的空间,在安装了全部5个并行升级后,并行变为`Long.MAX`,在绝大多数的整合包中都相当于无穷(这甚至大于`1.12.2 AE`的终端显示上限!)

而速度升级允许将速度从默认的`20ticks`提升至`1tick`,合成不再是拖延工厂发展的阻碍了.

额外的,多方块提供了类似高版本`EAEP`装配矩阵上传核心的**自动上传模块**,允许你把编码好的合成样板自动上传到多方块中,同时具有重复检测功能,对于重复样板不会重复上传.

---
## 性能

与大型分子装配室,装配矩阵不同, 装配枢纽采取了虚拟合成+真实合成的混合机制, 即使对于极大数目的下单, `mspt`也不会受到明显影响 ,对于绝大多数AE允许的下单均完成了适配.

>额外补充
>在crt魔改合成配方时, 简单的`.reuse`并不能让AE知道这个物品不被消耗,下单时仍会正常请求对应份数


---
## 需求

- **Minecraft**: 1.12.2
- **Forge**: 14.23.5.2768+
- **AE2-UEL**: v0.56.7+
- **MixinBooter**: 8.9+

---
## 下载

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/ae2enhanced)  
[Releases](https://github.com/aeddddd/AE2Enhanced/releases)

---

## 兼容性

兼容**Cleanroom**, 对其他AE附属保持良好兼容, 在大型整合包`Divine Journey 2`中测试无问题(需要更新AE2-uel版本至最新)

---

### 额外合成设定:黑洞合成

独特的合成系统, 允许玩家创建 **微型奇点** 并且将物品投入事件视界,转化为所需材料

**注意**: 微型奇点只会维持`300s`,需要右键来开启合成,**不要离它太近!!!**

支持**CraftTweaker**修改配方:
```zenscript
import mods.ae2enhanced.BlackHole;
BlackHole.addRecipe([<minecraft:stone> * 8, <minecraft:diamond>], <minecraft:obsidian>);
BlackHole.removeRecipe("test_obsidian");
```


