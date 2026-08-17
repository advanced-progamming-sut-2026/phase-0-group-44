PVZ2 Phase 2 — Frostbite Caves model-aligned visual pass

Apply AFTER the current Big Wave Beach strong-water patch and your current HUD/pause patches.

PHASE-1 MODEL VALUES (phase1/assets/Data/adventure-chapters.csv)
- icy_wind_rows = 2
- slippery_up = 4:1
- slippery_down = 5:3
- frozen_zombies = NORMAL@7.5:2

WHAT THIS PATCH DOES
1) ICY WIND
- Existing BattlefieldChapterEffects remains the source of the animated wind event.
- Real gameplay reads the exact rows from the engine event:
    Icy wind affected rows [...].
- DEV PREVIEW still shows exactly two affected rows when E is pressed.

2) SLIPPERY / SLIDER TILES
- Corrects DEV PREVIEW to the actual model coordinates:
    UP   -> column 4, row 1
    DOWN -> column 5, row 3
- Keeps the supplied native TILESLIDER_ICEAGE_UP/DOWN PAM art.
- Adds a restrained frosted rim + short directional chevrons so the mechanic is
  visible even over the already-blue Frostbite background.

3) FROZEN PLANTS
- The Phase-1 GameEngine sets FROZEN=true after 3 ICE_HITS and places an ICE
  obstacle with payload FROZEN_PLANT.
- EnvironmentLayer now renders the supplied PLANT_BEHIND ice PAM underneath
  the future plant actor.
- BattlefieldFrostbiteStateLayer renders the supplied plant ice FRONT PAM above
  the future plant actor.
- Result: the plant remains visible INSIDE the ice block, as Phase 2 requires.
- ICE_HITS 1/2 can show a small supplied chill cue before the full freeze.

4) FROZEN ZOMBIES
- Any zombie for which GameEngine reports zombie.isFrozen() receives the supplied
  full FROSTBITE_ICE_BLOCK_ZOMBIE PAM ABOVE the zombie entity layer.
- This intentionally hides the zombie body, matching Phase 2: showing only the
  ice block is sufficient.
- DEV PREVIEW places the configured frozen NORMAL zombie visual at x=7.5,row=2.

IMPORTANT MODEL NOTE
The currently active GameEngine preplaces the configured frozen zombie with
applyFreeze(Double.MAX_VALUE). It does NOT expose a separate 600-HP encasing-ice
health value to this GUI path. Therefore this patch does not invent or change
combat logic: the ice block remains while zombie.isFrozen() is true and disappears
when the model thaws it. The newer simulation-side classes do contain an explicit
encased-in-ice state, but GameplayScreen currently runs through GameEngine.

NO MODEL / COMBAT LOGIC CHANGED.
No plant or zombie teammate renderer is overwritten.
The new Frostbite state layer is designed to sit around their future actors:
  environment behind ice -> entityLayer -> front ice state -> icy wind -> HUD

FILES
- src/screen/GameplayScreen.java
- src/screen/gameplay/BattlefieldEnvironmentLayer.java
- src/screen/gameplay/BattlefieldFrostbiteStateLayer.java
