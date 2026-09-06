# Fix 14 — ground-up Power Bridge assembly

This build fixes the missing-model/magenta rendering caused by negative Y model
coordinates. The Power Bridge model stays inside its own 0..16 block space.

On placement, the bridge is raised two blocks and two dedicated OHE section
insulator blocks are placed underneath it, so placing the bridge on top of the
ground/support produces a visible two-block insulator instead of burying it.

CEE external node remains at the outside center of the south/front terminal
for the upright FACING=UP configuration. P&W gameplay logic is not changed.
