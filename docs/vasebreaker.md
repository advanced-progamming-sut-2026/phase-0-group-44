# Vasebreaker: three mandatory levels

Vasebreaker remains a strategy inside the mandatory minigame framework. It uses
`Simulation`, `SimulationWorld`, the standard board, the shared tick clock,
canonical plant/zombie data, `ZombieSpecialSystem`, and `ZombieCombatSystem`.
Vase layout, temporary packets, and their commands are isolated in
`VaseBreakerState`; other gameplay classes do not branch on the minigame mode.

## Levels

| Level | Vases | Empty | Normal zombie | Normal packet | Plant Vase | Gargantuar Vase | Packet lifetime |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 8 | 2 | 2 | 3 | 1 | 0 | 300 ticks |
| 2 | 12 | 2 | 4 | 3 | 2 | 1 | 240 ticks |
| 3 | 16 | 1 | 6 | 5 | 2 | 2 | 180 ticks |

Higher levels add more vases, increase a weighted threat score, widen the normal
zombie pool, add more Gargantuars, and shorten the packet deadline. The normal
vase outcomes remain exactly empty, one zombie, or one temporary seed packet.
A Plant Vase always creates a randomly selected packet from that level's
configured plant pool. A Gargantuar Vase always releases Gargantuar.

Vasebreaker starts with zero sun and sky sun is disabled. The player has no
plant-selection phase. A temporary packet must be collected and then planted;
its expiration timer continues while collected. It is consumed after one valid
planting and cannot be reused.

## Commands

The existing minigame command-extension envelope is retained:

```text
minigame command break vase -l (<x>, <y>)
minigame command collect packet -i <packet_id>
minigame command plant packet -i <packet_id> -l (<x>, <y>)
minigame command show vases
```

Coordinates follow the same `(x, y)` convention as board planting and sun
collection. `show vases` reports broken vases, live threats, active packets, and
the shared simulation tick.

## Results

The player wins only when every vase is broken and no released zombie remains.
Packets and surviving plants are not threats and do not block a win. Zombies use
the standard movement, eating, lawn-mower, and brain-loss behavior. A first
valid win is settled by the existing minigame service, which persists level
completion and unlocks the next level without double-counting replays.
