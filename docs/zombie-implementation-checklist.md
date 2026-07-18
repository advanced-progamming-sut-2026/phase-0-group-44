# Zombie implementation checklist

Canonical runtime data is loaded from `phase1/assets/Data/zombies.csv`. The separate `bonus-zombies.txt` file preserves the cyan/blue row classification transcribed from the supplied PDF. Factory registration covers every canonical row while preserving mandatory/bonus classification.

## Coverage summary

- 28 classified document/repository rows
- 22 mandatory rows implemented
- 6 blue/bonus rows implemented

| # | Canonical type / name | Status | Chapter | Base HP | Speed | Eat DPS | Wave cost | Armor | Required behavior | Implementation | Deterministic tests |
|---:|---|---|---|---:|---:|---:|---:|---|---|---|---|
| 1 | `NORMAL` / Normal Zombie | MANDATORY — implemented | COMMON | 190 | 0.185 | 100 | 100 | — | Moves left, eats blocking plant. | NormalZombieMovement + NormalZombieAttack | ZombieArmorAndEffectsTest; ZombieCommonBehaviorTest |
| 2 | `CONEHEAD` / Conehead | MANDATORY — implemented | COMMON | 190 | 0.185 | 100 | 200 | cone:370:false | Normal behavior; cone absorbs 370 normal damage. | Normal components + cone armor | ZombieRegistryTest |
| 3 | `BUCKETHEAD` / Buckethead | MANDATORY — implemented | COMMON | 190 | 0.185 | 100 | 400 | bucket:1100:true | Normal behavior; bucket absorbs 1100 and is magnetic. | Normal components + magnetic bucket armor | ZombieArmorAndEffectsTest; ZombieCommandTest |
| 4 | `KNIGHT` / Knight | MANDATORY — implemented | COMMON | 190 | 0.185 | 100 | 550 | helmet:1600:true|shoulderArmor:1600:false | Helmet then shoulder armor, 1600 each; helmet magnetic. | Normal components + helmet/shoulder armor | ZombieArmorAndEffectsTest |
| 5 | `BLOCKHEAD` / Blockhead | MANDATORY — implemented | COMMON | 190 | 0.185 | 100 | 700 | block:2200:false | Block absorbs 2200 normal damage. | Normal components + block armor | ZombieRegistryTest |
| 6 | `GARGANTUAR` / Gargantuar | MANDATORY — implemented | COMMON | 3600 | 0.240 | 100 | 1500 | — | Slow, one-hit plant attack, throws one Imp at half base HP into column 3. | GargantuarAbility + InstantDestroyZombieAttack | ZombieCommonBehaviorTest |
| 7 | `IMP` / Imp | MANDATORY — implemented | COMMON | 190 | 0.220 | 100 | 100 | — | Moves and eats 1.5× faster than a normal zombie. | fast NormalZombieMovement/Attack multipliers | ZombieCommonBehaviorTest |
| 8 | `ALL_STAR` / All-Star | MANDATORY — implemented | COMMON | 1100 | 0.160 | 100 | 1000 | — | 5× charge, instant first collision, then 0.35× slow state. | AllStarAbility + AllStarZombieAttack | ZombieCommonBehaviorTest |
| 9 | `ARCADE_ZOMBIE` / Arcade Zombie | BONUS — implemented | COMMON | 490 | 0.190 | 100 | 600 | arcadeMachine:1100:false | Pushes arcade machine and kills on collision. | ArmoredRamZombieAttack(arcadeMachine) | BonusZombieBehaviorTest; ZombieRegistryTest factory coverage |
| 10 | `PARASOL_ZOMBIE` / Parasol Zombie | MANDATORY — implemented | COMMON | 350 | 0.250 | 100 | 200 | — | Umbrella rejects lobbed projectiles. | ParasolAbility | ZombieCommonBehaviorTest |
| 11 | `TURQUOISE_ZOMBIE` / Turquoise Zombie | MANDATORY — implemented | COMMON | 250 | 0.185 | 100 | 500 | — | Within 4 tiles steals 25 sun/s for 5s, lasers four tiles, refunds half on death. | TurquoiseAbility | ZombieCommonBehaviorTest |
| 12 | `PROSPECTOR` / Prospector | MANDATORY — implemented | COMMON | 190 | 0.160 | 100 | 200 | — | After 10s jumps to left row end and reverses; ice extinguishes dynamite. | ProspectorAbility | ZombieCommonBehaviorTest |
| 13 | `PIANIST` / Pianist | MANDATORY — implemented | COMMON | 840 | 0.120 | 4000 | 450 | piano:1100:false | Piano collision destroys plants; every 4s moves another zombie to adjacent row. | PianistAbility + InstantDestroyZombieAttack | ZombieCommonBehaviorTest |
| 14 | `NEWSPAPER_ZOMBIE` / Newspaper Zombie | MANDATORY — implemented | COMMON | 460 | 0.220 | 200 | 700 | newspaper:190:false | Paper has normal-zombie HP; paper break multiplies movement/eating by 2.5. | NewspaperAbility + newspaper armor | ZombieCommonBehaviorTest |
| 15 | `BARREL_ROLLER` / Barrel Roller | MANDATORY — implemented | COMMON | 190 | 0.185 | 100 | 600 | barrel:1100:false | Barrel blocks shots; break releases two Imps; intact barrel remains after base-health death. | BarrelRollerAbility + barrel armor/obstacle | ZombieCommonBehaviorTest |
| 16 | `RA_ZOMBIE` / Ra Zombie | MANDATORY — implemented | ANCIENT_EGYPT | 190 | 0.200 | 100 | 100 | — | Steals on-ground suns and returns all on death. | RaAbility | ZombieChapterBehaviorTest |
| 17 | `EXPLORER` / Explorer | MANDATORY — implemented | ANCIENT_EGYPT | 250 | 0.250 | 100 | 250 | — | Lit torch destroys plants <1 tile ahead; ice extinguishes and fire relights. | ExplorerAbility + ExplorerZombieAttack | ZombieChapterBehaviorTest |
| 18 | `TOMBRAISER` / Tombraiser | MANDATORY — implemented | ANCIENT_EGYPT | 380 | 0.185 | 100 | 300 | — | Every 8s creates two graves on random valid empty tiles. | TombraiserAbility | ZombieChapterBehaviorTest |
| 19 | `DODO_RIDER` / Dodo Rider | MANDATORY — implemented | FROSTBITE_CAVES | 490 | 0.300 | 100 | 600 | — | Flies over defenders/movers/traps but not Tall-nut; Frostbite freeze immune. | DodoZombieMovement + DodoZombieAttack | ZombieChapterBehaviorTest |
| 20 | `HUNTER` / Hunter | MANDATORY — implemented | FROSTBITE_CAVES | 700 | 0.120 | 100 | 500 | — | Every 3s hits nearest row plant with ice; third hit freezes and blocks direct shots. | HunterAbility | ZombieChapterBehaviorTest; ZombieCommandTest |
| 21 | `TROGLOBITE` / Troglobite | BONUS — implemented | FROSTBITE_CAVES | 470 | 0.185 | 100 | 600 | groundIce:1100:false | Pushes ground ice. | ArmoredRamZombieAttack(groundIce) | BonusZombieBehaviorTest; ZombieRegistryTest factory coverage |
| 22 | `FISHERMAN` / Fisherman | BONUS — implemented | BIG_WAVE_BEACH | 1000 | 0.185 | 100 | 700 | — | Hooks plants from right edge. | StationaryZombieMovement + FishermanAbility | BonusZombieBehaviorTest; ZombieRegistryTest factory coverage |
| 23 | `SNORKEL` / Snorkel | MANDATORY — implemented | BIG_WAVE_BEACH | 350 | 0.185 | 100 | 200 | — | Submerges on water; direct shots blocked while submerged; surfaces to eat. | SnorkelAbility | ZombieChapterBehaviorTest |
| 24 | `OCTOPUS_ZOMBIE` / Octopus Zombie | MANDATORY — implemented | BIG_WAVE_BEACH | 910 | 0.120 | 100 | 900 | — | Every 5s disables nearest row plant and creates destructible direct-shot blocker. | OctopusAbility | ZombieChapterBehaviorTest |
| 25 | `JESTER` / Jester / Juggler | BONUS — implemented | DARK_AGES | 420 | 0.200 | 100 | 450 | — | Reflects direct shots while spinning. | JesterAbility + normal movement/attack | BonusZombieBehaviorTest; ZombieRegistryTest factory coverage |
| 26 | `WIZARD` / Wizard | BONUS — implemented | DARK_AGES | 490 | 0.120 | 100 | 800 | — | Transforms plants until Wizard death. | WizardZombieAttack + WizardAbility | BonusZombieBehaviorTest; ZombieRegistryTest factory coverage |
| 27 | `KING` / King | BONUS — implemented | DARK_AGES | 1000 | 0.185 | 100 | 750 | — | Upgrades simple zombies to Knights. | StationaryZombieMovement + KingAbility | BonusZombieBehaviorTest; ZombieRegistryTest factory coverage |
| 28 | `DRAGON_IMP` / Dragon Imp | MANDATORY — implemented | DARK_AGES | 190 | 0.185 | 100 | 150 | — | Imp behavior and fire-damage immunity. | fast Imp components + fire immunity | ZombieArmorAndEffectsTest |

## Shared architecture

- `ZombieRegistry` is the canonical lookup used by collection details, waves, spawn validation, factories, and commands.
- `ZombieBehaviorFactory` composes reusable movement, attack, armor/effect, and special-ability components.
- `Zombie` and `ZombieInstance` both preserve independent armor parts and timed effects; poison/true damage bypass armor.
- `ZombieSpecialSystem` mirrors canonical mandatory and bonus state transitions in the command-driven ten-ticks-per-second simulation.
- `zombies info` reports type, decimal position, base health, each intact armor part, and timed effects.
- `cheat spawn-zombie -t <zombie-type> -l <x, y>` accepts implemented canonical rows and rejects unknown, invalid-tile, and wrong-chapter spawns.

## Source normalization notes

- The repository JSON aliases `ZombieLostCityJane` and `ZombieCrystalSkull` correspond to the document names Parasol Zombie and Turquoise Zombie.
- Barrel Roller is specified by the document but absent from the legacy JSON, so its row is normalized from the document using Buckethead-equivalent barrel health.
- The document gives no numeric rage multiplier for Newspaper; the implementation uses 2.5× consistently for movement and eating and centralizes it in runtime state.
- Bonus rows retain their canonical classification, have factory strategies, and may spawn only in COMMON or their configured chapter.
- Legacy enum-only placeholders `POLE_VAULTING`, `SCREEN_DOOR`, `DANCING`, and `BALLOON` have no document/JSON row; they are retained only for old-save compatibility and are not classified registry rows or spawnable zombies.
