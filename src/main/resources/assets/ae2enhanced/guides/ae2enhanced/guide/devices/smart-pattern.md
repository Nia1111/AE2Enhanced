---
navigation:
  title: Smart Pattern
  parent: devices.md
  position: 60
  icon: smart_pattern_interface
item_ids: [smart_pattern_interface, smart_blank_pattern, smart_pattern]
---

# Smart Pattern

A smart pattern packs multiple recipes into a single pattern item, encoded against a specific machine.

## Workflow

1. Craft a <ItemLink id="smart_blank_pattern" /> **Smart Blank Pattern** via [Black Hole Crafting](systems/black-hole.md) (throw 64 blank patterns into the event horizon). ME interfaces ignore unencoded blanks.
2. Encode it in the <ItemLink id="smart_pattern_interface" /> **Smart Pattern Interface** GUI. JEI ghost ingredients can be dragged into the interface to pick recipes.
3. The result is a <ItemLink id="smart_pattern" /> **Smart Pattern**, which stores only an id; recipe data is saved in the world save. Individual recipes inside can be disabled one by one.

## Details

- One smart pattern can hold many recipes (config `smartPattern.maxRecipes`, default 256, up to 4096).
- Machines can be blacklisted from smart patterns (`smartPattern.blacklist`; furnace and the basic Extended Crafting table are excluded by default).
- Pattern expansion happens when the interface builds its crafting list, so the network sees the individual recipes contained in the pattern.
