PVZ2 Phase 2 — Frostbite natural visual polish

Apply AFTER:
  pvz2-phase2-frostbite-ground-and-ice-polish.zip

WHY
The previous screenshot showed correct Frostbite state positions, but:
- slippery tiles were too faint / icon-like
- the frozen-zombie state appeared as a large cyan rectangular placeholder

THIS PATCH
1) Slippery ground
- replaces the icon-like treatment with transparent irregular icy ground overlays
- UP/DOWN direction remains readable through low-profile chevrons
- preserves the normal Frostbite tile underneath
- no boxes / selection frames / text labels

2) Frozen zombie
- uses an intact ice-block sprite cropped directly from the supplied
  FROSTBITEICEBLOCKZOMBIEGROUP_768_00.PNG atlas
- entity-sized rather than cell-sized
- fully communicates "zombie is encased in ice" without needing to render
  the zombie inside, exactly as allowed by Phase 2
- removes the giant rectangular cyan placeholder look

3) Unchanged
- slippery coordinates
- frozen-zombie position/state
- icy-wind row logic and animation
- frozen-plant front/behind shell logic
- GameEngine / Phase-1 mechanics
- Beach / Egypt / Dark Ages behavior

FILES
src/screen/GameplayScreen.java
src/screen/gameplay/BattlefieldEnvironmentLayer.java
src/screen/gameplay/BattlefieldFrostbiteStateLayer.java
src/assets/ui/gameplay/frostbite/slippery_up.png
src/assets/ui/gameplay/frostbite/slippery_down.png
src/assets/ui/gameplay/frostbite/frozen_zombie_block.png
