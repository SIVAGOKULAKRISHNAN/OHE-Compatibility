# Master Design v2 Implementation Note

This revision implements the P&W-first feeder architecture requested for the project.

Implemented:
- P&W native `pantographsandwires:energy_wire` is recognized by registry id.
- Feeder edges are mirrored into a separate invisible CEE electrical shadow circuit so they cannot accidentally join the OHE catenary circuit.
- Added OHE Feeder Bridge CEE↔P&W terminal.
- Empty-hand right-click on OHE Feeder Bridge toggles Option 1/Option 2 support geometry.
- D empty-hand right-click toggles OHE section-switch mode / feeder-switch mode; wrench operates the existing CEE HV switch.
- C1-C4 models now include explicit paired sliding runner rods and end pin details.
- OHE catenary and feeder remain separate electrical circuits even when they share a physical P&W connector position.
- OHE Power Bridge remains the CEE supply interface for P&W catenary.
- CEE ground rod remains native CEE ground equipment.

Verification performed:
- P&W 1.21.1 runtime JAR inspected directly: `ModWireRegistry.ENERGY_WIRE` and `pantographsandwires:energy_wire` confirmed; `FeederWireItem` confirmed.
- CEE 1.21.1 runtime JAR inspected directly: ConnectorBlock, GroundRodBlock, HVSwitchBlock, TransformerBlock and InfrastructureSavedData confirmed.
- 36 Java files passed brace-balance static check.
- 52 JSON resources parsed successfully.
- Full Gradle compilation was NOT executed in this Linux sandbox because the supplied project contains Windows `gradlew.bat` only and the environment does not expose the user's Windows command interpreter/Gradle installation. Therefore this package is source-validated but not claimed as a successful compiled build.

## TSS feeder supply boundary (2026-09-07)
The final supply chain includes a CEE-side TSS Feeder Switch / Isolator based directly on CEE's native HV switch device. It is upstream of the OHE Feeder Bridge and is not a circuit breaker. The bridge connects that CEE supply to P&W's native `pantographsandwires:energy_wire`; P&W remains the physical feeder authority.
