# AE2Enhanced

> **Fold immense crafting timelines into a single tick.**

[中文版本 (Chinese)](/README_zh.md)

AE2Enhanced is a late-game addon for **AE2 Unofficial Extended Life (AE2-UEL)** that introduces the **Supercausal Assembly Hub** — a massive 344-block multiblock structure similar to LazyAE's Large Molecular Assembler, allowing extreme parallelism and speed for crafting patterns.

---

## Introduction

This multiblock allows running crafting patterns at a base of **64 parallel, 1 second per operation** — equivalent to LazyAE's Large Molecular Assembler at its strongest stock configuration.

**Importantly, the Assembly Hub's parallelism does NOT depend on CPU co-processors!**

With all 5 Parallel Extension Modules installed, parallelism reaches `Long.MAX_VALUE` — effectively infinite for virtually all modpacks (this even exceeds the AE2 terminal display limit in 1.12.2!).

Speed Modules reduce the cooldown from the default `20 ticks` down to `1 tick`, so crafting is no longer a bottleneck for factory expansion.

Additionally, the hub provides an **Auto-Upload Module** similar to AE2's Pattern Provider — encoded crafting patterns are automatically uploaded to the hub with duplicate detection. Duplicate patterns are silently ignored.

---

## Performance

Unlike Large Molecular Assemblers or pattern providers, the Assembly Hub uses a **hybrid virtual + real crafting mechanism**. Even for extremely large orders, **MSPT impact is negligible**. Nearly all AE2-craftable orders are fully supported.

> **Note on CraftTweaker:** When using `.reuse()` in CRT scripts, AE2 may still request the full amount during ordering since it does not recognize the item as non-consumed. This does not affect actual batch execution.

---

## Requirements

- **Minecraft**: 1.12.2
- **Forge**: 14.23.5.2768+
- **AE2-UEL**: v0.56.7+
- **MixinBooter**: 8.9+ (automatically loaded as a dependency)

## Download

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/ae2enhanced)  
[Releases](https://github.com/aeddddd/AE2Enhanced/releases)

---

## Compatibility

Compatible with **CleanroomMC**. Maintains good compatibility with other AE2 addons. Tested successfully in the large modpack **Divine Journey 2** (requires updating AE2-UEL to the latest version).

---

### 🌀 Black Hole Crafting

A unique crafting system that allows players to create a **Micro Singularity** and throw items into its event horizon to transmute them into desired materials.

**Warning**: The Micro Singularity only lasts for `300 seconds`. Right-click it to activate crafting. **Do not stand too close!!!**

Full **CraftTweaker** support:
```zenscript
import mods.ae2enhanced.BlackHole;
BlackHole.addRecipe(IItemStack output, IItemStack[] inputs);
//like
BlackHole.addRecipe(<minecraft:obsidian>, [<minecraft:stone> * 8, <minecraft:diamond>]);
BlackHole.removeRecipe("test_obsidian");
```

Built-in recipe name
```
id: "test_obsidian"
id: "stable_spacetime_manifold"
id: "differential_form_stabilizer"
id: "conformal_invariant_charge"
```

