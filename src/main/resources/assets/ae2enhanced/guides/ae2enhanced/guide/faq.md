---
navigation:
  title: FAQ
  parent: index.md
  position: 70
  icon: minecraft:book
---

# FAQ

Frequently asked questions about **AE2Enhanced**.

## How do I open this guide?

Hold **G** while hovering over an item that has a guide page. The key can be rebound in the controls menu.

## The guide shows no pages

Make sure the guide is enabled in the `Guide` config section, then reload resources with **F3+T**.

## Where are fluids, gases, essentia, Mana and starlight stored?

They use the same ME network as items, through fake items and dedicated storage channels. See [Hyperdimensional Storage Nexus](machines/storage-nexus.md) and [Integration](integration.md).

## How do I move a Hyperdimensional Storage Nexus?

The data is stored by nexusId, not by the blocks. Break the structure freely; a controller carrying the same nexusId (obtainable via `/ae2e recoverhd`) re-accesses the same storage.

## Why is my wireless channel link offline?

Check the transmitter has power and a channel, the receiver card is bound to that transmitter, and cross-dimension/range limits allow the link. Connections re-establish every 100 ticks by default. See [Wireless Channel](devices/wireless-channel.md).

## Does this mod work on servers?

Yes. All storage and crafting logic runs on the server; only the guide and rendering are client-side.

## Something is not working

Check the config file first: most features can be toggled per section (see [Commands and Config](systems/commands-config.md)). If the problem persists, report it with your log file.
