---
navigation:
  title: Cable Buses
  parent: index.md
  position: 30
  icon: part_stocking_bus
---

# Cable Buses

Cable parts that move resources between the ME network and adjacent containers. All of them handle multiple resource types from one part.

## Buses

- [Stocking Bus](buses/stocking-bus.md) — Maintains exact target amounts in an adjacent container.
- [Universal Buses](buses/universal-buses.md) — Import or export items, fluids, gases and essentia.
- [Energy Storage Bus](buses/energy-storage-bus.md) — Exposes an adjacent energy container as network RF storage.

## Shared Behavior

- Universal Import/Export Buses have 63 config slots; usable slots = min(18 + 9 per capacity card, 63).
- Universal Buses have 5 upgrade slots plus 2 extra slots from the wireless channel config.
- Slot traversal mode can be switched in the GUI: Sequential, Round Robin, or Random.
