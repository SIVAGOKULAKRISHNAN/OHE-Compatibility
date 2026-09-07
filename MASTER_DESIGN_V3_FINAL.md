# OHE Compatibility Master Design V3 — P&W-first / CEE-supply

## Core rule
P&W is the authoritative physical railway OHE system. CEE is the electrical source/simulation and ground system. The compatibility layer is an exact-endpoint power hand-off, not a second visible OHE system.

## C1–C4
All four section/neutral assemblies use the approved two-rod sliding runner concept:
- C1: full cantilever-mounted runner/neutral assembly.
- C2: direct-in-wire/mid-span assembly with the sliding transition principle.
- C3: compact short PTFE neutral with short runner transition.
- C4: cantilever-mounted short PTFE/runner transition.
The two rods are physical runner hardware. They do not create a separate CEE wire. Electrical separation is represented at the P&W endpoints/section state.

## Feeder
The feeder is P&W native `pantographsandwires:energy_wire` (Power Line). Two physical construction options share one electrical conductor system:
- Option 1: standard support arrangement.
- Option 2: alternate/higher-capacity support arrangement.
Empty-hand right-click toggles Option 1/2.

## D
D is a separate railway switching assembly. Empty-hand right-click toggles:
- OHE section mode
- feeder/power-line mode
Create Wrench operates the selected function. The physical P&W wire remains authoritative.

## OHE Power Bridge
The bridge is the CEE-to-P&W OHE boundary. It exposes the exact P&W endpoint and a CEE electrical node. No nearest-node discovery is allowed for electrical attachment.

## Feeder Bridge
The feeder bridge is the CEE-to-P&W Energy Wire boundary. It does not render or replace P&W's feeder wire.

## CEE side
CEE supplies electrical power and ground. Existing CEE transformer/substation/booster and ground-rod infrastructure remain authoritative. The compatibility layer may maintain invisible CEE electrical shadow nodes only as an implementation adapter so CEE can simulate a P&W circuit; those nodes are not a second visible OHE network.

## Connection rules
- Exact P&W connector endpoints only.
- No proximity-based electrical linking.
- OHE and feeder circuits remain separate.
- Breaking a complete three-block bridge assembly removes all three blocks and drops one bridge item.
- Native P&W rendering remains unchanged.
