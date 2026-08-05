---
navigation:
  title: Universal Memory Card
  parent: tools.md
  position: 30
  icon: universal_memory_card
item_ids: [universal_memory_card]
---

# Universal Memory Card

The <ItemLink id="universal_memory_card" /> **Universal Memory Card** copies machine configurations between devices and binds remote targets for the Central ME Interface and the ME Network Recycler.

## Operations

- **Shift + right-click** a machine: copy its configuration and upgrades.
- **Right-click** a machine: paste the copied configuration. Missing upgrades are reported; missing items can be requested from network autocrafting.
- **Ctrl + right-click**: select or deselect a machine (up to 64 similar blocks), then paste to all selected at once.
- **Alt + right-click**: clear bindings (Central ME Interface / ME Network Recycler).
- **Right-click air**: open the management GUI (view and clear copies and selections).

## Special Targets

- Right-click a [Central ME Interface](devices/central-interface.md): set it as the binding source for remote targets.
- Right-click a [ME Network Recycler](devices/collector-recycler.md): bind all selected machines to it in batch.
- Right-click a [Smart Pattern Interface](devices/smart-pattern.md): query JEI recipes of the selected targets and bind them.

## Supported Devices

- AE2 parts and block devices (always).
- Conditionally: Mekanism, Ender IO (machines and conduits), Thermal Expansion, NuclearCraft, TechReborn machines.
