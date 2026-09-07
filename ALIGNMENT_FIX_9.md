# Alignment Fix 9 — CEE holder / P&W endpoint compatibility

- CEE railway catenary holders now expose a native P&W wire connector endpoint.
- P&W endpoint coordinates use CEE's actual catenary node position, not the block bottom/center.
- P&W catenary wires can therefore terminate on CEE catenary holders.
- The compatibility manager bridges that P&W endpoint to CEE node #0 with the existing invisible `ohecompat:paw_shadow` electrical wire type.
- CEE-only catenary connections remain electrically native.
- CEE catenary rendering is aligned to the real holder node and made much thinner to visually match the P&W OHE scale.
- If the same two CEE holders are also connected by a P&W edge, the duplicate CEE visual is suppressed so only the P&W OHE wire is shown.
- No changes are made to normal CEE electrical wire connections outside railway catenary holders.
