# CEE + P&W OHE Compatibility — v0.2 robust architecture

Target: Minecraft 1.21.1 / NeoForge 21.1.x / Create 6 / CEE 1.21.1-1.1.1 / P&W 1.21.1-beta-0.2.3-C6.

## Ownership rule

- CEE owns electrical simulation and electric-train behavior.
- P&W owns physical catenary geometry, WireGraph, wire/tension behavior and pantograph mechanics.
- This mod adds an electrical shadow/adapter layer. It does not replace native CEE or P&W wires.

CEE's electrical implementation is a current-flow simulation with voltage drops and losses, and it already supports electrified railways. P&W supplies catenary wires, power lines, cantilevers and pantographs. Keep both native systems intact.

## Implemented architecture

### A / B
Existing CEE and P&W OHE: unchanged.

### C1–C4
Four physical section/neutral designs:
- C1 cantilever-mounted
- C2 direct-in-wire
- C3 short PTFE-style
- C4 cantilever-mounted short neutral

The electrical shadow system is designed to let these assemblies split/route the electrical representation while the native P&W physical wire remains continuous.

### D
OHE switch extends the real CEE HV switch implementation, including its CEE electrical device, node positions and native switch state/arc behavior. Only the block-entity type is replaced so our block can legally host the CEE switch block entity.

### E
Compact OHE substation extends the real CEE TransformerBlock. It therefore uses CEE's transformer electrical device rather than creating a second electrical simulation.

### F
New OHE booster/transfer transformer block also extends CEE's transformer implementation. It is an electrical transfer device, not a magic distance/range multiplier.

### P&W pantograph bridge
A mixin makes P&W's PantographBlock implement CEE's IPantographBlock. This lets CEE's existing train-contraption capture create its normal TrainPantographEntry for a P&W pantograph. No second train electrical model is introduced.

### P&W physical wire -> CEE electrical shadow
The server-side shadow manager mirrors P&W catenary topology into CEE's InfrastructureSavedData using CEE detached nodes and CatenaryConnectionData. P&W remains the geometry/tension authority; CEE remains the electrical authority.

## Safety rules

- Never modify a native P&W WireEdge just to make it electrical.
- Never replace a native CEE CatenaryConnection.
- Do not hard-code 40/50 km limits.
- Do not invent a second FE-style energy system.
- Never parallel two sections unless the section controller explicitly allows it.
- Always remove shadow nodes when their P&W source nodes disappear.
- Runtime failures in an optional bridge must leave native CEE/P&W behavior intact.

## Known runtime validation still required

This v0.2 binds to real APIs present in the supplied jars, but it still needs an actual NeoForge runtime test for:
1. P&W graph load/save and shadow-node persistence across world restart.
2. CEE pantograph attachment to the shadow catenary.
3. C4 section splitting/left-feed/right-feed controller.
4. CEE native wire compatibility endpoint discovery.
5. Visual renderer registration for the CEE-derived D switch and E/F transformer blocks.
6. Server/client synchronization.
