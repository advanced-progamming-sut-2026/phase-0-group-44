# Zombotany

Zombotany is a normal-style three-level defense with plant selection, sky sun,
canonical planting/cooldowns, finite progressively harder zombie waves, lawn
mowers, and normal win/loss rules. Levels contain 8, 12, and 16 zombies; spawn
intervals decrease from 120 to 90 to 60 ticks.

The four normal-zombie variants are strategy-owned overrides:

- Peashooter Zombie fires canonical-damage peas left toward plants.
- Wall-nut Zombie uses canonical Wall-nut health and normal-zombie speed/eating.
- Jalapeño Zombie destroys every plant in its row after surviving 100 ticks.
- Squash Zombie moves 3.5 times normal speed and destroys itself plus the first
  plant it reaches.

Commands before start:

```text
minigame command show available plants
minigame command show selected plants
minigame command add plant -t <plant_type>
minigame command remove plant -t <plant_type>
```

Commands after start:

```text
minigame command show zombotany
minigame command plant plant -t <plant_type> -l (<x>, <y>)
minigame command collect sun -l (<x>, <y>)
```
