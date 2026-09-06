# Corrected test status

Fixed:
- Removed invalid placeholder recipes.
- Removed unsafe incomplete CEE HV-switch subclassing.
- Added safe OHE switch control scaffold.
- Added explicit native CEE/P&W adapter boundary.
- Added deterministic section power rules.
- Preserved native CEE/P&W ownership.

Not falsely claimed complete:
- Live CEE electrical node <-> P&W WireGraph bridge.
- Live pantograph pickup bridge.
These require an actual 1.21.1 NeoForge runtime test.
