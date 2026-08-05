---
navigation:
  title: Personal Dimension
  parent: systems.md
  position: 10
  icon: personal_dimension
item_ids: [personal_dimension, yellow_stripes_block_b]
---

# Personal Dimension

Every player can own a private, flat, single-biome dimension. The <ItemLink id="personal_dimension" /> **Personal Dimension Core** is the key to it.

## Entering and Leaving

- **Right-click a block**: teleport. Outside your dimension it records a return point and teleports you in (creating the dimension on first use); inside, it teleports you back to the return point.
- **Shift + right-click a block**: bind the entry point (only inside your own dimension).
- **Right-click air**: open the rules GUI.
- You cannot set spawn in a personal dimension; dying returns you to the overworld.

## Rules (per dimension)

- Mob Spawning: deny natural mob spawns (default off).
- Lock Weather: clears rain and thunder every tick, blocks snow/ice/lightning.
- Lock Time / Daylight Cycle: freeze world time at the configured value (default 6000).
- Flight: allow flying inside the dimension (creative always can).
- Movement Speed: walk/fly speed, clamped between 0.05 and 2.0 (default 0.1).
- No Flight Inertia: stop drifting when not moving while flying.

## Floor Preset

The floor generates at y=64 (config `personalDimension.floorY`) with 2 layers of bedrock below. The pattern comes from a JSON preset (`personalDimension.presetPath`, default `ae2enhanced/personal_dimension_floor.json`, copied to the config folder on first start). The built-in preset tiles a 96x96 pattern of <ItemLink id="yellow_stripes_block_b" /> Caution Blocks and concrete. Unknown blocks fall back to bedrock.

## Sharing and Permissions

Invite others with `/ae2e pd invite <player>`. Four permissions exist: ENTER, BUILD, INTERACT, MANAGE_RULES; invites grant the first three. Manage per-player with `/ae2e pd setperm <player> <perm> <true|false>`, remove with `kick`, and inspect with `info`. Owners and level-2 operators bypass all checks. See [Commands and Config](systems/commands-config.md).
