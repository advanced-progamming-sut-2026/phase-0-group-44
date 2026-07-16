# PVZ2

## Name of the creators
Amirreza SeyedHossaini 404105948

Setareh Frozan  404171166

kiana Amani 404170997



## Visuals
nothing yet <.><.>


## Project status
phase 0 currently!!!

## Vasebreaker commands

Vasebreaker is selected from the Travel Log minigames page and started with the existing minigame lifecycle commands.
The mode-specific commands follow the existing `minigame <action>` convention:

- `minigame break vase <row> <column>` breaks the vase on that lawn tile.
- `minigame plant packet <packet-id> <row> <column>` immediately uses a dropped one-use packet on a lawn tile.
- `minigame show vasebreaker` shows remaining vases, dropped packets, and released threats.
- `minigame tick` advances the minigame rule strategy after the shared simulation tick.

Rows and columns are zero-based, matching the board model. Packets do not enter permanent inventory and disappear at their configured expiration tick.
