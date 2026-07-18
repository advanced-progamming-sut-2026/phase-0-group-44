# I, Zombie

I, Zombie is the third mandatory Travel Log minigame. It uses the common
`MiniGameSession`, board, ten-ticks-per-second clock, canonical plant and zombie
data, zombie special behavior, result settlement, persistence, unlock news, and
three-level progression. Its reversed economy, plant setup, sun producers, and
brain objectives live only in the `IZombie` strategy and `IZombieState`.

## Commands

Open the Minigames Travel Log page, select an unlocked level, and start it:

```text
travel log page minigame
minigame select -n i-zombie -l <1|2|3>
minigame start
```

Commands owned by the strategy use the existing `minigame command` extension
point and the same `-t` type and `-l (x, y)` coordinate convention used by other
placement commands:

```text
minigame command show i-zombie
minigame command show brains
minigame command place zombie -t <zombie_type> -l (<x>, <y>)
minigame advance -t <count> ticks
```

Columns are zero-based. Plants occupy columns strictly left of the red line.
Zombie placement is legal at the red-line column and to its right.

## Three-level balance

| Level | Red line | Pre-placed plants | Time limit | Selectable zombies and sun prices |
|---|---:|---:|---:|---|
| 1 | 5 | 8 | 1800 ticks | Normal 50, Conehead 75, Imp 50, Buckethead 125, Prospector 100 |
| 2 | 6 | 12 | 1500 ticks | Normal 50, Conehead 75, Buckethead 125, Newspaper 125, Explorer 100 |
| 3 | 7 | 16 | 1200 ticks | Imp 50, Knight 175, All-Star 200, Parasol 100, Wizard 175 |

Every level starts at 150 sun, has exactly five choices, and retains at least one
opening purchase. Across the campaign the rosters use eleven distinct canonical
zombie types. Difficulty rises through more plants, stronger plant pools, less
placement space, a shorter time limit, and slower initial producer output.

## Sun-producing zombies

One stationary, non-selectable producer starts in every row. Its health equals
canonical Buckethead base health plus all canonical Buckethead armor health.
A killed producer is removed and never replaced.

Each living producer creates 25 sun. Its interval accelerates according to:

```text
interval(t) = max(minimumInterval,
                  baseInterval - floor(t / rampPeriod) * intervalStep)
```

The ramp period is 600 ticks and the step is 20 ticks. Level 1 starts at a
240-tick interval with a 120-tick floor; levels 2 and 3 start at 260 and 280
with floors of 130 and 140. Thus production begins slowly and increases over
time, while later levels delay the early economy.

## Brains and results

There is one brain in each of the five rows and no lawn mower. A zombie crossing
the house edge consumes that row's brain and leaves play. Eating all five wins.
The attempt loses as a dead end only when all non-replaceable producers are
dead, no player-placed zombie remains alive, and the current sun is below the
cheapest legal roster price. A surviving producer prevents premature loss
because it can still create future sun.
