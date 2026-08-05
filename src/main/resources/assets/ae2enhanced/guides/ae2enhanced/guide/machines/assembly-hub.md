---
navigation:
  title: Assembly Hub
  parent: machines.md
  position: 10
  icon: assembly_controller
item_ids: [assembly_controller, assembly_me_interface, assembly_casing, assembly_inner_wall, assembly_stabilizer, upgrade_card]
---

# Assembly Hub

The <ItemLink id="assembly_controller" /> **Assembly Controller** is the core of a massively parallel autocrafting multiblock. Each pattern slot runs its jobs independently, so hundreds of crafting jobs can execute at the same time.

## Structure

The complete structure contains 344 blocks:

- 1 Assembly Controller
- 3 <ItemLink id="assembly_me_interface" /> Assembly ME Interface (network access points)
- 180 <ItemLink id="assembly_casing" /> Assembly Casing
- 128 <ItemLink id="assembly_inner_wall" /> Assembly Inner Wall
- 32 <ItemLink id="assembly_stabilizer" /> Assembly Stabilizer

The controller GUI can fill in missing structure blocks automatically: free in creative mode, or consuming blocks from your inventory in survival.

## Pattern Slots

- 102 slots per page (17 columns x 6 rows), 5 pages by default.
- Each **Dimensional Fold Module** (capacity) adds 5 pages, up to 30 pages.
- Maximum total: 2880 slots. Only crafting patterns are accepted.
- Up to 4096 pending outputs are buffered.

## Upgrade Cards

The controller has 6 upgrade slots; slot index matches the card type. <ItemLink id="upgrade_card" /> Upgrade cards:

- **Temporal Fold Module** (parallel, max 5): batch crafting parallel cap. 0 cards = 64, each card multiplies by 32. Capped at 67,108,864; with 5 cards the tooltip shows infinity.
- **Spacetime Dilation Module** (speed, max 5): batch crafting cooldown starts at 20 ticks and halves per card, down to 1 tick.
- **Energy Optimization Module** (efficiency): tooltip states it reduces hub energy usage.
- **Dimensional Fold Module** (capacity): see pattern slots above.
- **Auto-Upload Module** (max 1): crafting patterns encoded in the [Omni Terminal](tools/omni-terminal.md) are automatically uploaded to the nearest assembled hub. Processing patterns are not uploaded.
- **Extension Module (Reserved)**: no function yet.

Custom parallel/speed upgrade cards can be registered via CraftTweaker (see [Black Hole Crafting](systems/black-hole.md)).

## Black Hole Overflow

When the hub crafts with a stable black hole, items within the 3x3x3 event horizon are pulled in as ingredients, and entities inside are killed (damage type "spacetime", throttled to every 5 ticks). Up to 5 overflow item types are supported.
