PVZ2 Phase 2 - Big Wave Beach specification pass

Apply after your current battlefield/UI patches.

This is the FIRST world-specific mechanics pass, following the agreed order.

BIG WAVE BEACH:
- Permanent ocean body is visible on the RIGHT side of the screen.
- Current WATER coverage is rendered exactly cell-by-cell from GameMap terrain.
- Existing runtime water-level changes remain model-driven.
- Current shoreline still uses the supplied WATER_TIDE_LINE PAM.
- NEW fixed MAX TIDE marker reads ChapterRules.getMaximumWaterColumns().
- LOW_TIDE cells now have a separate, clearly visible terrain treatment.
- DEV PREVIEW uses current water on the right 3 columns and max tide at 4 columns.

No GameEngine / AdventureRuleSystem / GameMap / Tile logic is changed.
No plant or zombie renderer is changed.

Only file:
src/screen/gameplay/BattlefieldEnvironmentLayer.java
