---
navigation:
  title: Commands and Config
  parent: systems.md
  position: 30
  icon: minecraft:command_block
---

# Commands and Config

## The /ae2e Command

Main command `/ae2enhanced`, alias `/ae2e`, permission level 2:

- `/ae2e channels <enable|disable|status>`: toggle AE2 channel checks.
- `/ae2e fastpathing <enable|disable|status>`: experimental O(N) channel pathing, applies on next repath.
- `/ae2e specialcrafting <enable|disable|status>`: special crafting plans (self-referencing / cyclic recipes), executed on the Computation Core.
- `/ae2e recoverhd list` / `/ae2e recoverhd <uuid>`: list hyperdimensional storage UUIDs / receive a controller bound to one.
- `/ae2e migratefluids`: migrate AE2E fluid drops to ae2fc format (requires ae2fc).
- `/ae2e pd <list|info|delete|tp|invite|kick|setperm>`: [Personal Dimension](systems/personal-dimension.md) management.
- `/ae2e help`: show help.

## Keybindings

- **F**: copy the hovered JEI item name into the terminal search box.
- **Shift+E**: open the Omni Terminal.
- **H**: toggle the Advanced Magnet Card mode.
- **N / Shift+N / Ctrl+N / C**: ME Omni Tool mode / silk touch / drop mode / config GUI.
- **G** (in game): ME Placement Tool radial menu.
- **G** (hovering an item in a GUI): hold to open its guide page.

## Config Sections (ae2enhanced.cfg)

- `BlackHole`: `damageMode` (ALL / NON_CREATIVE / NONE).
- `Crafting`: `maxParallel` 16384, `maxActiveOrders` 8, `specialCrafting`, `dagPlannerMode`.
- `WirelessChannel`: `crossDimension`, `maxRange`, `transmitterPower` 512, `extraUpgradeSlots` 2, `reconnectIntervalTicks` 100.
- `Storage`: `flushIntervalSeconds` 5, `monitorFullScanIntervalTicks` 200.
- `Collector` / `Recycler`: range, power and target limits.
- `CentralInterface`: virtual batch timeouts, cooldowns and energy cost.
- `Energy` / `Mana` / `Starlight`: access node transfer limits and creative RF boost.
- `OmniTool`: blink distance, attack damage, upgrade toggles.
- `SmartPattern`: `maxRecipes` 256, `blacklist`.
- `PersonalDimension`: `presetPath`, `floorY` 64, `entryY` 65.
- `EMCInterface`: `enabled`, `idlePower` 5.
- `Guide`: `enabled`, `theme` (vscode-dark / github-light / dracula / nord).
