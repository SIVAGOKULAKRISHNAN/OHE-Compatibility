# P&W Native Connector Foundation — Fix 16

This revision is based on Fix 14 and uses the open-source Create: Pantographs & Wires 1.21.1 API rather than relying only on a nearest-node/shadow-network search.

## What is implemented

### Power Bridge
- Implements P&W `IWireConnector`.
- Has a real P&W `WireConnectorBlockEntity`.
- P&W endpoint is on the external face opposite the existing CEE terminal.
- CEE node remains unchanged on its external center face.
- The bridge therefore has two independent native interfaces:
  - CEE electrical node
  - P&W wire connector node

### C1–C4
The existing C1, C2, C3 and C4 section blocks now use a P&W `WireConnectorBlockEntity` and expose two native P&W connector points, one at each end of the assembly.

This is the foundation for later section/neutral behavior. It does not yet implement the final electrical isolation/topology rules.

### D — OHE switch
The existing OHE switch assembly now also exposes two native P&W connector points. Its open/closed state remains an electrical/topology concern handled by the CEE adapter; P&W remains the physical wire system.

### Feeder and feeder/sectioning post
No new feeder/post visual blocks are introduced in this revision because their final physical design has not yet been approved. The reusable `PawNativeConnector` adapter is the connector mechanism they will use when those components are implemented.

## Important ownership rule

- P&W owns physical wires, wire geometry, tension and pantograph contact.
- CEE owns electrical simulation/train power.
- Our compatibility layer provides the bridge between them.

## What this revision does NOT claim yet

- It does not claim that CEE current is already transferred through the native P&W graph.
- It does not finalize C1–C4 neutral electrical behavior.
- It does not finalize D switch electrical behavior.
- It does not yet implement a feeder/sectioning-post block.

Those require the next runtime tests.
