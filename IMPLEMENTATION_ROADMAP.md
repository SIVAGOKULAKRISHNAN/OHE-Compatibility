# Implementation roadmap — v0.2

1. Native CEE switch reuse — implemented in D.
2. Native CEE transformer reuse — implemented in E/F.
3. P&W pantograph -> CEE train capture — mixin implemented.
4. P&W WireGraph -> CEE shadow catenary — implemented.
5. Shadow persistence/recovery by P&W UUID labels — implemented.
6. Direct-in-wire C1-C4 link capture — implemented at collision/link layer.
7. Neutral electrical dead-gap — implemented in shadow topology.
8. Left-feed/right-feed section controller — next live runtime layer.
9. Controlled parallel/bridge switch — next live runtime layer.
10. CEE native wire endpoint adapter — next live runtime layer for CEE->P&W mixed construction.
11. Final 3D models based on the agreed real-world equipment references.
12. Runtime regression suite against the exact supplied mod versions.
