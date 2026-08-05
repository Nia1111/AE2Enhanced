---
navigation:
  title: Energy Storage Bus
  parent: buses.md
  position: 30
  icon: part_energy_storage_bus
item_ids: [part_energy_storage_bus]
---

# Energy Storage Bus

The <ItemLink id="part_energy_storage_bus" /> **Energy Storage Bus** works like an AE2 storage bus, but for Forge Energy: it exposes an adjacent `IEnergyStorage` container as RF storage of the ME network.

## Behavior

- The bus has no capacity of its own; capacity and transfer rates come entirely from the adjacent energy container.
- Draconic Evolution energy cores are supported with long-level readings through a dedicated adapter.
- The bus polls the container every tick and reports differences to the network, which also catches changes made by third-party pipes that bypass notifications.

## Configuration

- 63 filter slots (RF fake items as whitelist); usable slots = 18 + 9 per capacity card.
- An inverter card turns the whitelist into a blacklist.
- Supports priority, access mode (read-write / read-only / write-only) and storage filter settings, same as the AE2 storage bus.
- 5 upgrade slots.
