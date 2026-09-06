# Orientation Audit

The bridge treats `FACING` as the CEE-facing side and derives the P&W side as the exact opposite face.

- NORTH: CEE `(0.5,0.5,0.0)` / P&W `(0.5,0.5,1.0)`
- EAST: CEE `(1.0,0.5,0.5)` / P&W `(0.0,0.5,0.5)`
- SOUTH: CEE `(0.5,0.5,1.0)` / P&W `(0.5,0.5,0.0)`
- WEST: CEE `(0.0,0.5,0.5)` / P&W `(1.0,0.5,0.5)`

Placement uses the player's horizontal facing direction. The two insulator blocks receive the same FACING state.
