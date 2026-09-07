# Final scope

## Existing and untouched
A. CEE OHE — use CEE's existing wire/catenary/pantograph implementation.
B. P&W OHE — use P&W's existing wire/tension/cantilever/pantograph implementation.
E. CEE Substation — use the existing CEE transformer/substation electrical behavior; our compact version is only a smaller railway-oriented arrangement.

## New compatibility equipment
C1. Cantilever-mounted section/neutral assembly.
C2. Direct-in-wire section/neutral assembly.
C3. Short PTFE-style direct-in-wire neutral section.
C4. Combined cantilever-mounted short PTFE neutral section.
D. OHE sectioning switch: CEE HV switch behavior is the foundation; compatible CEE/P&W pole/support mounting.
F. OHE booster/autotransformer: new component, using CEE transformer concepts to transfer supply from an energized OHE/feeder section to the next section.

## Non-negotiable behavior
- Existing CEE OHE path is not rewritten.
- Existing P&W wire/tension path is not rewritten.
- The compatibility mod adds extra paths.
- CEE remains the authority for train electrical behavior.
- P&W remains the authority for physical wire geometry/tension/pantograph contact.
- No artificial 40/50 km distance limit.
- Section/neutral hardware can electrically separate sections while preserving pantograph passage.
- Direct-in-wire and cantilever-mounted section hardware are both supported.
- CEE/P&W mixed infrastructure is supported through adapters.
