FIX 13 TEST
- CEE node: exact center of the external south face for upright (FACING=UP): (0.5,0.5,1.0)
- Lower insulator: visible model from Y=-32 to Y=0 (2 blocks below the bridge block)
- Lower selection/collision shape: X/Z 3..13, Y=-32..0
- Local copy of CEE connector texture is used for the insulator.
- P&W runtime behavior is not being tested/changed in this stage.

IMPORTANT: If the bridge is placed directly on solid ground, the downward 2-block insulator is physically below the ground and will be occluded. For the visual test, place the bridge on top of the SF6 breaker/support with two blocks of clear space below it, as in the reference screenshots.
