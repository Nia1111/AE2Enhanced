---
navigation:
  title: Collector and Recycler
  parent: devices.md
  position: 50
  icon: advanced_me_collector
item_ids: [advanced_me_collector, me_network_recycler]
---

# Collector and Recycler

## Advanced ME Collector

The <ItemLink id="advanced_me_collector" /> **Advanced ME Collector** intercepts item entities before they spawn and injects them directly into the ME network.

- 63 filter slots: 18 usable by default, +9 per capacity card. Empty filter collects everything.
- 5 upgrade slots; internal buffer of 27 slots x 4096.
- Range: from 2 (5x5x5) up to 7 (15x15x15), config `collector.defaultRange` / `collector.maxRange`.
- Idle power 16 AE (`collector.idlePower`).
- Supports redstone control, fuzzy mode and craft-only settings.

## ME Network Recycler

The <ItemLink id="me_network_recycler" /> **ME Network Recycler** collects outputs from bound machines directly into the network (or into a Hyperdimensional Storage Nexus).

- Bind targets with the [Universal Memory Card](tools/memory-card.md): select machines, then right-click the recycler to bind the selection in batch. Alt + right-click clears bindings.
- Up to 1024 bound targets (`recycler.maxTargets`); works remotely and across dimensions.
- `recycler.forceHyperdimensionalStorage` (default on) writes recycled items into hyperdimensional storage; `recycler.machineOutputRedirect` (default on) can redirect machine outputs before they enter an inventory.
- Idle power 32 AE; status is shown in the GUI (Active / Powered / Offline) and synced to nearby players.
