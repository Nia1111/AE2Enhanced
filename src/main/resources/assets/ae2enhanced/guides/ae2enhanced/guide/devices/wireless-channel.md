---
navigation:
  title: Wireless Channel
  parent: devices.md
  position: 20
  icon: wireless_channel_transmitter
item_ids: [wireless_channel_transmitter, channel_receiver_card]
---

# Wireless Channel

The <ItemLink id="wireless_channel_transmitter" /> **Wireless Channel Transmitter** broadcasts the channels of its grid, letting remote devices share them without cables.

## Binding a Receiver Card

1. Put an unbound <ItemLink id="channel_receiver_card" /> **Channel Receiver Card** into the transmitter's slot. It binds automatically, recording the transmitter's position, dimension and facing.
2. Insert the bound card into the upgrade slot of an AE2 device (part or machine). The mod lifts the usual slot restrictions, so most upgrade slots accept it.
3. The device establishes a remote grid connection to the transmitter and draws channels through the transmitter's controller path.

## Details

- The transmitter connects cables on its back only (dense smart), requires a channel, and has a default idle power of 512 AE (config `wirelessChannel.transmitterPower`).
- Cross-dimension links are enabled by default (`wirelessChannel.crossDimension`); range is unlimited by default (`wirelessChannel.maxRange` = 0).
- Connections re-establish on a timer (`wirelessChannel.reconnectIntervalTicks`, default 100 ticks).
- Bus upgrade slot count: 5 + `wirelessChannel.extraUpgradeSlots` (default 2).
- Fast pathing: `/ae2e fastpathing` switches an experimental O(N) channel pathing algorithm (see [Commands and Config](systems/commands-config.md)).
