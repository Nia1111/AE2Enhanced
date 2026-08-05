---
navigation:
  title: Computation Core
  parent: machines.md
  position: 30
  icon: computation_core
item_ids: [computation_core, constant_tensor_field_casing, constant_spinor_field_casing, causal_anchor_core, super_crafting_interface]
---

# Supercausal Computation Core

The <ItemLink id="computation_core" /> **Supercausal Computation Core** is a multiblock crafting CPU: clusters created through it get Long.MAX_VALUE crafting storage and a large accelerator value.

## Structure

- 1 Supercausal Computation Core (controller)
- 1 <ItemLink id="super_crafting_interface" /> Supercausal Crafting Interface (network access point)
- 144 <ItemLink id="constant_tensor_field_casing" /> Constant Tensor Field Casing
- 366 <ItemLink id="constant_spinor_field_casing" /> Constant Spinor Field Casing
- 343 <ItemLink id="causal_anchor_core" /> Causal Anchor Core (required; missing any fails validation)

## Behavior

- Each crafting cluster is created with `Long.MAX_VALUE` available storage and an accelerator of 16384.
- The parallel cap is set by config `crafting.maxParallel` (default 16384); active order limit is `crafting.maxActiveOrders` (default 8).
- Auto-splitting keeps an idle CPU cluster available for new jobs.
- Special crafting plans (self-referencing or cyclic recipes, solved in closed form) are executed on this core; toggle with `/ae2e specialcrafting` or config `crafting.specialCrafting` (see [Commands and Config](systems/commands-config.md)).
