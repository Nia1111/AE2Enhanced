---
navigation:
  title: Hyperdimensional Storage Nexus
  parent: machines.md
  position: 20
  icon: hyperdimensional_controller
item_ids: [hyperdimensional_controller, hyperdimensional_me_interface, hyperdimensional_casing, hyperdimensional_singularity_core]
---

# Hyperdimensional Storage Nexus

The <ItemLink id="hyperdimensional_controller" /> **Hyperdimensional Storage Controller** exposes file-backed storage channels to the ME network. Item amounts are counted with BigInteger, so there is no hard capacity limit; disk space is the only bound.

## Structure

A flat, single-layer structure:

- 1 Hyperdimensional Storage Controller
- 1 <ItemLink id="hyperdimensional_me_interface" /> Hyperdimensional ME Interface (network access point, the controller itself does not connect cables)
- 5 <ItemLink id="hyperdimensional_singularity_core" /> Hyperdimensional Singularity Core
- 14 <ItemLink id="hyperdimensional_casing" /> Hyperdimensional Metric Anchor

## Storage Channels

- Always available: items, fluids, energy (RF), Mana, Starlight.
- Conditional: gases (Mekanism) and essentia (Thaumcraft) when the corresponding mods are installed.
- If Flux Applied or Botania Applied provides the energy/mana channel, the nexus uses that external channel instead.

## Persistence

- Each nexus is identified by a UUID (nexusId). Data lives in `<world>/ae2enhanced/storage/<nexusId>/` as `items.bin`, `fluids.bin`, `energy.bin`, `mana.bin`, `starlight.bin`, plus optional `gases.bin` and `essentias.bin`.
- Dirty sections are flushed to disk on a timer (config: `storage.flushIntervalSeconds`, default 5 seconds).
- Breaking the controller or the whole structure does **not** delete data; a controller item carrying a nexusId can re-access the same storage.
- Admin commands: `/ae2e recoverhd list` and `/ae2e recoverhd <uuid>` hand out controller items bound to a given nexusId (see [Commands and Config](systems/commands-config.md)).
- Statistics are displayed with BigInteger formatting (K/M/G/T/P/E/Z/Y, scientific notation beyond 1e27).
