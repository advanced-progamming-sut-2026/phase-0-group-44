# Quest canonical-data report

Canonical source located: `Archive(4).zip/Quests.csv` (19 non-empty quest rows), with a matching `quests.xlsx`.
The CSV is copied verbatim to `src/assets/quests.csv` and is the runtime source of quest definitions.

The canonical table does **not** contain:

1. Quest IDs. The framework therefore persists the exact canonical quest name as the key; it does not fabricate IDs.
2. A fourth category's quest rows. The rows contain only Main, Daily, and Epic categories. Travel Log still accepts the required `minigame` page, which remains empty until canonical rows exist.
3. A daily reset clock/time zone. Daily state resets on the first access/event observed on a different local date supplied by the injected `Clock`.
4. Concrete unlock targets for plant or level rewards. The table has one random-plant reward but names no eligible pool, and no row names a chapter/level transition. The reward architecture supports both; level unlocks reject missing `chapter:level` data rather than guessing.
5. Instantiated values for template variables such as `chapter`, `plant`, `family_type`, `n`, and `sun_amount`. `QuestService.activate` requires the caller/content scheduler to supply the selected target value. No variable choice is invented.
6. A canonical recurrence policy for Main or Epic quests. They therefore do not reset automatically.

Gameplay code should publish domain events through `QuestEventBus`. For conditions whose full context is evaluated by a gameplay/session summary component, publish the exact canonical quest name in the event's `quest` attribute. This keeps individual plants, zombies, shops, levels, and minigames independent of the quest catalog.
