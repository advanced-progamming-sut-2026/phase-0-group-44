# Canonical zombie data

`zombies.csv` is the normalized Phase-1 zombie registry. Base statistics were
transcribed from `src/assets/zombies.json`; armor values and special behavior
come from the Phase-1 document.

The original PDF uses blue/cyan zombie names to mark bonus rows. Because CSV
cannot preserve text color, `bonus-zombies.txt` records the six visually blue
rows. Runtime loading fails when that classification references an unknown row.
Every other CSV row is mandatory.

Legacy enum-only tokens `POLE_VAULTING`, `SCREEN_DOOR`, `DANCING`, and
`BALLOON` have no row in either the supplied PDF zombie list or
`src/assets/zombies.json`. They remain parse/save compatibility constants only;
they are not registry entries and cannot be spawned. `BASIC`, `FOOTBALL`, and
`NEWSPAPER` are compatibility aliases for Normal, All-Star, and Newspaper
Zombie respectively.
