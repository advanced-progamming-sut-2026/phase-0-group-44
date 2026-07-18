# Bonus plants and zombies

Canonical sources:

- `phase1/assets/Data/plants.csv` with bonus classification from
  `phase1/assets/Data/bonus-plants.txt`
- `phase1/assets/Data/zombies.csv` with bonus classification from
  `phase1/assets/Data/bonus-zombies.txt`

The rows remain classified as bonus. This implementation enables their
factories, collection/selection access, chapter-aware spawning, detailed combat
behaviors, and the command-simulation state transitions that apply there.

## Plants

| Plant | Runtime behavior |
|---|---|
| Pea Pod | Up to five heads stack on one tile; each head fires, and plant food fires one 20x pea per head. |
| Caulipower | Every canonical action interval, launches a board-wide homing hypnosis projectile at a random hostile zombie. |
| Electric Blueberry | Launches a board-wide homing 5000-damage lightning strike; plant food strikes up to three random zombies. |
| Starfruit | Fires in five represented directions, including backward lanes; plant food repeats the five-way burst. |
| Goo Peashooter | Direct poison projectile bypasses armor and applies timed poison damage. |
| Grapeshot | Instant 3x3 explosion plus eight bouncing grapes; the 30-tile range equals five seconds at projectile speed 6. |
| Chomper | Instantly swallows one contact zombie, then digests for 40 seconds; plant food swallows up to three random zombies. |
| Wasabi Whip | Every two seconds damages zombies one tile ahead and behind with fire; plant food hits a 3x3 area. |
| Kiwibeast | Grows at 24 and 72 seconds and deals 15/30/45 area damage by stage. |
| Sweet Potato | Pulls hostile zombies from adjacent rows into its row; plant food attracts a wider area and fully heals it. |
| Hypno-shroom | Hypnotizes the eating zombie; plant food replaces the nearest hostile with an allied Gargantuar. |
| Cat-tail | Fires homing shots at the nearest hostile anywhere on the board; plant food fires a barrage. |

## Zombies

| Zombie | Runtime behavior |
|---|---|
| Arcade Zombie | Pushes arcade-machine armor; while the machine survives, a collision destroys the blocking plant. |
| Troglobite | Pushes ground-ice armor with the same collision-crush rule. |
| Fisherman | Remains at the right edge and every five seconds pulls the nearest row plant one tile right, destroying an adjacent catch. |
| Jester / Juggler | Reflects direct projectiles into a plant and enters a faster 1.5-second spinning state; lobbed shots are not reflected. |
| Wizard | Every five seconds transforms a random plant into an inactive, uneatable cat; only that Wizard's death restores it. |
| King | Remains at the right edge and every five seconds gives a nearby Normal zombie Knight helmet and shoulder armor. |

## Integration rules

- `PlantFactory` and `ZombieBehaviorFactory` cover every canonical row.
- Bonus plants may be purchased, selected, upgraded, used in the greenhouse,
  and selected by shop packet rewards under the same rules as other unlocked
  plants.
- Bonus zombies may be spawned by the cheat command only when COMMON or allowed
  in the current chapter. The chapter-filtered wave source may also compose
  them after they are enabled.
- Invalid purchases, selections, placements, and spawns remain atomic and do
  not mutate balances or board state.
