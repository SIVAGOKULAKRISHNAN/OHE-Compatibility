# TSS Feeder Switch / Isolator

## Locked design

The TSS Feeder Switch is a **CEE-side traction-substation feeder isolator**. It is intentionally based on CEE's native `HVSwitchBlock` and `HVSwitchDevice` so CEE owns the electrical topology, switching resistance/arc simulation, and two-terminal electrical state.

It is not a circuit breaker. The existing CEE breaker remains the protection device upstream.

### Power path

`CEE Substation/Transformer -> CEE Circuit Breaker -> TSS Feeder Switch -> CEE feeding-side connection -> OHE Feeder Bridge -> P&W native Energy Wire`

The feeder conductor is P&W's native `pantographsandwires:energy_wire`. No second visible feeder wire is introduced.

### Controls

- Empty-hand right-click: open/close the isolator.
- CEE simulation: native HV-switch device controls electrical continuity.
- Open: feeder supply is isolated.
- Closed: feeder supply is available to the P&W feeder bridge.

### Physical model

Three visible insulated pole assemblies, steel cross-frame, copper switch blades, and a front operating box. The open and closed positions use separate block models. Horizontal placement rotates the entire assembly.

### Relationship to D

This is deliberately separate from the railway-side `D` OHE switch/sectioning assembly. D operates the P&W-side railway section/feeder routing; TSS Feeder Switch is the upstream CEE-side supply isolator.
