# Fix 27 — Migrate Saved Shadow Wires

Fix 26 prevented new `paw_shadow` compatibility links from rendering as CEE
catenaries. Existing worlds could still have the same link saved with CEE's
standard (visible) wire type from an earlier fix.

Fix 27 keeps every matching electrical connection, but replaces its saved wire
data with `ohecompat:paw_shadow` during the normal synchronization tick. The
client-side Fix 26 visual filter can then hide it correctly.

No bridge geometry, textures, insulator blocks, native CEE wires, native P&W
wires, or endpoint matching logic was changed.

After installing, open the existing test world once so the migration runs, then
restart the client or reload the world before judging the catenary visual.
