# OHE Compatibility – Master Design v2

## Locked architecture

**P&W is the physical railway/OHE authority. CEE is the electrical supply and ground authority.**

- P&W owns visible catenary/contact wire, native Energy Wire (feeder/power line), wire graph, wire rendering, tension/geometry and pantograph interaction.
- CEE owns generation/supply, transformer/substation/booster behavior, electrical simulation and ground.
- The OHE Power Bridge is the exact CEE-to-P&W electrical boundary.
- CEE shadow nodes are **not a second visible OHE system**. They are an invisible electrical mirror required only because P&W does not itself run CEE's electrical simulation.
- OHE and feeder shadow circuits are kept separate even when a P&W connector block is shared.

## Components

### C1 – Cantilever-mounted full neutral/section assembly
- Realistic two-rod sliding runner assembly.
- Left/right runners, runner brackets, pivot/pin details, cross beam and insulator.
- Pantograph transition is physically represented by P&W wire plus the runner geometry.
- Electrical separation is represented in the CEE electrical mirror at the section boundary.
- P&W wire remains the visible conductor.

### C2 – Direct-in-wire / mid-span section
- Compact direct section geometry.
- Two sliding transition rods/runners.
- No unnecessary large cantilever.
- Two exact P&W endpoints, electrically separable.

### C3 – Short PTFE neutral section
- Short PTFE/composite insulated center.
- Compact runner/transition pieces.
- Two exact P&W endpoints.
- Electrical gap is represented in the CEE mirror without changing P&W geometry.

### C4 – Cantilever-mounted short PTFE neutral
- C3 short/PTFE center combined with a cantilever support.
- Two transition runners/rods.
- Exact P&W endpoints and independent electrical separation.

### D – OHE switch assembly
One physical railway switch assembly with two operating modes:
- **OHE mode:** sectioning/switching of the P&W catenary circuit.
- **Feeder mode:** switching of the P&W Energy Wire feeder circuit.
- Empty-hand right-click toggles mode.
- Create Wrench operates the selected mode.
- The physical P&W wire stays present; the CEE electrical mirror opens/closes the corresponding circuit.
- Existing CEE HV switch remains the electrical switching device.

### Feeder Option 1 / Option 2
A dedicated CEE-to-P&W feeder terminal/bridge, not a replacement for P&W's feeder wire.
- Option 1: standard feeder support arrangement.
- Option 2: alternate/higher-capacity physical arrangement.
- Empty-hand right-click toggles Option 1/2.
- Both use P&W native `pantographsandwires:energy_wire`.
- The option changes physical support geometry only; it does not create a second electrical technology.

### OHE Power Bridge
- CEE-side electrical connector.
- P&W-side native catenary connector.
- Exact endpoint matching only.
- No nearest-node/proximity linking.
- Provides the CEE supply into the P&W OHE network.

### CEE ground rod
- Remains entirely CEE-side.
- Provides the electrical ground/reference used by CEE.
- It is not duplicated as a P&W OHE component.

### E – Compact OHE substation
- Uses native CEE TransformerBlock/TransformerBlockEntity behavior.
- Compact physical railway arrangement only; not a duplicate electrical system.

### F – OHE booster
- Separate transformer topology.
- Uses native CEE transformer electrical behavior.
- Connects to the compatibility electrical boundary rather than replacing P&W wire behavior.

## Native P&W feeder discovery

P&W 1.21.1 exposes:
- `de.mrjulsen.paw.registry.ModWireRegistry.ENERGY_WIRE`
- registry id `pantographsandwires:energy_wire`
- `de.mrjulsen.paw.item.FeederWireItem`
- `de.mrjulsen.paw.block.PowerLineBracketBlock`

The compatibility layer uses that native Energy Wire instead of inventing another feeder renderer.

## Electrical flow

```text
CEE Substation / Transformer / Booster
                 |
                 v
          OHE Power Bridge
                 |
                 v
        P&W native catenary
          |    C1-C4    |
          |      D      |
          +-------------+

CEE supply
    |
    v
Feeder Bridge
    |
    v
P&W native Energy Wire
    |
    v
D in feeder mode / feeder circuit
```

The visible railway network is therefore P&W. CEE only supplies and simulates the electrical side.

## Safety/connection rules

1. Never connect by nearest node.
2. Match the exact P&W attachment point.
3. Never electrically join catenary and feeder merely because their P&W connector block positions coincide.
4. Breaking C1-C4 removes their complete physical assembly according to the block's assembly rules.
5. Breaking the OHE Power Bridge removes its complete bridge/insulator assembly.
6. P&W wire rendering remains authoritative.
7. CEE compatibility wires remain hidden.
8. Do not invent recipe IDs when native recipe IDs have not been verified.

## Verification status

The uploaded P&W 1.21.1 runtime JAR in the compatibility project was inspected directly. Its native Energy Wire is confirmed as `pantographsandwires:energy_wire`, implemented by `PowerWireType`, and its feeder item is `FeederWireItem`.

The uploaded CEE 1.21.1 JAR was inspected directly. It contains `ConnectorBlock`, `GroundRodBlock`, `HVSwitchBlock`, `TransformerBlock`, `InWorldNode`, `InfrastructureSavedData` and the CEE simulation infrastructure used by this compatibility layer.

The separately uploaded P&W source archive is a **1.20.1** source tree, not the 1.21.1 runtime. It is useful for implementation context but is not treated as authoritative for 1.21.1 signatures.
