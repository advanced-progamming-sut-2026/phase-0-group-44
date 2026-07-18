# Beghouled

Beghouled has three progressively harder configurations. All use a random,
deterministic-under-seed 5-type plant board. Levels require 12, 18, and 24
matches while zombie intervals fall from 120 to 90 to 60 ticks and time limits
fall from 2400 to 2100 to 1800 ticks.

Only orthogonally adjacent swaps that create a horizontal or vertical match of
at least three are accepted. Matched plants disappear, survivors fall, and new
plants refill from above. If no legal match-producing swap remains, the board is
regenerated. Tiles where zombies eat plants become permanent craters.

A size-3/4/5+ group grants 1/2/3 units of 50 sun. Every cascade group caused
after the initiating move receives one extra unit. Zombie spawning is endless
until the configured match target is reached; then spawning stops and the
remaining zombies must be defeated.

Commands:

```text
minigame command show beghouled
minigame command swap plants -l (<x1>, <y1>) -l (<x2>, <y2>)
minigame command upgrade plant -t <plant_type>
```

Configured conversions are Peashooter→Repeater→Mega Gatling Pea,
Wall-nut→Tall-nut, Puff-shroom→Fume-shroom, and
Cabbage-pult→Melon-pult→Winter Melon, using the supplied costs.
