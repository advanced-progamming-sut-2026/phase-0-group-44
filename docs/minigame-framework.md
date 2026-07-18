# Mandatory minigame framework

The mandatory Travel Log campaign contains five three-level strategy-backed
minigames in this order: Vase Breaker, Bowling Wall-nut, I, Zombie, Beghouled,
and Zombotany. Winning a level for the first time unlocks the next level;
clearing level 3 counts that minigame once and unlocks the next minigame with a
news item. Replays and losses never duplicate progress or statistics.

Every attempt uses `MiniGameSession`, `Simulation`, and `SimulationWorld`.
Mode-specific setup, outcome rules, state, and commands stay in `MiniGame`
strategies and `MiniGameCommandExtension` implementations rather than shared
mode conditionals. Selection-time extensions are explicitly marked as available
before start, which Zombotany uses for plant selection.

Open and run minigames with:

```text
travel log page minigame
minigame select -n <name> -l <1|2|3>
minigame command <pre-start strategy command>
minigame start
minigame status
minigame advance -t <count> ticks
minigame command <running strategy command>
minigame forfeit
```
