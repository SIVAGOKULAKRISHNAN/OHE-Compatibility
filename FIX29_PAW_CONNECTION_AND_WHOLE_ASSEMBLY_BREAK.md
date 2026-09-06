# Fix 29 — P&W connection and whole-assembly breaking

## Changes
- Explicitly allows the native P&W Catenary Wire item to select the OHE Power Bridge as a connector endpoint.
- Keeps the existing centered external P&W endpoint and native P&W connector/block entity.
- Keeps `ohecompat:paw_shadow` electrical-only; the CEE visual suppression remains unchanged.
- Breaking either of the two bridge insulator blocks now finds and destroys the complete OHE Power Bridge assembly.
- Breaking the complete assembly produces the OHE Power Bridge item rather than leaving/floating an insulator.
- Bridge body texture/design and insulator appearance were not changed.

## Runtime test
1. Place the bridge.
2. On the P&W-facing/blue-indicator side, use the native P&W catenary wire on the bridge and confirm it connects to the external P&W endpoint.
3. Confirm only the native P&W wire is visible on that span; no CEE shadow wire appears.
4. Break the lower insulator with the normal break action: the bridge, upper insulator, and lower insulator must all disappear and one OHE Power Bridge item must drop.
5. Repeat by breaking the upper insulator.
6. Repeat by breaking the bridge body.
