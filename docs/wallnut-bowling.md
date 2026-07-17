# Wall-nut Bowling

Wall-nut Bowling is the second mandatory Travel Log minigame. It reuses the
common simulation clock, board coordinates, canonical zombie registry, zombie
movement/abilities, lawn mowers, brain-loss result, persistence, and minigame
progression service. Its conveyor, red-line placement rule, moving bowling
plants, and collision rules are isolated in the Wall-nut Bowling strategy.

## Commands

Open and start the minigame from the Travel Log as usual:

```text
travel log page minigame
minigame select -n bowling-wallnut -l <1|2|3>
minigame start
```

During the attempt:

```text
minigame command show conveyor
minigame command plant conveyor -i <packet_id> -l (<x>, <y>)
minigame advance -t <count> ticks
minigame status
minigame forfeit
```

`plant conveyor` follows the existing action/noun, `-i`, and `-l (x, y)` command
conventions. There is no plant-selection phase, normal seed packet, or sun cost.
A packet is consumed only after its location passes validation.

Coordinates use the shared board convention: `x` is the column from the house
edge and `y` is the row. A level's red-line column is inclusive, so a red line at
column 2 permits launch tiles in columns 0, 1, and 2 only.

## Explicit level configuration

| Level | Zombies | Initial conveyor plants | Conveyor interval | Zombie interval | Red line | Time limit |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 8 | 3 | 30 ticks | 90 ticks | column 2 | 1600 ticks |
| 2 | 12 | 2 | 36 ticks | 70 ticks | column 2 | 1300 ticks |
| 3 | 16 | 1 | 42 ticks | 50 ticks | column 1 | 1000 ticks |

Later levels add armored and Gargantuar threats, deliver zombies faster, deliver
bowling plants more slowly, start with fewer conveyor choices, and eventually
move the red line closer to the house. Sky sun is disabled and starting sun is
zero in every level.

## Bowling plants

- **Bowling Wall-nut** deals damage equal to the canonical Normal Zombie health.
  Its first zombie collision changes a straight heading by 45 degrees. Each
  later zombie or top/bottom boundary collision reverses its diagonal component,
  a 90-degree heading change. It continues after every hit.
- **Explode-o-nut** travels straight. Its first zombie collision consumes it and
  applies the canonical Cherry Bomb damage to zombies in the surrounding 3x3
  tile area.
- **Giant Wall-nut** travels straight, kills every zombie it contacts regardless
  of armor or health, and continues moving.

The canonical values are resolved from `zombies.csv` and `plants.csv` through the
existing registries; the minigame does not duplicate numeric damage constants.

## Specification inconsistency

The source text says there are "two special plants" but explicitly describes
three plant types: Bowling Wall-nut, Explode-o-nut, and Giant Wall-nut. This
implementation follows the three explicit descriptions, as required, and does
not add a fourth inferred type.
