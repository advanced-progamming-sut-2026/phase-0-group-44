PVZ2 Phase 2 — Big Wave Beach model-aligned visual polish

This patch supersedes:
  pvz2-phase2-big-wave-beach-spec-pass.zip

MODEL ALIGNMENT
The Phase-1 adventure chapter config defines:
  max_water_columns = 5
  water_schedule = 3;5;4;5
  low_tide = 5:1;6:3

AdventureRuleSystem:
- floods the rightmost targetColumns
- restores configured low-tide coordinates when exposed
- spawns low-tide zombies from configured low-tide coordinates when flooded

WHAT THIS PATCH FIXES
1. DEV PREVIEW now mirrors wave 1 exactly:
   - WATER = columns 6,7,8
   - maximum tide boundary = left edge of column 4 (5 possible flooded columns)
   - LOW_TIDE identities = only (5,1) and (6,3)
   - (6,3) remains visually identifiable even while flooded

2. Runtime rendering reads ChapterRules from AdventureRuntimeState:
   - max tide comes from getMaximumWaterColumns()
   - low-tide identities come from getLowTideTiles()
   - current shoreline still comes from actual WATER terrain

3. Visual semantics are now deliberately distinct:
   - CURRENT WATER / SHORELINE = aqua body + native WATER_TIDE_LINE PAM + soft surf glow
   - MAXIMUM TIDE REACH = warm amber dashed boundary + small MAX tag
   - LOW-TIDE SPAWN CELLS = compact receding-wave marks / wet-sand treatment
   - PERMANENT SEA = vertical depth bands with a few short foam strokes
     (removes the previous long horizontal artificial lines)

No GameEngine, AdventureRuleSystem, ChapterRules, terrain mutation,
plant/zombie behavior, or tide logic is changed.
Only the environment view is changed.
