# Mandatory minigame framework

## Source boundary

The repository UML names `VaseBreaker`, `BowlingWallnut`, and `Zombotany`.
This mandatory framework registers only **Vase Breaker** and **Bowling Wall-nut**.
`Zombotany` is not registered, and no Beghouled implementation is added.

No repository asset supplies numeric three-level tuning. The values in
`MiniGameCatalog.mandatoryDefaults()` are therefore explicit framework defaults,
kept in one replaceable catalog rather than embedded in gameplay classes. Every
level is validated as harder than the previous level by increasing objective and
threat values, reducing time, and never increasing starting resources.

## Progression and counting

- Vase Breaker level 1 is the initial mandatory minigame level.
- Winning a level for the first time unlocks the next level.
- First completion of all three Vase Breaker levels unlocks Bowling Wall-nut and
  creates one minigame-unlocked news item.
- `completedMiniGames` counts a fully cleared three-level minigame once. It does
  not count losses, individual levels, or replayed wins.
- Unlocks, completed levels, the one-time statistic marker, and news are stored
  in the persisted `User` profile.

## Runtime architecture

`MiniGameSession` uses the existing `Simulation` and `SimulationWorld`, so board,
ticks, sun/cooldowns, entities, and output messages share the normal runtime.
`MiniGame` subclasses are strategies for mode-specific setup, outcome evaluation,
and command extensions. New mode commands implement `MiniGameCommandExtension`;
the shared controller does not branch on individual minigame names.

An active attempt is process-local and belongs to one username. Persisted profile
state changes only when an attempt settles.

## Commands

Open the page first:

```text
travel log page minigame
```

Select and start an available level:

```text
minigame select -n <name> -l <1|2|3>
minigame start
```

Shared runtime commands:

```text
minigame status
minigame advance -t <count> ticks
minigame command <strategy-specific command>
minigame forfeit
```

Leaving the running minigame through `menu exit` is treated as a forfeit and
returns to Travel Log.
