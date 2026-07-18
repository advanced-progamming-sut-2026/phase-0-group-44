# Daily scored game

The scored game reuses the normal 5x9 board, simulation tick, sky-sun, planting,
cooldowns, canonical plant/zombie data, zombie abilities, lawn mowers, and brain
loss. Its primary result is the final **Mew Point** score.

## Daily fairness

`DailyZombieSequenceGenerator` hashes `scored-game-v1|YYYY-MM-DD` with SHA-256
and uses the first eight bytes as the deterministic random seed. Usernames are
not part of the seed, so every local account receives the same 30 zombie types,
rows, and spawn ticks for a given date. Changing the rules requires changing the
rules-version string, making generator changes explicit and reproducible.

## Five scoring patterns

1. Quick kill: 300 points when a zombie dies within 30 ticks of spawning.
2. Multi-kill projectile: 200 points for every kill after the first by one shot.
3. Simultaneous kills: 150 points per zombie when at least two die on one tick.
4. Sun efficiency: on victory, one point per remaining sun.
5. Perfect defense: 1000 points on victory with no lost plant and no used mower.

Finalization is idempotent. Only a score strictly greater than the persisted
profile high score replaces it; an unset score remains zero.

## Routes and commands

The project previously documented scored game from both Main and Game. Both are
supported by the same route:

```text
menu scored-game
menu enter scored-game
```

Inside the menu:

```text
scored game start
scored game status
scored game plant -t <plant_type> -l (<x>, <y>)
scored game collect sun -l (<x>, <y>)
scored game advance -t <count> ticks
scored game forfeit
```
