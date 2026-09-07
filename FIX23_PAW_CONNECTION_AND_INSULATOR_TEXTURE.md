# Fix 23 — P&W endpoint + insulator texture

- CEE-facing bridge design is unchanged.
- P&W-facing bridge model/design is unchanged.
- P&W `BlockConnectorNodeData` now stores the node at the bridge's external P&W attachment point instead of the block origin. This keeps the endpoint outside/centered and lets the native P&W wire render to the visible connector.
- CEE-holder handling remains unchanged.
- Insulator texture updated only: white/gray ceramic ribs with copper separators, based on the supplied reference.
- Bridge body/top/front/CEE/P&W textures are not replaced.
- Existing wrench and two-block assembly behavior retained.
