---
navigation:
  title: Universal Buses
  parent: buses.md
  position: 20
  icon: part_universal_import_bus
item_ids: [part_universal_import_bus, part_universal_export_bus]
---

# Universal Buses

One bus for every resource type: items, fluids, gases (Mekanism) and essentia (Thaumcraft).

## Universal Import Bus

The <ItemLink id="part_universal_import_bus" /> **Universal Import Bus** pulls resources from the adjacent container into the network.

- Items: up to min(2^speed cards, 64) per operation.
- Fluids and gases: up to 1000 mB per operation.
- With an empty filter it imports everything.

## Universal Export Bus

The <ItemLink id="part_universal_export_bus" /> **Universal Export Bus** pushes configured resources from the network into the adjacent container.

- Items: up to min(2^speed cards, 64) per operation.
- Fluids: min(2^speed cards x 100, 8000) mB per operation.
- Fuzzy card enables fuzzy matching for item exports.

## Shared Details

- 63 config slots; usable slots = min(18 + 9 per capacity card, 63).
- 5 upgrade slots + 2 extra slots from the wireless channel config.
- Traversal mode: Sequential (default), Round Robin, or Random, switched by GUI button.
- Tick rates follow the AE2 import/export bus defaults.
