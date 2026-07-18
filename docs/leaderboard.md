# Global leaderboard

The global leaderboard is a read-only snapshot of every registered local user currently loaded from `data/users.json`. It does not depend on the optional scored-game mode: a user with no score is displayed with score `0`.

## Routes

The pre-existing canonical command was already implemented in the Game menu, so that route is preserved:

```text
menu leaderboard
```

The project document also refers to leaderboard access from Main. For compatibility, the same command and sorting form are accepted from Main without introducing a second leaderboard implementation. The command is not enabled in unrelated menus.

## Sorting

The default is overall adventure progress descending:

```text
menu leaderboard
```

An explicit sort uses:

```text
menu leaderboard -s <column> -o <asc|desc>
```

Canonical column tokens:

- `username`
- `progress` (chapter first, then level)
- `minigames`
- `daily-quests`
- `non-daily-quests`
- `highest-score`

For equal primary values, usernames are always compared ascending (case-insensitive, then exact case). This secondary order stays ascending even when the primary column is descending, making every result deterministic.

## Persistence

Leaderboard rows are projected directly from persisted `User` fields:

- `latestCompletedChapter` and `latestCompletedLevel`
- `completedMiniGames`
- `completedDailyQuests`
- `completedNonDailyQuests`
- `mioPoint` / highest Mew Point

No separate leaderboard save file or duplicate cached score is maintained, so restart behavior follows the existing user-profile persistence path.
