# Phase-1 bonus scope

The following features are implemented but are **not counted as mandatory
Phase-1 coverage**:

1. SHA-256 password hashing and safe migration of compatible plaintext saves.
2. Daily scored game with five score components, deterministic date/rules seed,
   and persisted personal high score.
3. Beghouled with three levels, match/cascade economy, craters, and upgrades.
4. Zombotany with three levels and Peashooter, Wall-nut, Jalapeno, and Squash
   zombie traits.
5. All 12 blue-row plants in `bonus-plants.txt`, with canonical data-driven
   statistics and dedicated runtime strategies.
6. All 6 blue-row zombies in `bonus-zombies.txt`, with detailed-engine and
   command-simulation behavior support.

The bonus classification remains represented in canonical metadata; implemented
bonus content is not reclassified as mandatory. `MiniGameId.BEGHOULED` and
`MiniGameId.ZOMBOTANY` remain bonus, while Vase Breaker, Bowling Wall-nut, and
I, Zombie remain mandatory.

## Remaining intentionally unimplemented bonus items

None from the supplied Phase-1 document. Boss gameplay is not bonus scope and
remains deferred to Phase 2 exactly as required.

See [`../bonus-plants-and-zombies.md`](../bonus-plants-and-zombies.md) for the
blue-row behavior mapping and documented configuration choices.
