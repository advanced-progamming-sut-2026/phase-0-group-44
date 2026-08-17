PVZ2 Phase 2 — Frostbite ground + frozen-block polish

Apply after:
  pvz2-phase2-frostbite-model-aligned-visual-pass.zip

WHY
The first Frostbite pass was model-aligned, but the screenshot showed:
- slippery tiles looked like large placed icons instead of slippery ground
- the configured frozen-zombie block was not visually obvious in DEV preview

CHANGES
- SLIPPERY_UP / SLIPPERY_DOWN now read as a whole icy/slippery TILE.
- Native slider PAM is retained only as a small center directional glyph.
- Removes the heavy rectangular rim around slippery cells.
- Adds subtle frost edges, ice scratches, and restrained directional chevrons.
- Frozen zombie gets a guaranteed opaque turquoise ice core plus native ice-block PAM.
- Frozen zombie block is sized to one lane/cell area and hides the zombie underneath.
- Plant frozen-shell logic is unchanged.
- Icy-wind row logic is unchanged.

MODEL LOGIC UNCHANGED
- slippery up: (4,1)
- slippery down: (5,3)
- preview frozen zombie: NORMAL @ x=7.5,row=2
- wind rows still come from the existing engine/chapter effect state

Files changed:
  src/screen/gameplay/BattlefieldEnvironmentLayer.java
  src/screen/gameplay/BattlefieldFrostbiteStateLayer.java
