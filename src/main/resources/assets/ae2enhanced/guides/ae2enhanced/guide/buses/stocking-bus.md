---
navigation:
  title: Stocking Bus
  parent: buses.md
  position: 10
  icon: part_stocking_bus
item_ids: [part_stocking_bus]
---

# Stocking Bus

The <ItemLink id="part_stocking_bus" /> **Stocking Bus** keeps an adjacent container stocked at exact target amounts: below target it supplies from the network, above target it recovers the excess back.

## Configuration

- 9 config slots, each with an independent target amount. The resource type is detected from the fake item placed in the slot: item, fluid (mB), gas, or essentia.
- Set the amount by scrolling on a slot: wheel +/-1, Shift+wheel +/-10, Ctrl+wheel +/-100.
- **Middle-click** a slot to open the amount input GUI. Clearing a slot resets its target to 1.
- Items default to a target of 1; fluids and gases default to 1000.

## Modes

- **Bidirectional** (default): supply and recover.
- **Supply Only**: only exports to the container.
- **Recover Only**: only pulls excess back. The GUI button cycles modes; right-click cycles backwards.

## Speed and Upgrades

- Work per tick = min(2^speed cards, 64) x min(2^capacity cards, 16); fluids and gases are scaled by 1000 mB.
- Fuzzy card enables fuzzy matching for item slots.
- Supports redstone control and scheduling mode settings.
