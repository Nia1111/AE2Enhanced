---
navigation:
  title: Chunk Nodes
  parent: devices.md
  position: 40
  icon: chunk_power_node
item_ids: [chunk_power_node, compressed_chunk_power_node, chunk_mana_node, compressed_chunk_mana_node]
---

# Chunk Nodes

Chunk nodes supply energy to every compatible machine inside a chunk area, drawing from the ME network.

## Power Nodes

- <ItemLink id="chunk_power_node" /> **Chunk Power Node**: scans its own chunk every 20 ticks and pushes FE into every tile that accepts Forge Energy. Unused energy is returned to the network.
- <ItemLink id="compressed_chunk_power_node" /> **Compressed Chunk Power Node**: same behavior over a 3x3 chunk area (9 chunks).
- Costs 1 channel and 32 AE idle power. The Network Access Node is blacklisted (it is never powered).
- **Shift + right-click** highlights the powered targets for 100 ticks.

## Mana Nodes (require Botania)

- <ItemLink id="chunk_mana_node" /> **Chunk Mana Node**: draws from the network Mana channel and supplies Botania mana receivers in its chunk.
- <ItemLink id="compressed_chunk_mana_node" /> **Compressed Chunk Mana Node**: same over a 3x3 chunk area.
- Mana Voids and generating flora are excluded as targets.
