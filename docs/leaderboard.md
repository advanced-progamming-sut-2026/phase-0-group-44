# Leaderboard route and ordering

The existing canonical command `menu leaderboard` was already implemented in the Game menu, so it remains available there. Because the specification also refers to leaderboard access from Main, the same command is accepted directly in the Main menu as a compatibility route. No separate leaderboard menu is introduced.

Optional sorting syntax:

```
menu leaderboard -s <progress|minigames|daily-quests|non-daily-quests|highest-score> -o <asc|desc>
```

The default is progress descending. Every sort uses username (case-insensitive, then exact spelling) as a stable deterministic tie-breaker. A missing scored-game score is represented by the persisted default value `0`; no scored-game implementation is required to list that user.
