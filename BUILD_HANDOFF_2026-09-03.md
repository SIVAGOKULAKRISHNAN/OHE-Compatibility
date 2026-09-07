# OHE Compatibility – Fix 29 Handoff

Baseline:
- Fix 29: P&W exact endpoint connection work + whole 3-block assembly break behavior.
- Internal block ID: `ohecompat:ohe_power_bridge`
- Player-facing name: OHE Power Bridge.

Important implemented behavior:
- P&W native connector is separate from the CEE node.
- No proximity/nearest-node linking.
- P&W wire rendering remains native P&W rendering.
- CEE shadow/compatibility rendering is kept separate from P&W physical connectivity.
- Breaking the bridge or either insulator is intended to remove the complete 3-block assembly and drop one OHE Power Bridge item.
- C1/C2/C3/C4/D and existing CEE components are not duplicated.

Build recovery status as of 2026-09-03:
- Gradle 8.13: available/uploaded.
- Java 21: required.
- ModDev 2.0.107: identified and locally supplied.
- NeoForm Runtime 1.0.40: supplied.
- NeoForge 21.1.219: supplied.
- picocli 4.7.6: supplied.
- The remaining build blocker was Gradle/NeoForge/NeoForm module-variant metadata in the offline local Maven setup.
- Do NOT treat the build as successful until a clean `build` task completes.

Files in this handoff:
- Full Fix 29 source/project.
- Latest design, implementation, build, and validation notes.
- Final diagram.
- This handoff note.

Next chat:
1. Use this ZIP as the Fix 29 baseline.
2. Continue repairing the offline Maven/Gradle metadata setup.
3. Run Gradle 8.13.
4. Only after a successful build, proceed to runtime testing of the P&W connection and whole-assembly breaking behavior.
