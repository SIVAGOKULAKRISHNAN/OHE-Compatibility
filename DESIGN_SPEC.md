# Final OHE Design Specification

## OHE switch
- CEE HV-switch behavior is the electrical foundation.
- Real railway isolator appearance.
- Bottom operating mechanism.
- Create wrench interaction.
- Two OHE-side electrical endpoints.
- Open/closed/controlled-parallel operation.

## Section/neutral assembly
- Real short-neutral/section-insulator visual reference.
- Can be placed directly in a wire span, not only on a cantilever.
- Can also be used with support/cantilever arrangements.
- Accepts CEE and P&W wires in all four combinations.
- Physical pantograph path remains continuous.
- Electrical networks can be separated.
- Controlled left-feed/right-feed/parallel behavior is allowed only when valid.

## Compact OHE substation
- Use CEE transformer/electrical concepts.
- Compact physical railway installation.
- Supplies a local OHE section.
- No hard-coded 40–50 km distance.
- Optional in the mod's power topology.

## OHE booster/autotransformer
- Optional additional power component.
- Uses CEE transformer concepts.
- Takes power from an existing OHE/feeder arrangement and transfers it onward according to the selected railway topology.
- Never behaves as a magic voltage multiplier.

## Native ownership
CEE: electrical simulation + train behavior.
P&W: wire graph + geometry + tension + pantograph contact.
Compatibility mod: translation/bridging only.
