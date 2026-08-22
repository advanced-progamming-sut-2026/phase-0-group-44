package model.sim.wave;

import model.GameEngine;
import model.inGame.GameOutcome;
import model.enums.ZombieType;
import model.inGame.zombie.Zombie;
import model.level.SpecialLevelType;
import model.level.TimedWarObjective;
import model.sim.zombie.ZombieSpec;
import model.sim.zombie.ZombieSpecSource;
import util.RandomSource;
import util.RandomSourceAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveSystem {

    public static final double NEXT_WAVE_THRESHOLD = 0.75;
    private static final double TIMED_WAR_NEXT_WAVE_THRESHOLD = 0.50;
    private static final double GLOWING_ZOMBIE_CHANCE = 0.05;

    private final WaveConfig config;
    private final ZombieSpecSource specSource;

    private int currentWave;
    private int currentWaveInitialHealth;
    private boolean started;
    private boolean allWavesSpawned;
    /** Ensure the level visibly demonstrates the Plant Food carrier mechanic at least once. */
    private boolean guaranteedGlowingSpawned;
    /** Zombies spawned by the currently active wave only. */
    private List<Zombie> currentWaveZombies = List.of();

    public WaveSystem(WaveConfig config, ZombieSpecSource specSource) {
        this.config = config;
        this.specSource = specSource;
    }

    public void tick(GameEngine engine) {
        if (!engine.isRunning() || !engine.areWavesStarted()) {
            return;
        }
        if (!started) {
            startWave(engine, 1);
            started = true;
            return;
        }
        if (!allWavesSpawned && shouldStartNextWave(engine)) {
            startWave(engine, currentWave + 1);
            return;
        }
        if (allWavesSpawned && !engine.hasHostileZombiesRemaining()) {
            declareWin(engine);
        }
    }

    private boolean shouldStartNextWave(GameEngine engine) {
        /*
         * Chapter setup can place zombies before wave 1 (for example the
         * frozen zombie in Frostbite Caves). Those zombies are not part of
         * the wave and must not hold back wave progression.
         */
        int currentHealth = aggregateHealth(currentWaveZombies);
        int lost = currentWaveInitialHealth - currentHealth;
        double threshold = isZombieKillTimedWar(engine)
                ? TIMED_WAR_NEXT_WAVE_THRESHOLD
                : NEXT_WAVE_THRESHOLD;
        return lost >= (int) Math.ceil(currentWaveInitialHealth * threshold);
    }

    private void startWave(GameEngine engine, int waveNumber) {
        currentWave = waveNumber;
        engine.setCurrentWave(waveNumber);

        boolean finalWave = config.isFinalWave(waveNumber);
        if (finalWave) {
            engine.recordEvent("The final wave has come.");
        }
        engine.recordEvent("Wave " + waveNumber + " started.");

        currentWaveZombies = spawnWave(engine, waveNumber);
        currentWaveInitialHealth = aggregateHealth(currentWaveZombies);

        if (finalWave) {
            allWavesSpawned = true;
        }
    }

    /**
     * The general cost formula (×1.25 growth) has no visibility into which
     * zombies are actually available for this level's chapter, so its target
     * is not guaranteed to be composable even when it is a multiple of 50 (some
     * multiples of 50, like 150, still cannot be formed from every available
     * cost set). Search outward in steps of 50 — the shared unit every zombie's
     * wave cost is defined in — for the closest cost that is composable, so a
     * plain formula mismatch never crashes the game.
     */
    private static int resolveComposableCost(int targetCost, List<ZombieSpec> availableSpecs) {
        if (WaveComposer.isComposable(targetCost, availableSpecs)) {
            return targetCost;
        }

        int maxSearchDistance = 5000;

        for (int distance = 1; distance <= maxSearchDistance; distance++) {
            int higher = targetCost + distance;
            if (WaveComposer.isComposable(higher, availableSpecs)) {
                return higher;
            }
            int lower = targetCost - distance;
            if (lower >= 0 && WaveComposer.isComposable(lower, availableSpecs)) {
                return lower;
            }
        }

        return targetCost;
    }

    private List<Zombie> spawnWave(GameEngine engine, int waveNumber) {
        int targetCost = config.costOfWave(waveNumber);
        Random random = engine.getRandom();
        RandomSource randomSource = RandomSourceAdapter.wrap(random); // ← اصلاح شد

        List<ZombieSpec> availableSpecs = specSource.availableSpecs();
        int resolvedCost = resolveComposableCost(targetCost, availableSpecs);
        List<ZombieSpec> composition =
                WaveComposer.compose(resolvedCost, availableSpecs, randomSource);

        if (composition == null) {
            throw new IllegalStateException(
                    "Wave " + waveNumber + " cost " + targetCost
                            + " cannot be composed from the available zombies.");
        }

        if (isZombieKillTimedWar(engine)) {
            int target = engine.getAdventureState().getConfig().getTimedWarTarget();
            int waves = Math.max(1, config.getWaveCount());
            int minimumPerWave = Math.max(1, (int) Math.ceil(target / (double) waves));
            ZombieSpec cheapest = cheapestSpec(availableSpecs);
            while (cheapest != null && composition.size() < minimumPerWave) {
                composition.add(cheapest);
            }
        }

        int rows = engine.getGameMap().getRows();
        int spawnColumn = engine.getGameMap().getColumns();
        boolean tornadoes = config.isFinalWave(waveNumber)
                && engine.getAdventureState() != null
                && engine.getAdventureState().getConfig().getChapterRules().hasFinalWaveTornadoes();

        List<Zombie> spawned = new ArrayList<>(composition.size());
        for (ZombieSpec spec : composition) {
            int lane = random.nextInt(rows);
            double spawnX = spawnColumn;
            if (tornadoes && random.nextInt(2) == 0) {
                int advance = 1 + random.nextInt(4);
                spawnX = Math.max(0.5, spawnColumn - advance);
                engine.recordEvent("A tornado carried " + spec.getName() + " "
                        + advance + " columns into lane " + lane + ".");
            }
            Zombie zombie = engine.spawnZombie(spec.getType(), lane, spawnX);
            boolean glowing = !guaranteedGlowingSpawned
                    || random.nextDouble() < GLOWING_ZOMBIE_CHANCE;
            if (glowing) {
                zombie.putState("GLOWING", true);
                guaranteedGlowingSpawned = true;
                engine.recordEvent("A glowing zombie entered lane " + lane + ".");
            }
            spawned.add(zombie);
            engine.recordEvent("Zombie " + spec.getName() + " spawned at wave " + waveNumber
                    + " in lane " + lane + " which costed " + spec.getWaveCost() + ".");
        }
        return spawned;
    }

    private boolean isZombieKillTimedWar(GameEngine engine) {
        return engine.getAdventureState() != null
                && engine.getAdventureState().getConfig().getSpecialType() == SpecialLevelType.TIMED_WAR
                && engine.getAdventureState().getConfig().getTimedWarObjective() == TimedWarObjective.ZOMBIE_KILLS;
    }

    private ZombieSpec cheapestSpec(List<ZombieSpec> specs) {
        ZombieSpec cheapest = null;
        for (ZombieSpec spec : specs) {
            if (spec == null || spec.getWaveCost() <= 0) {
                continue;
            }
            if (cheapest == null || spec.getWaveCost() < cheapest.getWaveCost()) {
                cheapest = spec;
            }
        }
        return cheapest;
    }

    private int aggregateHealth(List<Zombie> zombies) {
        int total = 0;
        for (Zombie zombie : zombies) {
            total += Math.max(0, zombie.getHealth());
        }
        return total;
    }

    private void declareWin(GameEngine engine) {
        if (engine.getAdventureState() != null
                && engine.getAdventureState().getConfig().getSpecialType() == SpecialLevelType.TIMED_WAR) {
            return;
        }
        engine.setOutcome(GameOutcome.WON);
        engine.recordEvent("Dear humanz, zis is not done yet; "
                + "we will come back to eat your brainz, humanz.");
    }

    public int getCurrentWave() {
        return currentWave;
    }
}