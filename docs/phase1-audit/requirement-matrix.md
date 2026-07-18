# Phase-1 requirement-to-code matrix

Source of truth: `project.pdf` (59 pages) plus repository assets under
`phase1/assets/Data`. `PASS` means the mandatory Phase-1 behavior is present and
covered by an automated test. `BONUS` means implemented but not required for the
mandatory score. `DEFERRED` means the specification explicitly reserves the
feature for Phase 2.

## Menus, authentication, and profiles

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| M-01 | Global `menu enter`, `menu show current`, and `menu exit` forms | PASS | `Command`, `MenuController`, `MenuGraph`, `MenuExit` | `MenuTransitionTest`, `Phase1CommandContractTest` |
| M-02 | Registration with username, password confirmation, nickname, email, and gender validation | PASS | `RegisterMenuController`, `User.Validation` | `AuthFlowTest` |
| M-03 | Security-question selection before account creation | PASS | `SecurityQuestionCatalog`, `RegisterMenuController` | `AuthFlowTest` |
| M-04 | Login, distinct unknown-user/wrong-password errors, optional stay-logged-in | PASS | `LoginMenuController`, `UserService` | `AuthFlowTest`, `UserPersistenceTest` |
| M-05 | Password recovery by username/email, stored question, answer, and new password | PASS | `LoginMenuController`, `PasswordService` | `AuthFlowTest` |
| M-06 | Persist all registered users and restore compatible sessions after restart | PASS | `JsonUserRepository`, `SaveFile`, `SaveFileMigrator`, `AtomicFileWriter` | `UserPersistenceTest` |
| M-07 | Main/Game/settings/network/news/profile/collection routes | PASS | `MenuGraph`, `MenuRouter` | `MenuTransitionTest` |
| M-08 | Logout persists and returns to registration | PASS | `MainMenuController.logout` | `MenuCommandsTest` |
| M-09 | Difficulty 1-5, default 3, and all five required scaling axes | PASS | `Settings`, `DifficultyScaling` | `MenuCommandsTest` |
| M-10 | News for plant, zombie, level, and minigame unlocks; unread state persists | PASS | `NewsService`, `NewsMenuController` | `MenuCommandsTest`, progression tests |
| M-11 | Profile edits reuse validation and reject unchanged/conflicting values | PASS | `ProfileMenuController` | `MenuCommandsTest` |
| M-12 | Profile info reports identity, games, currencies, completed levels, and high score | PASS | `ProfileMenuController.showInfo` | `MenuCommandsTest` |

## Game menu, collection, and plant selection

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| G-01 | Enter an unlocked chapter with the exact chapter command | PASS | `GameMenuController.enterLatestPlayableLevel`, `GameMenuView.openChapter` | `MenuCommandsTest`, `Phase1CommandFlowTest` |
| G-02 | Chapter-only syntax resolves deterministically without inventing a level command | PASS | Highest unlocked non-boss level 1-3 is selected | `MenuCommandsTest.chapterOnlyCommandResolvesTheLatestUnlockedNonBossLevel` |
| G-03 | Wallet, travel-log, greenhouse, leaderboard, and cheat commands | PASS | `GameMenuController`, `Command` | `MenuCommandsTest`, `Phase1CommandContractTest` |
| G-04 | Collection lists owned/all plants and seen/all zombies and prints details | PASS | `CollectionMenuController`, canonical registries | `PlantCollectionIntegrationTest`, `ZombieRegistryTest` |
| G-05 | Purchase mandatory plants for 2000 coins; bonus rows are also enabled; reject insufficient balance | PASS | `CollectionMenuController.purchasePlant` | `PlantCollectionIntegrationTest` |
| G-06 | Plant upgrades consume increasing coins/seed packets and stop at canonical maximum | PASS | `PlantCard`, `CollectionMenuController.upgradePlant` | `PlantCollectionIntegrationTest`, `PlantRegistryTest` |
| G-07 | New accounts unlock Sunflower, Peashooter, and Wall-nut | PASS | `RegisterMenuController.applyStarterUnlocks` | `AuthFlowTest`, `Phase1CommandFlowTest` |
| G-08 | Exact six pre-game plant-selection commands | PASS | `Command`, `PlantSelectionView`, `PlantSelectionController` | `PlantSelectionTest`, `Phase1CommandContractTest` |
| G-09 | Default eight slots plus per-level restrictions, forced plants, and bypass | PASS | `LevelSelectionRules` | `PlantSelectionTest`, `SpecialLevelRulesTest` |
| G-10 | Auto-start with all available plants when their count is below slot capacity | PASS | `PlantSelectionController.beginForPlayer` | `PlantSelectionTest`, `Phase1CommandFlowTest` |
| G-11 | Gameplay rejects unselected plants and enforces recharge/cooldown | PASS | `BoardController`, `CooldownSystem` | `BoardMechanicsTest` |
| G-12 | Invalid selection/gameplay operations are atomic | PASS | Validation precedes mutation in controllers | `PlantSelectionTest`, `BoardMechanicsTest`, `Phase1CommandFlowTest` |

## Shared gameplay mechanics and exact result text

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| S-01 | Ten ticks per second and one deterministic simulation clock | PASS | `Simulation`, `TickContext` | `SunSimulationTest` |
| S-02 | Exact `advance time -t <count> ticks` command and positive-count validation | PASS | `Command.ADVANCE_TIME`, `GameplayController` | `SunSimulationTest`, `Phase1CommandContractTest` |
| S-03 | Producer sun blocks its next cycle until collected and emits exact output | PASS | `SunProducer.System` | `SunSimulationTest` |
| S-04 | Sky-sun schedule, 5-second fall, 80/15/5 bands, values, and exact outputs | PASS | `FallingSunSystem`, `SunDropSchedule`, `SunCollector` | `SunSimulationTest` |
| S-05 | Radioactive falling-sun explosion and ground conversion | PASS | `SunCollector`, `FallingSunSystem` | `SunSimulationTest` |
| S-06 | 5x9 board, terrain, stacking, water, graves, ice, and slippery tiles | PASS | `Board`, `Tile`, `AdventureInitializer` | `BoardMechanicsTest`, `AdventureChapterRulesTest` |
| S-07 | Planting cost/cooldown, pluck, plant food, and exact destruction/drop messages | PASS | `BoardController`, `RewardService`, `ZombieCombatSystem` | `BoardMechanicsTest` |
| S-08 | Wave cost composition, +25% growth, final-wave doubling, and 75% trigger | PASS | `WaveComposer`, `WaveSystem` | `ZombieWaveTest` |
| S-09 | Exact wave, spawn, zombie-death, mower, loss, and victory strings | PASS | `WaveSystem`, `ZombieCombatSystem`, `GameplayController` | `ZombieWaveTest`, `BoardMechanicsTest` |
| S-10 | One mower per row, first breach clears row, second breach loses | PASS | `ZombieCombatSystem` | `ZombieWaveTest` |
| S-11 | Nuke kills all non-boss zombies without normal rewards | PASS | `GameplayController.releaseNuke` | `ZombieWaveTest` |
| S-12 | Map, plant status, tile status, and zombie info expose required state | PASS | `BoardController`, `GameplayController` | `BoardMechanicsTest`, `ZombieCommandTest` |
| S-13 | Parser-level invalid commands and semantic rejections do not mutate state | PASS | Views route only matched commands; controllers validate first | `Phase1CommandFlowTest`, rejection tests across controllers |

## Canonical plants and zombies

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| C-01 | Load all 69 canonical plant rows and classify 12 blue/bonus rows | PASS | `CsvPlantRepository`, `bonus-plants.txt` | `PlantRegistryTest` |
| C-02 | Factory/strategy coverage for all 69 canonical plant rows | PASS | `PlantBehaviorFactory`, `BonusPlantBehavior`, `PlantRegistry` | `PlantRegistryTest.everyCanonicalRowHasAFactoryStrategy`, `BonusPlantBehaviorTest` |
| C-03 | Plant tags, intervals, recharge, upgrades, projectiles, effects, supports, and specials | PASS | Plant strategies and shared projectile/effect components | `PlantRegistryTest`, `PlantCombatTest`, `PlantBoardSupportTest` |
| C-04 | Load all 28 canonical zombie rows and classify 6 blue/bonus rows | PASS | `CsvZombieRepository`, `bonus-zombies.txt` | `ZombieRegistryTest` |
| C-05 | Factory/component coverage for all 28 canonical zombie rows | PASS | `ZombieBehaviorFactory`, `ZombieSpecialSystem`, `ZombieRegistry` | `ZombieRegistryTest.factoryCoversEveryCanonicalRow`, `BonusZombieBehaviorTest` |
| C-06 | Armor, effects, common behaviors, and chapter-specific behaviors | PASS | Zombie movement/attack/ability components | `ZombieArmorAndEffectsTest`, `ZombieCommonBehaviorTest`, `ZombieChapterBehaviorTest` |

## Adventure, chapters, and special levels

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| A-01 | Four canonical chapters, four rows each | PASS | `adventure-chapters.csv`, `adventure-levels.csv`, `AdventureCatalog` | `AdventureCatalogTest` |
| A-02 | Level 1 normal, levels 2-3 special, level 4 boss in every chapter | PASS | `AdventureConfigLoader` | `AdventureCatalogTest` |
| A-03 | All eight special-level types appear exactly once | PASS | Eight unique CSV values | `AdventureCatalogTest`, `SpecialLevelRulesTest` |
| A-04 | Ancient Egypt graves/tornado behavior | PASS | `AdventureInitializer`, `AdventureRuleSystem` | `AdventureChapterRulesTest` |
| A-05 | Frostbite freeze levels, ice health, fire thawing, frozen zombies, slippery terrain | PASS | Adventure and zombie systems | `AdventureChapterRulesTest`, zombie tests |
| A-06 | Big Wave Beach water schedule, water planting, low-tide behavior | PASS | `AdventureRuleSystem`, board terrain rules | `AdventureChapterRulesTest`, `BoardMechanicsTest` |
| A-07 | Dark Ages night sun rules, graves with rewards, necromancy | PASS | `AdventureRuleSystem` | `AdventureChapterRulesTest` |
| A-08 | Completion persists, unlocks next level/chapter, and emits news | PASS | `ChapterCatalog.completeLevel`, `GameConclusionService` | `AdventureProgressionTest` |
| A-09 | Boss rows remain visible but gameplay is deferred to Phase 2 | DEFERRED | `BossLevel`, entry guards | `SpecialLevelRulesTest`, `AdventureProgressionTest` |

## Greenhouse and shop

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| H-01 | Fixed 4x5 greenhouse; first row unlocked; purchased slots persist to 20 | PASS | `GreenHouse`, `GreenhouseSlot` | `GreenhouseLayoutTest`, `GreenhousePersistenceTest` |
| H-02 | Exact show/plant/collect/grow commands | PASS | `Command`, `GreenhouseView` | `GreenhouseCommandTest`, `Phase1CommandContractTest` |
| H-03 | Injected 50/50 Marigold/eligible-plant roll | PASS | `GreenhouseController`, `RandomSource` | `GreenhouseControllerTest` |
| H-04 | 2h/8h system-clock growth, restart continuation, rounded acceleration cost | PASS | Injected `Clock` through `UserService` | `GreenhouseControllerTest`, `GreenhousePersistenceTest` |
| H-05 | Marigold 500 coins and one stored per-plant boost | PASS | `GreenhouseBoostService` | `GreenhouseControllerTest`, `PlantSelectionTest` |
| H-06 | Permanent shop catalog and exact buy form | PASS | `ShopItem`, `ShopController`, `ShopView` | `ShopCommandTest`, `ShopControllerTest` |
| H-07 | Daily offer refreshes at injected-clock midnight and is purchasable once | PASS | `DailyShopState`, `ShopController` | `DailyOfferPersistenceTest` |
| H-08 | Purchases are atomic and enforce balance/capacity/unlock requirements | PASS | `ShopController` | `ShopControllerTest` |

## Quests, Travel Log, and leaderboard

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| Q-01 | Canonical quest source located and exported without invented IDs/rows | PASS | `phase1/assets/Data/quests.csv`, `quests-README.md` | `QuestCatalogTest` |
| Q-02 | All 20 workbook rows match exactly and retain stable source order | PASS | `QuestCatalog` | `QuestCatalogTest.canonicalWorkbookExportLoadsAllTwentyRowsInStableOrder` |
| Q-03 | Event-driven progress without quest conditionals in every gameplay class | PASS | `DomainEventBus`, `DomainEventPublisher`, `QuestService` | `QuestServiceTest` |
| Q-04 | Currency, unlock, and inventory rewards; idempotent claim and persisted state/counters | PASS | `QuestRewardService`, `QuestProgress`, `User` | `QuestServiceTest` |
| Q-05 | Main/Epic/Daily/Minigame pages, unknown-page rejection, required ordering | PASS | `TravelLogPage`, `TravelMenuController` | `TravelLogTest` |
| Q-06 | Global leaderboard includes every local registered user and all five progress columns | PASS | `LeaderboardService`, `LeaderboardEntry` | `LeaderboardTest` |
| Q-07 | Every column sorts both directions with username-ascending tie break | PASS | `LeaderboardColumn`, comparator chain | `LeaderboardTest` |
| Q-08 | Leaderboard works after restart and includes zero/unset scored-game values | PASS | Profile-backed service | `LeaderboardTest` |
| Q-09 | Canonical Game route plus Main compatibility route for leaderboard/scored game | PASS | `GameMenuView`, `CommonMenuView`, `MenuGraph` | `LeaderboardTest`, `ScoredGameTest` |

## Minigames

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| N-01 | Travel Log Minigame page, unlock/completion persistence, unlock news | PASS | `MiniGameService`, `MiniGameProgress`, `NewsService` | `MiniGameLifecycleTest`, `MiniGameTravelLogTest` |
| N-02 | Strategy-owned rule overrides and command-extension point | PASS | `MiniGame`, `MiniGameCommandExtension`, per-game state/rules | `MiniGameLifecycleTest` |
| N-03 | Shared board/tick/zombie/result infrastructure where applicable | PASS | `MiniGameSession` owns shared `Simulation` | lifecycle and individual minigame tests |
| N-04 | Vase Breaker: three harder levels, all vase types, packet expiry, standard defense | PASS | `VaseBreaker`, `VaseBreakerState`, `VaseBreakerLevelRules` | `VaseBreakerTest` |
| N-05 | Wall-nut Bowling: three harder levels, conveyor/red line, three described nuts | PASS | `BowlingWallnut`, `WallNutBowlingState` | `WallNutBowlingTest` |
| N-06 | I, Zombie: three harder levels, five rosters, 11 distinct types, producers/brains/dead-end loss | PASS | `IZombie`, `IZombieState`, `IZombieLevelRules` | `IZombieTest` |
| N-07 | Completed-minigame statistic increments once after first valid three-level clear | PASS | `MiniGameService.finish` | `MiniGameLifecycleTest` |
| N-08 | Every mandatory minigame has exactly three explicitly harder configurations | PASS | `MiniGameCatalog.phaseOneDefaults` | `MiniGameCatalogTest` |

## Implemented bonus scope

| ID | Requirement | Status | Code evidence | Automated evidence |
|---|---|---|---|---|
| B-01 | SHA-256 password storage and compatible plaintext migration | BONUS | `Sha256PasswordService`, `SaveFileMigrator` | `AuthFlowTest`, `PasswordMigrationTest` |
| B-02 | Daily scored game with five scoring patterns and persisted high score | BONUS | `ScoredGameService`, `DailyZombieSequenceGenerator`, `ScoredGameSession` | `ScoredGameTest` |
| B-03 | Beghouled, three levels | BONUS | `Beghouled`, `BeghouledState` | `BeghouledTest`, `AdvancedMiniGameProgressionTest` |
| B-04 | Zombotany, three levels and four required traits | BONUS | `Zombotany`, `ZombotanyState` | `ZombotanyTest`, `AdvancedMiniGameProgressionTest` |
| B-05 | All 12 canonical blue-row plants are purchasable/selectable and have runtime behavior | BONUS | `BonusPlantBehavior`, `PlantBehaviorFactory`, `BoardController` | `BonusPlantBehaviorTest`, `PlantCollectionIntegrationTest`, `PlantRegistryTest` |
| B-06 | All 6 canonical blue-row zombies have factory, chapter, and runtime behavior support | BONUS | `ZombieBehaviorFactory`, `BonusZombieAbilities`, `ZombieSpecialSystem` | `BonusZombieBehaviorTest`, `ZombieCommandTest`, `ZombieRegistryTest` |

## Quality, determinism, and architecture

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| V-01 | Exact documented command forms are registered | PASS | `Phase1CommandContractTest`: every prescribed form passes; every `Command` constant is referenced by a view |
| V-02 | Required exact strings are carried through `Result`/simulation message lists | PASS | Exact-string assertions in `BoardMechanicsTest`, `SunSimulationTest`, and `ZombieWaveTest` |
| V-03 | Tick-time versus injected wall clock is separated | PASS | Simulation uses ticks; greenhouse/shop/persistence/scored game use injected `Clock` |
| V-04 | RNG tests are deterministic | PASS | `RandomSource` injection and fixed seeds/roll fakes throughout tests; daily scored seed is date/rules-version based |
| V-05 | Persistent profile state survives restart | PASS | Persistence tests for accounts, settings, collection, news, greenhouse, shop, quests, minigames, progression, and high score |
| V-06 | UML matches final controller/service/model/persistence/strategy architecture | PASS | `UML.puml`, `docs/uml/main-class-diagram.puml`, `docs/uml/final.png` |
| V-07 | Checkstyle and PMD tasks are configured in the normal Gradle build | PASS | `build.gradle`, `config/checkstyle`, `config/pmd` |
