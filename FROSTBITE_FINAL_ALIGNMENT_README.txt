PVZ2 Phase 2 - Frostbite final alignment polish

Apply after the Frostbite natural visual polish + Scaling import fix.

Fixes:
- Corrects frozen-zombie visual x mapping:
  model x=7.5 is centered in column 7, so the overlay no longer shifts it half a cell right.
- Frozen zombie ice is broader and shorter, reading as an ice block rather than a tall shard.
- Ice block sits closer to the tile and gets a slightly stronger cold contact glow.
- Slippery-ground artwork is only slightly stronger/brighter while remaining embedded in the tile.

No Phase-1 mechanics or state logic are changed.
No plant/zombie renderer files are overwritten.

Files:
- src/screen/gameplay/BattlefieldFrostbiteStateLayer.java
- src/screen/gameplay/BattlefieldEnvironmentLayer.java
