---
navigation:
  title: Network Access Node
  parent: devices.md
  position: 30
  icon: network_access_node
item_ids: [network_access_node]
---

# Network Access Node

The <ItemLink id="network_access_node" /> **Network Access Node** bridges energy-like resources between adjacent blocks and the ME network's storage channels. It handles three resource types, each with an input and an output mode.

## Usage

- **Shift + right-click** the node to cycle its mode; the result is shown in chat.
- RF: exposes a Forge Energy (`IEnergyStorage`) capability. Transfer limit is `energy.rfAccessNodeMaxTransfer` (default: unlimited).
- Mana (requires Botania): moves Mana between the network Mana channel and adjacent mana pools. Limit `mana.manaAccessNodeMaxTransfer` (default 10000).
- Starlight (requires Astral Sorcery): moves starlight between the network and adjacent altars. Input limit `starlight.starlightAccessNodeMaxInput` (default 100), output limit `starlight.starlightAccessNodeMaxOutput` (default 1000). Input only works at night (13000-23000) when the altar sees the sky.

## Creative RF Source Boost

When a Draconic Evolution creative RF source is adjacent, the node injects energy into the network. Controlled by `energy.creativeRfSourceBoostEnabled` (default on) and `energy.creativeRfSourceBoostAmount` (default 1.0E12).
