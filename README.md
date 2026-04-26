# AE2Enhanced

> **Fold immense crafting timelines into a single tick.**

AE2Enhanced is a late-game addon for **AE2 Unofficial Extended Life (AE2-UEL)** that introduces the **Supercausal Assembly Hub** — a massive 344-block multiblock structure designed to obliterate crafting bottlenecks through temporal batch processing and contained singularity physics.

If your ME network chokes on large-scale autocrafting, this is the answer.

---

## Requirements

- **Minecraft**: 1.12.2
- **Forge**: 14.23.5.2768+
- **AE2-UEL**: v0.56.7+
- **MixinBooter**: 8.9+ (automatically loaded as a dependency)

## Download

CurseForge: *(Coming Soon)*

---

## Core Features

### 🌌 Supercausal Assembly Hub

A 344-block multiblock that becomes a single, hyper-efficient crafting endpoint for your AE network.

| Block | Count |
|---|---|
| Assembly Controller | 1 |
| Assembly ME Interface | 3 |
| Assembly Casing | 180 |
| Assembly Inner Wall | 128 |
| Assembly Stabilizer | 32 |

### ⚡ True Batch Crafting

Virtual patterns are executed in bulk directly inside the CPU's inventory. No per-tick pattern pushing. Parallelism scales from 64 up to **theoretically infinite** with upgrade cards.

- **Virtual Track**: Patterns with no container returns are batched instantly.
- **Real Track**: Catalysts deduct only 1 unit regardless of batch size; transform slots process one-at-a-time when needed.

### 🃏 Upgrade Card System

| Module | Effect | Stack Limit |
|---|---|---|
| **Temporal Fold** (Parallel) | 64 → 2K → 65K → 2M → 67M → ∞ | 5 |
| **Spacetime Dilation** (Speed) | Halves batch cooldown per card. Min: **1 tick** | 5 |
| **Dimensional Fold** (Capacity) | +480 pattern slots (5 pages) per card | 10 |
| **Energy Optimization** (Efficiency) | Reduces AE network power draw | 10 |
| **Auto-Upload** | Auto-imports crafting patterns from ME Pattern Terminal | 1 |

### 🌀 Black Hole Crafting

A unique crafting system using the **Micro Singularity**. Throw items into the event horizon to transmute them into exotic spacetime materials.

**Generation Ritual**: Throw 64 AE2 Singularities + 4 Nether Stars onto the ground, then hold 1 Nether Star and right-click an ME Controller.

Full **JEI** and **CraftTweaker** support:
```zenscript
import mods.ae2enhanced.BlackHole;
BlackHole.addRecipe([<minecraft:stone> * 8, <minecraft:diamond>], <minecraft:obsidian>);
```

---

## Compatibility

- **CleanroomMC / Kirino**: GL state management verified against modern render pipelines.
- **Mixin-powered**: Uses Late Mixin for safe, non-destructive injection into AE2's crafting CPU cluster.
- **Server-safe**: All logic is server-authoritative.

---

## License

GPL-2.0

---

*"This very moment is all of eternity."*
