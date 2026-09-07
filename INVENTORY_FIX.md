# Inventory fix

The screenshots showed the mod loaded successfully but none of the `ohecompat` items
appeared in creative search.

Root cause:
`ModBlocks.register()` attached `ITEMS` to the mod event bus BEFORE registering the
BlockItems. The blocks could register, but their BlockItem entries were not correctly
published to the item registry.

Fixed:
- All BlockItems are registered before `ITEMS.register(bus)`.
- Added a dedicated **CEE + P&W OHE Compatibility** creative tab containing all seven
  compatibility blocks.
- Added language names for all seven blocks.

After installing this build, restart Minecraft completely (do not rely on `/reload`),
enter the world, and open the new compatibility creative tab. The blocks should appear
there even if search indexing is delayed.
