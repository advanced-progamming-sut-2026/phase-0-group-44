# Concise Phase-1 reviewer guide

Run from the repository root.

## Verification

```bash
gradle clean test checkstyleMain checkstyleTest pmdMain pmdTest build
gradle run
```

The audit did not create a commit, push, or Phase-1 tag.

## 1. Register and enter Adventure

```text
register -u reviewer -p Abcdef1! Abcdef1! -n Reviewer -e reviewer@example.com -g male
pick question -q 1 -a answer -c answer
login -u reviewer -p Abcdef1!
menu enter game
menu enter chapter -c Ancient Egypt
```

A new user owns three plants while a normal level has eight slots, so the level
starts automatically with Sunflower, Peashooter, and Wall-nut.

Useful gameplay commands:

```text
show map
show plants status
show sun amount
plant plant -t peashooter -l (2, 2)
advance time -t 10 ticks
zombies info
```

## 2. Collection, greenhouse, and shop

```text
menu exit
menu enter collection
menu collection show-plants
menu collection show-all-plants
menu exit
menu greenhouse
show greenhouse
plant pot at (1, 1)
enter shop
shop list
shop daily
```

## 3. Travel Log and quests

From Game:

```text
menu travel-log
travel log page main
travel log page epic
travel log page daily
travel log page minigame
```

Unknown pages are rejected without changing the current page.

## 4. Mandatory minigames

```text
travel log page minigame
minigame select -n vase-breaker -l 1
minigame start
minigame command show vases

minigame select -n bowling-wallnut -l 1
minigame start
minigame command show conveyor

minigame select -n i-zombie -l 1
minigame start
minigame command show i-zombie
```

Each has three explicit configurations. Later levels unlock only after a valid
first completion of the previous level.

## 5. Leaderboard

From Main or Game:

```text
menu leaderboard
menu leaderboard -s progress -o desc
menu leaderboard -s minigames -o asc
menu leaderboard -s daily-quests -o desc
menu leaderboard -s non-daily-quests -o asc
menu leaderboard -s highest-score -o desc
menu leaderboard -s username -o asc
```

All registered local profiles are included; ties use username ascending.

## 6. Bonus demonstrations

```text
menu scored-game
scored game start
scored game status

menu travel-log
travel log page minigame
minigame select -n beghouled -l 1
minigame select -n zombotany -l 1
```

Blue-row plant demonstration after adding coins and entering Collection:

```text
menu cheat add 2000 coin
menu enter collection
menu collection purchase-plant -p pea-pod
menu collection show-plant -p pea-pod
```

Blue-row zombie demonstration from the matching Adventure chapter:

```text
cheat spawn-zombie -t arcade-zombie -l (8, 2)
zombies info
```

Beghouled, Zombotany, the scored game, SHA-256 password storage, all 12 blue-row
plants, and all 6 blue-row zombies are bonus scope and remain marked as such in
canonical metadata and the audit matrix.

## 7. Persistence check

Exit normally, restart, and log in again:

```text
menu logout
menu exit
```

Account data, unlocks, collection, currencies, settings, news, greenhouse,
shop daily state, quest progress, minigame progress, and high score reload from
`data/users.json`.
