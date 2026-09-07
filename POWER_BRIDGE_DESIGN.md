# First in-game component — OHE Power Bridge

This is the first component to test before implementing the remaining physical OHE equipment.

## Visual design
- 1x1x1 Minecraft block footprint.
- Compact dark metal enclosure with copper structural corners/rails.
- Three insulated top terminals to visually communicate the CEE-side electrical apparatus.
- Front service panel and warning detail.
- No visible wire leaves the block toward the P&W catenary.

## Electrical ownership
- The block extends CEE `ConnectorBlock`, so its electrical node remains a native CEE connector node.
- The compatibility layer only adds the invisible CEE shadow connection to a nearby P&W shadow catenary node.
- CEE remains authoritative for electrical simulation and train power.
- P&W remains authoritative for catenary geometry, tension and pantograph contact.

## CEE issues explicitly avoided
1. Do not replace CEE's electrical network with a second energy system.
2. Do not replace native CEE catenary connections.
3. Do not render a second CEE wire beside P&W OHE.
4. Do not hard-code a 40/50 km electrical range.
5. Keep native CEE train electrical behavior unchanged.
6. Keep the real P&W wire/cantilever/pantograph path unchanged.
7. Use the actual P&W attachment point for shadow-node alignment; never use a guessed block-center offset.
8. The bridge is only a power hand-off point, not a new pantograph or motor system.

## First test
1. Start the exact target runtime: Minecraft 1.21.1, NeoForge 21.1.219, Create 6.0.10-280, CEE 1.1.1 and P&W beta-0.2.3-C6.
2. Place a native CEE power source/substation.
3. Place the OHE Power Bridge.
4. Connect a native CEE feeder wire to the bridge.
5. Build a native P&W catenary span with a real connected P&W endpoint close to the bridge.
6. Confirm no ordinary CEE wire is visually rendered between bridge and P&W OHE.
7. Put a P&W pantograph under the P&W wire.
8. Verify CEE train electrical power reaches the CEE traction motor.
9. Break/remove the P&W endpoint and confirm the compatibility link disappears without breaking the native CEE network.
10. Restart the world and repeat the connection test to validate shadow-node persistence/recovery.
