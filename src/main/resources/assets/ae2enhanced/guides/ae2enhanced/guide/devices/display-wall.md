---
navigation:
  title: Display Wall
  parent: devices.md
  position: 70
  icon: display_panel
item_ids: [display_panel, display_frame_dark, display_frame_light]
---

# Display Wall

The <ItemLink id="display_panel" /> **Display Panel** forms a large multi-block screen that plots how item/fluid storage levels in your ME network change over time.

## Building

1. Place display panels as a solid rectangle, **2~16 wide x 2~9 tall**, all facing the same direction (the side you look at).
2. The screen forms automatically. The bottom-left panel becomes the controller.
3. Surround it with <ItemLink id="display_frame_dark" /> dark or <ItemLink id="display_frame_light" /> light frames (decorative). More light frames than dark ones selects the light theme.
4. Attach the screen to your ME network with a cable on any side. It **uses no channel** and draws `width x height x 2 AE/t` (config `displayWall.powerPerBlock`).

## Configuring

Right-click the screen to open the config GUI:

- Up to **8 tracked entries**. Drop an item into a fake slot, or click with a fluid container to track a fluid.
- Click the **color swatch** to cycle the line color; click the **eye** to hide/show an entry.
- Choose the **chart type** (Line / Area / Delta / Rate / Stacked), **time range** (5m / 30m / 2h / 24h) and **Y-axis mode** (Auto / Fixed / Log).

Sneak + right-click the screen to cycle chart types without opening the GUI.

## Details

- Samples are taken once per second and down-sampled: 1s resolution for 5 minutes, 10s for 1 hour, 1min for 24 hours. History persists across restarts.
- While the network is offline or the chunk unloaded, gaps render as broken lines.
- The Y axis uses nice steps (1/2/5) with k/M abbreviations; Auto mode smoothly rescales.
- Breaking any panel disbands the screen; it re-forms automatically when repaired.
