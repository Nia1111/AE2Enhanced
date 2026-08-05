---
navigation:
  title: Multiblock Machines
  parent: index.md
  position: 10
  icon: assembly_controller
---

# Multiblock Machines

AE2Enhanced adds three large multiblock machines. Each is formed by placing the exact structure blocks around a controller; the structure is verified automatically (re-validated 20 ticks after neighbor changes, block breaking, or chunk loading).

## Machines

- [Assembly Hub](machines/assembly-hub.md) — Massively parallel autocrafting array, up to 2880 pattern slots.
- [Hyperdimensional Storage Nexus](machines/storage-nexus.md) — File-backed storage with no hard capacity limit.
- [Computation Core](machines/computation-core.md) — Super crafting CPU with Long.MAX_VALUE storage and 16384 parallel.

## Notes

- Unloaded chunks are skipped during validation, so structures spanning chunk borders do not break apart by accident.
- The Assembly Hub GUI can auto-complete missing structure blocks (creative: free; survival: consumes blocks from your inventory).
